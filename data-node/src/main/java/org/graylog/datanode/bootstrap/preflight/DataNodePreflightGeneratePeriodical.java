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
package org.graylog.datanode.bootstrap.preflight;

import org.bouncycastle.operator.OperatorCreationException;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog.security.certutil.csr.storage.CsrMongoStorage;
import org.graylog.security.certutil.csr.storage.CsrStorage;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedFileStorage;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodePreflightConfig;
import org.graylog2.cluster.NodePreflightConfigService;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class DataNodePreflightGeneratePeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(DataNodePreflightGeneratePeriodical.class);

    private final NodePreflightConfigService nodePreflightConfigService;
    private final NodeService nodeService;
    private final NodeId nodeId;
    private final PrivateKeyEncryptedFileStorage privateKeyEncryptedStorage;
    private final CsrStorage csrStorage;
    private final CsrGenerator csrGenerator;

    //TODO: decide on password handling
    private static final String DEFAULT_PASSWORD = "admin";

    @Inject
    public DataNodePreflightGeneratePeriodical(final NodePreflightConfigService nodePreflightConfigService,
                                               final NodeService nodeService,
                                               final NodeId nodeId,
                                               final CsrMongoStorage csrStorage,
                                               final CsrGenerator csrGenerator) {
        this.nodePreflightConfigService = nodePreflightConfigService;
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        this.csrStorage = csrStorage;
        this.csrGenerator = csrGenerator;
        // TODO: merge with real storage
        this.privateKeyEncryptedStorage = new PrivateKeyEncryptedFileStorage("privateKeyFilename.cert");
    }

    @Override
    public void doRun() {
        LOG.debug("checking if this DataNode is supposed to take configuration steps.");
        var cfg = nodePreflightConfigService.getPreflightConfigFor(nodeId.getNodeId());
        if (cfg == null) {
            // write default config if none exists for this node
            nodePreflightConfigService.save(NodePreflightConfig.builder().nodeId(nodeId.getNodeId()).state(NodePreflightConfig.State.UNCONFIGURED).build());
        } else if (NodePreflightConfig.State.CONFIGURED.equals(cfg.state())) {
            try {
                var node = nodeService.byNodeId(nodeId);
                var csr = csrGenerator.generateCSR(DEFAULT_PASSWORD.toCharArray(), node.getHostname(), cfg.altNames(), privateKeyEncryptedStorage);
                csrStorage.writeCsr(csr);
                LOG.info("created CSR for this node");
            } catch (CSRGenerationException | IOException | NodeNotFoundException | OperatorCreationException ex) {
                LOG.error("error generating a CSR: " + ex.getMessage(), ex);
                nodePreflightConfigService.save(cfg.toBuilder().state(NodePreflightConfig.State.ERROR).errorMsg(ex.getMessage()).build());
            }
        } else if (NodePreflightConfig.State.SIGNED.equals(cfg.state())) {
            // write certificate to local truststore
            // configure SSL
            // start DataNode
            // set state to State.CONNECTED
            // nodePreflightConfigService.save(cfg.toBuilder().state(NodePreflightConfig.State.CONNECTED).build());
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean leaderOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 2;
    }
}
