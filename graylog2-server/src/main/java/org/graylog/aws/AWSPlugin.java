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
package org.graylog.aws;

import org.graylog2.Configuration;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

public class AWSPlugin implements Plugin {
    @Inject
    private Configuration configuration;

    @Override
    public Collection<PluginModule> modules() {
        return Collections.singleton(new AWSModule(configuration));
    }

    @Override
    public PluginMetaData metadata() {
        return new AWSPluginMetadata();
    }
}
