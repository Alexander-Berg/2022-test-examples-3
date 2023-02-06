package ru.yandex.market.billing.tasks.pp;

import java.io.StringReader;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.pp.storage.PpDescriptionsAll;
import ru.yandex.market.billing.pp.storage.PpJsonUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.billing.tasks.pp.PpPublicInfoService.ENV_MSTAT_PP_LAST_STORED_HASH_KEY;
import static ru.yandex.market.billing.tasks.pp.PpPublicInfoService.ENV_MSTAT_PP_UPDATE_TIME_KEY;

/**
 * Тесты для {@link PpPublicInfoService}.
 *
 * @author vbudnev
 */
class PpPublicInfoServiceTest extends FunctionalTest {

    private static final PpDescriptionsAll PP_DESCRIPTIONS = PpJsonUtils.loadPpDescriptionsRaw(
            new StringReader("" +
                    "{\n" +
                    "  \"1101\": {" +
                    "    \"path\": \"touch/market/page/cart/accessories/default\"," +
                    "    \"importance\": true," +
                    "    \"description\": \"someDescription\"," +
                    "    \"validFor\": [\"CLICKS\", \"VENDOR_CLICKS\"]," +
                    "    \"nonBillableFor\": [\"CLICKS\", \"SHOWS\"]," +
                    "    \"ignoredFor\": [\"CLICKS\", \"CPA_CLICKS\"]" +
                    "  }," +
                    "  \"1102\": {" +
                    "    \"path\": \"touch/market/page/default\"," +
                    "    \"importance\": false," +
                    "    \"isFree\": false," +
                    "    \"description\": \"someDescription\"," +
                    "    \"validFor\": [\"CLICKS\"]," +
                    "    \"marketType\": \"BLUE\"" +
                    "  }," +
                    "  \"1103\": {" +
                    "    \"path\": \"touch/market/page/default\"," +
                    "    \"importance\": false," +
                    "    \"isFree\": true," +
                    "    \"description\": \"someDescription\"," +
                    "    \"validFor\": [\"CLICKS\"]," +
                    "    \"marketType\": \"BLUE\"" +
                    "  }" +
                    "}"
            )
    ).withEnrichedId();

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private EnvironmentService environmentService;

    static Stream<Arguments> freeConversionSource() {
        return Stream.of(
                Arguments.of("При null  надо биллить (т.е. НЕ free)", 1101, false),
                Arguments.of("При false надо биллить (т.е. НЕ free)", 1102, false),
                Arguments.of("При true не надо биллить (т.е. free)", 1103, true)
        );
    }

    @Test
    @DbUnitDataSet(after = "db/PpMstatExportServiceTest_mstatMeta.after.csv")
    void test_refreshPublicInfo() {

        PpPublicInfoService ppMstatExportService = new PpPublicInfoService(
                environmentService,
                namedParameterJdbcTemplate
        );

        ppMstatExportService.refreshPublicInfo(
                PP_DESCRIPTIONS.withEnrichedId().getPpDescriptionById().values()
        );
        //еще раз, чтобы проверить, что рефреш при неизмененном хэше ничего не затирает.
        ppMstatExportService.refreshPublicInfo(
                PP_DESCRIPTIONS.withEnrichedId().getPpDescriptionById().values()
        );
    }

    /**
     * Вынесено в отдельный тест, для простоты локализации проблемы на этапе конвератции.
     */
    @Test
    void test_conversion() {
        PpPublicInfo metaInfo = PpPublicInfoService.buildFrom(
                PP_DESCRIPTIONS.getPpDescriptionById().get(1102)
        );

        Assertions.assertEquals("someDescription", metaInfo.getDescription());
        Assertions.assertEquals(Integer.valueOf(1102), metaInfo.getId());
        Assertions.assertEquals("blue", metaInfo.getMarketType());
        Assertions.assertEquals(Integer.valueOf(0), metaInfo.getImportance());
        Assertions.assertEquals("clicks", metaInfo.getValidFor());
        Assertions.assertNull(metaInfo.getNonBillableFor());
        Assertions.assertNull(metaInfo.getIgnoredFor());
        Assertions.assertEquals("touch/market/page/default", metaInfo.getPath());
        Assertions.assertEquals("touch", metaInfo.getPathLevel(0));
        Assertions.assertEquals("market", metaInfo.getPathLevel(1));
        Assertions.assertEquals("page", metaInfo.getPathLevel(2));
        Assertions.assertEquals("default", metaInfo.getPathLevel(3));
        Assertions.assertEquals("unspecified (touch/market/page/default)", metaInfo.getPathLevel(4));
        Assertions.assertEquals("unspecified (touch/market/page/default)", metaInfo.getPathLevel(5));
        Assertions.assertEquals("unspecified (touch/market/page/default)", metaInfo.getPathLevel(6));
    }

    @MethodSource("freeConversionSource")
    @ParameterizedTest(name = "{0}")
    void test_free_conversion(String description, int ppDescriptionId, boolean expectedFree) {
        PpPublicInfo metaInfo = PpPublicInfoService.buildFrom(
                PP_DESCRIPTIONS.getPpDescriptionById().get(ppDescriptionId)
        );
        assertThat(metaInfo.getIsFree(), is(expectedFree));
    }

    /**
     * Проверяем, что проставляются маркеры в энвайрмент.
     */
    @Test
    void test_refreshUpdateTime() {
        EnvironmentService mockedEnvironmentService = Mockito.mock(EnvironmentService.class);

        PpPublicInfoService ppMstatExportService = new PpPublicInfoService(
                mockedEnvironmentService,
                namedParameterJdbcTemplate
        );

        ppMstatExportService.refreshPublicInfo(PP_DESCRIPTIONS.getPpDescriptionById().values());

        Mockito.verify(mockedEnvironmentService)
                .setValue(eq(ENV_MSTAT_PP_UPDATE_TIME_KEY), any());
        Mockito.verify(mockedEnvironmentService)
                .setValue(eq(ENV_MSTAT_PP_LAST_STORED_HASH_KEY), any());

    }

}
