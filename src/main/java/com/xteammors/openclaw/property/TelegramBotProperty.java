package com.xteammors.openclaw.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "telegram")
@Data
public class TelegramBotProperty {
    String id;
    String token;
    String name;
}
