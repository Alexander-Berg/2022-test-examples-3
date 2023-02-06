package ru.yandex.direct.mail;

import java.util.Arrays;
import java.util.Collection;

import javax.mail.MessagingException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class YServiceTokenCreatorCreateHmacTokenTest {
    private static final String SERVICE_NAME = "yadirect";
    private final Long operatorUid;
    private final Long clientId;
    private final String subject;
    private final String expectedToken;
    private final YServiceTokenCreator tokenCreator;

    public YServiceTokenCreatorCreateHmacTokenTest(
            String hmacSalt, Long operatorUid, Long clientId, String subject,
            String expectedToken) {
        this.operatorUid = operatorUid;
        this.clientId = clientId;
        this.subject = subject;
        this.expectedToken = expectedToken;
        this.tokenCreator = new YServiceTokenCreator(SERVICE_NAME, "", hmacSalt);
    }

    @Test
    public void createToken() {
        String actualToken = tokenCreator.createHmacToken(operatorUid, clientId, subject);
        assertEquals(expectedToken, actualToken);
    }

    @Parameterized.Parameters(name = "salt: {0}, operatorUid: {1}, clientId: {2}, emailSubject: {3}")
    public static Collection<Object[]> testData() throws MessagingException {
        return Arrays.asList(
                new Object[]{
                        "s12345", 405263787L, 5172863L, "Проверка подписи",
                        "8845a6403d47c09ed29783e1e101f2a0"
                },
                new Object[]{
                        "s12345", 405263787L, 5172863L, "Signature test",
                        "e6c0a0f77a56461e00ad702399961f6f"
                }
        );
    }
}
