package com.sequenceiq.cloudbreak.service.user;

import static com.sequenceiq.cloudbreak.api.model.v2.OrganizationStatus.ACTIVE;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.Tenant;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.organization.TenantRepository;
import com.sequenceiq.cloudbreak.repository.organization.UserRepository;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Service
public class UserService {

    @Inject
    private UserRepository userRepository;

    @Inject
    private TenantRepository tenantRepository;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    public User getCurrentUser() {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        return getOrCreate(identityUser);
    }

    public User getOrCreate(IdentityUser identityUser) {
        if (identityUser == null) {
            throw new NullIdentityUserException();
        }
        try {
            return transactionService.requiresNew(() -> {
                User user = userRepository.findByUserId(identityUser.getUsername());
                if (user == null) {
                    user = new User();
                    user.setUserId(identityUser.getUsername());

                    Tenant tenant = tenantRepository.findByName("DEFAULT");
                    user.setTenant(tenant);
                    user.setTenantPermissionSet(Collections.emptySet());
                    user = userRepository.save(user);

                    //create organization
                    Organization organization = new Organization();
                    organization.setTenant(tenant);
                    organization.setName(identityUser.getUsername());
                    organization.setStatus(ACTIVE);
                    organization.setDescription("Default organization for the user.");
                    organizationService.create(user, organization);
                }
                return user;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    public User getByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    public Set<User> getByUsersIds(Set<String> userIds) {
        return userRepository.findByUserIdIn(userIds);
    }

    public Set<User> getAll(IdentityUser identityUser) {
        User user = userRepository.findByUserId(identityUser.getUsername());
        return userRepository.findAllByTenant(user.getTenant());
    }
}
