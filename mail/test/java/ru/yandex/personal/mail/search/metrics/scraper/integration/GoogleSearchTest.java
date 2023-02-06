package ru.yandex.personal.mail.search.metrics.scraper.integration;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.personal.mail.search.metrics.scraper.controllers.AccountController;
import ru.yandex.personal.mail.search.metrics.scraper.controllers.SearchController;
import ru.yandex.personal.mail.search.metrics.scraper.metrics.basket.BasketQuery;
import ru.yandex.personal.mail.search.metrics.scraper.metrics.serp.Serp;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Using junit vintage (4) for spring 4 compatibility
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
public class GoogleSearchTest {
    private static final String SYSTEM_NAME = "google";
    private static final String ACCOUNT_NAME = "acc";

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private AccountController accountController;

    @Autowired
    private SearchController searchController;

    @Before
    public void setup() {
        AccountTestTools.createAccount(accountController, SYSTEM_NAME, ACCOUNT_NAME);
    }

    @Test
    public void testSearch() {
        String queryText = "some query text";
        BasketQuery query = new BasketQuery(queryText, Collections.emptyList());

        Serp serp = searchController.search(SYSTEM_NAME, ACCOUNT_NAME, query);
        assertEquals(queryText, serp.getQuery().getText());
        assertEquals(TestingConfiguration.SERP_SIZE, serp.getComponents().size());
    }

}
