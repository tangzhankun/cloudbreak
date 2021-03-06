package com.sequenceiq.cloudbreak.service.constraint;


import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.ConstraintTemplateRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class ConstraintTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintTemplateService.class);

    private static final String CONSTRAINT_NOT_FOUND_MSG = "Constraint template '%s' not found.";

    private static final String CONSTRAINT_NOT_FOUND_BY_ID_MSG = "Constraint template not found by id: '%d'.";

    @Inject
    private ConstraintTemplateRepository constraintTemplateRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private OrganizationService organizationService;

    public Set<ConstraintTemplate> retrievePrivateConstraintTemplates(IdentityUser user) {
        return constraintTemplateRepository.findForUser(user.getUserId());
    }

    public Set<ConstraintTemplate> retrieveAccountConstraintTemplates(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? constraintTemplateRepository.findAllInAccount(user.getAccount())
                : constraintTemplateRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
    }

    public ConstraintTemplate get(Long id) {
        return constraintTemplateRepository.findById(id)
                .orElseThrow(notFound("Constraint", id));
    }

    public ConstraintTemplate create(IdentityUser user, ConstraintTemplate constraintTemplate, Organization organization) {
        LOGGER.debug("Creating constraint template: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        constraintTemplate.setOwner(user.getUserId());
        constraintTemplate.setAccount(user.getAccount());
        if (organization != null) {
            constraintTemplate.setOrganization(organization);
        } else {
            constraintTemplate.setOrganization(organizationService.getDefaultOrganizationForCurrentUser());
        }
        try {
            return constraintTemplateRepository.save(constraintTemplate);
        } catch (RuntimeException e) {
            throw new DuplicateKeyValueException(APIResourceType.CONSTRAINT_TEMPLATE, constraintTemplate.getName(), e);
        }
    }

    public void delete(String name, IdentityUser user) {
        ConstraintTemplate constraintTemplate = constraintTemplateRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        delete(constraintTemplate);
    }

    public void delete(Long id, IdentityUser user) {
        ConstraintTemplate constraintTemplate = constraintTemplateRepository.findByIdInAccount(id, user.getAccount());
        delete(constraintTemplate);
    }

    public ConstraintTemplate getPublicTemplate(String name, IdentityUser user) {
        return constraintTemplateRepository.findOneByName(name, user.getAccount());
    }

    public ConstraintTemplate getPrivateTemplate(String name, IdentityUser user) {
        return constraintTemplateRepository.findByNameInUser(name, user.getUserId());
    }

    private void delete(ConstraintTemplate constraintTemplate) {
        LOGGER.info("Deleting constraint-template. {} - {}", new Object[]{constraintTemplate.getId(), constraintTemplate.getName()});
        List<Cluster> clusters = clusterService.findAllClustersForConstraintTemplate(constraintTemplate.getId());
        if (clusters.isEmpty()) {
            if (ResourceStatus.USER_MANAGED.equals(constraintTemplate.getStatus())) {
                constraintTemplateRepository.delete(constraintTemplate);
            } else {
                constraintTemplate.setName(NameUtil.postfixWithTimestamp(constraintTemplate.getName()));
                constraintTemplate.setStatus(ResourceStatus.DEFAULT_DELETED);
                constraintTemplateRepository.save(constraintTemplate);
            }
        } else if (isRunningClusterReferToTemplate(clusters)) {
            throw new BadRequestException(String.format(
                    "There are stacks associated with template '%s'. Please remove these before deleting the template.", constraintTemplate.getName()));
        } else {
            constraintTemplate.setName(NameUtil.postfixWithTimestamp(constraintTemplate.getName()));
            constraintTemplate.setDeleted(true);
            if (ResourceStatus.DEFAULT.equals(constraintTemplate.getStatus())) {
                constraintTemplate.setStatus(ResourceStatus.DEFAULT_DELETED);
            }
            constraintTemplateRepository.save(constraintTemplate);
        }
    }

    private boolean isRunningClusterReferToTemplate(Collection<Cluster> clusters) {
        return clusters.stream().anyMatch(c -> !c.isDeleteCompleted());
    }

    public ConstraintTemplate findByNameInAccount(String name, String account, String userId) {
        return constraintTemplateRepository.findByNameInAccount(name, account, userId);
    }
}
