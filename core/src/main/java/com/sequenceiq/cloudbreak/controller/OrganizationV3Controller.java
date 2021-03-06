package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v3.OrganizationV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.users.ChangeOrganizationUsersJson;
import com.sequenceiq.cloudbreak.api.model.users.OrganizationRequest;
import com.sequenceiq.cloudbreak.api.model.users.OrganizationResponse;
import com.sequenceiq.cloudbreak.api.model.users.OrganizationResponse.NameComparator;
import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson;
import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson.UserIdComparator;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Transactional(TxType.NEVER)
public class OrganizationV3Controller extends NotificationController implements OrganizationV3Endpoint {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private UserService userService;

    @Override
    public OrganizationResponse create(@Valid OrganizationRequest organizationRequest) {
        User user = userService.getCurrentUser();
        Organization organization = conversionService.convert(organizationRequest, Organization.class);
        organization = organizationService.create(user, organization);
        notify(ResourceEvent.ORGANIZATION_CREATED);
        return conversionService.convert(organization, OrganizationResponse.class);
    }

    @Override
    public SortedSet<OrganizationResponse> getAll() {
        Set<Organization> organizations = organizationService.retrieveForCurrentUser();
        return organizationsToSortedResponse(organizations);
    }

    @Override
    public OrganizationResponse getByName(String name) {
        Organization organization = organizationService.getByNameForCurrentUserOrThrowNotFound(name);
        return conversionService.convert(organization, OrganizationResponse.class);
    }

    @Override
    public OrganizationResponse deleteByName(String name) {
        Organization organization = organizationService.deleteByNameForCurrentUser(name);
        notify(ResourceEvent.ORGANIZATION_DELETED);
        return conversionService.convert(organization, OrganizationResponse.class);
    }

    @Override
    public SortedSet<UserResponseJson> addUsers(String orgName, @Valid Set<ChangeOrganizationUsersJson> addOrganizationUsersJson) {
        Set<User> users = organizationService.addUsers(orgName, addOrganizationUsersJson);
        return usersToSortedResponse(users);
    }

    @Override
    public SortedSet<UserResponseJson> changeUsers(String orgName, @Valid Set<ChangeOrganizationUsersJson> changeOrganizationUsersJson) {
        Set<User> users = organizationService.changeUsers(orgName, jsonToMap(changeOrganizationUsersJson));
        return usersToSortedResponse(users);
    }

    @Override
    public SortedSet<UserResponseJson> removeUsers(String orgName, @Valid Set<String> userIds) {
        Set<User> users = organizationService.removeUsers(orgName, userIds);
        return usersToSortedResponse(users);
    }

    @Override
    public SortedSet<UserResponseJson> updateUsers(String orgName, @Valid Set<ChangeOrganizationUsersJson> updateOrganizationUsersJson) {
        Set<User> users = organizationService.updateUsers(orgName, updateOrganizationUsersJson);
        return usersToSortedResponse(users);
    }

    private Map<String, Set<String>> jsonToMap(Set<ChangeOrganizationUsersJson> changeOrganizationUsersJsons) {
        return changeOrganizationUsersJsons.stream()
                .collect(Collectors.toMap(ChangeOrganizationUsersJson::getUserId, ChangeOrganizationUsersJson::getPermissions));
    }

    private SortedSet<OrganizationResponse> organizationsToSortedResponse(Set<Organization> organizations) {
        Set<OrganizationResponse> jsons = organizations.stream()
                .map(o -> conversionService.convert(o, OrganizationResponse.class))
                .collect(Collectors.toSet());

        SortedSet<OrganizationResponse> sortedResponses = new TreeSet<>(new NameComparator());
        sortedResponses.addAll(jsons);
        return sortedResponses;
    }

    private SortedSet<UserResponseJson> usersToSortedResponse(Set<User> users) {
        Set<UserResponseJson> jsons = users.stream()
                .map(u -> conversionService.convert(u, UserResponseJson.class))
                .collect(Collectors.toSet());

        SortedSet<UserResponseJson> sortedResponses = new TreeSet<>(new UserIdComparator());
        sortedResponses.addAll(jsons);
        return sortedResponses;
    }

}
