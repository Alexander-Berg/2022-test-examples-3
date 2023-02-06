package ru.yandex.market.gutgin.tms.pipeline.good.condition;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.partner.content.common.db.dao.CwSettingsDao;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.dao.SkipCwDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileDataProcessRequest;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.service.mappings.PartnerShopService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.eq;

public class SkipManualChecksConditionTest {
    private final long TEST_DATABUCKET_ID = 100L;
    private final int TEST_SHOP_ID = 1;
    private final int ANOTHER_TEST_SHOP_ID = 2;

    private PartnerShopService partnerShopService;
    private FileDataProcessRequestDao fileDataProcessRequestDao;
    private CwSettingsDao cwSettingsDao;
    private SkipCwDao skipCwDao;
    private SkipCwDao anotherSkipCwDao;
    private SkipCwDao emptySkipCwDao;

    @Before
    public void setUp() {
        partnerShopService = Mockito.mock(PartnerShopService.class);
        Mockito.when(partnerShopService.getShopIdByDataBucketIdOrFail(eq(TEST_DATABUCKET_ID)))
                .thenReturn(TEST_SHOP_ID);

        fileDataProcessRequestDao = Mockito.mock(FileDataProcessRequestDao.class);
        Mockito.when(fileDataProcessRequestDao.fetchOneByDataBucketId(eq(TEST_DATABUCKET_ID)))
                .thenReturn(null);
        cwSettingsDao = Mockito.mock(CwSettingsDao.class);
        Mockito.when(cwSettingsDao.isSkipCwPipeline()).thenReturn(false);

        skipCwDao = Mockito.mock(SkipCwDao.class);
        Mockito.when(skipCwDao.shouldSkip(eq(TEST_SHOP_ID))).thenReturn(true);

        anotherSkipCwDao = Mockito.mock(SkipCwDao.class);
        Mockito.when(anotherSkipCwDao.shouldSkip(eq(ANOTHER_TEST_SHOP_ID))).thenReturn(true);

        emptySkipCwDao = Mockito.mock(SkipCwDao.class);
    }

    @Test
    public void whenUseBlackListInBlackInWhiteThenSkip() {
        boolean result = new SkipManualChecksCondition(
                partnerShopService,
                fileDataProcessRequestDao,
                skipCwDao,
                set(TEST_SHOP_ID),
                cwSettingsDao, false
        )
                .doTest(new ProcessDataBucketData(TEST_DATABUCKET_ID));

        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void whenUseBlackListNotInBlackNotInWhiteThenSend() {
        boolean result = new SkipManualChecksCondition(
                partnerShopService,
                fileDataProcessRequestDao,
                anotherSkipCwDao,
                set(ANOTHER_TEST_SHOP_ID),
                cwSettingsDao, false
        )
                .doTest(new ProcessDataBucketData(TEST_DATABUCKET_ID));

        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void whenUseBlackListNotInBlackInWhiteThenSend() {
        boolean result = new SkipManualChecksCondition(
                partnerShopService,
                fileDataProcessRequestDao,
                anotherSkipCwDao,
                set(TEST_SHOP_ID),
                cwSettingsDao, false
        )
                .doTest(new ProcessDataBucketData(TEST_DATABUCKET_ID));

        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void whenUseBlackListInBlackNotInWhiteThenSkip() {
        boolean result = new SkipManualChecksCondition(
                partnerShopService,
                fileDataProcessRequestDao,
                skipCwDao,
                set(ANOTHER_TEST_SHOP_ID),
                cwSettingsDao, false
        )
                .doTest(new ProcessDataBucketData(TEST_DATABUCKET_ID));

        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void whenUseWhiteListInBlackInWhiteThenSend() {
        boolean result = new SkipManualChecksCondition(
                partnerShopService,
                fileDataProcessRequestDao,
                skipCwDao,
                set(TEST_SHOP_ID),
                cwSettingsDao, true
        )
                .doTest(new ProcessDataBucketData(TEST_DATABUCKET_ID));

        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void whenUseWhiteListNotInBlackNotInWhiteThenSkip() {
        boolean result = new SkipManualChecksCondition(
                partnerShopService,
                fileDataProcessRequestDao,
                anotherSkipCwDao,
                set(ANOTHER_TEST_SHOP_ID),
                cwSettingsDao, true
        )
                .doTest(new ProcessDataBucketData(TEST_DATABUCKET_ID));

        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void whenUseWhiteListInBlackNotInWhiteThenSkip() {
        boolean result = new SkipManualChecksCondition(
                partnerShopService,
                fileDataProcessRequestDao,
                skipCwDao,
                set(ANOTHER_TEST_SHOP_ID),
                cwSettingsDao, false
        )
                .doTest(new ProcessDataBucketData(TEST_DATABUCKET_ID));

        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void whenUseWhiteListNotInBlackInWhiteThenSend() {
        boolean result = new SkipManualChecksCondition(
                partnerShopService,
                fileDataProcessRequestDao,
                anotherSkipCwDao,
                set(TEST_SHOP_ID),
                cwSettingsDao, true
        )
                .doTest(new ProcessDataBucketData(TEST_DATABUCKET_ID));

        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void whenFileIsUploadedWithSkipManualChecksFlagThenSkip() {
        FileDataProcessRequest fileDataProcessRequest = new FileDataProcessRequest();
        fileDataProcessRequest.setSkipManualChecks(true);
        FileDataProcessRequestDao fileDataProcessRequestDao = Mockito.mock(FileDataProcessRequestDao.class);
        Mockito.when(fileDataProcessRequestDao.fetchOneByDataBucketId(eq(TEST_DATABUCKET_ID)))
            .thenReturn(fileDataProcessRequest);

        boolean result = new SkipManualChecksCondition(
            partnerShopService,
            fileDataProcessRequestDao,
            emptySkipCwDao,
            Collections.emptySet(),
                cwSettingsDao, false
        )
            .doTest(new ProcessDataBucketData(TEST_DATABUCKET_ID));

        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void whenFileIsUploadedWithoutSkipManualChecksFlagThenSend() {
        FileDataProcessRequest fileDataProcessRequest = new FileDataProcessRequest();
        fileDataProcessRequest.setSkipManualChecks(false);
        FileDataProcessRequestDao fileDataProcessRequestDao = Mockito.mock(FileDataProcessRequestDao.class);
        Mockito.when(fileDataProcessRequestDao.fetchOneByDataBucketId(eq(TEST_DATABUCKET_ID)))
            .thenReturn(fileDataProcessRequest);

        boolean result = new SkipManualChecksCondition(
            partnerShopService,
            fileDataProcessRequestDao,
            emptySkipCwDao,
            Collections.emptySet(),
                cwSettingsDao, false
        )
            .doTest(new ProcessDataBucketData(TEST_DATABUCKET_ID));

        Assertions.assertThat(result).isFalse();
    }

    @SafeVarargs
    private final <T> Set<T> set(T... values) {
        return Arrays.stream(values)
                .collect(Collectors.toSet());
    }
}
