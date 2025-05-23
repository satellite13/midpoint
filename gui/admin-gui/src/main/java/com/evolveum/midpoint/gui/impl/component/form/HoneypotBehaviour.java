/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.form;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import com.evolveum.midpoint.gui.impl.factory.wrapper.ItemWrapperFactoryImpl;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import jp.try0.wicket.honeypot.behavior.HoneypotBehavior;
import jp.try0.wicket.honeypot.behavior.HoneypotBehaviorConfig;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.util.string.StringValue;

/**
 * Behaviour have to be added to form. Create fake fields for every input and validate their values.
 */
public class HoneypotBehaviour extends AjaxFormSubmitBehavior {

    private static final Trace LOGGER = TraceManager.getTrace(HoneypotBehaviour.class);

    private static final String HONEYPOT_FIELD_NAME = "hpField-";

    public HoneypotBehaviour() {
        super("submit");
    }

    @Override
    protected void onBind() {
        super.onBind();

        Component component = getComponent();

        if (!(component instanceof Form<?> form)) {
            throw new UnsupportedOperationException("Add HoneypotBehavior to Form.");
        }

        component.add(new HoneypotBehavior(createConfiguration()));

        form.setOutputMarkupId(true);
        form.add(new AbstractFormValidator() {

            @Override
            public void validate(Form<?> form) {

                LOGGER.debug("Starting validation honeypots on form " + form.getId());

                IRequestParameters postParams = form.getRequest().getPostParameters();

                boolean findSomeParameter = false;

                for (String parameterName : postParams.getParameterNames()) {
                    if (parameterName.startsWith(HONEYPOT_FIELD_NAME)) {
                        LOGGER.debug("Validating parameter with name " + parameterName);
                        findSomeParameter = true;
                        StringValue paramValue = postParams.getParameterValue(parameterName);
                        if (!paramValue.isEmpty()) {
                            LOGGER.error("Parameter " + parameterName + " isn't empty, but it contains " + paramValue);
                            onError(form);
                            return;
                        }
                    }
                }

                if (!findSomeParameter) {
                    LOGGER.error("Whole form " + form.getId() + " doesn't contain any honeypot field.");
                    onError(form);
                    return;
                }

                LOGGER.debug("Finishing validation honeypots on form " + form.getId());

            }

            @Override
            public FormComponent<?>[] getDependentFormComponents() {
                return null;
            }
        });
    }

    private HoneypotBehaviorConfig createConfiguration() {
        HoneypotBehaviorConfig config = new HoneypotBehaviorConfig();
        config.setDelay(2000);
        config.setDetectHumanActivity(true);
        return config;
    }

    protected void onError(Form<?> form) {
        form.error(LocalizationUtil.translate("HoneypotBehavior.error"));
    }

    @Override
    protected void onEvent(AjaxRequestTarget target) {

    }
}
