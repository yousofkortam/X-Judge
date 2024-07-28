package com.xjudge.service.user;

import com.xjudge.entity.Token;
import com.xjudge.entity.User;
import com.xjudge.exception.XJudgeException;
import com.xjudge.mapper.UserMapper;
import com.xjudge.model.enums.TokenType;
import com.xjudge.model.invitation.InvitationModel;
import com.xjudge.model.user.UserModel;
import com.xjudge.repository.UserRepo;
import com.xjudge.service.email.EmailService;
import com.xjudge.service.invitiation.InvitationService;
import com.xjudge.service.token.TokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{
    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final TokenService tokenService;
    private final InvitationService invitationService;

    @Override
    public User save(User user) {
        return userRepo.save(user);
    }

    @Override
    public User findUserByHandle(String userHandle) {
        return userRepo.findByHandle(userHandle).orElseThrow(
                () -> new NoSuchElementException("There's no handle {" + userHandle + "}")
        );
    }

    @Override
    public UserModel findUserModelByHandle(String userHandle) {
        return userMapper.toModel(this.findUserByHandle(userHandle));
    }

    @Override
    public User findUserByEmail(String userEmail) {
        return userRepo.findUserByEmail(userEmail).orElseThrow(
                () -> new NoSuchElementException("There's no email {" + userEmail + "}")
        );
    }

    @Override
    public UserModel findUserModelByEmail(String userEmail) {
        return userMapper.toModel(this.findUserByEmail(userEmail));
    }

    @Override
    public User findUserById(Long userId) {
        return userRepo.findById(userId).orElseThrow(
                () -> new NoSuchElementException("There's no user with id {" + userId + "}")
        );
    }

    @Override
    public UserModel findUserModelById(Long userId) {
        return userMapper.toModel(this.findUserById(userId));
    }

    @Override
    public UserModel updateUser(Long id, UserModel user) {
        User oldUser = userRepo.findById(id).orElseThrow(
                () -> new NoSuchElementException("User not found")
        );
        oldUser.setHandle(user.getHandle());
        oldUser.setFirstName(user.getFirstName());
        oldUser.setLastName(user.getLastName());
        oldUser.setEmail(user.getEmail());
        oldUser.setSchool(user.getSchool());
        oldUser.setPhotoUrl(user.getPhotoUrl());
        return userMapper.toModel(oldUser);
    }

    @Override
    @Transactional
    public UserModel updateUserByHandle(String handle, UserModel user) {
        User oldUser = this.findUserByHandle(handle);
        oldUser.setFirstName(user.getFirstName());
        oldUser.setLastName(user.getLastName());
        oldUser.setSchool(user.getSchool());

        if (!oldUser.getEmail().equals(user.getEmail())) {
            if (userRepo.existsByEmail(user.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }
            oldUser.setIsVerified(false);
            oldUser.setEmail(user.getEmail());
            generateTokenAndSendEmail(oldUser);
        }

        return userMapper.toModel(
                userRepo.save(oldUser)
        );
    }

    @Override
    public void deleteUser(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(
                () -> new NoSuchElementException("User not found")
        );
        userRepo.delete(user);
    }

    @Override
    public List<UserModel> getAllUsers() {
        return userRepo.findAll().stream()
                .map(userMapper::toModel)
                .toList();
    }

    @Override
    public boolean existsByHandle(String userHandle) {
        return userRepo.existsByHandle(userHandle);
    }

    @Override
    public boolean existsByEmail(String userEmail) {
        return userRepo.existsByEmail(userEmail);
    }

    @Override
    public List<InvitationModel> getUserInvitations(String handle) {
        return invitationService.getInvitationByReceiverHandle(handle);
    }

    @Override
    public boolean updateProfilePicture(String handle, MultipartFile profilePicture) {
        User user = this.findUserByHandle(handle);
        String uploadDirectory = "src/main/resources/static/images/profiles/";
        String originalFileName = handle + UUID.randomUUID() + profilePicture.getOriginalFilename();
        Path filePath = Paths.get(uploadDirectory, originalFileName);
        try {
            profilePicture.transferTo(filePath);
            user.setPhotoUrl("http://localhost:7070/profile/" + originalFileName);
            userRepo.save(user);
            return true;
        } catch (Exception e) {
            throw new XJudgeException("Cannot upload profile picture", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public void generateTokenAndSendEmail(User user) {
        String verificationToken = UUID.randomUUID().toString() + '-' + UUID.randomUUID();
        tokenService.save(Token.builder()
                .token(verificationToken)
                .user(user)
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .verifiedAt(null)
                .build());
        String emailContent = "<div style='font-family: Arial, sans-serif; width: 80%; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>"
                + "<div style='text-align: center; padding: 10px; background-color: #f8f8f8; border-bottom: 1px solid #ddd;'>"
                + "<h1>Email Change Request</h1>"
                + "</div>"
                + "<div style='padding: 20px;'>"
                + "<p>Dear " + user.getUsername() + ",</p>"
                + "<p>e received a request to reset your password. Please click the link below to verify your email:<p>"
                + "<p><a href='http://localhost:7070/auth/verify-email?token=" + verificationToken + "'>Verify Email</a></p>"
                + "<p>If you did not register at XJudge, please ignore this email.</p>"
                + "<p>Best Regards,</p>"
                + "<p>The XJudge Team</p>"
                + "</div>"
                + "</div>";
        emailService.send(user.getEmail(), "Email Change Request", emailContent);
    }
}
