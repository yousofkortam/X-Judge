package com.xjudge.controller.user;

import com.xjudge.model.invitation.InvitationModel;
import com.xjudge.model.user.UserModel;
import com.xjudge.service.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/user")
@Tag(name = "User", description = "The end-points related to user operations.")
public class UserController {

    private final UserService userService;

    @GetMapping("/{handle}")
    public ResponseEntity<UserModel> getUserByHandle(@PathVariable String handle) {
        UserModel user = userService.findUserModelByHandle(handle);
        return ResponseEntity.ok(user);
    }

    @PutMapping
    public ResponseEntity<UserModel> updateUser(@Valid @RequestBody UserModel user, Authentication authentication) {
        com.xjudge.util.Authentication.checkAuthentication(authentication);
        return ResponseEntity.ok(userService.updateUserByHandle(authentication.getName(), user));
    }

    @PutMapping("/profile-picture")
    public ResponseEntity<Boolean> updateProfilePicture(@RequestParam("file") MultipartFile profilePicture, Principal connectedUser) {
        com.xjudge.util.Authentication.checkAuthentication(connectedUser);
        return ResponseEntity.ok(userService.updateProfilePicture(connectedUser.getName(), profilePicture));
    }

    @GetMapping("/invitations")
    public ResponseEntity<List<InvitationModel>> GetUserInvitation(Principal connectedUser) {
        com.xjudge.util.Authentication.checkAuthentication(connectedUser);
        return ResponseEntity.ok(userService.getUserInvitations(connectedUser.getName()));
    }

}
