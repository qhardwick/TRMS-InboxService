package com.skillstorm.dtos;

import com.skillstorm.entities.VerificationRequest;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class VerificationRequestDto {

    private String username;
    private UUID formId;
    private boolean viewed;

    public VerificationRequestDto(VerificationRequest verificationRequest) {
        this.username = verificationRequest.getUsername();
        this.formId = verificationRequest.getFormId();
        this.viewed = verificationRequest.isViewed();
    }

    public VerificationRequest mapToEntity() {
        VerificationRequest verificationRequest = new VerificationRequest();
        verificationRequest.setUsername(username);
        verificationRequest.setFormId(formId);
        verificationRequest.setViewed(viewed);

        return verificationRequest;
    }
}
