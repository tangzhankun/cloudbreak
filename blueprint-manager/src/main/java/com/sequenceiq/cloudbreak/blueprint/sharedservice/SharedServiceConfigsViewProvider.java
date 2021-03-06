package com.sequenceiq.cloudbreak.blueprint.sharedservice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Component
public class SharedServiceConfigsViewProvider {

    private static final String DEFAULT_RANGER_PORT = "6080";

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Inject
    private BlueprintUtils blueprintUtils;

    public SharedServiceConfigsView createSharedServiceConfigs(Blueprint blueprint, String ambariPassword, Stack dataLakeStack) {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        if (dataLakeStack != null) {
            BlueprintTextProcessor blueprintTextProcessor = blueprintProcessorFactory.get(dataLakeStack.getCluster().getBlueprint().getBlueprintText());
            Map<String, Map<String, String>> configurationEntries = blueprintTextProcessor.getConfigurationEntries();
            Map<String, String> rangerAdminConfigs = configurationEntries.getOrDefault("ranger-admin-site", new HashMap<>());
            String rangerPort = rangerAdminConfigs.getOrDefault("ranger.service.http.port", DEFAULT_RANGER_PORT);

            sharedServiceConfigsView.setRangerAdminPassword(dataLakeStack.getCluster().getPassword());
            sharedServiceConfigsView.setAttachedCluster(true);
            sharedServiceConfigsView.setDatalakeCluster(false);
            sharedServiceConfigsView.setDatalakeAmbariIp(dataLakeStack.getAmbariIp());
            sharedServiceConfigsView.setDatalakeComponents(prepareComponents(dataLakeStack.getCluster().getBlueprint().getBlueprintText()));
            sharedServiceConfigsView.setRangerAdminPort(rangerPort);
        } else if (blueprintUtils.isSharedServiceReqdyBlueprint(blueprint)) {
            sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
            sharedServiceConfigsView.setAttachedCluster(false);
            sharedServiceConfigsView.setDatalakeCluster(true);
            sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
        } else {
            sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
            sharedServiceConfigsView.setAttachedCluster(false);
            sharedServiceConfigsView.setDatalakeCluster(false);
            sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
        }

        return sharedServiceConfigsView;
    }

    public SharedServiceConfigsView createSharedServiceConfigs(Stack source, Stack dataLakeStack) {
        return createSharedServiceConfigs(source.getCluster().getBlueprint(), source.getCluster().getPassword(), dataLakeStack);
    }

    private Set<String> prepareComponents(String blueprintText) {
        Set<String> result = new HashSet<>();
        try {
            BlueprintTextProcessor blueprintTextProcessor = new BlueprintTextProcessor(blueprintText);
            Map<String, Set<String>> componentsByHostGroup = blueprintTextProcessor.getComponentsByHostGroup();
            componentsByHostGroup.values().forEach(result::addAll);
        } catch (BlueprintProcessingException exception) {
            result = new HashSet<>();
        }
        return result;
    }
}
