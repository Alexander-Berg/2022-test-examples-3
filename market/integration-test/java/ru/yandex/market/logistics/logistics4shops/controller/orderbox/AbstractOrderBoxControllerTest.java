package ru.yandex.market.logistics.logistics4shops.controller.orderbox;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderBox;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderBoxItem;
import ru.yandex.market.logistics.logistics4shops.config.properties.FeatureProperties;

import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Абстрактный класс для тестов ручек по работе с грузоместами заказов.
 */
@ParametersAreNonnullByDefault
abstract class AbstractOrderBoxControllerTest extends AbstractIntegrationTest {
    @Autowired
    protected CheckouterAPI checkouterAPI;

    @Nonnull
    protected static OrderBox buildOrderBox(
        long id,
        String barcode,
        long weight,
        long width,
        long length,
        long height,
        List<OrderBoxItem> boxItems
    ) {
        return new OrderBox()
            .id(id)
            .barcode(barcode)
            .weight(weight)
            .width(width)
            .length(length)
            .height(height)
            .items(boxItems);
    }

    @Nonnull
    protected static OrderBoxItem buildBoxItem(long id, int count) {
        return new OrderBoxItem().id(id).count(count);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(checkouterAPI);
    }

    protected void setSaveBoxesThreshold(long id) {
        setupFeature(FeatureProperties::getOrderIdThresholdForSavingBoxesInDb, id);
    }
}
