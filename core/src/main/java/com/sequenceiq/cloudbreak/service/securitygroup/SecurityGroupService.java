package com.sequenceiq.cloudbreak.service.securitygroup;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.SecurityGroupRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.util.NameUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

@Service
public class SecurityGroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityGroupService.class);

    @Inject
    private SecurityGroupRepository groupRepository;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private AuthorizationService authorizationService;

    public SecurityGroup create(IdentityUser user, SecurityGroup securityGroup) {
        LOGGER.info("Creating SecurityGroup: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        securityGroup.setOwner(user.getUserId());
        securityGroup.setAccount(user.getAccount());
        try {
            return groupRepository.save(securityGroup);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.SECURITY_GROUP, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public SecurityGroup get(Long id) {
        SecurityGroup securityGroup = groupRepository.findById(id);
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found", id));
        }
        authorizationService.hasReadPermission(securityGroup);
        return securityGroup;
    }

    public SecurityGroup getPrivateSecurityGroup(String name, IdentityUser user) {
        SecurityGroup securityGroup = groupRepository.findByNameForUser(name, user.getUserId());
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found for user", name));
        }
        return securityGroup;
    }

    public SecurityGroup getPublicSecurityGroup(String name, IdentityUser user) {
        SecurityGroup securityGroup = groupRepository.findByNameInAccount(name, user.getAccount());
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found in account", name));
        }
        return securityGroup;
    }

    public void delete(Long id, IdentityUser user) {
        LOGGER.info("Deleting SecurityGroup with id: {}", id);
        delete(get(id));
    }

    public void delete(String name, IdentityUser user) {
        LOGGER.info("Deleting SecurityGroup with name: {}", name);
        SecurityGroup securityGroup = groupRepository.findByNameInAccount(name, user.getAccount());
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found.", name));
        }
        delete(securityGroup);
    }

    public Set<SecurityGroup> retrievePrivateSecurityGroups(IdentityUser user) {
        return groupRepository.findForUser(user.getUserId());
    }

    public Set<SecurityGroup> retrieveAccountSecurityGroups(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? groupRepository.findAllInAccount(user.getAccount())
                : groupRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
    }

    private void delete(SecurityGroup securityGroup) {
        authorizationService.hasWritePermission(securityGroup);
        List<InstanceGroup> instanceGroupsWithThisSecurityGroup = new ArrayList<>(instanceGroupRepository.findBySecurityGroup(securityGroup));
        if (!instanceGroupsWithThisSecurityGroup.isEmpty()) {
            if (instanceGroupsWithThisSecurityGroup.size() > 1) {
                String clusters = instanceGroupsWithThisSecurityGroup
                        .stream()
                        .map(instanceGroup -> instanceGroup.getStack().getCluster().getName())
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with SecurityGroup '%s'(ID:'%s'). Please remove these before deleting the SecurityGroup. "
                                + "The following clusters are using this SecurityGroup: [%s]",
                        securityGroup.getName(), securityGroup.getId(), clusters));
            } else {
                throw new BadRequestException(String.format("There is a cluster ['%s'] which uses SecurityGroup '%s'(ID:'%s'). Please remove this "
                                + "cluster before deleting the SecurityGroup",
                        instanceGroupsWithThisSecurityGroup.get(0).getStack().getCluster().getName(), securityGroup.getName(), securityGroup.getId()));
            }
        }
        if (ResourceStatus.USER_MANAGED.equals(securityGroup.getStatus())) {
            groupRepository.delete(securityGroup);
        } else {
            securityGroup.setName(NameUtil.postfixWithTimestamp(securityGroup.getName()));
            securityGroup.setStatus(ResourceStatus.DEFAULT_DELETED);
            groupRepository.save(securityGroup);
        }
    }
}
