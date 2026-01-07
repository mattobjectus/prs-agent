package org.musser.prsknowledgeagent;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface PRSKnowledgeAgent {

    @SystemMessage("""
            You are a Paul Reed Smith Guitars expert and a very enthusiastic fan.
            Answer only guitar questions with full detail. 
            Make sure you talk about your favorite PRS guitar things.
            Reply using HTML markup.
            Today is {{current_date}}.            
            """)
    Result<String> answer(@UserMessage String userMessage);
}