package ru.yandex.market.mboc.common.datacamp.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.mboc.common.datacamp.model.DatacampImportQueueItem;
import ru.yandex.market.mboc.common.datacamp.repository.DatacampImportQueueRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mboc.common.datacamp.service.DatacampImportService.NEXT_TRY_DELAY_MAX_SECONDS;
import static ru.yandex.market.mboc.common.datacamp.service.DatacampImportService.NEXT_TRY_DELAY_STEP_SECONDS;

public class DatacampImportServiceImplTest {

    private DatacampImportQueueRepository datacampImportQueueRepository;
    private DatacampImportService datacampImportService;

    @Before
    public void setUp() {
        datacampImportQueueRepository = mock(DatacampImportQueueRepository.class);
        datacampImportService = new DatacampImportServiceImpl(
            TransactionHelper.MOCK, datacampImportQueueRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void markAsFailed() {
        var old = new DatacampImportQueueItem(1, "old", now().minusDays(2), now(), "cause", 97);
        var recent = new DatacampImportQueueItem(2, "recent", now(), now(), "cause", 10);
        var newKey = new BusinessSkuKey(3, "new");

        doReturn(List.of(old.copy(), recent.copy())).when(datacampImportQueueRepository)
            .findByIds(eq(Set.of(old.getBusinessSkuKey(), recent.getBusinessSkuKey(), newKey)));

        datacampImportService.markForImport(
                Map.of(
                    old.getBusinessSkuKey(), "error1",
                    recent.getBusinessSkuKey(), "error2",
                    newKey, "error3"
                )
        );

        var argCapture = (ArgumentCaptor<Collection<DatacampImportQueueItem>>)
                (Object) ArgumentCaptor.forClass(Collection.class);
        verify(datacampImportQueueRepository).insertOrUpdateFailedImports(argCapture.capture());

        var result = argCapture.getValue().stream()
                .collect(Collectors.toMap(DatacampImportQueueItem::getBusinessSkuKey, Function.identity()));
        assertThat(result).containsOnlyKeys(old.getBusinessSkuKey(), recent.getBusinessSkuKey(), newKey);

        var oldUpd = result.get(old.getBusinessSkuKey());
        assertThat(oldUpd.getFailedAt())
            .isCloseTo(old.getFailedAt(), within(1, SECONDS));
        assertThat(oldUpd.getNextTryAt())
            .isCloseTo(now().plusSeconds(NEXT_TRY_DELAY_MAX_SECONDS), within(10, SECONDS));
        assertThat(oldUpd.getCause()).isEqualTo("error1");
        assertThat(oldUpd.getAttempt()).isEqualTo(98);

        var recentUpd = result.get(recent.getBusinessSkuKey());
        assertThat(recentUpd.getFailedAt())
            .isCloseTo(recent.getFailedAt(), within(1, SECONDS));
        assertThat(recentUpd.getNextTryAt())
            .isCloseTo(now().plusSeconds(NEXT_TRY_DELAY_STEP_SECONDS * 11), within(10, SECONDS));
        assertThat(recentUpd.getCause()).isEqualTo("error2");
        assertThat(recentUpd.getAttempt()).isEqualTo(11);

        var newUpd = result.get(newKey);
        assertThat(newUpd.getFailedAt())
            .isCloseTo(now(), within(10, SECONDS));
        assertThat(newUpd.getNextTryAt())
            .isCloseTo(now().plusSeconds(NEXT_TRY_DELAY_STEP_SECONDS), within(10, SECONDS));
        assertThat(newUpd.getCause()).isEqualTo("error3");
    }
}
