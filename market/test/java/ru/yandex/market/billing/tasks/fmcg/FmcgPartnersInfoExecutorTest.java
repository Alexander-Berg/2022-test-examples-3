package ru.yandex.market.billing.tasks.fmcg;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.config.BillingMdsS3Config;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class FmcgPartnersInfoExecutorTest extends FunctionalTest {
    @Autowired
    private FmcgPartnersInfoExecutor fmcgPartnersInfoExecutor;

    @Autowired
    private NamedHistoryMdsS3Client namedHistoryMdsS3Client;

    @Test
    void testEmptyFmcgPartnersInfoExecutor() {
        mockAndCheck("emptyFmcgPartnersInfoResult.xml");

        fmcgPartnersInfoExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(before = "db/testFmcgPartnersInfoExecutor.before.csv")
    void testFmcgPartnersInfoExecutor() {
        mockAndCheck("fmcgPartnersInfoResult.xml");

        fmcgPartnersInfoExecutor.doJob(null);
    }

    private void mockAndCheck(String fileName) {
        Mockito.when(namedHistoryMdsS3Client.upload(eq(BillingMdsS3Config.FMCG_PARTNERS_RESOURCE), any()))
                .then(invocation -> {
                    ContentProvider contentProvider = invocation.getArgument(1);

                    String actualContent = IOUtils.toString(contentProvider.getInputStream(), StandardCharsets.UTF_8);
                    String expectedContent = StringTestUtil.getString(FmcgPartnersInfoExecutorTest.class,
                            "data/" + fileName);

                    MbiAsserts.assertXmlEquals(expectedContent, actualContent);

                    return null;
                });
    }
}