package ru.yandex.direct.sender;

import com.google.common.collect.ImmutableMap;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class YandexSenderClientTest {
    public static final String ACCOUNT_SLUG = "accountSlug";
    public static final String CAMPAIGN_SLUG = "campSlug";
    public static final String TO_EMAIL = "devnull@yandex-team.ru";
    public static final String ACCOUNT_TOKEN = "account_token";
    private static final String CLIENT_ID = "ClientID";

    @Rule
    public final MockedMailSenderService senderService = new MockedMailSenderService(ACCOUNT_TOKEN);

    private YandexSenderClient senderClient;

    @Before
    public void setup() {
        YandexSenderConfig senderConfig = new YandexSenderConfig("http", senderService.getBaseUrl(), ACCOUNT_SLUG, ACCOUNT_TOKEN);
        senderClient = new YandexSenderClient(senderConfig, new DefaultAsyncHttpClient());
    }

    @Test
    public void sendTemplate_ok() {
        YandexSenderTemplateParams templateParams = new YandexSenderTemplateParams.Builder()
                .withToEmail(TO_EMAIL)
                .withCampaignSlug(CAMPAIGN_SLUG)
                .withArgs(ImmutableMap.of(CLIENT_ID, "123"))
                .build();
        senderClient.sendTemplate(templateParams);
        MockedMailSenderService.LoggedSenderRequest expectedItem = new MockedMailSenderService.LoggedSenderRequest(ACCOUNT_SLUG,
                CAMPAIGN_SLUG, TO_EMAIL);
        assertThat(senderService.getActivityLog(), Matchers.hasItem(expectedItem));
    }

    @Test(expected = YandexSenderException.class)
    public void sendTemplate_expect_exception() {
        YandexSenderTemplateParams templateParams = new YandexSenderTemplateParams.Builder()
                .withToEmail("")
                .withCampaignSlug(CAMPAIGN_SLUG)
                .build();
        senderClient.sendTemplate(templateParams);
    }

    @Test(expected = YandexSenderException.class)
    public void sendTemplate_expect_auth_exception() {
        YandexSenderConfig senderConfig = new YandexSenderConfig("http", senderService.getBaseUrl(), ACCOUNT_SLUG, "non_token");
        YandexSenderClient nonAuthSenderClient = new YandexSenderClient(senderConfig, new DefaultAsyncHttpClient());

        YandexSenderTemplateParams templateParams = new YandexSenderTemplateParams.Builder()
                .withToEmail(TO_EMAIL)
                .withCampaignSlug(CAMPAIGN_SLUG)
                .build();
        nonAuthSenderClient.sendTemplate(templateParams);
    }
}
