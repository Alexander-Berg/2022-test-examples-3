package ru.yandex.market.health.configs.client.vault;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.clients.yav.VaultClient;
import ru.yandex.market.tsum.clients.yav.model.VaultGetSecretResponse;
import ru.yandex.market.tsum.clients.yav.model.VaultGetVersionResponse;
import ru.yandex.market.tsum.clients.yav.model.VaultSecret;
import ru.yandex.market.tsum.clients.yav.model.VaultSecretVersion;
import ru.yandex.market.tsum.clients.yav.model.VaultSecretVersionValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class HealthVaultServiceTest {
    public static final String SECRET_ID = "sec-testing";
    public static final String SECRET_VERSION_ID = "ver-lastOne";
    public static final String SOME_PASSWORD = "some_password";
    private static final String MDB_PASSWORD_KEY = "mdb.password";
    VaultClient vaultClient = mock(VaultClient.class);
    HealthVaultService vaultService = new HealthVaultService(vaultClient, 3600);

    @Test
    public void getMdbPasswordBySecretIdWhenVaultSecretIsNull() {
        String expectedExceptionMessage = "Failed to load Vault secret values by secret id sec-testing";

        when(vaultClient.getSecret(SECRET_ID)).thenReturn(new VaultGetSecretResponse());

        RuntimeException runtimeException = assertThrows(RuntimeException.class,
            () -> vaultService.getMdbPasswordBySecretId(SECRET_ID, MDB_PASSWORD_KEY));
        assertEquals(expectedExceptionMessage, runtimeException.getMessage());

        Mockito.verify(vaultClient).getSecret(SECRET_ID);
        Mockito.verify(vaultClient, times(0)).getVersion(anyString());
    }

    @Test
    public void getMdbPasswordBySecretIdWhenVaultSecretDoesNotHaveVersion() {
        VaultGetSecretResponse vaultGetSecretResponse = new VaultGetSecretResponse();
        VaultSecret vaultSecret = new VaultSecret();
        vaultSecret.setSecretVersions(Collections.emptyList());
        vaultGetSecretResponse.setSecret(vaultSecret);
        String expectedExceptionMessage = "Failed to load Vault secret values by secret id sec-testing";

        when(vaultClient.getSecret(SECRET_ID)).thenReturn(vaultGetSecretResponse);
        when(vaultClient.getVersion(SECRET_VERSION_ID)).thenReturn(new VaultGetVersionResponse());

        RuntimeException runtimeException = assertThrows(RuntimeException.class,
            () -> vaultService.getMdbPasswordBySecretId(SECRET_ID, MDB_PASSWORD_KEY));
        String actualCauseMessage = runtimeException.getCause().getMessage();
        assertEquals(expectedExceptionMessage, runtimeException.getMessage());
        assertTrue(actualCauseMessage.matches(".*There is no versions in secret: sec-testing$"));

        Mockito.verify(vaultClient).getSecret(SECRET_ID);
        Mockito.verify(vaultClient, times(0)).getVersion(anyString());
    }

    @Test
    public void getMdbPasswordBySecretIdWhenPasswordExistsInSecret() {
        prepareTestData(Collections.singletonList(new VaultSecretVersionValue(MDB_PASSWORD_KEY, SOME_PASSWORD)));

        String actualMdbPasswordBySecretId = vaultService.getMdbPasswordBySecretId(SECRET_ID, MDB_PASSWORD_KEY);
        assertEquals(SOME_PASSWORD, actualMdbPasswordBySecretId);

        Mockito.verify(vaultClient).getSecret(SECRET_ID);
        Mockito.verify(vaultClient).getVersion(SECRET_VERSION_ID);
    }

    @Test
    public void getMdbPasswordBySecretIdWhenPasswordExistsInSecretAndGetDataFromCahceInFollowingRequests()
        throws InterruptedException {
        prepareTestData(Collections.singletonList(new VaultSecretVersionValue(MDB_PASSWORD_KEY, SOME_PASSWORD)));

        String actualMdbPasswordBySecretId = vaultService.getMdbPasswordBySecretId(SECRET_ID, MDB_PASSWORD_KEY);
        assertEquals(SOME_PASSWORD, actualMdbPasswordBySecretId);
        Thread.sleep(1000);
        vaultService.getMdbPasswordBySecretId(SECRET_ID, MDB_PASSWORD_KEY);
        Thread.sleep(1000);
        vaultService.getMdbPasswordBySecretId(SECRET_ID, MDB_PASSWORD_KEY);

        Mockito.verify(vaultClient).getSecret(SECRET_ID);
        Mockito.verify(vaultClient).getVersion(SECRET_VERSION_ID);
    }

    @Test
    public void getMdbPasswordBySecretIdWhenNoPasswordInSecret() {
        String expectedExceptionMessage = "There is no mdb password in secret: sec-testing";

        prepareTestData(Collections.singletonList(new VaultSecretVersionValue("mdb.user", "logshatter")));

        RuntimeException runtimeException = assertThrows(RuntimeException.class,
            () -> vaultService.getMdbPasswordBySecretId(SECRET_ID, MDB_PASSWORD_KEY));
        assertEquals(expectedExceptionMessage, runtimeException.getMessage());

        Mockito.verify(vaultClient).getSecret(SECRET_ID);
        Mockito.verify(vaultClient).getVersion(SECRET_VERSION_ID);
    }

    private void prepareTestData(List<VaultSecretVersionValue> secretValues) {
        VaultGetSecretResponse vaultGetSecretResponse = new VaultGetSecretResponse();
        VaultSecret vaultSecret = new VaultSecret();
        VaultSecretVersion vaultSecretVersion = new VaultSecretVersion();
        vaultSecretVersion.setVersion(SECRET_VERSION_ID);
        vaultSecretVersion.setCreatedAt(1625205035);
        vaultSecret.setSecretVersions(Collections.singletonList(vaultSecretVersion));
        vaultGetSecretResponse.setSecret(vaultSecret);
        VaultGetVersionResponse version = new VaultGetVersionResponse();
        vaultSecretVersion.setValue(secretValues);
        version.setVersion(vaultSecretVersion);

        when(vaultClient.getSecret(SECRET_ID)).thenReturn(vaultGetSecretResponse);
        when(vaultClient.getVersion(SECRET_VERSION_ID)).thenReturn(version);
    }

}
