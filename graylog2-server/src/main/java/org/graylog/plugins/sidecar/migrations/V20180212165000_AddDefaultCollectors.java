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
package org.graylog.plugins.sidecar.migrations;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.ConfigurationVariable;
import org.graylog.plugins.sidecar.services.CollectorService;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog.plugins.sidecar.services.ConfigurationVariableService;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static org.graylog2.shared.utilities.StringUtils.f;

public class V20180212165000_AddDefaultCollectors extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20180212165000_AddDefaultCollectors.class);
    private final CollectorService collectorService;
    private final ConfigurationVariableService configurationVariableService;
    private final MongoCollection<Document> collection;
    private final URI httpExternalUri;
    private final ConfigurationService configurationService;

    @Inject
    public V20180212165000_AddDefaultCollectors(HttpConfiguration httpConfiguration,
                                                CollectorService collectorService,
                                                ConfigurationVariableService configurationVariableService,
                                                ConfigurationService configurationService,
                                                MongoConnection mongoConnection) {
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
        this.collectorService = collectorService;
        this.configurationVariableService = configurationVariableService;
        this.configurationService = configurationService;
        this.collection = mongoConnection.getMongoDatabase().getCollection(CollectorService.COLLECTION_NAME);
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-02-12T16:50:00Z");
    }

    @Override
    public void upgrade() {

        removeConfigPath();
        ensureConfigurationVariable("graylog_host", "Graylog Host.", httpExternalUri.getHost());

        final String beatsPreambel =
                """
                        # Needed for Graylog
                        fields_under_root: true
                        fields.collector_node_id: ${sidecar.nodeName}
                        fields.gl2_source_collector: ${sidecar.nodeId}

                        """;

        ensureCollector(
                "filebeat",
                "exec",
                "linux",
                "/usr/lib/graylog-sidecar/filebeat",
                "-c  %s",
                "test config -c %s",
                f("""
                                %s
                                filebeat.inputs:
                                - input_type: log
                                  paths:
                                    - /var/log/*.log
                                  type: log
                                output.logstash:
                                   hosts: ["${user.graylog_host}:5044"]
                                path:
                                  data: ${sidecar.spoolDir!"/var/lib/graylog-sidecar/collectors/filebeat"}/data
                                  logs: ${sidecar.spoolDir!"/var/lib/graylog-sidecar/collectors/filebeat"}/log"""
                        , beatsPreambel
                )
        );
        ensureCollector(
                "filebeat",
                "exec",
                "darwin",
                "/usr/share/filebeat/bin/filebeat",
                "-c  %s",
                "test config -c %s",
                f("""
                                %s
                                filebeat.inputs:
                                - input_type: log
                                  paths:
                                    - /var/log/*.log
                                  type: log
                                output.logstash:
                                   hosts: ["${user.graylog_host}:5044"]
                                path:
                                  data: ${sidecar.spoolDir!"/var/lib/graylog-sidecar/collectors/filebeat"}/data
                                  logs: ${sidecar.spoolDir!"/var/lib/graylog-sidecar/collectors/filebeat"}/log""",
                        beatsPreambel
                )
        );
        ensureCollector(
                "filebeat",
                "exec",
                "freebsd",
                "/usr/share/filebeat/bin/filebeat",
                "-c  %s",
                "test config -c %s",
                f("""
                                %s
                                filebeat.inputs:
                                - input_type: log
                                  paths:
                                    - /var/log/*.log
                                  type: log
                                output.logstash:
                                   hosts: ["${user.graylog_host}:5044"]
                                path:
                                  data: ${sidecar.spoolDir!"/var/lib/graylog-sidecar/collectors/filebeat"}/data
                                  logs: ${sidecar.spoolDir!"/var/lib/graylog-sidecar/collectors/filebeat"}/log""",
                        beatsPreambel
                )
        );
        ensureCollector(
                "auditbeat",
                "exec",
                "linux",
                "/usr/lib/graylog-sidecar/auditbeat",
                "-c  %s",
                "test config -c %s",
                f("""
                                %s
                                auditbeat.modules:
                                - module: file_integrity
                                  paths:
                                  - /bin
                                  - /usr/bin
                                  - /sbin
                                  - /usr/sbin
                                  - /etc
                                output.logstash:
                                   hosts: ["${user.graylog_host}:5044"]
                                path:
                                  data: /var/lib/graylog-sidecar/collectors/auditbeat/data
                                  logs: /var/lib/graylog-sidecar/collectors/auditbeat/log""",
                        beatsPreambel)
        ).ifPresent(collector -> ensureDefaultConfiguration("default-linux", collector));
        ensureCollector(
                "winlogbeat",
                "svc",
                "windows",
                "C:\\Program Files\\Graylog\\sidecar\\winlogbeat.exe",
                "-c \"%s\"",
                "test config -c \"%s\"",
                f("""
                                %s
                                output.logstash:
                                   hosts: ["${user.graylog_host}:5044"]
                                path:
                                  data: ${sidecar.spoolDir!"C:\\\\Program Files\\\\Graylog\\\\sidecar\\\\cache\\\\winlogbeat"}\\data
                                  logs: ${sidecar.spoolDir!"C:\\\\Program Files\\\\Graylog\\\\sidecar"}\\logs
                                tags:
                                 - windows
                                winlogbeat:
                                  event_logs:
                                   - name: Application
                                   - name: System
                                   - name: Security""",
                        beatsPreambel
                )
        ).ifPresent(collector -> ensureDefaultConfiguration("default-windows", collector));
        ensureCollector(
                "nxlog",
                "exec",
                "linux",
                "/usr/bin/nxlog",
                "-f -c %s",
                "-v -c %s",
                """
                        define ROOT /usr/bin

                        <Extension gelfExt>
                          Module xm_gelf
                          # Avoid truncation of the short_message field to 64 characters.
                          ShortMessageLength 65536
                        </Extension>

                        <Extension syslogExt>
                          Module xm_syslog
                        </Extension>

                        User nxlog
                        Group nxlog

                        Moduledir /usr/lib/nxlog/modules
                        CacheDir ${sidecar.spoolDir!"/var/spool/nxlog"}/data
                        PidFile ${sidecar.spoolDir!"/var/run/nxlog"}/nxlog.pid
                        LogFile ${sidecar.spoolDir!"/var/log/nxlog"}/nxlog.log
                        LogLevel INFO


                        <Input file>
                        \tModule im_file
                        \tFile '/var/log/*.log'
                        \tPollInterval 1
                        \tSavePos\tTrue
                        \tReadFromLast True
                        \tRecursive False
                        \tRenameCheck False
                        \tExec $FileName = file_name(); # Send file name with each message
                        </Input>

                        #<Input syslog-udp>
                        #\tModule im_udp
                        #\tHost 127.0.0.1
                        #\tPort 514
                        #\tExec parse_syslog_bsd();
                        #</Input>

                        <Output gelf>
                        \tModule om_tcp
                        \tHost ${user.graylog_host}
                        \tPort 12201
                        \tOutputType  GELF_TCP
                        \t<Exec>
                        \t  # These fields are needed for Graylog
                        \t  $gl2_source_collector = '${sidecar.nodeId}';
                        \t  $collector_node_id = '${sidecar.nodeName}';
                        \t</Exec>
                        </Output>


                        <Route route-1>
                          Path file => gelf
                        </Route>
                        #<Route route-2>
                        #  Path syslog-udp => gelf
                        #</Route>


                        """
        );
        ensureCollector(
                "nxlog",
                "svc",
                "windows",
                "C:\\Program Files (x86)\\nxlog\\nxlog.exe",
                "-c \"%s\"",
                "-v -f -c \"%s\"",
                """
                        define ROOT ${sidecar.spoolDir!"C:\\\\Program Files (x86)"}\\nxlog

                        Moduledir %ROOT%\\modules
                        CacheDir %ROOT%\\data
                        Pidfile %ROOT%\\data\\nxlog.pid
                        SpoolDir %ROOT%\\data
                        LogFile %ROOT%\\data\\nxlog.log
                        LogLevel INFO

                        <Extension logrotate>
                            Module  xm_fileop
                            <Schedule>
                                When    @daily
                                Exec    file_cycle('%ROOT%\\data\\nxlog.log', 7);
                             </Schedule>
                        </Extension>


                        <Extension gelfExt>
                          Module xm_gelf
                          # Avoid truncation of the short_message field to 64 characters.
                          ShortMessageLength 65536
                        </Extension>

                        <Input eventlog>
                                Module im_msvistalog
                                PollInterval 1
                                SavePos True
                                ReadFromLast True
                               \s
                                #Channel System
                                #<QueryXML>
                                #  <QueryList>
                                #   <Query Id='1'>
                                #    <Select Path='Security'>*[System/Level=4]</Select>
                                #    </Query>
                                #  </QueryList>
                                #</QueryXML>
                        </Input>


                        <Input file>
                        \tModule im_file
                        \tFile 'C:\\Windows\\MyLogDir\\\\*.log'
                        \tPollInterval 1
                        \tSavePos\tTrue
                        \tReadFromLast True
                        \tRecursive False
                        \tRenameCheck False
                        \tExec $FileName = file_name(); # Send file name with each message
                        </Input>


                        <Output gelf>
                        \tModule om_tcp
                        \tHost ${user.graylog_host}
                        \tPort 12201
                        \tOutputType  GELF_TCP
                        \t<Exec>
                        \t  # These fields are needed for Graylog
                        \t  $gl2_source_collector = '${sidecar.nodeId}';
                        \t  $collector_node_id = '${sidecar.nodeName}';
                        \t</Exec>
                        </Output>


                        <Route route-1>
                          Path eventlog => gelf
                        </Route>
                        <Route route-2>
                          Path file => gelf
                        </Route>

                        """
        );
        ensureCollector(
                "filebeat",
                "svc",
                "windows",
                "C:\\Program Files\\Graylog\\sidecar\\filebeat.exe",
                "-c \"%s\"",
                "test config -c \"%s\"",
                f("""
                                %s
                                output.logstash:
                                   hosts: ["${user.graylog_host}:5044"]
                                path:
                                  data: ${sidecar.spoolDir!"C:\\\\Program Files\\\\Graylog\\\\sidecar\\\\cache\\\\filebeat"}\\data
                                  logs: ${sidecar.spoolDir!"C:\\\\Program Files\\\\Graylog\\\\sidecar"}\\logs
                                tags:
                                 - windows
                                filebeat.inputs:
                                - type: log
                                  enabled: true
                                  paths:
                                    - C:\\logs\\log.log
                                """,
                        beatsPreambel
                )
        );
    }

    private void removeConfigPath() {
        final FindIterable<Document> documentsWithConfigPath = collection.find(exists("configuration_path"));
        for (Document document : documentsWithConfigPath) {
            final ObjectId objectId = document.getObjectId("_id");
            document.remove("configuration_path");
            final UpdateResult updateResult = collection.replaceOne(eq("_id", objectId), document);
            if (updateResult.wasAcknowledged()) {
                LOG.debug("Successfully updated document with ID <{}>", objectId);
            } else {
                LOG.error("Failed to update document with ID <{}>", objectId);
            }
        }
    }

    private Optional<Collector> ensureCollector(String collectorName,
                                                String serviceType,
                                                String nodeOperatingSystem,
                                                String executablePath,
                                                String executeParameters,
                                                String validationCommand,
                                                String defaultTemplate) {
        Collector collector = null;
        try {
            collector = collectorService.findByNameAndOs(collectorName, nodeOperatingSystem);
            if (collector == null) {
                LOG.error("Couldn't find collector '{} on {}' fixing it.", collectorName, nodeOperatingSystem);
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ignored) {
            LOG.info("{} collector on {} is missing, adding it.", collectorName, nodeOperatingSystem);
            try {
                return Optional.of(collectorService.save(Collector.create(
                        null,
                        collectorName,
                        serviceType,
                        nodeOperatingSystem,
                        executablePath,
                        executeParameters,
                        validationCommand,
                        defaultTemplate
                )));
            } catch (Exception e) {
                LOG.error("Can't save collector '{}', please restart Graylog to fix this.", collectorName, e);
            }
        }

        if (collector == null) {
            LOG.error("Unable to access fixed '{}' collector, please restart Graylog to fix this.", collectorName);
            return Optional.empty();
        }

        return Optional.of(collector);
    }

    private void ensureConfigurationVariable(String name, String description, String content) {
        ConfigurationVariable variable = null;
        try {
            variable = configurationVariableService.findByName(name);
            if (variable == null) {
                LOG.error("Couldn't find sidecar configuration variable '{}' fixing it.", name);
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ignored) {
            LOG.info("'{}' sidecar configuration variable is missing, adding it.", name);
            try {
                variable = configurationVariableService.save(ConfigurationVariable.create(name, description, content));
            } catch (Exception e) {
                LOG.error("Can't save sidecar configuration variable '{}', please restart Graylog to fix this.", name, e);
            }
        }

        if (variable == null) {
            LOG.error("Unable to access '{}' sidecar configuration variable, please restart Graylog to fix this.", name);
        }
    }

    private void ensureDefaultConfiguration(String name, Collector collector) {
        Configuration config = null;
        try {
            config = configurationService.findByName(name);
            if (config == null) {
                LOG.error("Couldn't find sidecar default configuration'{}' fixing it.", name);
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ignored) {
            LOG.info("'{}' sidecar default configuration is missing, adding it.", name);
            try {
                config = configurationService.save(Configuration.createWithoutId(
                        collector.id(),
                        name,
                        "#ffffff",
                        collector.defaultTemplate(),
                        Set.of("default")));
            } catch (Exception e) {
                LOG.error("Can't save sidecar default configuration '{}', please restart Graylog to fix this.", name, e);
            }
        }

        if (config == null) {
            LOG.error("Unable to access '{}' sidecar default configuration, please restart Graylog to fix this.", name);
        }
    }

}
