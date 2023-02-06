package ru.yandex.market.pers.notify;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import freemarker.template.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.codec.Hex;

import ru.yandex.market.pers.notify.api.service.sk.SecretKeyManager;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.sk.SecretKeyData;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.templates.EmailSnippetType;
import ru.yandex.market.pers.notify.templates.FTLoaderFactory;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SecretKeyManagerTest extends MockedDbTest {
    private static final Pattern LINK = Pattern.compile("action=(.*)&sk=(.*)$");
    Random rnd = new Random();
    @Autowired
    private SecretKeyManager secretKeyManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;
    @Autowired
    private Configuration configuration;
    @Autowired
    private FTLoaderFactory ftLoaderFactory;

    public int rndId() {
        return rnd.nextInt(100_000);
    }

    @Test
    public void testBase64() {
        String url = "uid=232351&type=0&time=134232451235";
        byte[] encoded = Base64.encodeBase64(url.getBytes(StandardCharsets.UTF_8));
        System.out.println(new String(encoded, StandardCharsets.UTF_8));
        byte[] decoded = Base64.decodeBase64(encoded);
        System.out.println(new String(decoded, StandardCharsets.UTF_8));
        assertEquals(url, new String(decoded, StandardCharsets.UTF_8));
    }

    @Test
    public void testHex() {
        String url = "uid=232351&type=0&time=134232451235";
        char[] encoded = Hex.encode(url.getBytes(StandardCharsets.UTF_8));
        System.out.println(encoded);
        byte[] decoded = Hex.decode(new String(encoded));
        System.out.println(new String(decoded, StandardCharsets.UTF_8));
        assertEquals(url, new String(decoded, StandardCharsets.UTF_8));
    }

    @Test
    public void testSk() throws Exception {
        jdbcTemplate.update("DELETE FROM EMAIL_SUBSCRIPTION");

        EmailSubscription subscription = new EmailSubscription();
        subscription.setSubscriptionType(NotificationType.ADVERTISING);
        subscription.setSubscriptionStatus(EmailSubscriptionStatus.CONFIRMED);
        subscription.setParameters(new HashMap<>());

        String email = "valter123@yandex.ru";
        List<EmailSubscription> subscriptions = subscriptionAndIdentityService.getEmailSubscriptions(email);
        int countCurrent = subscriptions.size();


        subscriptionAndIdentityService.createSubscriptions(email, Collections.singletonList(subscription), new Uid(1L));
        subscriptions = subscriptionAndIdentityService.getEmailSubscriptions(email);
        assertEquals(countCurrent + 1, subscriptions.size());
        SecretKeyData data = new SecretKeyData().addEmailSubscription(subscriptions.get(0))
            .addType(NotificationType.ADVERTISING).addEmail(email);

        testData(data);

        data.setType(NotificationType.ADVERTISING);
        testData(data);
    }

    @Test
    public void testGenerateKey() {
        SecretKeyManager.SKGenerator generator = new SecretKeyManager.SKGenerator();
        String key = generator.generate();
        System.out.println(key);
    }

    @BeforeEach
    public void afterPropertiesSet() {
        configuration.setTemplateLoader(ftLoaderFactory.getNewTemplateLoader());
    }

    @Test
    public void testGenerateUrlNoException() {
        assertNotNull(generateSubscribeUrl(5545761L, "ym-ia-test-4@yandex.ru", null,
            NotificationType.ADVERTISING, EmailSnippetType.SUBSCRIBE_URL));

        assertNotNull(generateSubscribeUrl(5545761L, "ym-ia-test-4@yandex.ru", null,
            NotificationType.ADVERTISING, EmailSnippetType.UNSUBSCRIBE_URL));
    }

    private String generateSubscribeUrl(Long userId, String email, Long uid,
                                        NotificationType type,
                                        EmailSnippetType snippetType) {
        return secretKeyManager.generateLink(new SecretKeyData()
            .addEmail(email)
            .addType(type));
    }

    private void testData(SecretKeyData data) throws GeneralSecurityException {
        String link = secretKeyManager.generateLink(data);
        System.out.println("Link: https://market.yandex.ru/unsubscribe?" + link);

        Matcher matcher = LINK.matcher(link);
        assertTrue(matcher.matches());
        String action = matcher.group(1);
        String token = matcher.group(2);
        SecretKeyData cipherData = secretKeyManager.resolveLink(action, token, null);
        assertNotNull(cipherData);
        assertEquals(data, cipherData);

        cipherData = secretKeyManager.resolveLink(action, token + "Fake", null);
        assertNull(cipherData);
    }


    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    @Test
    @Disabled // способ получения данных secretKeyData из sk и action,
    // только в SekretKeyManager надо закомментить IllegalArgumentException
    public void resolveSk() throws GeneralSecurityException {
        String sk = "4b462d01435abc6dde8e74a1e7569ef61872de61a4ac24098c1d8b0d8336d769";
        String action = "74696d653d3134373730383435333439363126747970653d3126656d61696c3d65737465726d6574616c4079616e6465782e727526737562736372697074696f6e49643d3137313333343530267573657249643d33303230373232266d6f64656c49643d3134323133373835";
        System.out.println(secretKeyManager.resolveLink(action, sk, "unsubscribe"));
    }
}
