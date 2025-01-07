    package com.skillstorm.dtos;

    import com.fasterxml.jackson.annotation.JsonIgnore;
    import com.skillstorm.entities.ApprovalRequest;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.time.LocalDateTime;
    import java.util.UUID;

    @Data
    @NoArgsConstructor
    public class ApprovalRequestDto {

        private String username;
        private UUID formId;
        private LocalDateTime timeCreated;
        private LocalDateTime approvalDeadline;
        private boolean viewed;

        public ApprovalRequestDto(ApprovalRequest approvalRequest) {
            this.username = approvalRequest.getUsername();
            this.formId = approvalRequest.getFormId();
            this.timeCreated = approvalRequest.getTimeCreated();
            this.approvalDeadline = approvalRequest.getApprovalDeadline();
            this.viewed = approvalRequest.isViewed();
        }

        @JsonIgnore
        public ApprovalRequest mapToEntity() {
            ApprovalRequest approvalRequest = new ApprovalRequest();
            approvalRequest.setUsername(username);
            approvalRequest.setFormId(formId);
            approvalRequest.setTimeCreated(timeCreated);
            approvalRequest.setApprovalDeadline(approvalDeadline);
            approvalRequest.setViewed(viewed);

            return approvalRequest;
        }
    }
