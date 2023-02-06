package ru.yandex.market.stat.dicts.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.typesafe.config.Config;
import lombok.SneakyThrows;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.commune.bazinga.impl.BazingaIdUtils;
import ru.yandex.market.stat.dicts.loaders.anaplan.AnaplanApiV2;
import ru.yandex.market.stat.dicts.loaders.anaplan.AnaplanLoader;
import ru.yandex.market.stat.dicts.loaders.anaplan.CertificateInfo;
import ru.yandex.market.stat.dicts.services.DictionaryStorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class AnaplanLoadTasksConfigTest {
    private static final String KEY_STORE_PATH = "/test_anaplan_keystore.pkcs12";
    private static final String KEY_STORE_PASS = "test_password";
    private static final String CERT_ALIAS = "testanaplancerts";
    private static final String PRIVATE_KEY_PASS = "test_password";

    @Test
    public void testProductionAnaplanConfig() {
        Config config = AnaplanLoadTasksConfig.parseConfig("test");
        Set<String> exportNames = new HashSet<>();
        for (Config workspace : config.getConfigList("dictionaries.yt.anaplan.workspaces")) {
            for (Config model : workspace.getConfigList("models")) {
                for (String exportName : model.getStringList("exports")) {
                    assertFalse("Duplicate export " +  exportName, exportNames.contains(exportName));
                    exportNames.add(exportName);
                    BazingaIdUtils.validate(exportName);
                }
            }
        }
    }

    @Test
    @SneakyThrows
    public void testLoadCertificateInfo() {
        AnaplanLoadTasksConfig anaplanLoadTasksConfig = new AnaplanLoadTasksConfig();
        InputStream keyStoreFile = AnaplanLoadTasksConfigTest.class.getResourceAsStream(KEY_STORE_PATH);
        CertificateInfo certificateInfo = AnaplanLoadTasksConfig.loadCertificateInfo(keyStoreFile, KEY_STORE_PASS,
                CERT_ALIAS, PRIVATE_KEY_PASS);
        assertEquals("Wrong keystore alias!", Collections.list(certificateInfo.getKeyStore().aliases()).get(0), CERT_ALIAS);
        assertEquals("Wrong cert alias!", certificateInfo.getAlias(), CERT_ALIAS);
        assertEquals("Wrong cert pass!", certificateInfo.getPrivateKeyPassword(), PRIVATE_KEY_PASS);
    }

    @Test
    public void testLoadConfig() {
        AnaplanLoadTasksConfig anaplanLoadTasksConfig = new AnaplanLoadTasksConfig(
                "test", "keyStorePath", "keyStorePassword", "alias", "pkPassword", "username", "password",
                mock(DictionaryStorage.class), mock(AnaplanApiV2.class),
                mock(RetryTemplate.class), mock(CloseableHttpClient.class)
        );
        Config config = AnaplanLoadTasksConfig.parseConfig("test", "configs/anaplan_test.conf");
        ArrayList<AnaplanLoader> loaders = new ArrayList<>();
        for (Config workspace : config.getConfigList("dictionaries.yt.anaplan.workspaces")) {
            for (Config model : workspace.getConfigList("models")) {
                anaplanLoadTasksConfig.createLoaderForApiV2(loaders, workspace.getString("workspace_id"), model);
            }
        }
        assertEquals("Wrong size of a resulting list", 2, loaders.size());
        assertEquals("anaplan/testing__promo_export_csv", loaders.get(0).getRelativePath());
        assertEquals("anaplan/production__promo_export_csv", loaders.get(1).getRelativePath());
    }
}
