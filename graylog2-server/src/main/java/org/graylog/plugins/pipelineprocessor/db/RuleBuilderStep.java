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
package org.graylog.plugins.pipelineprocessor.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
public abstract class RuleBuilderStep {

    public static final String FIELD_FUNCTION = "function";
    public static final String FIELD_PARAMETERS = "parameters";
    public static final String FIELD_OUTPUT = "output";

    @JsonProperty(FIELD_FUNCTION)
    public abstract String function();

    @JsonProperty(FIELD_PARAMETERS)
    @Nullable
    public abstract Map<String, Object> parameters();

    @JsonProperty(FIELD_OUTPUT)
    @Nullable
    public abstract String output();

    @JsonCreator
    public static RuleBuilderStep create(@JsonProperty(FIELD_FUNCTION) String function,
                                         @JsonProperty(FIELD_PARAMETERS) @Nullable Map<String, Object> parameters,
                                         @JsonProperty(FIELD_OUTPUT) @Nullable String output) {
        return builder()
                .function(function)
                .parameters(parameters)
                .output(output)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_RuleBuilderStep.Builder();
    }

    public abstract Builder toBuilder();


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder function(String function);

        public abstract Builder parameters(Map<String, Object> parameters);

        public abstract Builder output(String output);

        public abstract RuleBuilderStep build();
    }
}
