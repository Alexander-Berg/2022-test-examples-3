package ru.yandex.market.ff.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.implementation.RequestSizeCalculationServiceImpl;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.when;

public class RequestSizeCalculationServiceTest {

    private RequestSizeCalculationService service;
    private ShopRequestFetchingService shopRequestService;
    private RequestItemService requestItemService;
    private SoftAssertions assertions;

    @BeforeEach
    public void init() {
        shopRequestService = Mockito.mock(ShopRequestFetchingService.class);
        requestItemService = Mockito.mock(RequestItemService.class);
        service = new RequestSizeCalculationServiceImpl(shopRequestService, requestItemService);
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    @Test
    public void countTakenItemsWorksCorrect() {
        ShopRequest request = new ShopRequest();
        request.setItems(asList(createRequestItem(1, null, null, null, 1),
            createRequestItem(2, null, null, null, 12)));
        when(shopRequestService.getRequestOrThrow(10)).thenReturn(request);
        long takenItems = service.countTakenItems(10);
        assertions.assertThat(takenItems).isEqualTo(13);
    }

    @Test
    public void countTakenPalletsWorksCorrect() {
        ShopRequest request = new ShopRequest();
        request.setItems(asList(
            createRequestItem(1, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(141), 2),
            createRequestItem(2, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(166), 1),
            createRequestItem(3, null, BigDecimal.valueOf(100), BigDecimal.valueOf(166), 10),
            createRequestItem(4, BigDecimal.valueOf(100), null, BigDecimal.valueOf(166), 12),
            createRequestItem(5, BigDecimal.valueOf(100), BigDecimal.valueOf(100), null, 14)
        ));
        when(shopRequestService.getRequestOrThrow(10)).thenReturn(request);
        long takenPallets = service.countTakenPallets(10);
        assertions.assertThat(takenPallets).isEqualTo(4);
    }

    @Test
    public void calcRequiredPalletsForItemWorksCorrect() {
        RequestItem item =
            createRequestItem(1, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(141), 2);
        BigDecimal requiredPallets = service.calcRequiredPalletsForItem(item);
        assertions.assertThat(requiredPallets).isEqualTo(BigDecimal.valueOf(2.02));
    }

    @Test
    public void calcRequiredPalletsForRequestItemsWhenOneItemHasNotDimensions() {
        RequestItem item =
                createRequestItem(1, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(141), 2);
        RequestItem item2 = createRequestItem(2, null, null, null, 2);

        List<RequestItem> items = List.of(item, item2);
        Map<Long, BigDecimal> requiredPalletsForItems = service.calcRequiredPalletsForRequestItems(items);
        BigDecimal firstItemPalletsCount = requiredPalletsForItems.get(1L);
        BigDecimal secondItemPalletsCount = requiredPalletsForItems.get(2L);
        assertions.assertThat(firstItemPalletsCount).isEqualTo(BigDecimal.valueOf(2.02));
        assertions.assertThat(secondItemPalletsCount).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void calcRequiredPalletsForRequestItemsWhenAllItemsHasNotDimensions() {
        RequestItem item =
                createRequestItem(1, null, BigDecimal.valueOf(100), BigDecimal.valueOf(141), 3);
        RequestItem item2 = createRequestItem(2, null, null, null, 1);

        List<RequestItem> items = List.of(item, item2);
        Map<Long, BigDecimal> requiredPalletsForItems = service.calcRequiredPalletsForRequestItems(items);
        BigDecimal firstItemPalletsCount = requiredPalletsForItems.get(1L);
        BigDecimal secondItemPalletsCount = requiredPalletsForItems.get(2L);
        assertions.assertThat(firstItemPalletsCount).isEqualTo(BigDecimal.valueOf(0.75));
        assertions.assertThat(secondItemPalletsCount).isEqualTo(BigDecimal.valueOf(0.25));
    }

    @Nonnull
    private RequestItem createRequestItem(long id,
                                          @Nullable BigDecimal width,
                                          @Nullable BigDecimal height,
                                          @Nullable BigDecimal length,
                                          int count) {
        RequestItem item = new RequestItem();
        item.setId(id);
        item.setWidth(width);
        item.setHeight(height);
        item.setLength(length);
        item.setCount(count);
        return item;
    }
}
