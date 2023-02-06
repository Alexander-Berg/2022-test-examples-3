package ru.yandex.market.tanker.client;

import java.io.IOException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToXmlPattern;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.HttpClients;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.market.tanker.client.model.KeySet;
import ru.yandex.market.tanker.client.model.KeySetTranslation;
import ru.yandex.market.tanker.client.model.Language;
import ru.yandex.market.tanker.client.model.ProjectTranslation;
import ru.yandex.market.tanker.client.model.TankerToken;
import ru.yandex.market.tanker.client.model.TranslationStatus;
import ru.yandex.market.tanker.client.request.AddTranslationRequestBuilder;
import ru.yandex.market.tanker.client.request.AdditionMode;
import ru.yandex.market.tanker.client.request.TranslationData;
import ru.yandex.market.tanker.client.request.TranslationDataBuilder;
import ru.yandex.market.tanker.client.request.TranslationOperation;
import ru.yandex.market.tanker.client.request.TranslationParam;
import ru.yandex.market.tanker.client.request.TranslationRequestBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okXml;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Vadim Lyalin
 */
public class TankerClientTest {

    private static final String ERROR_BODY = "<error/>";

    private TankerClient tankerClient;
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        TankerToken token = new TankerToken("token123", true);
        tankerClient = new TankerClient(token, HttpClients.createDefault(), "http://localhost:" + wireMockRule.port());
    }

    @Test
    public void testKeyset() throws IOException {
        stubFor(get(urlPathEqualTo(TranslationOperation.KEYSETS.getUrl()))
                .withQueryParam(TranslationParam.PROJECT_ID.getName(), equalTo("test"))
                .willReturn(okJson(
                        IOUtils.toString(this.getClass().getResourceAsStream("keysets.json"), Charsets.UTF_8))));

        KeySetTranslation keySetTranslation = tankerClient.translations().keySet(
                new TranslationRequestBuilder().withProject("test"));

        KeySet keySet = keySetTranslation.getKeySet(Language.RU);
        assertEquals("", keySet.getText("empty"));
        assertNull(keySet.getText("null"));
        assertEquals("Заголовки", keySet.getText("title"));
        assertThat(keySet.getTexts("письмо"), CoreMatchers.hasItems("письмо", "письма", "писем"));
    }

    @Test
    public void testProject() throws IOException {
        stubFor(get(urlPathEqualTo(TranslationOperation.PROJECT_KEYSETS.getUrl()))
                .withQueryParam(TranslationParam.PROJECT_ID.getName(), equalTo("test"))
                .willReturn(okJson(
                        IOUtils.toString(this.getClass().getResourceAsStream("project.json"), Charsets.UTF_8))));

        ProjectTranslation projectTranslation = tankerClient.translations().project(
                new TranslationRequestBuilder().withProject("test"));

        KeySet keySet = projectTranslation.getKeysets(Language.RU).getKeySet("keySet");
        assertEquals("Заголовки", keySet.getText("title"));
        assertThat(keySet.getTexts("письмо"), CoreMatchers.hasItems("письмо", "письма", "писем"));
    }

    @Test
    public void testBadRequest() {
        stubFor(get(urlPathEqualTo(TranslationOperation.KEYSETS.getUrl()))
                .willReturn(badRequest().withBody(ERROR_BODY)));

        thrown.expect(TankerClientException.class);
        thrown.expectMessage(ERROR_BODY);

        tankerClient.translations().keySet(new TranslationRequestBuilder());
    }

    @Test
    public void testCreateKeySetOk() throws IOException {
        final TranslationData data = new TranslationDataBuilder()
                .addProject("test_proj")
                .addKeySet("test")

                .addKey("key1", true)
                .addValue(Language.RU, TranslationStatus.APPROVED,
                        "value_one_1", "value_some_1", "value_many_1", "value_none_1")
                .addValue(Language.EN, TranslationStatus.TRANSLATED, "value2")
                .buildKey()

                .addKey("key2", false)
                .addValue(Language.RU, TranslationStatus.APPROVED, "value2")
                .addValue(Language.EN, TranslationStatus.TRANSLATED, "value3")
                .buildKey()

                .buildKeySet()
                .buildProject()
                .build();

        final String expectedRequest = IOUtils.toString(
                this.getClass().getResourceAsStream("create-request.xml"), Charsets.UTF_8);
        final String actualRequest = IOUtils.toString(data.getData(), Charsets.UTF_8);
        final MatchResult match = new EqualToXmlPattern(expectedRequest).match(actualRequest);
        Assert.assertTrue(match.isExactMatch());

        AddTranslationRequestBuilder builder = new AddTranslationRequestBuilder()
                .mode(AdditionMode.CREATE).projectId("test_proj").translationData(data);

        stubFor(post(urlPathEqualTo(TranslationOperation.CREATE.getUrl()))
                .willReturn(okXml(
                        IOUtils.toString(this.getClass().getResourceAsStream("create-ok.xml"), Charsets.UTF_8))));

        tankerClient.translations().addTranslations(builder);
    }

    @Test
    public void testCreateKeySetFail() throws IOException {
        AddTranslationRequestBuilder builder = new AddTranslationRequestBuilder()
                .mode(AdditionMode.CREATE).projectId("test_proj").keySetId("test");

        stubFor(post(urlPathEqualTo(TranslationOperation.CREATE.getUrl()))
                .willReturn(badRequest().withBody(ERROR_BODY)));

        thrown.expect(TankerClientException.class);
        thrown.expectMessage(ERROR_BODY);

        tankerClient.translations().addTranslations(builder);
    }
}
