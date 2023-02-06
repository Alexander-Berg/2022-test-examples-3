package ru.yandex.market.logistics.logistics4shops.jobs;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.logging.code.YtEventCode;
import ru.yandex.market.logistics.logistics4shops.service.yt.dto.YtPartnerExternalParam;
import ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тест синка partnerExternalParams")
@DatabaseSetup("/jobs/sync_partner_external_params/before/setup.xml")
class SyncPartnerExternalParamsJobTest extends AbstractIntegrationTest {
    private static final String EXPECTED_QUERY = """
            pepv.id AS id,
            pepv.partner_id AS partnerId,
            pept.key AS type,
            pepv.value AS value
            FROM [partner_external_param_value] AS pepv
            JOIN [partner_external_param_type] AS pept ON pepv.type_id = pept.id
            WHERE pept.key IN ('READY_TO_SHIP_ON_DEADLINE')
        """;

    @Autowired
    private SyncPartnerExternalParamsJob job;
    @Autowired
    private Yt yt;
    @Autowired
    private YtTables ytTables;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(yt, ytTables);
    }

    @Test
    @DisplayName("Параметры обновляются")
    @ExpectedDatabase(
        value = "/jobs/sync_partner_external_params/after/updated.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @SneakyThrows
    @JpaQueriesCount(4)
    void successParametersSync() {
        mockYt(List.of(
            new YtPartnerExternalParam()
                .setId(123L)
                .setPartnerId(101L)
                .setType("READY_TO_SHIP_ON_DEADLINE")
                .setValue("updated value"),
            new YtPartnerExternalParam()
                .setId(1234567L)
                .setPartnerId(104L)
                .setType("READY_TO_SHIP_ON_DEADLINE")
                .setValue("new value")
        ));

        job.execute(null);

        verifyMocks(true);
    }

    @Test
    @DisplayName("Параметры удаляются из-за пустого результата из YT")
    @ExpectedDatabase(
        value = "/jobs/sync_partner_external_params/after/deleted.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @SneakyThrows
    @JpaQueriesCount(2)
    void successEmptyParametersSync() {
        mockYt(List.of());

        job.execute(null);

        verifyMocks(true);
    }

    @Test
    @DisplayName("Исключение в джобе")
    @ExpectedDatabase(
        value = "/jobs/sync_partner_external_params/before/setup.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @SneakyThrows
    @JpaQueriesCount(0)
    void exceptionParametersSync() {
        mockYt(List.of(
            new YtPartnerExternalParam()
                .setId(123L)
                .setPartnerId(101L)
                .setType("UNKNOWN_TYPE")
                .setValue("updated value")
        ));

        softly.assertThatThrownBy(() -> job.execute(null));

        verifyMocks(false);
    }

    private void mockYt(List<YtPartnerExternalParam> expected) {
        Mockito.when(yt.tables()).thenReturn(ytTables);

        Mockito.when(ytTables.selectRows(
                eq(EXPECTED_QUERY),
                refEq(YTableEntryTypes.YSON),
                ArgumentMatchers.<Function<Iterator<YTreeMapNode>, Object>>any()
            ))
            .thenReturn(expected);
    }

    private void verifyMocks(boolean success) {
        verify(yt).tables();
        verify(ytTables).selectRows(
            eq(EXPECTED_QUERY),
            refEq(YTableEntryTypes.YSON),
            ArgumentMatchers.<Function<Iterator<YTreeMapNode>, Object>>any()
        );
        if (success) {
            assertLogs().anyMatch(BackLogAssertions.logEqualsTo(
                TskvLogRecord.info("Successfully synced params with types [READY_TO_SHIP_ON_DEADLINE]")
                    .setLoggingCode(YtEventCode.PARTNER_EXTERNAL_PARAMS_SYNC)
            ));
        }
    }
}
