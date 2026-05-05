package com.estate.parser.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PlaywrightService {

    private Playwright playwright;
    private Browser browser;

    @PostConstruct
    public void init() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        log.info("Playwright browser started");
    }

    public Document getDocument(String url) {
        try (var context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 900))) {
            var page = context.newPage();
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            var html = page.content();
            return Jsoup.parse(html, url);
        } catch (Exception e) {
            log.error("Playwright failed to load {}", url, e);
            throw e;
        }
    }

    @PreDestroy
    public void close() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        log.info("Playwright browser stopped");
    }
}
