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
package org.graylog.storage.opensearch2;

import com.google.common.collect.Sets;
import org.graylog.shaded.opensearch2.org.opensearch.client.Node;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.sniff.NodesSniffer;
import org.graylog.shaded.opensearch2.org.opensearch.client.sniff.OpenSearchNodesSniffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NodeListSniffer implements NodesSniffer {
    private static final Logger LOG = LoggerFactory.getLogger(NodeListSniffer.class);
    private final NodesSniffer nodesSniffer;
    private static final Set<String> savedNodes = ConcurrentHashMap.newKeySet();

    static NodeListSniffer create(RestClient restClient, long sniffRequestTimeoutMillis, OpenSearchNodesSniffer.Scheme scheme) {
        final NodesSniffer nodesSniffer = new OpenSearchNodesSniffer(restClient, sniffRequestTimeoutMillis, scheme);
        return new NodeListSniffer(nodesSniffer);
    }

    public NodeListSniffer(NodesSniffer nodesSniffer) {
        this.nodesSniffer = nodesSniffer;
    }

    @Override
    public List<Node> sniff() throws IOException {
        final List<Node> nodes = this.nodesSniffer.sniff();

        final Set<String> currentNodes = nodes.stream().map(n -> n.getHost().toURI()).collect(Collectors.toSet());

        final Set<String> nodesAdded = Sets.difference(currentNodes, savedNodes);
        final Set<String> nodesDropped = Sets.difference(savedNodes, currentNodes);

        if(!nodesAdded.isEmpty()) {
            LOG.info("Added node(s): {}", nodesAdded);
        }
        if(!nodesDropped.isEmpty()) {
            LOG.info("Dropped node(s): {}", nodesDropped);
        }
        if(!nodesAdded.isEmpty() || !nodesDropped.isEmpty()) {
            LOG.info("Current node list: {}", currentNodes);
        }

        savedNodes.clear();
        savedNodes.addAll(currentNodes);

        return nodes;
    }
}

