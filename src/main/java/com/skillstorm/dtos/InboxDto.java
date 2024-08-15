package com.skillstorm.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.skillstorm.entities.Inbox;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class InboxDto {

    private String username;
    private UUID formId;

    public InboxDto(Inbox inbox) {
        this.username = inbox.getUsername();
        this.formId = inbox.getFormId();
    }

    @JsonIgnore
    public Inbox mapToEntity() {
        Inbox inbox = new Inbox();
        inbox.setUsername(username);
        inbox.setFormId(formId);

        return inbox;
    }
}
