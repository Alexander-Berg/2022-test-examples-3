package ru.yandex.market.tsum.pipe.engine.isolation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.tsum.clients.yav.VaultClient;
import ru.yandex.market.tsum.clients.yav.model.VaultGetVersionResponse;
import ru.yandex.market.tsum.clients.yav.model.VaultLastSecretVersion;
import ru.yandex.market.tsum.clients.yav.model.VaultSecretVersion;
import ru.yandex.market.tsum.clients.yav.model.VaultSecrets;
import ru.yandex.market.tsum.pipe.engine.isolation.impl.VaultServiceImpl;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 20/11/2018
 */
public class VaultServiceImplTest {
    private VaultClient client;

    @Before
    public void setup() {
        client = mock(VaultClient.class);

        VaultSecrets secret = new VaultSecrets();
        secret.setName("test");
        secret.setUuid("uuid");
        secret.setLastSecretVersion(new VaultLastSecretVersion("version"));

        when(client.getAllSecrets()).thenReturn(Collections.singletonList(secret));

        VaultSecretVersion version = new VaultSecretVersion();
        version.setSecretUuid("uuid");
        version.setSecretName("test");

        VaultGetVersionResponse versionResponse = new VaultGetVersionResponse();
        versionResponse.setVersion(version);

        when(client.getVersion(eq("version"))).thenReturn(versionResponse);
    }

    @Test
    public void cachesSecrets() {
        VaultServiceImpl service = new VaultServiceImpl(client, Long.MAX_VALUE);
        service.getAllSecretVersions();
        service.getAllSecretVersions();

        Mockito.verify(client, Mockito.times(1)).getAllSecrets();
    }

    @Test
    public void cachesVersion() {
        VaultServiceImpl service = new VaultServiceImpl(client, Long.MAX_VALUE);
        service.getLastSecretVersion("uuid");
        service.getLastSecretVersion("uuid");

        Mockito.verify(client, Mockito.times(1)).getAllSecrets();
        Mockito.verify(client, Mockito.times(1)).getVersion(eq("version"));
    }
}
