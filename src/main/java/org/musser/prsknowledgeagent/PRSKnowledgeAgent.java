package org.musser.prsknowledgeagent;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * PRS Knowledge Agent - AI-powered guitar expert assistant.
 * 
 * This interface uses the @AiService annotation which automatically generates an implementation
 * that performs the following steps under the hood:
 * 
 * 1. RETRIEVAL (RAG - Retrieval Augmented Generation):
 *    - Takes the user's question and converts it to an embedding
 *    - Queries the EmbeddingStore (configured in PRSKnowledgeAgentConfiguration)
 *    - Retrieves the most relevant text segments using the EmbeddingStoreContentRetriever
 *    - Filters results by similarity score (minScore=0.6, maxResults=1)
 * 
 * 2. CONTEXT INJECTION:
 *    - Takes the retrieved documents and injects them into the system message
 *    - Replaces placeholders like {{current_date}} with actual values
 *    - Builds a comprehensive context for the LLM
 * 
 * 3. MEMORY MANAGEMENT:
 *    - Retrieves chat history from ChatMemoryProvider (per memoryId)
 *    - Builds complete message list: [system message, chat history, new user message]
 *    - Maintains conversation context across multiple interactions
 * 
 * 4. LLM INVOCATION:
 *    - Calls OpenAiChatModel.generate() with the complete message list
 *    - Waits for response from the AI model (Grok in this case)
 *    - Captures response including token usage and finish reason
 * 
 * 5. MEMORY STORAGE:
 *    - Stores the user message in chat memory
 *    - Stores the AI response in chat memory
 *    - Ensures conversation continuity for future interactions
 * 
 * 6. RESULT PACKAGING:
 *    - Wraps the AI response in a Result object
 *    - Includes the retrieved source documents (for transparency)
 *    - Includes token usage metrics
 *    - Includes finish reason (STOP, LENGTH, etc.)
 * 
 * To see this logic explicitly implemented, check the git history or create a custom
 * implementation that doesn't use @AiService. The annotation provides convenience
 * but hides these important details.
 * 
 * Configuration beans used (defined in PRSKnowledgeAgentConfiguration):
 * - OpenAiChatModel: The LLM (configured for Grok/xAI)
 * - EmbeddingStoreContentRetriever: Retrieves relevant context documents
 * - ChatMemoryProvider: Manages conversation history per user
 * - EmbeddingModel: Converts text to embeddings (AllMiniLmL6V2)
 * - EmbeddingStore: Stores document embeddings (InMemoryEmbeddingStore)
 */
@AiService
public interface PRSKnowledgeAgent {

    /**
     * Answers a guitar-related question using RAG (Retrieval Augmented Generation).
     * 
     * The @SystemMessage annotation defines the AI's personality and instructions.
     * The {{current_date}} placeholder is automatically replaced with today's date.
     * Retrieved context documents are automatically injected into the conversation.
     * 
     * @param userMessage The user's question
     * @return Result containing:
     *         - content: The AI's response as HTML
     *         - sources: List of TextSegments used as context (from RAG retrieval)
     *         - tokenUsage: Input/output token counts
     *         - finishReason: Why the generation stopped (STOP, LENGTH, etc.)
     */
    @SystemMessage("""
            You are a Paul Reed Smith Guitars expert.
            Answer only guitar questions with medium detail. 
            Reply using HTML markup.
            Today is {{current_date}}.            
            """)
    Result<String> answer(@UserMessage String userMessage);
}
