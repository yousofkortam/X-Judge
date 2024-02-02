package com.xjudge.service.group;

import com.xjudge.entity.Contest;
import com.xjudge.entity.Group;
import com.xjudge.entity.User;
import com.xjudge.enums.GroupVisibility;
import com.xjudge.exception.SubmitException;
import com.xjudge.model.group.GroupRequest;
import com.xjudge.repository.ContestRepository;
import com.xjudge.repository.GroupRepository;
import com.xjudge.repository.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class groupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final UserRepo userRepository;
    private final ContestRepository contestRepository;

    @Override
    public List<Group> publicGroups() {
        return groupRepository.findByGroupVisibility(GroupVisibility.PUBLIC);
    }

    @Override
    public Group getSpecificGroup(Long id) {
        return groupRepository.findById(id).orElse(null);
    }


    @Override
    public Group create(GroupRequest groupRequest) {
        Group group=new Group();
        group.setGroupName(groupRequest.getName());
        group.setGroupDescription(groupRequest.getDescription());
        group.setGroupVisibility(groupRequest.getVisibility());
        return groupRepository.save(group);
    }


    @Override
    public Group update(Long groupId, GroupRequest groupRequest) {
        Optional<Group> optionalGroup = groupRepository.findById(groupId);

        if (optionalGroup.isPresent()) {
            Group group = optionalGroup.get();
            group.setGroupName(groupRequest.getName());
            group.setGroupDescription(groupRequest.getDescription());
            group.setGroupVisibility(groupRequest.getVisibility());

            return groupRepository.save(group);
        } else {
            return null;
        }
    }

    @Override
    public void delete(Long groupId) {
        Optional<Group> optionalGroup = groupRepository.findById(groupId);
        if (optionalGroup.isPresent()) {
            groupRepository.deleteById(groupId);
        } else {
            throw new EntityNotFoundException("Group with ID " + groupId + " not found");
        }
    }

    @Override
    public void addContest(Long contestId, Long groupId) {

        Optional<Contest> optionalContest = contestRepository.findById(contestId);

        Optional<Group> optionalGroup = groupRepository.findById(groupId);

        if (optionalContest.isPresent() && optionalGroup.isPresent()) {
            Contest contest = optionalContest.get();
            Group group = optionalGroup.get();
            group.addContest(contest);
            groupRepository.save(group);
        } else {
            throw new EntityNotFoundException("Contest or Group not found");
        }
    }

    @Override
    public void inviteUser(Long groupId, Long userId) {
        // TODO
    }

    @Override
    @Transactional
    public void join(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new SubmitException("Group not found", HttpStatus.NOT_FOUND)
        );
        User user = userRepository.findById(userId).orElseThrow(
                () -> new SubmitException("User not found", HttpStatus.NOT_FOUND)
        );
        if (isPublic(groupId)) {
            group.addUser(user);
        }else {
            throw new SubmitException("Group visibility is not public.", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @Transactional
    public void leave(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new SubmitException("Group not found", HttpStatus.NOT_FOUND)
        );
        User user = userRepository.findById(userId).orElseThrow(
                () -> new SubmitException("User not found", HttpStatus.NOT_FOUND)
        );
        group.deleteUser(user);
    }

    @Override
    public List<Contest> Contests(Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new SubmitException("Group not found", HttpStatus.NOT_FOUND)
        );
        return group.getGroupContests();
    }

    @Override
    public List<User> Users(Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new SubmitException("Group not found", HttpStatus.NOT_FOUND)
        );
        return group.getGroupUsers();
    }

    @Override
    public boolean isPublic(Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(
                () -> new SubmitException("Group not found", HttpStatus.NOT_FOUND)
        );
        return group.getGroupVisibility() == GroupVisibility.PUBLIC;
    }

    @Override
    public boolean isPrivate(Long groupId) {
        return !isPublic(groupId);
    }
}
