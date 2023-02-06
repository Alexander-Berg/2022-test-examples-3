package ru.yandex.market.checkout.checkouter.returns;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.checkout.checkouter.returns.factories.BuyerReturnViewFactory;
import ru.yandex.market.checkout.checkouter.storage.returns.buyers.filters.BuyerReturnFilter;
import ru.yandex.market.checkout.checkouter.viewmodel.returns.BuyerReturnViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.returns.BuyerReturnViewModelCollection;

import static org.assertj.core.api.Assertions.assertThat;

class BuyerReturnServiceImplTest {

    private BuyerReturnService buyerReturnService;
    @Mock
    private BuyerReturnViewFactory buyerReturnViewFactory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        buyerReturnService = new BuyerReturnServiceImpl(buyerReturnViewFactory);
    }

    @Test
    void exceptionWhenLimitGreaterThanMaxLimit() {
        BuyerReturnFilter filter = BuyerReturnFilter.builder(123L)
                .withLimit(BuyerReturnService.MAX_RETURN_LIMIT + 1)
                .build();

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> buyerReturnService.getBuyerReturn(filter));
        assertThat(exception.getMessage()).contains("filter.limit must be less than");
    }

    @Test
    void okWhenNothingFound() {
        BuyerReturnFilter filter = BuyerReturnFilter.builder(123L).build();

        BuyerReturnViewModelCollection buyerReturn = buyerReturnService.getBuyerReturn(filter);

        assertThat(buyerReturn.getValues()).isEmpty();
        assertThat(buyerReturn.getOffset()).isEqualTo(0);
        assertThat(buyerReturn.getSize()).isEqualTo(0);
        assertThat(buyerReturn.hasNext()).isFalse();
    }

    @Test
    void returnsLessThanLimit() {
        BuyerReturnFilter filter = BuyerReturnFilter.builder(123L)
                .withLimit(4)
                .build();
        Mockito.when(buyerReturnViewFactory.loadByFilter(Mockito.any(BuyerReturnFilter.class)))
                .thenReturn(initReturnViews(3));

        BuyerReturnViewModelCollection buyerReturn = buyerReturnService.getBuyerReturn(filter);

        assertThat(buyerReturn.getValues()).hasSize(3);
        assertThat(buyerReturn.getOffset()).isEqualTo(0);
        assertThat(buyerReturn.getSize()).isEqualTo(3);
        assertThat(buyerReturn.hasNext()).isFalse();
    }

    @Test
    void returnsMoreThanLimit() {
        BuyerReturnFilter filter = BuyerReturnFilter.builder(123L)
                .withLimit(4)
                .build();
        Mockito.when(buyerReturnViewFactory.loadByFilter(Mockito.any(BuyerReturnFilter.class)))
                .thenReturn(initReturnViews(5));

        BuyerReturnViewModelCollection buyerReturn = buyerReturnService.getBuyerReturn(filter);

        assertThat(buyerReturn.getValues()).hasSize(4);
        assertThat(buyerReturn.getOffset()).isEqualTo(0);
        assertThat(buyerReturn.getSize()).isEqualTo(4);
        assertThat(buyerReturn.hasNext()).isTrue();
    }

    private List<BuyerReturnViewModel> initReturnViews(int count) {
        List<BuyerReturnViewModel> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BuyerReturnViewModel model = new BuyerReturnViewModel();
            model.setId((long) i);
            model.setOrderId(123L);
            model.setOrderItems(List.of());
            model.setStatus(ReturnStatus.STARTED_BY_USER);
            model.setCreatedDate(LocalDateTime.now());
            model.setStatusUpdatedDate(LocalDateTime.now());
            model.setLargeSize(false);
            model.setFastReturn(true);
            model.setOrderCreatedDate(LocalDateTime.now());
            model.setDeliveryCompensationType(DeliveryCompensationType.UNKNOWN);
            model.setBasicCompensationSum(BigDecimal.ONE);
            model.setCashbackCompensationSum(BigDecimal.ONE);
            model.setMerchantType(MerchantType.FBY);
            results.add(model);
        }
        return results;
    }
}
