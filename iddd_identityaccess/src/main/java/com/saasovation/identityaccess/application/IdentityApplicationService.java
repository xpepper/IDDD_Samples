//   Copyright 2012,2013 Vaughn Vernon
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package com.saasovation.identityaccess.application;

import com.saasovation.identityaccess.application.command.*;
import com.saasovation.identityaccess.domain.model.identity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class IdentityApplicationService {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private GroupMemberService groupMemberService;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private TenantProvisioningService tenantProvisioningService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    public IdentityApplicationService() {
        super();

        // IdentityAccessEventProcessor.register();
    }

    @Transactional
    public void activateTenant(ActivateTenantCommand aCommand) {
        Tenant tenant = existingTenant(aCommand.getTenantId());

        tenant.activate();
    }

    @Transactional
    public void addGroupToGroup(AddGroupToGroupCommand aCommand) {
        Group parentGroup =
            existingGroup(
                aCommand.getTenantId(),
                aCommand.getParentGroupName());

        Group childGroup =
            existingGroup(
                aCommand.getTenantId(),
                aCommand.getChildGroupName());

        parentGroup.addGroup(childGroup, groupMemberService());
    }

    @Transactional
    public void addUserToGroup(AddUserToGroupCommand aCommand) {
        Group group =
            existingGroup(
                aCommand.getTenantId(),
                aCommand.getGroupName());

        User user =
            existingUser(
                aCommand.getTenantId(),
                aCommand.getUsername());

        group.addUser(user);
    }

    @Transactional(readOnly = true)
    public UserDescriptor authenticateUser(AuthenticateUserCommand aCommand) {

        return authenticationService()
            .authenticate(
                new TenantId(aCommand.getTenantId()),
                aCommand.getUsername(),
                aCommand.getPassword());
    }

    @Transactional
    public void deactivateTenant(DeactivateTenantCommand aCommand) {
        Tenant tenant = existingTenant(aCommand.getTenantId());

        tenant.deactivate();
    }

    @Transactional
    public void changeUserContactInformation(ChangeContactInfoCommand aCommand) {
        User user = existingUser(aCommand.getTenantId(), aCommand.getUsername());

        internalChangeUserContactInformation(
            user,
            new ContactInformation(
                new EmailAddress(aCommand.getEmailAddress()),
                new PostalAddress(
                    aCommand.getAddressStreetAddress(),
                    aCommand.getAddressCity(),
                    aCommand.getAddressStateProvince(),
                    aCommand.getAddressPostalCode(),
                    aCommand.getAddressCountryCode()),
                new Telephone(aCommand.getPrimaryTelephone()),
                new Telephone(aCommand.getSecondaryTelephone())));
    }

    @Transactional
    public void changeUserEmailAddress(ChangeEmailAddressCommand aCommand) {
        User user = existingUser(aCommand.getTenantId(), aCommand.getUsername());

        internalChangeUserContactInformation(
            user,
            user.person()
                .contactInformation()
                .changeEmailAddress(new EmailAddress(aCommand.getEmailAddress())));
    }

    @Transactional
    public void changeUserPostalAddress(ChangePostalAddressCommand aCommand) {
        User user = existingUser(aCommand.getTenantId(), aCommand.getUsername());

        internalChangeUserContactInformation(
            user,
            user.person()
                .contactInformation()
                .changePostalAddress(
                    new PostalAddress(
                        aCommand.getAddressStreetAddress(),
                        aCommand.getAddressCity(),
                        aCommand.getAddressStateProvince(),
                        aCommand.getAddressPostalCode(),
                        aCommand.getAddressCountryCode())));
    }

    @Transactional
    public void changeUserPrimaryTelephone(ChangePrimaryTelephoneCommand aCommand) {
        User user = existingUser(aCommand.getTenantId(), aCommand.getUsername());

        internalChangeUserContactInformation(
            user,
            user.person()
                .contactInformation()
                .changePrimaryTelephone(new Telephone(aCommand.getTelephone())));
    }

    @Transactional
    public void changeUserSecondaryTelephone(ChangeSecondaryTelephoneCommand aCommand) {
        User user = existingUser(aCommand.getTenantId(), aCommand.getUsername());

        internalChangeUserContactInformation(
            user,
            user.person()
                .contactInformation()
                .changeSecondaryTelephone(new Telephone(aCommand.getTelephone())));
    }

    @Transactional
    public void changeUserPassword(ChangeUserPasswordCommand aCommand) {
        User user = existingUser(aCommand.getTenantId(), aCommand.getUsername());

        user.changePassword(aCommand.getCurrentPassword(), aCommand.getChangedPassword());
    }

    @Transactional
    public void changeUserPersonalName(ChangeUserPersonalNameCommand aCommand) {
        User user = existingUser(aCommand.getTenantId(), aCommand.getUsername());

        user.person().changeName(new FullName(aCommand.getFirstName(), aCommand.getLastName()));
    }

    @Transactional
    public void defineUserEnablement(DefineUserEnablementCommand aCommand) {
        User user = existingUser(aCommand.getTenantId(), aCommand.getUsername());

        user.defineEnablement(
            new Enablement(
                aCommand.isEnabled(),
                aCommand.getStartDate(),
                aCommand.getEndDate()));
    }

    @Transactional(readOnly = true)
    public Group group(String aTenantId, String aGroupName) {
        return groupRepository()
            .groupNamed(new TenantId(aTenantId), aGroupName);
    }

    @Transactional(readOnly = true)
    public boolean isGroupMember(String aTenantId, String aGroupName, String aUsername) {
        Group group =
            existingGroup(
                aTenantId,
                aGroupName);

        User user =
            existingUser(
                aTenantId,
                aUsername);

        return group.isMember(user, groupMemberService());
    }

    @Transactional
    public Group provisionGroup(ProvisionGroupCommand aCommand) {
        Tenant tenant = existingTenant(aCommand.getTenantId());

        Group group =
            tenant.provisionGroup(
                aCommand.getGroupName(),
                aCommand.getDescription());

        groupRepository().add(group);

        return group;
    }

    @Transactional
    public Tenant provisionTenant(ProvisionTenantCommand aCommand) {

        return
            tenantProvisioningService().provisionTenant(
                aCommand.getTenantName(),
                aCommand.getTenantDescription(),
                new FullName(
                    aCommand.getAdministorFirstName(),
                    aCommand.getAdministorLastName()),
                new EmailAddress(aCommand.getEmailAddress()),
                new PostalAddress(
                    aCommand.getAddressStateProvince(),
                    aCommand.getAddressCity(),
                    aCommand.getAddressStateProvince(),
                    aCommand.getAddressPostalCode(),
                    aCommand.getAddressCountryCode()),
                new Telephone(aCommand.getPrimaryTelephone()),
                new Telephone(aCommand.getSecondaryTelephone()));
    }

    @Transactional
    public User registerUser(RegisterUserCommand aCommand) {
        Tenant tenant = existingTenant(aCommand.getTenantId());

        User user =
            tenant.registerUser(
                aCommand.getInvitationIdentifier(),
                aCommand.getUsername(),
                aCommand.getPassword(),
                new Enablement(
                    aCommand.isEnabled(),
                    aCommand.getStartDate(),
                    aCommand.getEndDate()),
                new Person(
                    new TenantId(aCommand.getTenantId()),
                    new FullName(aCommand.getFirstName(), aCommand.getLastName()),
                    new ContactInformation(
                        new EmailAddress(aCommand.getEmailAddress()),
                        new PostalAddress(
                            aCommand.getAddressStateProvince(),
                            aCommand.getAddressCity(),
                            aCommand.getAddressStateProvince(),
                            aCommand.getAddressPostalCode(),
                            aCommand.getAddressCountryCode()),
                        new Telephone(aCommand.getPrimaryTelephone()),
                        new Telephone(aCommand.getSecondaryTelephone()))));

        if (user == null) {
            throw new IllegalStateException("User not registered.");
        }

        userRepository().add(user);

        return user;
    }

    @Transactional
    public void removeGroupFromGroup(RemoveGroupFromGroupCommand aCommand) {
        Group parentGroup =
            existingGroup(
                aCommand.getTenantId(),
                aCommand.getParentGroupName());

        Group childGroup =
            existingGroup(
                aCommand.getTenantId(),
                aCommand.getChildGroupName());

        parentGroup.removeGroup(childGroup);
    }

    @Transactional
    public void removeUserFromGroup(RemoveUserFromGroupCommand aCommand) {
        Group group =
            existingGroup(
                aCommand.getTenantId(),
                aCommand.getGroupName());

        User user =
            existingUser(
                aCommand.getTenantId(),
                aCommand.getUsername());

        group.removeUser(user);
    }

    @Transactional(readOnly = true)
    public Tenant tenant(String aTenantId) {
        return tenantRepository().tenantOfId(new TenantId(aTenantId));
    }

    @Transactional(readOnly = true)
    public User user(String aTenantId, String aUsername) {
        return userRepository().userWithUsername(new TenantId(aTenantId), aUsername);
    }

    @Transactional(readOnly = true)
    public UserDescriptor userDescriptor(
        String aTenantId,
        String aUsername) {

        UserDescriptor userDescriptor = null;

        User user = user(aTenantId, aUsername);

        if (user != null) {
            userDescriptor = user.userDescriptor();
        }

        return userDescriptor;
    }

    private AuthenticationService authenticationService() {
        return authenticationService;
    }

    private Group existingGroup(String aTenantId, String aGroupName) {
        Group group = group(aTenantId, aGroupName);

        if (group == null) {
            throw new IllegalArgumentException(
                "Group does not exist for: "
                    + aTenantId + " and: " + aGroupName);
        }

        return group;
    }

    private Tenant existingTenant(String aTenantId) {
        Tenant tenant = tenant(aTenantId);

        if (tenant == null) {
            throw new IllegalArgumentException(
                "Tenant does not exist for: " + aTenantId);
        }

        return tenant;
    }

    private User existingUser(String aTenantId, String aUsername) {
        User user = user(aTenantId, aUsername);

        if (user == null) {
            throw new IllegalArgumentException(
                "User does not exist for: "
                    + aTenantId + " and " + aUsername);
        }

        return user;
    }

    private GroupMemberService groupMemberService() {
        return groupMemberService;
    }

    private GroupRepository groupRepository() {
        return groupRepository;
    }

    private void internalChangeUserContactInformation(
        User aUser,
        ContactInformation aContactInformation) {

        if (aUser == null) {
            throw new IllegalArgumentException("User must exist.");
        }

        aUser.person().changeContactInformation(aContactInformation);
    }

    private TenantProvisioningService tenantProvisioningService() {
        return tenantProvisioningService;
    }

    private TenantRepository tenantRepository() {
        return tenantRepository;
    }

    private UserRepository userRepository() {
        return userRepository;
    }
}
