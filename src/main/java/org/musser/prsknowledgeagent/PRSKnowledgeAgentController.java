package org.musser.prsknowledgeagent;

import dev.firecrawl.exception.FirecrawlException;
import dev.langchain4j.service.Result;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class PRSKnowledgeAgentController {

    private final PRSKnowledgeAgent prsKnowledgeAgent;

    public PRSKnowledgeAgentController(PRSKnowledgeAgent prsKnowledgeAgent) {
        this.prsKnowledgeAgent = prsKnowledgeAgent;
    }

    @Autowired
    DataPopulation dp;

    @GetMapping("/prsKnowledgeAgent")
    public String prsKnowledgeAgent(@RequestParam String userMessage) {
        try {
        Result<String> result = prsKnowledgeAgent.answer(userMessage);
        return result.content();
        } catch (Throwable t) {
            return t.getMessage();
        }
    }

    @GetMapping("/loadDocs")
    public String loadDocs() throws IOException, FirecrawlException {
        String result = dp.loadPrsDocs();
        return result;
    }

    @GetMapping("/")
    public RedirectView redirectToIndex() {
        return new RedirectView("/index.html");
    }
}
