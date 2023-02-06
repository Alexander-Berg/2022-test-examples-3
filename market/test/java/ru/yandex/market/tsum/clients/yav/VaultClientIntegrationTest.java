package ru.yandex.market.tsum.clients.yav;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.tsum.clients.yav.model.VaultCreateTokenResponse;
import ru.yandex.market.tsum.clients.yav.model.VaultGetSecretsRequest;
import ru.yandex.market.tsum.clients.yav.model.VaultGetSecretsResponse;
import ru.yandex.market.tsum.clients.yav.model.VaultGetVersionResponse;
import ru.yandex.market.tsum.clients.yav.model.VaultResponseStatus;
import ru.yandex.market.tsum.clients.yav.model.VaultSecrets;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 19/11/2018
 */

@Ignore
public class VaultClientIntegrationTest {
    private final VaultClient client = new VaultClient(
        "https://vault-api.passport.yandex.net", ""
    );

    @Test
    public void getSecrets() {
        List<VaultSecrets> secrets = client.getAllSecrets();
        Assert.assertTrue(secrets.size() > 0);

        VaultGetSecretsRequest request = new VaultGetSecretsRequest()
            .setQuery('"' + "my-very-secret" + '"');

        VaultGetSecretsResponse secretsResponse = client.getSecrets(request);
        Assert.assertEquals(VaultResponseStatus.OK, secretsResponse.getStatus());
        Assert.assertTrue(secretsResponse.getSecrets().size() > 0);
    }

    @Test
    public void getAllSecrets() {
        Iterable<VaultSecrets> secrets = client.getSecretsAllPages();
        VaultSecrets mySecret = null;
        int counter = 0;
        for (VaultSecrets vaultSecrets : secrets) {
            if (vaultSecrets.getName().equals("my-very-secret")) {
                mySecret = vaultSecrets;
                counter++;
            }
        }
        Assert.assertTrue(counter == 1);
        Assert.assertEquals(mySecret.getName(), "my-very-secret");
    }

    @Test
    public void getVersion() {
        VaultGetVersionResponse response = client.getVersion("ver-01cvpzyf1bnb274bvfg9bstt7k");

        Assert.assertNotNull(response.getVersion());
    }

    @Test
    public void createToken() {
        VaultCreateTokenResponse response = client.createToken(
            "sec-01d4mpvfn1hfawsqmrkdk3cndh",
            "2002924",
            "my-signature-olol",
            "int test token"
        );
        Assert.assertEquals(VaultResponseStatus.OK, response.getStatus());
        Assert.assertNotNull(response.getToken());
        Assert.assertNotNull(response.getTokenUuid());
        Assert.assertFalse(response.getToken().isEmpty());
        Assert.assertFalse(response.getTokenUuid().isEmpty());
    }
}
