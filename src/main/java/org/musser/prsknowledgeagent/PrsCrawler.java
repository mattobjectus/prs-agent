package org.musser.prsknowledgeagent;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;


public class PrsCrawler {
    //private static final String BASE_URL = "https://prsguitars.com";
    private static final String FORUM_URL = "https://forums.prsguitars.com";
    private static final int MAX_DEPTH = 3; // Limit crawl depth to avoid too many pages
    private static final Set<String> visited = new HashSet<>();
    private static final Queue<String> queue = new LinkedList<>();
    public static final String OUTPUT_FILE = "src/main/resources/prs-docs.txt"; // Output markdown file
    public static final String EMBEDDER_FILE = "src/main/resources/prs-docs-embedding-store.json"; // Output markdown file
    
    private final EmbeddingModel embeddingModel;
    private final TokenCountEstimator tokenizer;
    PrsCrawler(EmbeddingModel embeddingModel,   TokenCountEstimator tokenizer) {
        this.embeddingModel = embeddingModel;
        this.tokenizer = tokenizer;
        
    }

    public void crawlPrs() throws IOException {
        //queue.add(BASE_URL); 
        queue.add(FORUM_URL);
        //visited.add(BASE_URL);
        visited.add(FORUM_URL);

        try (FileWriter writer = new FileWriter(OUTPUT_FILE)) {
            int depth = 0;
            while (!queue.isEmpty() && depth <= MAX_DEPTH) {
                int levelSize = queue.size();
                for (int i = 0; i < levelSize; i++) {
                    String url = queue.poll();
                    crawlPage(url, writer);   
                    Thread.sleep(1000);                 
                }
                depth++;
                Thread.sleep(2000); // Delay to be polite to the server (2 seconds)
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Crawl complete. Output saved to " + OUTPUT_FILE);
    }

    private void crawlPage(String url, FileWriter writer) throws IOException {
        System.out.println("Crawling: " + url);        
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3") // Polite user agent
                .get();

        // Extract clean text/markdown (simple conversion: headers, paragraphs, lists)
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(doc.title()).append("\n\n"); // Page title as H1

        // Extract main content (adjust selector based on site structure, e.g., PRS uses 'section' or 'div.model-detail')
        Elements content = doc.select("body"); // Broad selector; narrow to main content if needed, e.g., ".content-section"
        for (Element element : content.select("h1, h2, h3, p, ul, ol, li")) {
            if (element.tagName().startsWith("h")) {
                int level = Integer.parseInt(element.tagName().substring(1));
                markdown.append("#".repeat(level)).append(" ").append(element.text()).append("\n\n");
            } else if (element.tagName().equals("p")) {
                markdown.append(element.text()).append("\n\n");
            } else if (element.tagName().equals("li")) {
                markdown.append("- ").append(element.text()).append("\n");
            }
        }

        // Write to file
        writer.write("## Page: " + url + "\n\n");
        writer.write(markdown.toString());
        writer.write("\n---\n\n"); // Separator between pages

        // Follow links (only within PRS domain, model pages, etc.)
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String nextUrl = link.absUrl("href");
            if (nextUrl.startsWith(FORUM_URL) && !visited.contains(nextUrl) && isRelevantLink(nextUrl)) {
                queue.add(nextUrl);
                visited.add(nextUrl);
            }
        }
    }

    // Filter for relevant links (e.g., guitar models, forums; adjust as needed)
    private boolean isRelevantLink(String url) {
        return url.contains("/electrics/") || url.contains("/models/") || url.contains("/products/") || url.contains("/forums/")|| url.contains("/threads/"); // PRS-specific paths
    }

    public void createEmbeddingStore() throws IOException {
                // 1. Create an in-memory embedding store
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 2. Load an example document from classpath (works inside JAR)     
        
        try (FileInputStream inputStream = new FileInputStream(OUTPUT_FILE)) {
            TextDocumentParser parser = new TextDocumentParser();
            dev.langchain4j.data.document.Document document = parser.parse(inputStream);

            // 3. Split the document into segments 100 tokens each
            // 4. Convert segments into embeddings
            // 5. Store embeddings into embedding store
            // All this can be done manually, but we will use EmbeddingStoreIngestor to automate this:
            DocumentSplitter documentSplitter = DocumentSplitters.recursive(100, 0, tokenizer);
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(documentSplitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            ingestor.ingest(document);
        }
        System.out.println("Serializing embedding store...");

        try (FileWriter fw = new FileWriter(EMBEDDER_FILE)) {
            fw.write(embeddingStore.serializeToJson());
        }
        System.out.println("Serialized embedding store.");
    }

}