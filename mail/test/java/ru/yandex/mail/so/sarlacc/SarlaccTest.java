package ru.yandex.mail.so.sarlacc;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.mail.so.sarlacc.config.SarlaccConfigBuilder;
import ru.yandex.parser.config.IniConfig;

public class SarlaccTest extends SarlaccTestBase {

    // CSOFF: MethodLength
    @Test
    public void testSimpleStat() throws Exception {
        try (SarlaccCluster cluster = new SarlaccCluster(this);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            try (CloseableHttpResponse response = client.execute(new HttpGet(cluster.sarlacc().host() + STAT))) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
        }
    }
    // CSON: MethodLength

    @Test
    public void testConfigOfProd() throws Exception {
        try (SarlaccCluster cluster = new SarlaccCluster(this)) {
            cluster.start();
            Path searchMap = Files.createFile(java.nio.file.Paths.get("", System.getProperty("SEARCHMAP_PATH")));
            String configProdContent =
                cluster.loadSource("mail/so/daemons/sarlacc/sarlacc_config/files/sarlacc-prod.conf");
            IniConfig configProdIni = new IniConfig(new StringReader(configProdContent));
            new SarlaccConfigBuilder(configProdIni);
            configProdIni.checkUnusedKeys();
            Files.delete(searchMap);
        }
    }
}
