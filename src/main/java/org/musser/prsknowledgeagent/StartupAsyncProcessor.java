package org.musser.prsknowledgeagent;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;


@Component
public class StartupAsyncProcessor {


    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final TokenCountEstimator tokenizer;
    private final ResourceLoader resourceLoader;

    public StartupAsyncProcessor(ResourceLoader resourceLoader,EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel,
        TokenCountEstimator tokenizer) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.tokenizer = tokenizer;
        this.resourceLoader = resourceLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Start background logic in a new thread
        
        new Thread(() -> {
          Resource emResource = resourceLoader.getResource("classpath:prs-docs.txt");        
          if (emResource == null) {
            throw new RuntimeException("Could nto find \"classpath:prs-docs.txt\"");
         }
         
          try (InputStream inputStream = emResource.getInputStream()) {
            TextDocumentParser parser = new TextDocumentParser();
            dev.langchain4j.data.document.Document document = parser.parse(inputStream);
            DocumentSplitter documentSplitter = DocumentSplitters.recursive(500, 100, tokenizer);
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(documentSplitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            ingestor.ingest(document);
            
        } catch (Exception e ) {
            e.printStackTrace();
        }
            
        }).start();
    }
}



