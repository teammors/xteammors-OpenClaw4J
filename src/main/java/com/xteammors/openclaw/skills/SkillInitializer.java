package com.xteammors.openclaw.skills;

import com.xteammors.openclaw.rag.service.vector.VectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SkillInitializer implements CommandLineRunner {

    private final VectorStoreService vectorStoreService;

    public SkillInitializer(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Skill Initialization...");

        // 1. Clean up existing data
        vectorStoreService.deleteAll();

        // 2. Scan and initialize skills
        File skillsDir = new File("skills");
        if (skillsDir.exists() && skillsDir.isDirectory()) {
            scanAndRegisterSkills(skillsDir);
        } else {
            log.warn("Skills directory not found: {}", skillsDir.getAbsolutePath());
        }

        log.info("Skill Initialization Completed.");
    }

    private void scanAndRegisterSkills(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanAndRegisterSkills(file);
            } else if (file.getName().equalsIgnoreCase("SKILL.md")) {
                registerSkill(file);
            }
        }
    }

    private void registerSkill(File skillFile) {
        try {
            String content = Files.readString(skillFile.toPath());
            String skillName = skillFile.getParentFile().getName(); // Use directory name as skill name

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", skillFile.getName());
            metadata.put("path", skillFile.getAbsolutePath());
            metadata.put("skill_name", skillName);
            metadata.put("type", "skill_doc");

            vectorStoreService.addDocument(content, metadata);
            log.info("Registered skill: {}", skillName);

        } catch (IOException e) {
            log.error("Failed to read skill file: {}", skillFile.getAbsolutePath(), e);
        }
    }
}
