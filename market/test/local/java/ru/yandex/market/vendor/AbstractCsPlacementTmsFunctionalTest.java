package ru.yandex.market.vendor;

import java.util.Collections;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.JettyFunctionalTest;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;
import ru.yandex.vendor.documents.S3Connection;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

/**
 * Базовый класс для написания функциональных тестов в cs-placement-tms
 */
@SpringJUnitConfig(locations = "classpath:/ru/yandex/market/vendor/functional-test-config.xml")
@PreserveDictionariesDbUnitDataSet
public abstract class AbstractCsPlacementTmsFunctionalTest extends JettyFunctionalTest {

    @Autowired
    private S3Connection s3Connection;

    @Autowired
    private WireMockServer staffMock;

    @Autowired
    private WireMockServer reportMock;

    @Autowired
    private WireMockServer pricelabsMock;

    @Autowired
    private WireMockServer blackboxMock;

    @Autowired
    public WireMockServer emailSenderMock;

    @Autowired
    private WireMockServer mbiBiddingMock;

    @Autowired
    private WireMockServer abcMock;

    @Autowired
    private WireMockServer advIncutMock;

    @BeforeEach
    void initS3() {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        Mockito.when(amazonS3.listBuckets()).thenReturn(Collections.emptyList());
        Bucket bucket = mock(Bucket.class);
        Mockito.when(amazonS3.createBucket(anyString())).thenReturn(bucket);
        Mockito.doReturn(amazonS3).when(s3Connection).getS3();

        S3Connection.S3Config config = new S3Connection.S3Config();
        config.setPublicUrlPattern("https://{bucket}.s3.mdst.yandex.net");
        Mockito.when(s3Connection.getConfig()).thenReturn(config);

        resetWireMockServers();
    }

    private void resetWireMockServers() {
        staffMock.resetAll();
        reportMock.resetAll();
        blackboxMock.resetAll();
        emailSenderMock.resetAll();
        pricelabsMock.resetAll();
        mbiBiddingMock.resetAll();
        abcMock.resetAll();
        advIncutMock.resetAll();
    }

}
