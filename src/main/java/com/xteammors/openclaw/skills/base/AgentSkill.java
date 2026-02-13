package com.xteammors.openclaw.skills.base;

public interface AgentSkill {
    /**
     * Get the unique name of the skill.
     * This name should match the one used in the RAG documentation.
     */
    String getName();

    /**
     * Get a description of what the skill does.
     */
    String getDescription();

    /**
     * Execute the skill.
     * @param input The input parameters or context for the skill.
     * @param chatId The chat ID of the current conversation.
     * @return The result of the execution.
     */
    String execute(String input, String chatId);
}
