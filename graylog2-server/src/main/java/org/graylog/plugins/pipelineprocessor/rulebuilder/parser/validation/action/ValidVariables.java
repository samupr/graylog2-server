/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.action;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.ValidationResult;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.Validator;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ValidVariables implements Validator {

    private final Map<String, RuleFragment> actions;
    private Map<String, Object> variables;

    @Inject
    public ValidVariables(RuleBuilderRegistry ruleBuilderRegistry) {
        this.actions = ruleBuilderRegistry.actions();
        this.variables = new HashMap<>();
    }

    @Override
    public ValidationResult validate(RuleBuilderStep step) {
        final RuleFragment ruleFragment = actions.get(step.function());
        FunctionDescriptor<?> functionDescriptor = ruleFragment.descriptor();
        Map<String, Object> stepParameters = step.parameters();

        ImmutableList<ParameterDescriptor> parameterDescriptors = functionDescriptor.params();
        for(ParameterDescriptor parameterDescriptor: parameterDescriptors) {
            String parameterName = parameterDescriptor.name();
            Object value = stepParameters.get(parameterName);
            Class<?> variableType = getVariableType(value);

            if (!parameterDescriptor.optional() && value == null) {
                return new ValidationResult(step, true, "Function %s missing parameter %s ".formatted(functionDescriptor.name(), parameterName));
            }

            //$ means it is stored in another variable and we need to fetch and verify that type
            if (value instanceof String s && s.startsWith("$")) {
                String substring = s.substring(1);
                Object variable = variables.get(substring);
                if(Objects.isNull(variable)) {
                    return new ValidationResult(step, true, "Function %s missing variable %s ".formatted(functionDescriptor.name(), value));
                }
                variableType = variable.getClass();
            }

            //Check if variable type matches functionDescriptor expectation
            if (value != null &&  variableType != parameterDescriptor.type()) {
                String errorMsg = "Function %s found wrong parameter type %s for parameter %s. Required type %s";
                return new ValidationResult(step, true, errorMsg.formatted(functionDescriptor.name(), variableType, parameterName, parameterDescriptor.type()));
            }
        }

        //Add output to map
        String outputvariable = step.outputvariable();
        if (StringUtils.isNotBlank(outputvariable)) {

            if (functionDescriptor.returnType() == Void.class) {
                return new ValidationResult(step, true, "Function %s is of return typ void. No out put variable allowed ".formatted(functionDescriptor.name()));
            }

            storeVariable(ruleFragment, outputvariable, functionDescriptor.returnType());
        }
        return new ValidationResult(step, false,"");
    }

    private void storeVariable(RuleFragment ruleFragment, String name, Class<?> type) {
        variables.put(name, type);
    }

    private Class<?> getVariableType(Object type) {
        return switch (type) {
            case Double aDouble -> Double.class;
            case Integer integer -> Integer.class;
            case String s -> String.class;
            case Boolean aBoolean -> Boolean.class;
            case null, default -> Object.class;
        };
    }
}
