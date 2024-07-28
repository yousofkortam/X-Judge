package com.xjudge.controller.group;

import com.xjudge.entity.Group;
import com.xjudge.model.group.GroupContestModel;
import com.xjudge.model.group.GroupMemberModel;
import com.xjudge.model.group.GroupModel;
import com.xjudge.model.group.GroupRequest;
import com.xjudge.model.invitation.InvitationRequest;
import com.xjudge.service.group.GroupService;
import com.xjudge.service.group.userGroupService.UserGroupService;
import com.xjudge.util.Authentication;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
@Tag(name = "Group", description = "The group end-points for handling group operations.")
public class GroupController {

    private final GroupService groupService;
    private final UserGroupService userGroupService;

    @GetMapping("/public")
    public ResponseEntity<Page<GroupModel>> getAllGroups(Principal connectedUser, @RequestParam(defaultValue = "0") Integer pageNo,
                                                @RequestParam(defaultValue = "25") Integer size) {
        Pageable paging = PageRequest.of(pageNo, size);
        Page<GroupModel> paginatedData = groupService.getAllPublicGroups(connectedUser, paging);

        return new ResponseEntity<>(paginatedData, HttpStatus.OK);
    }

    @GetMapping("/userHandle")
    public ResponseEntity<Page<GroupModel>> getGroupsByUserHandle(
            Principal connectedUser,
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "25") Integer size
    ) {
        Authentication.checkAuthentication(connectedUser);
        Pageable paging = PageRequest.of(pageNo, size);
        String userHandle = connectedUser.getName();
        Page<GroupModel> paginatedData = groupService.getGroupsByUserHandle(connectedUser, userHandle, paging);
        return new ResponseEntity<>(paginatedData, HttpStatus.OK);
    }


    @GetMapping("/{groupId}")
    public ResponseEntity<GroupModel> getGroupById(Principal connectedUser, @PathVariable  Long groupId) {
        GroupModel group = groupService.getGroupById(groupId, connectedUser);
        return new ResponseEntity<>(group, HttpStatus.OK);
    }

    @GetMapping("/owned")
    public ResponseEntity<List<GroupModel>> getGroupsOwnedByUser(Principal connectedUser) {
        Authentication.checkAuthentication(connectedUser);
        List<GroupModel> groups = groupService.getGroupsOwnedByUser(connectedUser);
        return new ResponseEntity<>(groups, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<GroupModel> createGroup(@Valid @RequestBody GroupRequest groupRequest, Principal connectedUser) {
        Authentication.checkAuthentication(connectedUser);
        GroupModel group = groupService.create(groupRequest, connectedUser);
        return new ResponseEntity<>(group, HttpStatus.OK);
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("@groupSecurity.hasAnyRole(principal.username, #groupId, {'LEADER', 'MANAGER'})")
    public ResponseEntity<GroupModel> updateGroup(@PathVariable Long groupId, @Valid @RequestBody GroupRequest groupRequest) {
        GroupModel group = groupService.update(groupId, groupRequest);
        return new ResponseEntity<>(group, HttpStatus.OK);
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("@groupSecurity.hasRole(principal.username, #groupId, 'LEADER')")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId) {
        groupService.delete(groupId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/invite")
    @PreAuthorize("@groupSecurity.hasAnyRole(principal.username, #invitationRequest.groupId, {'LEADER','MANAGER'})")
    public ResponseEntity<Void> inviteUserToGroup(@Valid @RequestBody InvitationRequest invitationRequest, Principal connectedUser) {
        Authentication.checkAuthentication(connectedUser);
        groupService.inviteUser(invitationRequest.getGroupId(), invitationRequest.getReceiverHandle(), connectedUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/accept-invitation/{invitationId}")
    public ResponseEntity<Void> acceptInvitation(@PathVariable Long invitationId, Principal connectedUser) {
        Authentication.checkAuthentication(connectedUser);
        groupService.acceptInvitation(invitationId, connectedUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/decline-invitation/{invitationId}")
    public ResponseEntity<Void> declineInvitation(@PathVariable Long invitationId, Principal connectedUser) {
        Authentication.checkAuthentication(connectedUser);
        groupService.declineInvitation(invitationId, connectedUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/request-join/{groupId}")
    public ResponseEntity<Void> requestJoin(@PathVariable Long groupId, Principal connectedUser) {
        Authentication.checkAuthentication(connectedUser);
        groupService.requestJoin(groupId, connectedUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/accept-request/{requestId}")
    @PreAuthorize("@groupSecurity.hasAnyRole(principal.username, #requestId, {'LEADER','MANAGER'})")
    public ResponseEntity<Void> acceptRequest(@PathVariable Long requestId) {
        groupService.acceptRequest(requestId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/decline-request/{requestId}")
    @PreAuthorize("@groupSecurity.hasAnyRole(principal.username, #requestId, {'LEADER','MANAGER'})")
    public ResponseEntity<Void> declineRequest(@PathVariable Long requestId) {
        groupService.declineRequest(requestId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{groupId}/join") // request private group join
    public ResponseEntity<Void> joinUserGroup(@PathVariable Long groupId, Principal connectedUser) {
        Authentication.checkAuthentication(connectedUser);
        groupService.join(groupId, connectedUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{groupId}/leave") // security handled in user group service layer ✅
    public ResponseEntity<Void> leaveUserGroup(@PathVariable Long groupId, Principal connectedUser) {
        Authentication.checkAuthentication(connectedUser);
        groupService.leave(groupId, connectedUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/{groupId}/contests")
    @PreAuthorize("@groupSecurity.isPublic(#groupId)")
    public ResponseEntity<List<GroupContestModel>> getGroupContests(@PathVariable Long groupId) {
        List<GroupContestModel> groupContests = groupService.getGroupContests(groupId);
        return new ResponseEntity<>(groupContests, HttpStatus.OK);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<Page<GroupMemberModel>> getGroupMembers(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "15") Integer size) {
        Pageable paging = PageRequest.of(pageNo, size);
        Page<GroupMemberModel> groupMembers = groupService.getGroupMembers(groupId, paging);
        return new ResponseEntity<>(groupMembers, HttpStatus.OK);
    }

    @GetMapping("/userRole/{groupId}")
   public String getUserRole(Principal connectedUser, @PathVariable Long groupId){
        Authentication.checkAuthentication(connectedUser);
        return userGroupService.findRoleByUserAndGroupId(connectedUser,groupId);
   }

    @GetMapping("/search")
    public ResponseEntity<Page<Group>> searchByName(
            @RequestParam(defaultValue = "", required = false) String name,
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "25") Integer size) {
        Pageable paging = PageRequest.of(pageNo, size);
        Page<Group> paginatedData = groupService.searchGroupByName(name, paging);
        return new ResponseEntity<>(paginatedData, HttpStatus.OK);
    }

}
