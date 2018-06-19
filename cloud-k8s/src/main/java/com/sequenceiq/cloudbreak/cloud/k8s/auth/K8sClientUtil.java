package com.sequenceiq.cloudbreak.cloud.k8s.auth;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.k8s.client.K8sClient;

@Component
public class K8sClientUtil {
    public K8sClient createK8sClient(AuthenticatedContext authenticatedContext) {
        K8sCredentialView k8sCredentialView = new K8sCredentialView(authenticatedContext.getCloudCredential());
        return null;
    }
}
