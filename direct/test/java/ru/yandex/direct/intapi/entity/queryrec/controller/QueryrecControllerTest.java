package ru.yandex.direct.intapi.entity.queryrec.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.utils.JsonUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static ru.yandex.direct.common.db.PpcPropertyNames.CLIENTS_WITH_ENABLED_UZBEK_LANGUAGE;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.regions.Region.UZBEKISTAN_REGION_ID;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class QueryrecControllerTest {

    private static final String RECOGNIZE_TEXTS_URL = "/queryrec/recognize_texts";
    private static final String ANALYZE_TEXT_LANG = "/queryrec/analyze_texts_lang";

    @Autowired
    private Steps steps;

    @Autowired
    private QueryrecController controller;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private MockMvc mockMvc;

    @Before
    public void before() {
        ppcPropertiesSupport.set(CLIENTS_WITH_ENABLED_UZBEK_LANGUAGE.getName(), "-1");

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testControllerReturnsValidResponse() throws Exception {
        List<Pair<String, String>> textsToExpectedLangs =
                Arrays.asList(
                        Pair.of("Хлопчык чакаў іх каля ўваходу на пасадку.", "bel"),
                        Pair.of("Izmirin Yeme Icme Rehberi ile En Guzel Restoranlara, Cafelere Hemen Ulasin", "tur")
                );

        List<String> texts = textsToExpectedLangs.stream()
                .map(Pair::getLeft)
                .collect(Collectors.toList());
        List<String> expectedLangs = textsToExpectedLangs.stream()
                .map(Pair::getRight)
                .collect(Collectors.toList());

        String stringResponse = mockMvc
                .perform(MockMvcRequestBuilders.post(RECOGNIZE_TEXTS_URL)
                        .content(JsonUtils.toJson(texts))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Double>> response = parseJson(stringResponse);

        for (int i = 0; i < expectedLangs.size(); ++i) {
            String text = texts.get(i);
            String expectedLang = expectedLangs.get(i);
            Map<String, Double> responseEntry = response.get(i);

            String assertReason = String.format("Тексту '%1$s' должен соответствовать язык %2$s", text, expectedLang);
            assertThat(assertReason, responseEntry.keySet(), hasItem(expectedLang));
        }
    }

    @Test
    public void analyzeTextsLangTest() throws Exception {
        List<Pair<String, String>> textsToExpectedLangs =
                Arrays.asList(
                        Pair.of("Хлопчык чакаў іх каля ўваходу на пасадку.", "bel"),
                        Pair.of("Izmirin Yeme Icme Rehberi ile En Guzel Restoranlara, Cafelere Hemen Ulasin", "tur")
                );

        List<String> texts = textsToExpectedLangs.stream()
                .map(Pair::getLeft)
                .collect(Collectors.toList());
        List<String> expectedLangs = textsToExpectedLangs.stream()
                .map(Pair::getRight)
                .collect(Collectors.toList());

        String responseRaw = mockMvc
                .perform(MockMvcRequestBuilders.post(ANALYZE_TEXT_LANG)
                        .content(JsonUtils.toJson(texts))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> response = JsonUtils.fromJson(responseRaw, List.class);

        assertThat(response, is(expectedLangs));
    }

    @Test
    public void analyzeTextsLang_UzbekClientWithUzbekTexts_UzbekLanguageRecognized() throws Exception {
        ClientInfo clientInfo =
                steps.clientSteps().createClient(defaultClient().withCountryRegionId(UZBEKISTAN_REGION_ID));

        List<Pair<String, String>> textsToExpectedLangs =
                Arrays.asList(
                        Pair.of("Сизга узоқ умр ва бахт-саодат тилайман", "uzb"),
                        Pair.of("Ie, ishtahamni ochib yubording, qani ketdik", "uzb")
                );

        List<String> texts = textsToExpectedLangs.stream()
                .map(Pair::getLeft)
                .collect(Collectors.toList());
        List<String> expectedLangs = textsToExpectedLangs.stream()
                .map(Pair::getRight)
                .collect(Collectors.toList());

        String responseRaw = mockMvc
                .perform(MockMvcRequestBuilders.post(ANALYZE_TEXT_LANG)
                        .param("client_id", clientInfo.getClientId().toString())
                        .content(JsonUtils.toJson(texts))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> response = JsonUtils.fromJson(responseRaw, List.class);

        assertThat(response, is(expectedLangs));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Double>> parseJson(String json) {
        return JsonUtils.fromJson(json, List.class);
    }
}
