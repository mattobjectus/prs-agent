package org.musser.prsknowledgeagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration.class)
public class PRSKnowledgeAgentApplication {

    public static void main(String[] args) {        
        SpringApplication.run(PRSKnowledgeAgentApplication.class, args);
    }
}
