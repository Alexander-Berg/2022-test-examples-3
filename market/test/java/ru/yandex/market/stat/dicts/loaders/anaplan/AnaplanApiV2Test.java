package ru.yandex.market.stat.dicts.loaders.anaplan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.market.stat.dicts.loaders.tvm.TvmTicketSupplier;
import ru.yandex.market.stat.dicts.services.MetadataService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class AnaplanApiV2Test {
    // Anaplan certificate required
    private static final String KEY_STORE_FILE = "/test_anaplan_keystore.pkcs12";
    private static final String ANAPLAN_CERT_ALIAS = "testanaplancerts";
    private static final String ANAPLAN_KEY_PASS = "test_password";
    private static final String KEY_STORE_PASSWORD = "test_password";

    private static final String YANDEX_MARKET_WORKSPACE_ID = "8a81b01067620e45016766207301057a";
    private static final String MARKET_POC_MODEL_ID = "7A8FA1056EB64F20AC499AAE724BFC3A";

    // won't exist in several weeks
    private static final String MY_EXPORT = "01.98 PL Export - oroboros - tabular single c";

    private AnaplanApiV2 api;
    private CertificateInfo certificateInfo;
    @Mock
    private MetadataService metadataService;
    @Mock
    private CuratorFramework curatorFramework;
    private AnaplanTokenProvider tokenProvider;

    @Before
    @Ignore("requires tvm secret")
    public void init() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1,
            ImmutableMap.of(IOException.class, true)));
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        TvmTicketSupplier tvm = new TvmTicketSupplier(
            2011512,
            // insert tvm secret from: https://abc.yandex-team.ru/services/mstat/resources/?show-resource=5513782
            "",
            "tvm-api.yandex.net",
            httpClient,
            retryTemplate);

        ZoraHttpClient zora = new ZoraHttpClient(
            tvm,
            httpClient,
            "zora.yandex.net:8166",
            "testing_market_mstat_anaplan",
            2000193);

        // FIXME: https://st.yandex-team.ru/MSTAT-9139#5d94a902701665001cc2b624
        tokenProvider = new AnaplanTokenProvider(metadataService, httpClient,curatorFramework, 10, "anaplan");
        api = new AnaplanApiV2(retryTemplate, httpClient, tokenProvider);
        certificateInfo=getTestCertInfo();
    }

    @SneakyThrows
    private CertificateInfo getTestCertInfo(){
        KeyStore keyStore=KeyStore.getInstance("PKCS12");
        keyStore.load(AnaplanApiV2Test.class.getResourceAsStream(KEY_STORE_FILE), KEY_STORE_PASSWORD.toCharArray());
        CertificateInfo certificateInfo=
                new CertificateInfo(keyStore, ANAPLAN_CERT_ALIAS, ANAPLAN_KEY_PASS);
        return certificateInfo;
    }

    @Test
    @Ignore("Anaplan certificate required. Also connects directly to anaplan")
    public void testGetToken() {
        String token = tokenProvider.getToken(certificateInfo);
        assertTrue(token.length() > 0);
    }

    @Test
    @Ignore("Anaplan password required")
    public void testGetTokenAndGetExportInfo() {
        System.out.println(api.getExportId(
                YANDEX_MARKET_WORKSPACE_ID,
                MARKET_POC_MODEL_ID,
                MY_EXPORT,
                certificateInfo
        ));
    }

    @Test
    @SneakyThrows
    public void testMetadataParsing() {
        String metadataJson = "{\n" +
            "    \"columnCount\" : 5,\n" +
            "    \"dataTypes\" : [ \"ENTITY\", \"ENTITY\", \"ENTITY\", \"ENTITY\", \"MIXED\" ],\n" +
            "    \"delimiter\" : \"\\\"\",\n" +
            "    \"encoding\" : \"UTF-8\",\n" +
            "    \"exportFormat\" : \"text/csv\",\n" +
            "    \"headerNames\" : [ \"Line Item\", \"Ls.3 PL\", \"L4.1 Версии\", \"Period\", \"Value\" ],\n" +
            "    \"listNames\" : [ \"\", \"\", \"\", \"\", \"\" ],\n" +
            "    \"rowCount\" : 39529,\n" +
            "    \"separator\" : \",\"\n" +
            "  }";
        AnaplanExportMetadata metadata = new ObjectMapper().readValue(metadataJson, AnaplanExportMetadata.class);
        assertEquals(39529L, metadata.getRowCount().longValue());
        assertEquals("text/csv", metadata.getExportFormat());
        assertEquals("MIXED", metadata.getDataTypes().get(4));
    }

    @Test
    @Ignore("Anaplan password required")
    public void testGetExportInfo() {
        String exportId = api.getExportId(
                YANDEX_MARKET_WORKSPACE_ID,
                MARKET_POC_MODEL_ID,
                MY_EXPORT,
                certificateInfo
        );
        assertEquals(",", api.getExportInfo(YANDEX_MARKET_WORKSPACE_ID,
                MARKET_POC_MODEL_ID,
                exportId,
                certificateInfo
        ).getSeparator());
    }

    @Test
    @SneakyThrows
    public void testPrepareTokenRequest() {
        certificateInfo=getTestCertInfo();
        HttpPost request = tokenProvider.prepareTokenRequest(certificateInfo);
//      Проверять качество подписки кажется немного бессмысленным - это будет копирование ровно того же кода
//      Так что просто сверяю, что хэдер и тело проставились и ничего не упало
        assertTrue(request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue().startsWith("CACertificate"));
        assertTrue(IOUtils.toString(request.getEntity().getContent(), StandardCharsets.UTF_8.name()).contains("\"encodedData\":"));
        assertTrue(IOUtils.toString(request.getEntity().getContent(), StandardCharsets.UTF_8.name()).contains("\"encodedSignedData\":"));
    }

}
