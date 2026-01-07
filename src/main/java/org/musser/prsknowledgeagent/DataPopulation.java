package org.musser.prsknowledgeagent;

import java.io.IOException;

import org.springframework.stereotype.Service;


@Service
public class DataPopulation {

    PrsCrawler prsCrawler;
    public DataPopulation(PrsCrawler prsCrawler) {
        this.prsCrawler = prsCrawler;
    }


    public String loadPrsDocs() throws IOException 
    {   
        prsCrawler.crawlPrs();
        return "Complete";
    }

    public String createEmbedding() throws IOException 
    {   
        prsCrawler.createEmbeddingStore();;
        return "Complete";
    }


    
}
