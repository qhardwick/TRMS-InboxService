package com.skillstorm.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.util.UUID;

@Data
@NoArgsConstructor
public class VerificationRequest {

    @PrimaryKeyColumn(name = "username", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String username;

    @PrimaryKeyColumn(name = "form_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private UUID formId;
}
