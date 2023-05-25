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
package org.graylog2.bootstrap.preflight.web.resources;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.bootstrap.preflight.PreflightConstants;
import org.graylog2.bootstrap.preflight.web.resources.model.CA;
import org.graylog2.bootstrap.preflight.web.resources.model.CertParameters;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodePreflightConfig;
import org.graylog2.cluster.NodePreflightConfigService;
import org.graylog2.cluster.NodeService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path(PreflightConstants.API_PREFIX)
@Produces(MediaType.APPLICATION_JSON)
public class PreflightResource {

    private final NodeService nodeService;
    private final NodePreflightConfigService nodePreflightConfigService;
    private final CaService caService;

    @Inject
    public PreflightResource(final NodeService nodeService,
                             final NodePreflightConfigService nodePreflightConfigService,
                             final CaService caService) {
        this.nodeService = nodeService;
        this.nodePreflightConfigService = nodePreflightConfigService;
        this.caService = caService;
    }

    @GET
    @Path("/data_nodes")
    public List<Node> listDataNodes() {
        final Map<String, Node> activeDataNodes = nodeService.allActive(Node.Type.DATANODE);
        return new ArrayList<>(activeDataNodes.values());
    }

    @GET
    @Path("/ca")
    public CA get() {
        return caService.get();
    }

    @POST
    @Path("/ca/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @NoAuditEvent("No Audit Event needed")
    public void createCA() throws CACreationException {
        // TODO: get validity from preflight UI
        caService.create(null);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/upload")
    @NoAuditEvent("No Audit Event needed")
    public String uploadCA(@FormDataParam("password") String password, FormDataMultiPart body) throws CACreationException {
        caService.upload(password, body);
        return "Ok";
    }

    @DELETE
    @Path("/startOver")
    @NoAuditEvent("No Audit Event needed")
    public void startOver() {
        caService.startOver();
        nodePreflightConfigService.deleteAll();
    }

    @DELETE
    @Path("/startOver/{nodeID}")
    @NoAuditEvent("No Audit Event needed")
    public void startOver(@PathParam("nodeID") String nodeID) {
        //TODO:  reset a specific datanode
        nodePreflightConfigService.delete(nodeID);
    }

    @POST
    @Path("/generate")
    @NoAuditEvent("No Audit Event needed")
    public void generate() {
        final Map<String, Node> activeDataNodes = nodeService.allActive(Node.Type.DATANODE);
        activeDataNodes.values().forEach(node -> nodePreflightConfigService.changeState(node.getNodeId(), NodePreflightConfig.State.CONFIGURED));
    }

    @POST
    @Path("/{nodeID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @NoAuditEvent("No Audit Event needed")
    public void addParameters(@PathParam("nodeID") String nodeID,
                              @NotNull CertParameters params) {
        var cfg = nodePreflightConfigService.getPreflightConfigFor(nodeID);
        var builder = cfg != null ? cfg.toBuilder() : NodePreflightConfig.builder().nodeId(nodeID);
        builder.altNames(params.altNames()).validFor(params.validFor());
        nodePreflightConfigService.save(builder.build());

    }
}
