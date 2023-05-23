package org.graylog2.migrations;

import org.graylog.events.event.EventDto;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.systemnotification.SystemNotificationEventEntityScope;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class V20230523160600_PopulateEventDefinitionEnabled extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20230523160600_PopulateEventDefinitionEnabled.class);

    private final ClusterConfigService clusterConfigService;
    private final DBEventDefinitionService dbEventDefinitionService;
    private final DBJobDefinitionService dbJobDefinitionService;

    @Inject
    public V20230523160600_PopulateEventDefinitionEnabled(ClusterConfigService clusterConfigService,
                                                          DBEventDefinitionService dbEventDefinitionService,
                                                          DBJobDefinitionService dbJobDefinitionService) {
        this.clusterConfigService = clusterConfigService;
        this.dbEventDefinitionService = dbEventDefinitionService;
        this.dbJobDefinitionService = dbJobDefinitionService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-05-26T16:06:00Z");
    }

    @Override
    public void upgrade() {
        LOG.info("V20230523160600_PopulateEventDefinitionEnabled");
        if (clusterConfigService.get(V20230523160600_PopulateEventDefinitionEnabled.MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
            return;
        }
        // Collect a list of all event definitions with a defined job (ie, enabled event definitions) as well as all
        // system event definitions to be marked as enabled
        List<String> enabledEventDefinitionIds = new ArrayList<>();
        dbEventDefinitionService.streamAll().forEach(dto -> {
            Optional<JobDefinitionDto> jobDefinition = dbJobDefinitionService.getByConfigField(EventDto.FIELD_EVENT_DEFINITION_ID, dto.id());
            if (dto.scope().equals(SystemNotificationEventEntityScope.NAME) || jobDefinition.isPresent()) {
                enabledEventDefinitionIds.add(dto.id());
            }
        });

        // Mark enabled event definitions as such
        dbEventDefinitionService.bulkEnableDisable(enabledEventDefinitionIds, true);
        clusterConfigService.write(new V20230523160600_PopulateEventDefinitionEnabled.MigrationCompleted());
    }

    public record MigrationCompleted() {}
}
