package com.xteammors.openclaw.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "teammors")
public class TeammorsBotProperty {
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "TeammorsBotProperty{" +
                "token='" + token + '\'' +
                '}';
    }
}
