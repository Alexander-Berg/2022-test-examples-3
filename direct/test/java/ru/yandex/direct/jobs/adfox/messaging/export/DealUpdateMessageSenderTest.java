package ru.yandex.direct.jobs.adfox.messaging.export;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealDirectSync;
import ru.yandex.direct.core.testing.data.TestDeals;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled("Не работающая фича 'частные сделки'")
class DealUpdateMessageSenderTest {

    private static final String OUTPUT_QUEUE_PATH = "//home/direct/test/export/adfox/events_to_adfox";

    private ApiServiceTransaction transaction;

    @BeforeEach
    void setUp() {
        transaction = mock(ApiServiceTransaction.class);
        when(transaction.modifyRows(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void sendDeals_success() {
        Deal dealWithNullStatus = TestDeals.defaultPrivateDeal(ClientId.fromLong(1L), 123L);
        dealWithNullStatus.setDirectStatus(null);
        Deal dealWithoutId = TestDeals.defaultPrivateDeal(ClientId.fromLong(1L), 124L);
        dealWithoutId.setId(null);
        Deal normalDeal = TestDeals.defaultPrivateDeal(ClientId.fromLong(1L), 125L);

        Collection<DealDirectSync> deals = asList(dealWithNullStatus, dealWithoutId, normalDeal);

        DealUpdateMessageSender sender = new DealUpdateMessageSender(OUTPUT_QUEUE_PATH);
        List<DealDirectSync> dealsSynced = sender.sendDeals(transaction, deals);
        // ожидаем, что сделки с null-значениями необходимых полей не попадут в ответ
        assertThat(dealsSynced).containsExactly(normalDeal);
    }

}
