package org.musser.prsknowledgeagent;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestTemplate;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import io.lettuce.core.RedisClient;

@Configuration
public class PRSKnowledgeAgentConfiguration {

    
    @Bean
    RedisClient redisClient(@Value("${redis.url:redis://localhost:6379}") String redisUrl) {
        return RedisClient.create(redisUrl);
    }


    @Bean
    ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(100)                
                .build();
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2QuantizedEmbeddingModel();
    }

    public EmbeddingStore<TextSegment> redisEmbeddingStore(@Value("${redis.url:redis://localhost:6379}") String uri,
                        @Value("${redis.dimension:384}") int dimension)  {        
           try {Thread.sleep(5000);} catch (Throwable t) {};

          try {

            RedisEmbeddingStore rs = 
                RedisEmbeddingStore.builder().uri(uri).dimension(dimension)
                    .build();            
            return rs;
          } catch (Throwable t) {
            t.printStackTrace();
            throw t;
          }
           
    }

    @Bean
    PrsCrawler prsCrawler(EmbeddingModel embeddingModel, TokenCountEstimator tokenizer) {
        return new PrsCrawler(embeddingModel, tokenizer);
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel, ResourceLoader resourceLoader, TokenCountEstimator tokenizer) throws IOException {

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        return embeddingStore;
    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {

        // You will need to adjust these parameters to find the optimal setting,
        // which will depend on multiple factors, for example:
        // - The nature of your data
        // - The embedding model you are using
        int maxResults = 1;
        double minScore = 0.6;

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    @Bean
    TokenCountEstimator tokenCountEstimator() {
        return new OpenAiTokenCountEstimator(GPT_4_O_MINI);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .connectTimeout(Duration.ofMillis(120000)) // Time to establish the connection
            .readTimeout(Duration.ofMillis(120000))   // Time to wait for data after connection established
            .build();
    }
    
}
