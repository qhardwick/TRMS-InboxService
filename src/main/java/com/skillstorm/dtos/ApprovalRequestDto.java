    package com.skillstorm.dtos;

    import com.fasterxml.jackson.annotation.JsonGetter;
    import com.fasterxml.jackson.annotation.JsonIgnore;
    import com.skillstorm.entities.ApprovalRequest;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.time.temporal.ChronoUnit;
    import java.util.UUID;

    @Data
    @NoArgsConstructor
    public class ApprovalRequestDto {

        private String username;
        private UUID formId;
        private String requester;
        private String eventDate;
        private LocalDateTime timeCreated;
        private LocalDateTime approvalDeadline;
        private boolean viewed;

        public ApprovalRequestDto(ApprovalRequest approvalRequest) {
            this.username = approvalRequest.getUsername();
            this.formId = approvalRequest.getFormId();
            this.requester = approvalRequest.getRequester();
            this.eventDate = approvalRequest.getEventDate().toString() ;
            this.timeCreated = approvalRequest.getTimeCreated().truncatedTo(ChronoUnit.MILLIS);
            this.approvalDeadline = approvalRequest.getApprovalDeadline();
            this.viewed = approvalRequest.isViewed();
        }

        @JsonIgnore
        public ApprovalRequest mapToEntity() {
            ApprovalRequest approvalRequest = new ApprovalRequest();
            approvalRequest.setUsername(username);
            approvalRequest.setFormId(formId);
            approvalRequest.setRequester(requester);
            approvalRequest.setEventDate(LocalDate.parse(eventDate));
            approvalRequest.setTimeCreated(timeCreated.truncatedTo(ChronoUnit.MILLIS));
            approvalRequest.setApprovalDeadline(approvalDeadline);
            approvalRequest.setViewed(viewed);

            return approvalRequest;
        }

        @JsonGetter
        public boolean isUrgent() {
            return LocalDate.parse(eventDate)
                    .isBefore(LocalDate.now().plusWeeks(2));
        }
    }
