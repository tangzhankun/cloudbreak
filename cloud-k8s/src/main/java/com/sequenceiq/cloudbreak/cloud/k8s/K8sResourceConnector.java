package com.sequenceiq.cloudbreak.cloud.k8s;

import static com.sequenceiq.cloudbreak.common.type.ResourceType.K8S_APPLICATION;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingDoesNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.k8s.auth.K8sClientUtil;
import com.sequenceiq.cloudbreak.cloud.k8s.auth.K8sCredentialView;
import com.sequenceiq.cloudbreak.cloud.k8s.client.model.core.K8sComponent;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.AppsV1beta1Deployment;
import io.kubernetes.client.models.AppsV1beta1DeploymentSpec;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ConfigMapVolumeSource;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerPort;
import io.kubernetes.client.models.V1LabelSelector;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1ResourceRequirements;
import io.kubernetes.client.models.V1SecurityContext;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServiceSpec;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeMount;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.authenticators.GCPAuthenticator;

@Service
public class K8sResourceConnector implements ResourceConnector<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(K8sResourceConnector.class);

    private static final String ARTIFACT_TYPE_DOCKER = "DOCKER";

    private static final String CB_CLUSTER_NAME = "cb-cluster-name";

    private static final String CB_CLUSTER_GROUP_NAME = "cb-cluster-name";

    private static final String APP_NAME = "app";

    private static final int SSH_PORT = 22;

    private static final int NGNIX_PORT = 9443;

    private static final int AMBARI_PORT = 443;

    private static final int DEFAULT_MODE = 0744;

    @Inject
    private K8sClientUtil k8sClientUtil;

    @Inject
    private K8sResourceNameGenerator k8sResourceNameGenerator;

    @Value("${cb.k8s.defaultNameSpace}")
    private String defaultNameSpace;

    @Value("${cb.k8s.defaultLifeTime:}")
    private int defaultLifeTime;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception {
        K8sCredentialView k8sClient = k8sClientUtil.createK8sClient(authenticatedContext);
        String applicationName = createApplicationName(authenticatedContext);


        if (!checkApplicationAlreadyCreated(k8sClient, applicationName)) {
            createApplication(authenticatedContext, k8sClient, stack);
        }

        CloudResource k8sApplication = new Builder().type(K8S_APPLICATION).name(applicationName).build();
        persistenceNotifier.notifyAllocation(k8sApplication, authenticatedContext.getCloudContext());
        return check(authenticatedContext, Collections.singletonList(k8sApplication));

        K8sComponent k8sComponent = new K8sComponent();
        K8sApiUtils.createK8sApp(stack., applicationName);
    }

    public ApiClient apiClient() throws IOException {
        KubeConfig.registerAuthenticator(new GCPAuthenticator());
        return Config.defaultClient();
    }

    public CoreV1Api coreV1Api() throws IOException {
        return new CoreV1Api();
    }

    private void createApplication(AuthenticatedContext ac, K8sCredentialView k8sCredential, CloudStack stack) throws IOException, ApiException {
        createApiClient();
        createConfigMap(ac, stack);
        for (Group group : stack.getGroups()) {
            int i = 0;
            for (CloudInstance cloudInstance : group.getInstances()) {
                createInstance(ac, stack, group, i);
                i++;
            }
        }
    }

    public Map<String, String> getLabels(String clusterName, Optional<String> groupName) {
        Map<String, String> labels = new HashMap<>();
        labels.put(APP_NAME, clusterName);
        labels.put(CB_CLUSTER_NAME, clusterName);
        if (groupName.isPresent()) {
            labels.put(CB_CLUSTER_GROUP_NAME, groupName.orElse(null));
        }
        return labels;
    }

    private void createInstance(AuthenticatedContext ac, CloudStack stack, Group group, int i) {
        String instanceName = k8sResourceNameGenerator.getInstanceContainerName(ac, group, i);
        String groupName = k8sResourceNameGenerator.getGroupName(group);
        String clusterName = k8sResourceNameGenerator.getClusterName(ac);

        AppsV1beta1Deployment deployment = new AppsV1beta1Deployment();

        deployment.setKind("Deployment");
        deployment.setApiVersion("apps/v1beta1");
        V1ObjectMeta meta = new V1ObjectMeta();
        deployment.setMetadata(meta);

        meta.setName(clusterName);
        meta.setLabels(getLabels(clusterName, Optional.empty()));

        AppsV1beta1DeploymentSpec spec = new AppsV1beta1DeploymentSpec();
        deployment.setSpec(spec);

        spec.setReplicas(1);

        V1LabelSelector selector = new V1LabelSelector();
        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put(APP_NAME, clusterName);
        matchLabels.put(CB_CLUSTER_NAME, clusterName);
        selector.matchLabels(matchLabels);
        spec.setSelector(selector);

        V1PodTemplateSpec podTemplateSpec = new V1PodTemplateSpec();
        spec.setTemplate(podTemplateSpec);

        V1ObjectMeta podMeta = new V1ObjectMeta();
        podMeta.setLabels(matchLabels);
        podTemplateSpec.setMetadata(podMeta);

        V1PodSpec podSpec = new V1PodSpec();
        podTemplateSpec.setSpec(podSpec);

        List<V1Container> containers = new ArrayList<>();
        podSpec.setContainers(containers);

        V1Container container = new V1Container();
        containers.add(container);

        container.setName(instanceName);
        container.setImage(stack.getImage().getImageName());

        List<String> cmd = new ArrayList<>();
        cmd.add("/sbin/init");
        container.setCommand(cmd);

        V1SecurityContext secContext = new V1SecurityContext();
        secContext.setPrivileged(true);
        container.setSecurityContext(secContext);

        List<V1VolumeMount> volumeMounts = new ArrayList<>();
        container.setVolumeMounts(volumeMounts);

        V1VolumeMount configVolumeMount = new V1VolumeMount();
        configVolumeMount.setName("config-volume");
        configVolumeMount.setMountPath("/configmap");
        volumeMounts.add(configVolumeMount);

        List<V1ContainerPort> ports = new ArrayList<>();
        container.setPorts(ports);

        V1ContainerPort sshPort = new V1ContainerPort();
        sshPort.setContainerPort(SSH_PORT);
        sshPort.setName("ssh");
        ports.add(sshPort);

        V1ContainerPort ngnixPort = new V1ContainerPort();
        ngnixPort.setName("ngnix");
        ngnixPort.setContainerPort(NGNIX_PORT);
        ports.add(ngnixPort);

        V1ContainerPort saltPort = new V1ContainerPort();
        saltPort.setName("ambari");
        saltPort.setContainerPort(AMBARI_PORT);
        ports.add(saltPort);

        V1ResourceRequirements resources = new V1ResourceRequirements();
        resources.putLimitsItem("memory", Quantity.fromString(group.getReferenceInstanceConfiguration().getTemplate(). + "Mi"));
        resources.putLimitsItem("cpu", Quantity.fromString(component.getResource().getCpus() + ""));
        container.setResources(resources);
        List<V1Volume> volumes = new ArrayList<>();
        podSpec.setVolumes(volumes);

        V1Volume configVolume = new V1Volume();
        configVolume.setName("config-volume");
        V1ConfigMapVolumeSource configMap = new V1ConfigMapVolumeSource();
        configMap.setDefaultMode(DEFAULT_MODE);
        configMap.setName(configName);

        configVolume.setConfigMap(configMap);
        volumes.add(configVolume);

        appsV1beta1Api.createNamespacedDeployment(DEFAULT_NAMESPACE, deployment, null);

        LOGGER.info("Created Deployment " + appName);

        V1Service serviceBody = new V1Service();

        serviceBody.setMetadata(meta);

        meta.setName(appName);
        Map<String, String> labels = new HashMap<>();
        labels.put(HWX_DPS_CLUSTER_NAME, hdpClusterName);
        labels.put(HWX_DPS_CLUSTER_GROUP, group);
        meta.setLabels(labels);

        V1ServiceSpec serviceSpec = new V1ServiceSpec();
        serviceSpec.setType("LoadBalancer");
        Map<String, String> selectorMap = new HashMap<>();
        serviceSpec.setSelector(selectorMap);

        selectorMap.put("app", appName);

        serviceBody.setSpec(serviceSpec);

        api.createNamespacedService(DEFAULT_NAMESPACE, serviceBody, null);

        LOGGER.info("Created Service " + appName);
    }

    private void createApiClient() throws IOException {
        ApiClient client = apiClient();
        Configuration.setDefaultApiClient(client);


    }

    private void createConfigMap(AuthenticatedContext ac, CloudStack stack) throws IOException, ApiException {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> e : stack.getParameters().entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue()).append("\n");
        }
        String configPropsString = sb.toString();

        V1ConfigMap body = new V1ConfigMap();

        V1ObjectMeta configMeta = new V1ObjectMeta();
        body.setMetadata(configMeta);
        Map<String, String> labels = new HashMap<>();
        configMeta.setLabels(labels);
        labels.put(CB_CLUSTER_NAME, k8sResourceNameGenerator.getClusterName(ac));


        String configMapName = k8sResourceNameGenerator.getConfigMapName(ac);

        configMeta.setName(configMapName);
        Map<String, String> configData = new HashMap<>();
        body.setData(configData);

        configData.put("cloudbreak-config.props", configPropsString);

        coreV1Api().createNamespacedConfigMap(defaultNameSpace, body, null);
        LOGGER.info("Created ConfigMap " + configMapName);
    }

    private boolean checkApplicationAlreadyCreated(K8sCredentialView k8sCredential, String applicationName) throws MalformedURLException {
        return true;
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : resources) {
            switch (resource.getType()) {
                case K8S_APPLICATION:
                    result.add(new CloudResourceStatus(resource, ResourceStatus.CREATED));
                    break;
                default:
                    throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
            }
        }
        return result;
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext,
            CloudStack stack, List<CloudResource> cloudResources) throws Exception {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : cloudResources) {
            switch (resource.getType()) {
                case K8S_APPLICATION:
                    K8sApiUtils.deleteK8sApp(resource.getName());
                    result.add(new CloudResourceStatus(resource, ResourceStatus.DELETED));
                    break;
                default:
                    throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
            }
        }
        return result;
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        throw new CloudOperationNotSupportedException("Upscale stack operation is not supported on K8S");
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            List<CloudInstance> vms, Object resourcesToRemove) {
        throw new CloudOperationNotSupportedException("Downscale stack operation is not supported on K8S");
    }

    @Override
    public Object collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms) {
        throw new CloudOperationNotSupportedException("Downscale resources collection operation is not supported on K8S");
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() throws TemplatingDoesNotSupportedException {
        throw new TemplatingDoesNotSupportedException();
    }

}
