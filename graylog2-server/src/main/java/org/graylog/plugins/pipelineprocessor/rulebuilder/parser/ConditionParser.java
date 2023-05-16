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
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser;

import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConditionParser {

    public static final String NL = System.lineSeparator();
    protected final Map<String, RuleFragment> conditions;

    @Inject
    public ConditionParser(RuleBuilderRegistry ruleBuilderRegistry) {
        this.conditions = ruleBuilderRegistry.conditions();
    }


    public String generate(List<RuleBuilderStep> ruleConditions) {
        return "  true" + NL +
                ruleConditions.stream()
                        .map(step -> generateCondition(step))
                        .collect(Collectors.joining(NL));
    }

    String generateCondition(RuleBuilderStep step) {
        if (!conditions.containsKey(step.function())) {
            throw new IllegalArgumentException("Function " + step.function() + " not available as condition for rule builder.");
        }

        String syntax = "  && ";
        if (step.negate()) {
            syntax += "! ";
        }

        final RuleFragment ruleFragment = conditions.get(step.function());
        FunctionDescriptor<?> function = ruleFragment.descriptor();

        if (ruleFragment.isFragment()) {
            syntax += ParserUtil.generateForFragment(step, ruleFragment);
        } else {
            syntax += ParserUtil.generateForFunction(step, function);
        }
        return syntax;

    }

}
