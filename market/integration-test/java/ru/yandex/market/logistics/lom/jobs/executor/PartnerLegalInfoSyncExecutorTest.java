package ru.yandex.market.logistics.lom.jobs.executor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.RichYPath;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.YtPartnerLegalInfo;
import ru.yandex.market.logistics.lom.service.yt.YtService;
import ru.yandex.market.logistics.lom.service.yt.dto.YtCluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.YtUtils.buildMapNode;
import static ru.yandex.market.logistics.lom.utils.YtUtils.getIterator;
import static ru.yandex.market.logistics.lom.utils.YtUtils.getLowerRowIndex;
import static ru.yandex.market.logistics.lom.utils.YtUtils.getUpperRowIndex;

@DisplayName("Тесты на синхронизацию юридической информации о партнёрах")
class PartnerLegalInfoSyncExecutorTest extends AbstractContextualTest {
    private static final String TABLE_PATH = "//home/mbi_partner_info/latest";

    @Autowired
    private PartnerLegalInfoSyncExecutor partnerLegalInfoSyncExecutor;

    @Autowired
    private Yt hahnYt;

    @Autowired
    private Yt arnoldYt;

    @Autowired
    private YtTables ytTables;

    @Autowired
    private YtService ytService;

    @Test
    @DisplayName("Синхронизация данных по одному партнёру (данных ещё нет)")
    @ExpectedDatabase(
        value = "/jobs/executor/partnerLegalInfoSyncExecutor/after/sync_single_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void singlePartnerSync() {
        when(hahnYt.tables()).thenReturn(ytTables);
        when(ytTables.read(any(), refEq(YTableEntryTypes.YSON)))
            .then(invocation -> {
                if (getLowerRowIndex(invocation.getArgument(0)) == 0) {
                    return getIterator(Set.of(buildYTreeMapNode("1", "SUPPLIER", 2L, "88005553535")));
                }
                return null;
            });
        partnerLegalInfoSyncExecutor.doJob(null);

        verifyYtRead(ytTables, List.of(buildYPath(0, 1), buildYPath(1, 2)));
    }

    @Test
    @DisplayName("Синхронизация данных по одному партнёру (данных ещё нет) nullable поля")
    @ExpectedDatabase(
        value = "/jobs/executor/partnerLegalInfoSyncExecutor/after/sync_single_partner_nullable_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void singlePartnerSyncNullableFields() {
        when(hahnYt.tables()).thenReturn(ytTables);
        when(ytTables.read(any(), refEq(YTableEntryTypes.YSON)))
            .then(invocation -> {
                if (getLowerRowIndex(invocation.getArgument(0)) == 0) {
                    return getIterator(Set.of(buildYTreeMapNode("1", "SUPPLIER", null, null)));
                }
                return null;
            });
        partnerLegalInfoSyncExecutor.doJob(null);

        verifyYtRead(ytTables, List.of(buildYPath(0, 1), buildYPath(1, 2)));
    }

    @Test
    @DisplayName("Синхронизация данных по двум партнёрам")
    @ExpectedDatabase(
        value = "/jobs/executor/partnerLegalInfoSyncExecutor/after/two_partners_sync.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void twoPartnersSync() {
        when(hahnYt.tables()).thenReturn(ytTables);
        when(ytTables.read(any(), refEq(YTableEntryTypes.YSON)))
            .then(invocation -> {
                if (getLowerRowIndex(invocation.getArgument(0)) == 0) {
                    return getIterator(Set.of(buildYTreeMapNode("1", "SUPPLIER", 2L, "88005553535")));
                }
                if (getLowerRowIndex(invocation.getArgument(0)) == 1) {
                    return getIterator(Set.of(buildYTreeMapNode("2", "SHOP", 3L, "89005553535")));
                }
                return null;
            });
        partnerLegalInfoSyncExecutor.doJob(null);

        verifyYtRead(ytTables, List.of(buildYPath(0, 1), buildYPath(1, 2), buildYPath(2, 3)));
    }

    @Test
    @DisplayName("Синхронизация данных по одному партнёру (обновление существующих данных)")
    @DatabaseSetup("/jobs/executor/partnerLegalInfoSyncExecutor/before/single_partner_update.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/partnerLegalInfoSyncExecutor/after/single_partner_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void singlePartnerUpdate() {
        when(hahnYt.tables()).thenReturn(ytTables);
        when(ytTables.read(any(), refEq(YTableEntryTypes.YSON)))
            .then(invocation -> {
                if (getLowerRowIndex(invocation.getArgument(0)) == 0) {
                    return getIterator(Set.of(buildYTreeMapNode("1", "SUPPLIER", 2L, "89999999")));
                }
                return null;
            });
        partnerLegalInfoSyncExecutor.doJob(null);

        verifyYtRead(ytTables, List.of(buildYPath(0, 1), buildYPath(1, 2)));
    }

    @Test
    @DisplayName("Синхронизация данных по партнёру - hahn и arnold недоступны")
    void singlePartnerSyncFail() {
        when(hahnYt.tables()).thenThrow(new RuntimeException("Error connecting to hahn"));
        when(arnoldYt.tables()).thenThrow(new RuntimeException("Error connecting to arnold"));
        softly.assertThatThrownBy(() -> partnerLegalInfoSyncExecutor.doJob(null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContainingAll(
                "ARNOLD=java.lang.RuntimeException: Error connecting to arnold",
                "HAHN=java.lang.RuntimeException: Error connecting to hahn"
            );
    }

    private void verifyYtRead(YtTables ytTables, List<RichYPath> expectedPaths) {
        ArgumentCaptor<RichYPath> requestCaptor = ArgumentCaptor.forClass(RichYPath.class);

        verify(ytTables, times(expectedPaths.size())).read(
            requestCaptor.capture(),
            refEq(YTableEntryTypes.YSON)
        );
        List<RichYPath> actualValue = requestCaptor.getAllValues();
        StreamEx.of(actualValue).zipWith(expectedPaths.stream()).forKeyValue(this::assertYPathsAreEqual);

        expectedPaths.forEach(
            path -> verify(ytService).readTableFromRowToRow(
                List.of(YtCluster.HAHN, YtCluster.ARNOLD),
                TABLE_PATH,
                YtPartnerLegalInfo.class,
                getLowerRowIndex(path),
                getUpperRowIndex(path),
                Set.of("partner_id", "market_id", "service_id", "phone_number")
            )
        );
    }

    void assertYPathsAreEqual(RichYPath actualValue, RichYPath expectedValue) {
        softly.assertThat(actualValue.getColumns())
            .containsExactlyInAnyOrderElementsOf(expectedValue.getColumns());
        softly.assertThat(actualValue.justPath().toString()).isEqualTo(expectedValue.justPath().toString());
        softly.assertThat(actualValue.getRanges())
            .containsExactlyInAnyOrderElementsOf(expectedValue.getRanges());
    }

    @Nonnull
    private YTreeMapNode buildYTreeMapNode(String partnerId, String serviceId, Long marketId, String phoneNumber) {
        Map<String, Object> map = new HashMap<>();
        map.put("partner_id", partnerId);
        map.put("service_id", serviceId);
        map.put("market_id", marketId);
        map.put("phone_number", phoneNumber);
        return buildMapNode(map);
    }

    @Nonnull
    private RichYPath buildYPath(long fromRow, long toRow) {
        return (RichYPath) YPath.simple(TABLE_PATH)
            .withRange(fromRow, toRow)
            .withColumns(Set.of("partner_id", "market_id", "service_id", "phone_number"));
    }
}
