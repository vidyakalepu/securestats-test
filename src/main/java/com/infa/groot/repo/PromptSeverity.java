package com.infa.groot.repo;

/**
 * 
 * @author pujain
 *
 */
public enum PromptSeverity {
    OK,
	LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static PromptSeverity fromCategory(String category) {
        switch (category.toUpperCase()) {
            case "ANTIGPT":
            case "ENCODING":
            case "KNOWN_BAD_SIGNATURES":
            case "SQLMAP":
            case "DOMAIN_DRIVEN":
            case "PROFANITY_TOXICITY_HARRASSMENT":
                return HIGH;
            case "COMPLEX_PUZZLES":
            case "CONTINUATION":
            case "HARMFUL_STEREOTYPES":
            case "INSTRUCTION_MANIPULATION":
            case "MISLEADING":
            case "ROLE_PLAY":
                return MEDIUM;
            case "MALWARE_GENERATION":
                return CRITICAL;
            case "FACTUAL_INACCURACY":
            case "GENERAL_QUESTIONS":
            case "GLITCH":
            case "REPLAY":
                return LOW;
            default:
                return LOW;
        }
    }
}
