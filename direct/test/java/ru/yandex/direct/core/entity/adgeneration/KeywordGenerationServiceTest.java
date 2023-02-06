package ru.yandex.direct.core.entity.adgeneration;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import NAdvMachine.Searchqueryrec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.validation.KeywordsAddValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.searchqueryrecommendation.SearchQueryRecommendationClient;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordGenerationServiceTest {

    private KeywordGenerationService service;
    private SearchQueryRecommendationClient searchQueryRecommendationClient;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private KeywordsAddValidationService keywordsAddValidationService;

    @Before
    public void createService() {
        searchQueryRecommendationClient = mock(SearchQueryRecommendationClient.class);
        service = new KeywordGenerationService(
                null, ppcPropertiesSupport, null, null, null,
                searchQueryRecommendationClient, keywordsAddValidationService, null);
    }

    @Test
    public void generateKeywords_empty() {
        mockSearchQueryRecResponse(Collections.emptyList());
        Result<Collection<Keyword>> result = service.generateKeywords(createFullRequest(), new HashMap<>());
        assertThat(result.getResult(), hasSize(0));
    }

    @Test
    public void generateKeywords_onePhrase() {
        mockSearchQueryRecResponse(List.of("ALDU"));
        Result<Collection<Keyword>> result = service.generateKeywords(createFullRequest(), new HashMap<>());
        assertThat(result.getResult(), hasSize(1));
    }

    @Test
    public void generateKeywords_onlyInvalidPhrase() {
        mockSearchQueryRecResponse(List.of("раз два три четыре пять шесть семь восемь девять десять"));
        Result<Collection<Keyword>> result = service.generateKeywords(createFullRequest(), new HashMap<>());
        assertThat(result.getResult(), hasSize(0));
    }

    @Test
    public void generateKeywords_oneInvalidPhrase() {
        mockSearchQueryRecResponse(List.of(
                "раз два три четыре пять шесть семь восемь девять десять",
                "раз",
                "два",
                "три",
                "четыре",
                "пять",
                "раз два три четыре пять шесть семь восемь девять десять",
                "шесть",
                "семь",
                "-шесть",
                "восемь",
                "-десять",
                "девять",
                "раз два три четыре пять шесть семь восемь девять десять"
        ));
        Result<Collection<Keyword>> result = service.generateKeywords(createFullRequest(), new HashMap<>());
        assertThat(result.getResult(), hasSize(9));
    }

    private Searchqueryrec.TSearchQueryRecRequest createFullRequest() {
        return Searchqueryrec.TSearchQueryRecRequest.newBuilder()
                .setBannerTitle("title")
                .setBannerText("text")
                .build();
    }

    private void mockSearchQueryRecResponse(List<String> phrases) {
        Searchqueryrec.TSearchQueryRecResponse.Builder responseBuilder = Searchqueryrec.TSearchQueryRecResponse.newBuilder();
        phrases.forEach(phrase -> responseBuilder
                .addCandidatesBuilder()
                .setSearchQueryProfile(
                        NAdvMachine.QueryProfile.TSearchQueryProfile.newBuilder().setSearchQuery(phrase)
                )
        );
        try {
            when(
                    searchQueryRecommendationClient.getSearchQueryRecommendations(any(Searchqueryrec.TSearchQueryRecRequest.class))
            ).thenReturn(responseBuilder.build());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
