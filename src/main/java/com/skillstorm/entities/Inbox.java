package com.skillstorm.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@Table("inbox")
public class Inbox {

    @PrimaryKeyColumn(name = "username", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String username;

    @PrimaryKeyColumn(name = "form_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private UUID formId;
}
