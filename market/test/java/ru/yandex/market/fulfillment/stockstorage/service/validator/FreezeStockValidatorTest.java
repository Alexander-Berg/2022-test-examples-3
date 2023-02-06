package ru.yandex.market.fulfillment.stockstorage.service.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReason;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReasonType;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.DuplicateFreezeException;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.FreezeNotFoundException;
import ru.yandex.market.fulfillment.stockstorage.repository.StockFreezeRepository;

import static org.mockito.Mockito.when;

public class FreezeStockValidatorTest {

    private static final FreezeReason REASON = FreezeReason.of("AAA", FreezeReasonType.ORDER);
    private StockFreezeRepository reservedStockRepository = Mockito.mock(StockFreezeRepository.class);
    private FreezeStockValidator freezeStockValidator = new FreezeStockValidator(reservedStockRepository);

    @Test
    public void newFreeze() {
        when(reservedStockRepository.existsByReason(REASON)).thenReturn(false);
        freezeStockValidator.validateFreezeNotExist(REASON);
    }

    @Test
    public void throwExceptionWhenFreezeAlreadyCreated() {
        when(reservedStockRepository.existsByReason(REASON)).thenReturn(true);

        Assertions.assertThrows(DuplicateFreezeException.class,
                () -> freezeStockValidator.validateFreezeNotExist(REASON),
                "Duplicate freeze");
    }

    @Test
    public void forUnfreeze() {
        when(reservedStockRepository.existsByReason(REASON)).thenReturn(true);
        freezeStockValidator.validateFreezeExist(REASON);
    }

    @Test
    public void throwExceptionWhenFreezeNotFound() {
        when(reservedStockRepository.existsByReason(REASON)).thenReturn(false);

        Assertions.assertThrows(FreezeNotFoundException.class,
                () -> freezeStockValidator.validateFreezeExist(REASON),
                FreezeNotFoundException.MESSAGE);
    }
}
