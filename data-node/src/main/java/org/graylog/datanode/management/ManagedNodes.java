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
package org.graylog.datanode.management;

import org.graylog.datanode.DataNodeRunner;
import org.graylog.datanode.process.OpensearchProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Scope("singleton")
public class ManagedNodes {

    private final Set<OpensearchProcess> processes = new LinkedHashSet<>();

    @Value("${opensearch.version}")
    private String opensearchVersion;
    @Value("${opensearch.location}")
    private String openseachLocation;

    @Autowired
    private DataNodeRunner dataNodeRunner;

    @EventListener(ApplicationReadyEvent.class)
    public void startOpensearchProcesses() {

        // TODO: obtain configuration from outside, one for each node

        final LinkedHashMap<String, String> config = new LinkedHashMap<>();
        config.put("discovery.type", "single-node");
        config.put("plugins.security.ssl.http.enabled", "false");
        config.put("plugins.security.disabled", "true");

        this.processes.add(dataNodeRunner.start(Path.of(openseachLocation), opensearchVersion, config));
    }

    public Set<OpensearchProcess> getProcesses() {
        return processes;
    }
}