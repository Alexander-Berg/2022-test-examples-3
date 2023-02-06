package ru.yandex.market.tsum.pipelines.startrek.jobs;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.platform.PlatformClient;
import ru.yandex.market.tsum.clients.platform.model.SecretModel;
import ru.yandex.market.tsum.clients.yav.VaultClient;
import ru.yandex.market.tsum.clients.yav.model.VaultGetSecretResponse;
import ru.yandex.market.tsum.clients.yav.model.VaultGetVersionResponse;
import ru.yandex.market.tsum.clients.yav.model.VaultResponseStatus;
import ru.yandex.market.tsum.clients.yav.model.VaultSecret;
import ru.yandex.market.tsum.clients.yav.model.VaultSecretVersion;
import ru.yandex.market.tsum.clients.yav.model.VaultSecretVersionValue;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobActionsContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.ResourcesJobContext;
import ru.yandex.market.tsum.pipelines.startrek.config.SecretConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpdatePlatformSecretsJobTest {

    @InjectMocks
    private UpdatePlatformSecretsJob job = new UpdatePlatformSecretsJob();

    @Mock
    private PlatformClient platformClient;

    @Mock
    private VaultClient vaultClient;

    @Mock
    private JobContext context;

    @Mock
    private JobActionsContext actionsContext;

    @Mock
    private JobProgressContext progressContext;

    @Mock
    private ResourcesJobContext resourcesContext;

    private static final String PLATFORM_SECRET_NAME = "platformSecretName";
    private static final String PLATFORM_SECRET_TYPE = "private key";
    private static final String UUID = "sec-vaultUuid";
    private static final String VAULT_SECRET_KEY = "vaultSecretKey";
    private static final String VAULT_SECRET_VALUE = "vaultSecretValue";
    private static final String VERSION = "version";

    @Test
    public void testSingleSecretUpdate() throws Exception {
        List<SecretConfig> secretConfigList = new ArrayList<>();
        FieldSetter.setField(job, job.getClass().getDeclaredField("secretConfigList"), secretConfigList);

        secretConfigList.add(new SecretConfig(PLATFORM_SECRET_NAME,
            PLATFORM_SECRET_TYPE, UUID, VAULT_SECRET_KEY));

        List<VaultSecretVersionValue> secretVersionValues = new ArrayList<>();
        addSecretVersionValue(secretVersionValues, VAULT_SECRET_KEY, VAULT_SECRET_VALUE);

        List<String> requestSecretNames = new ArrayList<>();
        mockVaultClientGetSecretResponse(VERSION, UUID);
        mockVaultClientGetVersionResponse(VERSION, secretVersionValues);
        addMocks(PLATFORM_SECRET_NAME, PLATFORM_SECRET_TYPE, VAULT_SECRET_VALUE, requestSecretNames);

        job.execute(context);

        Assert.assertEquals(1, requestSecretNames.size());
        Assert.assertEquals(PLATFORM_SECRET_NAME, requestSecretNames.get(0));
        verify(platformClient, times(1)).updateSecret(any());
    }

    @Test
    public void testMultipleSecretUpdates() throws Exception {
        List<SecretConfig> secretConfigList = new ArrayList<>();
        FieldSetter.setField(job, job.getClass().getDeclaredField("secretConfigList"), secretConfigList);

        String platformSecretName2 = "platformSecretName2";
        String vaultSecretKey2 = "vaultSecretKey2";
        String vaultSecretValue2 = "vaultSecretValue2";

        secretConfigList.add(new SecretConfig(PLATFORM_SECRET_NAME,
            PLATFORM_SECRET_TYPE, UUID, VAULT_SECRET_KEY));
        secretConfigList.add(new SecretConfig(platformSecretName2,
            PLATFORM_SECRET_TYPE, UUID, vaultSecretKey2));

        List<VaultSecretVersionValue> secretVersionValues = new ArrayList<>();
        addSecretVersionValue(secretVersionValues, VAULT_SECRET_KEY, VAULT_SECRET_VALUE);
        addSecretVersionValue(secretVersionValues, vaultSecretKey2, vaultSecretValue2);

        List<String> requestSecretNames = new ArrayList<>();
        mockVaultClientGetSecretResponse(VERSION, UUID);
        mockVaultClientGetVersionResponse(VERSION, secretVersionValues);
        addMocks(PLATFORM_SECRET_NAME, PLATFORM_SECRET_TYPE,
            VAULT_SECRET_VALUE, requestSecretNames);
        addMocks(platformSecretName2, PLATFORM_SECRET_TYPE,
            vaultSecretValue2, requestSecretNames);

        job.execute(context);

        Assert.assertEquals(2, requestSecretNames.size());
        Assert.assertEquals(PLATFORM_SECRET_NAME, requestSecretNames.get(0));
        Assert.assertEquals(platformSecretName2, requestSecretNames.get(1));
        verify(platformClient, times(2)).updateSecret(any());
    }

    @Test
    public void testNoSecretUpdates() throws Exception {
        List<SecretConfig> secretConfigList = new ArrayList<>();
        FieldSetter.setField(job, job.getClass().getDeclaredField("secretConfigList"), secretConfigList);

        secretConfigList.add(new SecretConfig(PLATFORM_SECRET_NAME,
            PLATFORM_SECRET_TYPE, UUID, "not matching vault key"));

        List<VaultSecretVersionValue> secretVersionValues = new ArrayList<>();
        addSecretVersionValue(secretVersionValues, VAULT_SECRET_KEY, VAULT_SECRET_VALUE);

        List<String> requestSecretNames = new ArrayList<>();
        mockVaultClientGetSecretResponse(VERSION, UUID);
        mockVaultClientGetVersionResponse(VERSION, secretVersionValues);
        addMocks(PLATFORM_SECRET_NAME, PLATFORM_SECRET_TYPE,
            VAULT_SECRET_VALUE, requestSecretNames);

        job.execute(context);

        Assert.assertEquals(0, requestSecretNames.size());
        verify(platformClient, times(0)).updateSecret(any());
    }

    private void addSecretVersionValue(List<VaultSecretVersionValue> versionValues, String key, String value) {
        VaultSecretVersionValue vaultSecretVersionValue = new VaultSecretVersionValue();
        vaultSecretVersionValue.setKey(key);
        vaultSecretVersionValue.setValue(value);
        versionValues.add(vaultSecretVersionValue);
    }

    private void addMocks(String platformSecretName,
                          String platformSecretType,
                          String vaultSecretValue,
                          List<String> requestSecretNames) {
        Mockito.doAnswer(invocation -> {
            SecretModel secretUpdate = (SecretModel) invocation.getArguments()[0];
            Assert.assertEquals(vaultSecretValue, secretUpdate.getContent());
            Assert.assertEquals(platformSecretName, secretUpdate.getName());
            Assert.assertEquals(platformSecretType, secretUpdate.getType());
            requestSecretNames.add(platformSecretName);
            return invocation;
        }).when(platformClient).updateSecret(
            ArgumentMatchers.argThat(secretModel -> secretModel.getName().equals(platformSecretName))
        );
    }

    private void mockVaultClientGetSecretResponse(String version, String uuid) {
        VaultGetSecretResponse vaultGetSecretResponse = new VaultGetSecretResponse();
        vaultGetSecretResponse.setStatus(VaultResponseStatus.OK);
        VaultSecret vaultSecret = new VaultSecret();
        VaultSecretVersion vaultSecretVersionGetSecret = new VaultSecretVersion();
        vaultSecretVersionGetSecret.setVersion(version);
        List<VaultSecretVersion> vaultSecretVersions = new ArrayList<>();
        vaultSecretVersions.add(vaultSecretVersionGetSecret);
        vaultSecret.setSecretVersions(vaultSecretVersions);
        vaultGetSecretResponse.setSecret(vaultSecret);
        when(vaultClient.getSecret(uuid)).thenReturn(vaultGetSecretResponse);
    }

    private void mockVaultClientGetVersionResponse(String version, List<VaultSecretVersionValue> secretVersionValues) {
        VaultGetVersionResponse vaultGetVersionResponse = new VaultGetVersionResponse();
        VaultSecretVersion vaultSecretVersionGetVersion = new VaultSecretVersion();
        vaultSecretVersionGetVersion.setValue(secretVersionValues);
        vaultGetVersionResponse.setVersion(vaultSecretVersionGetVersion);
        vaultGetVersionResponse.setStatus(VaultResponseStatus.OK);
        when(vaultClient.getVersion(version)).thenReturn(vaultGetVersionResponse);
    }
}
