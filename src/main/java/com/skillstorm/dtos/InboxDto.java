package com.skillstorm.dtos;

import com.skillstorm.entities.Inbox;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InboxDto {

    private String username;

    public InboxDto(Inbox inbox) {
        this.username = inbox.getUsername();
    }
}
