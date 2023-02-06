package ru.yandex.market.ff.service.implementation;

import java.util.function.Consumer;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.exception.http.RequestConcurrentUpdateException;
import ru.yandex.market.ff.exception.http.RequestNotFoundException;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.ShopRequestLockingService;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ShopRequestLockingServiceTest extends IntegrationTest {

    @Autowired
    @Qualifier("requiresNewTransactionTemplate")
    private TransactionTemplate requiresNewTransactionTemplate;

    @Autowired
    private ShopRequestLockingService shopRequestLockingService;

    @Autowired
    private ShopRequestRepository shopRequestRepository;

    @AfterEach
    void resetSpies() {
        super.resetMocks();
        Mockito.reset(shopRequestRepository);
    }

    @BeforeEach
    void setSpies() {
        shopRequestRepository = spy(shopRequestRepository);
    }

    /**
     * Кейс с конкурентным апдейтом totalCount:
     * 1. C взятием оптимистичной блокировки пытаемся увеличить totalCount=60 на 9.
     * 2. В момент первой попытки, другая транзакция увеличивает totalCount на 3 и коммитит изменения. totalCount = 63.
     * 3. При коммите первой транзакции ловим исключение из-за версии (optimistic lock).
     * 4. На второй попытке корректно отрабатываем и увеличиваем totalCount=63 на 9 => totalCount=72.
     */
    @Test
    @DatabaseSetup("classpath:service/shop-request-locking/before-update.xml")
    @ExpectedDatabase(value = "classpath:service/shop-request-locking/after-update.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void successUpdateOnRetry() {

        var requestId = 0L;

        Consumer<ShopRequest> shopRequestConsumer = shopRequest -> {

            if (shopRequest.getVersion() == 1L) {
                requiresNewTransactionTemplate.execute(action2 -> {
                    var request = shopRequestRepository.findById(requestId);
                    request.setItemsTotalCount(request.getItemsTotalCount() + 3);
                    shopRequestRepository.save(request);

                    return null;
                });
            }

            shopRequest.setItemsTotalCount(shopRequest.getItemsTotalCount() + 9);
            shopRequestRepository.save(shopRequest);
        };

        @SuppressWarnings("unchecked")
        var shopRequestConsumerSpy = (Consumer<ShopRequest>) mock(Consumer.class, delegatesTo(shopRequestConsumer));

        shopRequestLockingService.doWithOptimisticLockingAndRetries(requestId, shopRequestConsumerSpy);

        verify(shopRequestConsumerSpy, times(2)).accept(any());
        verify(shopRequestRepository, times(3)).save(any(ShopRequest.class));
    }

    /**
     * При попытке каждого апдейта, другая транзакция опережает текущую.
     * В итоге, после 3-х попыток получим исключение.
     * <p>
     * Состояние до: totalCount=60
     * Состояние после: totalCount=72 (4 * кол-во ретраев = 12)
     */
    @Test
    @DatabaseSetup("classpath:service/shop-request-locking/before-update.xml")
    @ExpectedDatabase(value = "classpath:service/shop-request-locking/after-update.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void invalidUpdateAfterAllRetires() {

        var requestId = 0L;

        Consumer<ShopRequest> shopRequestConsumer = shopRequest -> {

            requiresNewTransactionTemplate.execute(action2 -> {
                var request = shopRequestRepository.findById(requestId);
                request.setItemsTotalCount(request.getItemsTotalCount() + 4);
                shopRequestRepository.save(request);

                return null;
            });

            shopRequest.setItemsTotalCount(shopRequest.getItemsTotalCount() + 7);
            shopRequestRepository.save(shopRequest);
        };

        @SuppressWarnings("unchecked")
        var shopRequestConsumerSpy = (Consumer<ShopRequest>) mock(Consumer.class, delegatesTo(shopRequestConsumer));

        Assertions.assertThrows(RequestConcurrentUpdateException.class, () ->
                shopRequestLockingService.doWithOptimisticLockingAndRetries(requestId, shopRequestConsumerSpy)
        );

        verify(shopRequestConsumerSpy, times(3)).accept(any());
        verify(shopRequestRepository, times(6)).save(any(ShopRequest.class));
    }

    /**
     * Делаем инкремент версии в вызове с оптимистичной блокировкой, даже если нет никакого апдейта shopRequest.
     */
    @Test
    @DatabaseSetup("classpath:service/shop-request-locking/before-update.xml")
    @ExpectedDatabase(value = "classpath:service/shop-request-locking/after-with-increment-version-only.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void incrementVersionEvenWithoutUpdate() {

        var requestId = 0L;

        Consumer<ShopRequest> shopRequestConsumer = shopRequest ->
                requiresNewTransactionTemplate.execute(action2 -> shopRequestRepository.findById(requestId));

        @SuppressWarnings("unchecked")
        var shopRequestConsumerSpy = (Consumer<ShopRequest>) mock(Consumer.class, delegatesTo(shopRequestConsumer));

        shopRequestLockingService.doWithOptimisticLockingAndRetries(requestId, shopRequestConsumerSpy);

        verify(shopRequestConsumerSpy, times(1)).accept(any());
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-locking/before-update.xml")
    void lockOnNotFoundRequestId() {

        var requestId = 1111L;

        Consumer<ShopRequest> shopRequestConsumer =
                shopRequest -> shopRequest.setItemsTotalCount(shopRequest.getItemsTotalCount() + 7);

        @SuppressWarnings("unchecked")
        var shopRequestConsumerSpy = (Consumer<ShopRequest>) mock(Consumer.class, delegatesTo(shopRequestConsumer));

        Assertions.assertThrows(RequestNotFoundException.class, () ->
                shopRequestLockingService.doWithOptimisticLockingAndRetries(requestId, shopRequestConsumerSpy)
        );

        verify(shopRequestConsumerSpy, times(0)).accept(any());
    }

}
