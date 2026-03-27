package com.xteammors.openclaw.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "skills")
public class SkillsDirProperty {
    private String dir;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    @Override
    public String toString() {
        return "SkillsDirProperty{" +
                "dir='" + dir + '\'' +
                '}';
    }
}
