package com.xjudge.service.invitiation;

import com.xjudge.entity.Invitation;
import com.xjudge.mapper.InvitationMapper;
import com.xjudge.model.enums.InvitationStatus;
import com.xjudge.model.invitation.InvitationModel;
import com.xjudge.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final InvitationRepository invitationRepository;
    private final InvitationMapper invitationMapper;

    @Override
    public void save(Invitation invitation) {
        invitationRepository.save(invitation);
    }

    @Override
    public Invitation findById(Long id) {
        return invitationRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("Invitation not found")
        );
    }

    @Override
    public List<InvitationModel> getInvitationByReceiverHandle(String handle) {
        List<Invitation> invitation = invitationRepository.getInvitationByReceiverHandleAndStatus(handle, InvitationStatus.PENDING);
        return invitation.stream().map(invitationMapper::toModel).toList();
    }
}
