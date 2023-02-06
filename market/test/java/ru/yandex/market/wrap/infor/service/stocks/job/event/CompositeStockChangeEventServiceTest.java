package ru.yandex.market.wrap.infor.service.stocks.job.event;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.entity.ChangedStockSku;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeStockChangeEventServiceTest extends SoftAssertionSupport {

    private static final ChangedStockSku CHANGED_STOCK_SKU = new ChangedStockSku("SKU", "STORER", 100);

    private StockChangeEventService firstStockChangeEventService;
    private StockChangeEventService secondStockChangeEventService;
    private CompositeStockChangeEventService multiStockChangeEventService;

    @BeforeEach
    void setUp() {
        firstStockChangeEventService = createDefaultStockChangeEventServiceMock();
        secondStockChangeEventService = createDefaultStockChangeEventServiceMock();
        multiStockChangeEventService = new CompositeStockChangeEventService(
            ImmutableList.of(firstStockChangeEventService, secondStockChangeEventService));
    }

    /**
     * Сценарий #1: оба сервиса уперлись в квоту limit / 2.
     */
    @Test
    void bothServicesUseAllQuota() {
        final int limit = 20;
        final int delay = 0;
        List<Collection<ChangedStockSku>> skusOfChangedStocks =
            multiStockChangeEventService.getSkusOfChangedStocks(limit, delay);

        softly.assertThat(skusOfChangedStocks.get(0).size()).isEqualTo(limit / 2);
        softly.assertThat(skusOfChangedStocks.get(1).size()).isEqualTo(limit / 2);
    }

    /**
     * Сценарий #2: первый сервис не испоьзовал всю квоту limit / 2, а второй ее добрал.
     */
    @Test
    void firstServiceDoesNotUseAllQuotaButSecondDoes() {
        final int limit = 20;
        final int delay = 0;
        final int firstAnswerCount = 3;
        final int secondAnswerCount = 20 - firstAnswerCount;

        mockAnswer(firstStockChangeEventService, firstAnswerCount);
        List<Collection<ChangedStockSku>> skusOfChangedStocks =
            multiStockChangeEventService.getSkusOfChangedStocks(limit, delay);

        softly.assertThat(skusOfChangedStocks.get(0).size()).isEqualTo(firstAnswerCount);
        softly.assertThat(skusOfChangedStocks.get(1).size()).isEqualTo(secondAnswerCount);
    }

    /**
     * Сценарий #3: оба сервиса не испоьзовали всю квоту.
     */
    @Test
    void bothServicesNotUseAllQuota() {
        final int limit = 20;
        final int delay = 0;
        final int firstAnswerCount = 3;
        final int secondAnswerCount = 2;

        mockAnswer(firstStockChangeEventService, firstAnswerCount);
        mockAnswer(secondStockChangeEventService, secondAnswerCount);

        List<Collection<ChangedStockSku>> skusOfChangedStocks =
            multiStockChangeEventService.getSkusOfChangedStocks(limit, delay);

        softly.assertThat(skusOfChangedStocks.get(0).size()).isEqualTo(firstAnswerCount);
        softly.assertThat(skusOfChangedStocks.get(1).size()).isEqualTo(secondAnswerCount);
    }

    /**
     * Сценарий #5: первый сервис вернул пустой ответ.
     */
    @Test
    void firstServiceReturnsEmptyResult() {
        final int limit = 20;
        final int delay = 0;
        final int firstAnswerCount = 0;
        final int secondAnswerCount = 20 - firstAnswerCount;

        mockAnswer(firstStockChangeEventService, firstAnswerCount);
        List<Collection<ChangedStockSku>> skusOfChangedStocks =
            multiStockChangeEventService.getSkusOfChangedStocks(limit, delay);

        softly.assertThat(skusOfChangedStocks.get(0).size()).isEqualTo(firstAnswerCount);
        softly.assertThat(skusOfChangedStocks.get(1).size()).isEqualTo(secondAnswerCount);
    }

    /**
     * Сценарий #6: второй сервис вернул пустой ответ.
     */
    @Test
    void secondServiceReturnsEmptyResult() {
        final int limit = 20;
        final int delay = 0;
        final int firstAnswerCount = 10;
        final int secondAnswerCount = 0;

        mockAnswer(secondStockChangeEventService, secondAnswerCount);
        List<Collection<ChangedStockSku>> skusOfChangedStocks =
            multiStockChangeEventService.getSkusOfChangedStocks(limit, delay);

        softly.assertThat(skusOfChangedStocks.get(0).size()).isEqualTo(firstAnswerCount);
        softly.assertThat(skusOfChangedStocks.get(1).size()).isEqualTo(secondAnswerCount);
    }

    /**
     * Сценарий #7: оба сервиса вернули пустой ответ.
     */
    @Test
    void bothServicesReturnsEmptyResult() {
        final int limit = 20;
        final int delay = 0;
        final int firstAnswerCount = 0;
        final int secondAnswerCount = 0;

        mockAnswer(firstStockChangeEventService, firstAnswerCount);
        mockAnswer(secondStockChangeEventService, secondAnswerCount);

        List<Collection<ChangedStockSku>> skusOfChangedStocks =
            multiStockChangeEventService.getSkusOfChangedStocks(limit, delay);

        softly.assertThat(skusOfChangedStocks.get(0).size()).isEqualTo(firstAnswerCount);
        softly.assertThat(skusOfChangedStocks.get(1).size()).isEqualTo(secondAnswerCount);
    }

    /**
     * Сценарий #8: случай, когда лимиит нечетное число.
     */
    @Test
    void withEvenLimit() {
        final int limit = 17;
        final int delay = 0;
        final int firstAnswerCount = 8;
        final int secondAnswerCount = 8;

        List<Collection<ChangedStockSku>> skusOfChangedStocks =
            multiStockChangeEventService.getSkusOfChangedStocks(limit, delay);

        softly.assertThat(skusOfChangedStocks.get(0).size()).isEqualTo(firstAnswerCount);
        softly.assertThat(skusOfChangedStocks.get(1).size()).isEqualTo(secondAnswerCount);
    }

    /**
     * Сценарий #9: случай, когда лимит равен 1 для 2-х сервисов.
     */
    @Test
    void withLimitIsEqualToOne() {
        final int limit = 1;
        final int delay = 0;
        final int firstAnswerCount = 0;
        final int secondAnswerCount = 0;

        List<Collection<ChangedStockSku>> skusOfChangedStocks =
            multiStockChangeEventService.getSkusOfChangedStocks(limit, delay);

        softly.assertThat(skusOfChangedStocks.get(0).size()).isEqualTo(firstAnswerCount);
        softly.assertThat(skusOfChangedStocks.get(1).size()).isEqualTo(secondAnswerCount);
    }

    /**
     * Сценарий #10: случай, когда лимит равен 0 для 2-х сервисов.
     */
    @Test
    void withLimitIsEqualToZero() {
        final int limit = 0;
        final int delay = 0;
        final int firstAnswerCount = 0;
        final int secondAnswerCount = 0;

        List<Collection<ChangedStockSku>> skusOfChangedStocks =
            multiStockChangeEventService.getSkusOfChangedStocks(limit, delay);

        softly.assertThat(skusOfChangedStocks.get(0).size()).isEqualTo(firstAnswerCount);
        softly.assertThat(skusOfChangedStocks.get(1).size()).isEqualTo(secondAnswerCount);
    }

    /**
     * Сценарий #11: один сервис использовал всю квоту.
     */
    @Test
    void singleServiceUsesAllQuota() {
        final int limit = 20;
        final int delay = 0;

        multiStockChangeEventService = new CompositeStockChangeEventService(ImmutableList.of(firstStockChangeEventService));

        List<Collection<ChangedStockSku>> skusOfChangedStocks =
            multiStockChangeEventService.getSkusOfChangedStocks(limit, delay);

        softly.assertThat(skusOfChangedStocks.get(0).size()).isEqualTo(limit);
    }

    /**
     * Сценарий #12: один сервис использовал часть квоты.
     */
    @Test
    void singleServiceDoesNotUseAllQuota() {
        final int limit = 20;
        final int delay = 0;
        final int firstAnswerCount = 3;

        mockAnswer(firstStockChangeEventService, firstAnswerCount);
        multiStockChangeEventService = new CompositeStockChangeEventService(ImmutableList.of(firstStockChangeEventService));

        List<Collection<ChangedStockSku>> skusOfChangedStocks =
            multiStockChangeEventService.getSkusOfChangedStocks(limit, delay);

        softly.assertThat(skusOfChangedStocks.get(0).size()).isEqualTo(firstAnswerCount);
    }

    private StockChangeEventService createDefaultStockChangeEventServiceMock() {
        StockChangeEventService service = mock(StockChangeEventService.class);

        when(service.getSkusOfChangedStocks(anyInt(), anyInt()))
            .thenAnswer(invocation -> {
                final int limit = invocation.getArgument(0);
                return Collections.nCopies(limit, CHANGED_STOCK_SKU);
            });

        return service;
    }

    private void mockAnswer(StockChangeEventService stockChangeEventService, int answer) {
        when(stockChangeEventService.getSkusOfChangedStocks(anyInt(), anyInt()))
            .thenAnswer(invocation -> {
                final int limit = invocation.getArgument(0);
                return Collections.nCopies(Math.min(limit, answer), CHANGED_STOCK_SKU);
            });
    }
}
