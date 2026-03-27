package com.xteammors.openclaw.skills;

import com.xteammors.openclaw.property.SkillsDirProperty;
import com.xteammors.openclaw.rag.service.vector.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Component
public class SkillInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SkillInitializer.class);


    @Autowired
    SkillsDirProperty skillsDirProperty;


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
        if(!skillsDirProperty.getDir().isEmpty()){
            File skillsDir = new File(skillsDirProperty.getDir());
            log.info("Scanning skills directory: {}", skillsDir.getAbsolutePath());
            scanAndRegisterSkills(skillsDir);
            log.info("Skill Initialization Completed.");
        }else {
            log.warn("Skills directory not found: {}", skillsDirProperty.getDir());
            // Fallback to local skills directory
            File skillsDir = new File("skills");
            if (skillsDir.exists() && skillsDir.isDirectory()) {
                log.info("Fallback to local skills directory: {}", skillsDir.getAbsolutePath());
                scanAndRegisterSkills(skillsDir);
                log.info("Skill Initialization Completed.");
            } else {
                log.warn("Local skills directory not found: {}", skillsDir.getAbsolutePath());
            }
        }

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

    /**
     * Add a single skill to the vector store
     * @param skillDirPath Path to the skill directory
     */
    public void addSkill(String skillDirPath) {
        File skillDir = new File(skillDirPath);
        if (skillDir.exists() && skillDir.isDirectory()) {
            File skillFile = new File(skillDir, "SKILL.md");
            if (skillFile.exists() && skillFile.isFile()) {
                registerSkill(skillFile);
            } else {
                log.warn("SKILL.md file not found in: {}", skillDirPath);
            }
        } else {
            log.warn("Skill directory not found: {}", skillDirPath);
        }
    }

    /**
     * Reinitialize all skills
     */
    public void reinitializeSkills() {
        try {
            log.info("Reinitializing skills...");
            // Clean up existing data
            vectorStoreService.deleteAll();

            // Scan and initialize skills
            if(!skillsDirProperty.getDir().isEmpty()){
                File skillsDir = new File(skillsDirProperty.getDir());
                scanAndRegisterSkills(skillsDir);
                log.info("Skill reinitialization completed.");
            } else {
                log.warn("Skills directory not found: {}", skillsDirProperty.getDir());
            }
        } catch (Exception e) {
            log.error("Failed to reinitialize skills", e);
        }
    }
}
