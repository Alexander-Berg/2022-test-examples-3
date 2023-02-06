package ru.yandex.market.tpl.billing.executor;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.service.yt.YtService;
import ru.yandex.market.tpl.billing.service.yt.imports.YtSortingCenterImportService;
import ru.yandex.market.tpl.billing.service.yt.imports.YtSortingCenterImportService.SortingCenterKey;
import ru.yandex.market.tpl.billing.task.executor.SyncSortingCentersExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SyncSortingCentersExecutorTest extends AbstractFunctionalTest {
    @Autowired
    YtService ytService;

    private SyncSortingCentersExecutor syncSortingCentersExecutor;
    @Autowired
    private YtSortingCenterImportService ytSortingCenterImportService;

    @BeforeEach
    void setup() {
        syncSortingCentersExecutor = new SyncSortingCentersExecutor(ytSortingCenterImportService);
    }

    @Test
    @DbUnitDataSet(after = "/database/executor/syncsortingcenters/after/id_1_2_3_added.csv"
    )
    void syncAllSortingCentersEmptyTable() {
        mockYtService(Set.of(1L, 2L, 3L));
        syncSortingCentersExecutor.doJob();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/executor/syncsortingcenters/before/sorting_center_id_1_already_exists.csv",
            after = "/database/executor/syncsortingcenters/after/id_1_2_3_added.csv")
    void syncAllSortingCentersNotEmptyTable() {
        mockYtService(Set.of(1L, 2L, 3L));
        syncSortingCentersExecutor.doJob();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/executor/syncsortingcenters/before/sorting_center_id_1_already_exists.csv",
            after = "/database/executor/syncsortingcenters/after/id_2_3_added.csv")
    void syncAllSortingCentersNotEmptyTableNotRemoveOldEntities() {
        mockYtService(Set.of(2L, 3L));
        syncSortingCentersExecutor.doJob();
    }

    private void mockYtService(Set<Long> response) {
        when(ytService.importFromYt(
            eq(SortingCenterKey.class),
            eq("//home/market/integration-test/tpl/sc"),
            eq("cdc"),
            eq("sorting_center"),
            any())
        )
            .thenReturn(
                Optional.of(response.stream()
                    .map(id -> SortingCenterKey.builder().id(id).name("name " + id).build())
                    .collect(Collectors.toSet()))
            );
    }
}
