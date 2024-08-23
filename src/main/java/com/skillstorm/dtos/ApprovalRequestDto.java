    package com.skillstorm.dtos;

    import com.datastax.oss.driver.api.core.type.DataTypes;
    import com.datastax.oss.driver.api.core.type.TupleType;
    import com.fasterxml.jackson.annotation.JsonIgnore;
    import com.skillstorm.entities.ApprovalRequest;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.time.LocalDateTime;
    import java.util.UUID;

    @Data
    @NoArgsConstructor
    public class ApprovalRequestDto {

        private static final TupleType tupleType = DataTypes.tupleOf(DataTypes.TEXT, DataTypes.UUID);

        private String username;
        private UUID formId;
        private LocalDateTime timeCreated;
        private LocalDateTime approvalDeadline;

        public ApprovalRequestDto(ApprovalRequest approvalRequest) {
            this.username = approvalRequest.getUsername();
            this.formId = approvalRequest.getFormId();
            this.timeCreated = approvalRequest.getTimeCreated();
            this.approvalDeadline = approvalRequest.getApprovalDeadline();
        }

        @JsonIgnore
        public ApprovalRequest mapToEntity() {
            ApprovalRequest approvalRequest = new ApprovalRequest();
            approvalRequest.setUsername(username);
            approvalRequest.setFormId(formId);
            approvalRequest.setTimeCreated(timeCreated);
            approvalRequest.setApprovalDeadline(approvalDeadline);

            return approvalRequest;
        }
    }
