package ru.yandex.market.tsum.pipelines.sre.jobs.dbaas;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.clients.yav.VaultClient;
import ru.yandex.market.tsum.clients.yav.model.VaultCreateSecretVersionRequest;
import ru.yandex.market.tsum.clients.yav.model.VaultGetSecretResponse;
import ru.yandex.market.tsum.clients.yav.model.VaultGetVersionResponse;
import ru.yandex.market.tsum.clients.yav.model.VaultKeyValueVersion;
import ru.yandex.market.tsum.clients.yav.model.VaultLastSecretVersion;
import ru.yandex.market.tsum.clients.yav.model.VaultSecret;
import ru.yandex.market.tsum.clients.yav.model.VaultSecretVersion;
import ru.yandex.market.tsum.clients.yav.model.VaultSecretVersionValue;
import ru.yandex.market.tsum.pipelines.common.resources.YaVaultSecretResource;

public class YaVaultNewVersionCreatorTest {

    private final YaVaultNewVersionCreator versionCreator = Mockito.spy(YaVaultNewVersionCreator.class);
    private final VaultClient client = Mockito.mock(VaultClient.class);
    private final ImmutableMap<String, String> keyValueMap = ImmutableMap.of(
        "superSecretKey", "new superSecret value", "superPuperSecretKey", "value2"
    );

    @Test
    public void saveNewYaVaultSecretVersion() {
        String secretUid = "secretUid";
        YaVaultSecretResource resource = new YaVaultSecretResource("secretName", secretUid);
        VaultGetSecretResponse secretResponse = Mockito.mock(VaultGetSecretResponse.class);
        VaultLastSecretVersion vaultSecretVersion = Mockito.mock(VaultLastSecretVersion.class);
        VaultSecret vaultSecret = Mockito.mock(VaultSecret.class);

        String lastVersionKey1 = "lastVersionKey";
        String lastVersionKey2 = "superSecretKey";
        VaultSecretVersionValue vaultSecretVersionValue1 = new VaultSecretVersionValue(
            lastVersionKey1, "lastVersionValue"
        );
        VaultSecretVersionValue vaultSecretVersionValue2 = new VaultSecretVersionValue(
            lastVersionKey2, "old superSecret value"
        );

        VaultGetVersionResponse version = Mockito.mock(VaultGetVersionResponse.class);

        VaultSecretVersion secretVersion = Mockito.mock(VaultSecretVersion.class);
        Mockito.when(version.getVersion()).thenReturn(secretVersion);
        Mockito.when(secretVersion.getValue()).thenReturn(
            Arrays.asList(vaultSecretVersionValue1, vaultSecretVersionValue2)
        );
        // first execution - lastVersion doesn't exist, second execution - lastVersion exists
        Mockito.when(vaultSecret.getLastVersion()).thenReturn(Optional.empty(), Optional.of(vaultSecretVersion));
        String xVersion = "x-version";
        Mockito.when(vaultSecretVersion.getVersion()).thenReturn(xVersion);
        Mockito.when(client.getVersion(xVersion)).thenReturn(version);
        Mockito.when(secretResponse.getSecret()).thenReturn(vaultSecret);
        Mockito.when(client.getSecret(secretUid)).thenReturn(secretResponse);


        lastVersionTest(resource, Collections.emptyList());
        lastVersionTest(resource, Arrays.asList(lastVersionKey1, lastVersionKey2));

    }

    private void lastVersionTest(YaVaultSecretResource resource, List<String> lastVersionKeys) {
        Set<String> requiredKeys = new HashSet<>();
        requiredKeys.addAll(lastVersionKeys);
        requiredKeys.addAll(keyValueMap.keySet());

        Mockito.doAnswer((invocation) -> {
            VaultCreateSecretVersionRequest req = invocation.getArgument(1);
            List<VaultKeyValueVersion> vaultKeyValueVersions = req.getKeyValueParams().get("value");

            List<String> keysInRequest = vaultKeyValueVersions.stream()
                .map(v -> v.getParams().get("key"))
                .collect(Collectors.toList());

            Assert.assertTrue(keysInRequest.containsAll(requiredKeys));
            Assert.assertEquals(
                "Request keys doesn't equal to required count",
                requiredKeys.size(),
                keysInRequest.size()
            );

            return null;
        }).when(client).createSecretVersion(Mockito.anyString(), Mockito.any(VaultCreateSecretVersionRequest.class));

        versionCreator.saveNewYaVaultSecretVersion(client, resource, keyValueMap);
    }
}
