package ru.yandex.market.logistics.nesu.jobs;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.executor.YtUpdateDbsLicensesExecutor;
import ru.yandex.market.logistics.nesu.jobs.producer.PushDbsShopLicenseToLmsProducer;
import ru.yandex.market.logistics.nesu.model.entity.type.ShopLicenseType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тесты джобы ручного обновления лицензий всех магазинов")
@DatabaseSetup("/jobs/executors/yt_update_dbs_licenses/before/update_license_setup.xml")
class YtUpdateDbsLicensesExecutorTest extends AbstractContextualTest {

    private static final String YQL_QUERY =
        "SELECT " +
        "   snapshot.id, " +
        "   snapshot.is_sells_medicine, " +
        "   snapshot.is_medicine_courier, " +
        "   snapshot.timestamp " +
        "FROM `partner_biz_snapshot_path` AS snapshot " +
        "WHERE snapshot.is_dropship_by_seller == 1";

    @Autowired
    private JdbcTemplate yqlJdbcTemplate;

    @Autowired
    private PushDbsShopLicenseToLmsProducer pushDbsShopLicenseToLmsProducer;

    @Autowired
    private YtUpdateDbsLicensesExecutor ytUpdateDbsLicensesExecutorManual;

    @Autowired
    private YtUpdateDbsLicensesExecutor ytUpdateDbsLicensesExecutorScheduled;

    @BeforeEach
    void setUp() {
        doNothing()
            .when(pushDbsShopLicenseToLmsProducer)
            .produceTask(
                anyLong(),
                any(ShopLicenseType.class)
            );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(pushDbsShopLicenseToLmsProducer);
    }

    @Test
    @DisplayName("В YT нет данных")
    @ExpectedDatabase(
        value = "/jobs/executors/yt_update_dbs_licenses/before/update_license_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noDataInYt() {
        doReturn(List.of())
            .when(yqlJdbcTemplate)
            .queryForList(YQL_QUERY);

        ytUpdateDbsLicensesExecutorScheduled.doJob(null);
    }

    @Test
    @DisplayName("Успешное обновление лицензий (force push == true)")
    @ExpectedDatabase(
        value = "/jobs/executors/yt_update_dbs_licenses/after/licenses_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSuccessfulManual() {
        doReturn(yqlResultNonEmpty())
            .when(yqlJdbcTemplate)
            .queryForList(YQL_QUERY);

        ytUpdateDbsLicensesExecutorManual.doJob(null);

        verifyPushesUniversal();
        verifyPushesForceOnly();
    }

    @Test
    @DisplayName("Успешное обновление лицензий (force push == false)")
    @ExpectedDatabase(
        value = "/jobs/executors/yt_update_dbs_licenses/after/licenses_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSuccessfulScheduled() {
        doReturn(yqlResultNonEmpty())
            .when(yqlJdbcTemplate)
            .queryForList(YQL_QUERY);

        ytUpdateDbsLicensesExecutorScheduled.doJob(null);

        verifyPushesUniversal();
    }

    @Nonnull
    private List<Map<String, Object>> yqlResultNonEmpty() {
        byte[] timestampBytes = LocalDateTime.of(2022, 1, 28, 0, 43, 0, 110144000)
            .format(DateTimeFormatter.ofPattern("yyyyMMddkkmmssSSSSSS"))
            .getBytes(StandardCharsets.UTF_8);

        return List.of(
            Map.of(
                "id", 1L,
                "is_sells_medicine", 1L,
                "is_medicine_courier", 1L,
                "timestamp", timestampBytes
            ),
            Map.of(
                "id", 2L,
                "is_sells_medicine", 1L,
                "is_medicine_courier", 0L,
                "timestamp", timestampBytes
            ),
            Map.of(
                "id", 4L,
                "is_sells_medicine", 1L,
                "is_medicine_courier", 0L,
                "timestamp", timestampBytes
            )
        );
    }

    private void verifyPushesUniversal() {
        verify(pushDbsShopLicenseToLmsProducer).produceTask(1L, ShopLicenseType.CAN_SELL_MEDICINE);
        verify(pushDbsShopLicenseToLmsProducer).produceTask(2L, ShopLicenseType.CAN_DELIVER_MEDICINE);
    }

    private void verifyPushesForceOnly() {
        verify(pushDbsShopLicenseToLmsProducer).produceTask(1L, ShopLicenseType.CAN_DELIVER_MEDICINE);
        verify(pushDbsShopLicenseToLmsProducer).produceTask(2L, ShopLicenseType.CAN_SELL_MEDICINE);
    }
}
