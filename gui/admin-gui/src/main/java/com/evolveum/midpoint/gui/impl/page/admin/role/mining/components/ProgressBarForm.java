/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.bar.RoleAnalysisAttributeProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisAttributeProgressBarDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributeAnalysisDto;

import org.apache.wicket.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Represents a form containing multiple progress bars, each visualizing the frequency of certain values.
 * The form displays a title based on the attribute name and the count of values.
 * It iterates through JSON information provided during initialization to create individual progress bars.
 * <p>
 * Example usage:
 * <pre>{@code
 * String jsonInformation = "{ \"attribute1\": [{\"value\": \"value1\", \"frequency\": 50}, {\"value\": \"value2\", \"frequency\": 30}] }";
 * ProgressBarForm progressBarForm = new ProgressBarForm("progressBarForm", jsonInformation);
 * add(progressBarForm);
 * }</pre>
 */
public class ProgressBarForm extends BasePanel<RoleAnalysisAttributeAnalysisDto> {
    private static final String ID_CONTAINER = "container";
    private static final String ID_FORM_TITLE = "progressFormTitle";
    private static final String ID_REPEATING_VIEW = "repeatingProgressBar";
    private Set<String> pathToMark;

    public ProgressBarForm(String id, IModel<RoleAnalysisAttributeAnalysisDto> analysisResultDto, Set<String> pathToMark) {
        super(id, analysisResultDto);
        this.pathToMark = pathToMark;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        IModel<String> labelModel = createStringResource("${displayNameKey}", getModel());
        IconWithLabel titleForm = new IconWithLabel(ID_FORM_TITLE, labelModel) {
            @Override
            protected String getIconCssClass() {
                Class<?> parentType = ProgressBarForm.this.getModelObject().getType();
                if (parentType == null) {
                    return super.getIconCssClass();
                }
                if (UserType.class.equals(parentType)) {
                    return GuiStyleConstants.CLASS_OBJECT_USER_ICON + " fa-sm";
                } else {
                    return GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " fa-sm";
                }
            }

            @Override
            protected Component getSubComponent(String id) {
                List<RoleAnalysisAttributeStatisticsType> attributeStatistics = ProgressBarForm.this.getModelObject().getAttributeStatistics();
                if (attributeStatistics == null) {
                    return super.getSubComponent(id);
                }
                int attributeCount = attributeStatistics.size();
                Label label = new Label(id, attributeCount);
                label.setOutputMarkupId(true);
                label.add(AttributeAppender.append("class", "badge border"));
                return label;
            }
        };

        titleForm.setOutputMarkupId(true);
        container.add(titleForm);

        RepeatingView repeatingProgressBar = new RepeatingView(ID_REPEATING_VIEW);
        repeatingProgressBar.setOutputMarkupId(true);
        container.add(repeatingProgressBar);

        initProgressBars(repeatingProgressBar, container);
    }

    private void initProgressBars(@NotNull RepeatingView repeatingProgressBar, WebMarkupContainer container) {
        //TODO incorrect model unwrapping.
        List<RoleAnalysisAttributeStatisticsType> roleAnalysisAttributeStructures = new ArrayList<>(getModelObject().getAttributeStatistics());
        roleAnalysisAttributeStructures.sort(Comparator.comparingDouble(RoleAnalysisAttributeStatisticsType::getFrequency).reversed());

        var markedAttributes = roleAnalysisAttributeStructures.stream()
                .filter(a -> pathToMark.contains(a.getAttributeValue()))
                .collect(Collectors.toList());
        Collections.reverse(markedAttributes); // reverse to keep original order for marked attributes
        for (var markedAttr : markedAttributes) {
            // keep marked attributes at the top (used to highlight outlier's attributes)
            roleAnalysisAttributeStructures.remove(markedAttr);
            roleAnalysisAttributeStructures.add(0, markedAttr);
        }

        int maxVisibleBars = 5;
        for (RoleAnalysisAttributeStatisticsType item : roleAnalysisAttributeStructures) {
            boolean isMarkedAttribute = pathToMark != null && pathToMark.contains(item.getAttributeValue());
            RoleAnalysisAttributeProgressBar progressBar = createProgressBar(repeatingProgressBar, item.getFrequency(), item, isMarkedAttribute);
            repeatingProgressBar.add(progressBar);
        }

        var bars = repeatingProgressBar.stream().toList();
        for (int i = 0; i < repeatingProgressBar.size(); i++) {
            if (i >= maxVisibleBars) {
                bars.get(i).setVisible(false);
            }
        }

        if (repeatingProgressBar.size() > maxVisibleBars) {
            container.add(createShowAllButton(repeatingProgressBar, container, maxVisibleBars));
        } else {
            WebMarkupContainer showAllButton = new WebMarkupContainer("showAllButton");
            showAllButton.setVisible(false);
            container.add(showAllButton);
        }
    }

    private RoleAnalysisAttributeProgressBar createProgressBar(
            RepeatingView repeatingProgressBar,
            double frequency,
            RoleAnalysisAttributeStatisticsType value,
            boolean isMarkedAttribute) {

        IModel<RoleAnalysisAttributeProgressBarDto> model = () -> {
            String colorClass = null;

            if (isMarkedAttribute) {
                colorClass = "inherit";
            }

            return new RoleAnalysisAttributeProgressBarDto(getPageBase(), frequency, colorClass, value);
        };

        var isUnusual = model.getObject().isUnusual();
        var bar = new RoleAnalysisAttributeProgressBar(repeatingProgressBar.newChildId(), model);
        if (isMarkedAttribute) {
            bar.add(AttributeModifier.append("class", "progress-bar-marked-attribute"));
            if (isUnusual) {
                bar.add(AttributeModifier.append("class", "progress-bar-marked-attribute-unusual"));
            }
        }
        return bar;
    }

    private IconWithLabel createShowAllButton(RepeatingView repeatingProgressBar, WebMarkupContainer container, int maxVisibleBars) {
        return new IconWithLabel("showAllButton", createStringResource("ProgressBarForm.showAllButton")) {
            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                int counter = 0;
                for (Component component : repeatingProgressBar) {
                    counter++;
                    component.setVisible(counter <= maxVisibleBars || !component.isVisible());
                }
                target.add(container);
            }

            @Override
            protected @NotNull Component getSubComponent(String id) {
                Label image = new Label(id);
                image.add(AttributeModifier.replace("class", "fa fa-long-arrow-right"));
                image.add(AttributeModifier.replace("style", "color:rgb(32, 111, 157)"));
                image.setOutputMarkupId(true);
                return image;
            }
        };
    }
}
