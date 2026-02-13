package com.xteammors.openclaw.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "teammors")
@Data
public class TeammorsBotProperty {
    String token;
}
