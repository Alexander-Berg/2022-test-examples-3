package ru.yandex.market.tsum.clients.nanny_vault;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.request.netty.NettyHttpClient;
import ru.yandex.market.request.netty.WrongStatusCodeException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 28.11.2017
 */
public class UntypedNannyVaultClientTest {
    @Test
    public void keychainSecretRevisionSuccessfulParsing() {
        String revisionAsString = "81110d2f-1ba6-4aeb-b630-131ffc1e1f18&initial-revision&1504704309089";
        KeychainSecretRevision revision = new KeychainSecretRevision(revisionAsString);
        assertEquals(revisionAsString, revision.getAsString());
        assertEquals("81110d2f-1ba6-4aeb-b630-131ffc1e1f18", revision.getId());
        assertEquals("initial-revision", revision.getName());
        assertEquals(1504704309089L, revision.getTimestampMs());
    }

    @Test(expected = RuntimeException.class)
    public void keychainSecretRevisionWrongNumberOfParts() {
        new KeychainSecretRevision("1&2");
    }

    @Test
    public void getLatestSecretRevision() {
        String oldRevision = "81110d2f-1ba6-4aeb-b630-131ffc1e1f18&initial-revision&1504704309089";
        String newRevision = "fb5753ef-82f8-4119-92ab-9fbfaab2e155&consumer-88&1505997760097";
        assertEquals(
            newRevision,
            UntypedNannyVaultClient.getLatestSecretRevision(Arrays.asList(oldRevision, newRevision)).get()
        );
        assertEquals(
            newRevision,
            UntypedNannyVaultClient.getLatestSecretRevision(Arrays.asList(newRevision, oldRevision)).get()
        );
    }

    @Test
    public void hidesOauthTokenInException() {
        String token = "asdf-zxcv223";
        String url = "some-url";
        NettyHttpClient nettyClient = Mockito.mock(
            NettyHttpClient.class
        );

        String payload = "{\"oauth\":\"" + token + "\"}";
        String message = String.format("url: %s\npayload %s\nresponse: %s", url, payload, "");

        Mockito.when(nettyClient.executeRequestSync(any(), any())).thenThrow(
            new WrongStatusCodeException(
                504,
                message,
                url,
                payload
            )
        );

        UntypedNannyVaultClient client = new UntypedNannyVaultClient(
            "https://notexistingurl/",
            token,
            nettyClient
        );

        try {
            client.getVaultToken();
        } catch (WrongStatusCodeException e) {
            assertFalse(e.getMessage().contains(token));
            assertFalse(e.getPayload().contains(token));
        }
    }
}
