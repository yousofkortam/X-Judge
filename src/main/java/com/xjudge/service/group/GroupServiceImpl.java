package com.xjudge.service.group;

import com.xjudge.entity.*;
import com.xjudge.entity.key.UserGroupKey;
import com.xjudge.exception.XJudgeException;
import com.xjudge.mapper.GroupMapper;
import com.xjudge.mapper.UserGroupMapper;
import com.xjudge.model.enums.GroupVisibility;
import com.xjudge.model.enums.InvitationStatus;
import com.xjudge.model.enums.UserGroupRole;
import com.xjudge.model.group.GroupContestModel;
import com.xjudge.model.group.GroupMemberModel;
import com.xjudge.model.group.GroupModel;
import com.xjudge.model.group.GroupRequest;
import com.xjudge.repository.GroupRepository;
import com.xjudge.repository.UserGroupRepository;
import com.xjudge.service.group.joinRequest.JoinRequestService;
import com.xjudge.service.group.userGroupService.UserGroupService;
import com.xjudge.service.invitiation.InvitationService;
import com.xjudge.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final UserService userService;
    private final InvitationService invitationService;
    private final UserGroupService userGroupService;
    private final GroupMapper groupMapper;
    private final JoinRequestService joinRequestService;
    private final UserGroupMapper userGroupMapper;
    private final UserGroupRepository userGroupRepository;

    private Page<GroupModel> getGroupModelPage(Page<Group> groups, Principal connectedUser) {
        return groups.map(group -> GroupModel.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .creationDate(group.getCreationDate())
                .visibility(group.getVisibility())
                .leaderHandle(group.getLeaderHandle())
                .userGroupRole(
                        connectedUser != null
                                ? userGroupService.findRoleByUserAndGroupId(connectedUser, group.getId())
                                : "")
                .members(group.getGroupUsers().size())
                .build());
    }

    @Override
    public Page<GroupModel> getAllGroups(Principal connectedUser, Pageable pageable) {
        Page<Group> groups = groupRepository.findAll(pageable);
        return getGroupModelPage(groups, connectedUser);
    }

    @Override
    public Page<GroupModel> getAllPublicGroups(Principal connectedUser, Pageable pageable) {
        Page<Group> groups = groupRepository.findByVisibility(GroupVisibility.PUBLIC ,pageable);
        return getGroupModelPage(groups, connectedUser);
    }

    @Override
    public Page<GroupModel> getGroupsByUserHandle(Principal connectedUser,String handle, Pageable pageable) {
        User user = userService.findUserByHandle(handle);
        Page<Group> groups = groupRepository.findGroupsByGroupUsersUser(user, pageable);
        return groups.map(group -> GroupModel.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .creationDate(group.getCreationDate())
                .visibility(group.getVisibility())
                .leaderHandle(group.getLeaderHandle())
                .members(group.getGroupUsers().size())
                .userGroupRole(userGroupService.findRoleByUserAndGroupId(connectedUser,group.getId()))
                .build());
    }


    @Override
    public GroupModel getSpecificGroup(Long id) {
        Group group = groupRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("Group not found")
        );
        return groupMapper.toModel(group, group.getGroupUsers().size());
    }

    @Override
    public GroupModel getGroupById(Long id, Principal connectedUser) {
        boolean isAuthenticated = connectedUser != null;
        if (!isAuthenticated) {
            return groupMapper.toModel(groupRepository.findById(id).orElseThrow(
                    () -> new NoSuchElementException("Group not found")
            ));
        }
        Group group = groupRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("Group not found")
        );
        UserGroup userGroup = userGroupService.findByUserHandleAndGroupIdOrElseNull(connectedUser.getName(), id);
        boolean isMember = userGroup != null;
        boolean isLeader = isMember && userGroup.getRole() == UserGroupRole.LEADER;


        return groupMapper.toModel(group, group.getGroupUsers().size(), isMember, isLeader, connectedUser.getName());
    }


    @Override
    @Transactional
    public GroupModel create(GroupRequest groupRequest, Principal connectedUser) {
        User leader = userService.findUserByHandle(connectedUser.getName());
        Group group = groupRepository.save(Group.builder()
                .name(groupRequest.getName())
                .description(groupRequest.getDescription())
                .visibility(groupRequest.getVisibility())
                .leaderHandle(connectedUser.getName())
                .creationDate(LocalDate.now())
                .build());

        UserGroupKey userGroupKey = new UserGroupKey(leader.getId(), group.getId());

        userGroupService.save(UserGroup.builder()
                .id(userGroupKey)
                .user(leader)
                .group(group)
                .joinDate(LocalDate.now())
                .role(UserGroupRole.LEADER)
                .build());
        return groupMapper.toModel(group);
    }


    @Override
    public GroupModel update(Long groupId, GroupRequest groupRequest) {
        return groupMapper.toModel(
                groupRepository.findById(groupId)
                        .map(group -> {
                            group.setName(groupRequest.getName());
                            group.setDescription(groupRequest.getDescription());
                            group.setVisibility(groupRequest.getVisibility());
                            return groupRepository.save(group);
                        }).orElseThrow(
                                () -> new NoSuchElementException("Group not found")
                        ));
    }

    @Override
    public void delete(Long groupId) {
        groupRepository.findById(groupId)
                .ifPresentOrElse(groupRepository::delete,
                        () -> {
                            throw new NoSuchElementException("Group not found");
                        });
    }

    @Override
    public void inviteUser(Long groupId, String receiverHandle, Principal connectedUser) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new NoSuchElementException("Group not found")
        );

        User receiver = userService.findUserByHandle(receiverHandle);
        User sender = (User) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

        if (sender.getId().equals(receiver.getId())) {
            throw new XJudgeException("User cannot invite himself", HttpStatus.FORBIDDEN);
        }

        invitationService.save(Invitation.builder()
                .receiver(receiver)
                .sender(sender)
                .group(group)
                .date(LocalDate.now())
                .status(InvitationStatus.PENDING)
                .build());
    }

    @Override
    public void join(Long groupId, Principal connectedUser) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new NoSuchElementException("Group not found")
        );
        User user = userService.findUserByHandle(connectedUser.getName());
        if (isPrivate(group)) {
            throw new XJudgeException("Group is private", HttpStatus.FORBIDDEN);
        }
        // Check if the user is not already in the group
        if (userGroupService.existsByUserAndGroup(user, group)) {
            throw new XJudgeException("User is already in the group", HttpStatus.ALREADY_REPORTED);
        }
        UserGroupKey userGroupKey = new UserGroupKey(user.getId(), groupId);

        userGroupService.save(UserGroup.builder()
                .user(user)
                .group(group)
                .id(userGroupKey)
                .joinDate(LocalDate.now())
                .role(UserGroupRole.MEMBER).build());
    }

    @Override
    public void join(GroupModel groupModel, User user) {
        // Check if the user is not already in the group
        Group group = groupMapper.toEntity(groupModel);
        if (userGroupService.existsByUserAndGroup(user, group)) {
            throw new XJudgeException("User is already in the group", HttpStatus.ALREADY_REPORTED);
        }
        UserGroupKey userGroupKey = new UserGroupKey(user.getId(), group.getId());

        userGroupService.save(UserGroup.builder()
                .user(user)
                .group(group)
                .id(userGroupKey)
                .joinDate(LocalDate.now())
                .role(UserGroupRole.MEMBER).build());
    }

    @Override
    public void requestJoin(Long groupId, Principal connectedUser) {
        User user = (User) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new NoSuchElementException("Group not found")
        );
        JoinRequest joinRequest = JoinRequest.builder()
                .group(group)
                .user(user)
                .status(InvitationStatus.PENDING)
                .date(LocalDate.now())
                .build();
        this.joinRequestService.save(joinRequest);
    }

    @Override
    public void acceptRequest(Long requestId) {
        JoinRequest joinRequest = joinRequestService.findById(requestId);
        if (joinRequest.getStatus() != InvitationStatus.PENDING) {
            throw new XJudgeException("Invalid request", HttpStatus.BAD_REQUEST);
        }
        joinRequest.setStatus(InvitationStatus.ACCEPTED);
        joinRequestService.save(joinRequest);
    }

    @Override
    public void declineRequest(Long requestId) {
        JoinRequest joinRequest = joinRequestService.findById(requestId);
        if (joinRequest.getStatus() != InvitationStatus.PENDING) {
            throw new XJudgeException("Invalid request", HttpStatus.BAD_REQUEST);
        }
        joinRequest.setStatus(InvitationStatus.DECLINED);
        joinRequestService.save(joinRequest);
    }

    @Override
    public void leave(Long groupId, Principal connectedUser) {
        UserGroup userGroup = userGroupService.findByUserHandleAndGroupId(connectedUser.getName(), groupId);
        if (userGroup.getRole() == UserGroupRole.LEADER) {
            throw new XJudgeException("Leader cannot leave the group", HttpStatus.FORBIDDEN);
        }
        userGroupService.delete(userGroup);
    }

    @Override
    public List<GroupContestModel> getGroupContests(Long groupId) {
        List<GroupContestModel> contests = groupRepository.findById(groupId).orElseThrow(
                () -> new NoSuchElementException("Group not found"))
                .getGroupContests()
                .stream()
                .map(groupMapper::toGroupContestModel)
                .collect(Collectors.toList());
        Collections.reverse(contests);
        return contests;
    }

    @Override
    public Page<GroupMemberModel> getGroupMembers(Long groupId, Pageable pageable) {
        return userGroupRepository.findByGroupId(groupId, pageable)
                .map(userGroupMapper::toGroupMemberModel);
    }

    @Override
    public List<GroupModel> getGroupsOwnedByUser(Principal connectedUser) {
        User user = userService.findUserByHandle(connectedUser.getName());
        return userGroupService.findAllByUserAndRole(user).stream()
                .map(userGroup -> groupMapper.toModel(userGroup.getGroup()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isPublic(Group group) {
        return group.getVisibility() == GroupVisibility.PUBLIC;
    }

    @Override
    public boolean isPrivate(Group group) {
        return !isPublic(group);
    }

    private void getDataAndSetInvitationStatus(Long invitationId, String userHandle, InvitationStatus status) {
        Invitation invitation = invitationService.findById(invitationId);
        User user = userService.findUserByHandle(userHandle);
        if (!invitation.getReceiver().equals(user)) {
            throw new XJudgeException("User is not the receiver of the invitation", HttpStatus.FORBIDDEN);
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new XJudgeException("Invitation is not pending", HttpStatus.FORBIDDEN);
        }
        invitation.setStatus(status);
        invitationService.save(invitation);
        if (InvitationStatus.ACCEPTED.equals(status)) join(groupMapper.toModel(invitation.getGroup()), user);
    }

    @Override
    @Transactional
    public void acceptInvitation(Long invitationId, Principal connectedUser) {
        getDataAndSetInvitationStatus(invitationId, connectedUser.getName(), InvitationStatus.ACCEPTED);
    }

    @Override
    public void declineInvitation(Long invitationId, Principal connectedUser) {
        getDataAndSetInvitationStatus(invitationId, connectedUser.getName(), InvitationStatus.DECLINED);
    }
    @Override
    public Page<Group> searchGroupByName(String name, Pageable pageable) {
        return groupRepository.searchGroupsByNameContainingIgnoreCaseAndVisibility(name, GroupVisibility.PUBLIC, pageable);
    }

}
