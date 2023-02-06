package ru.yandex.market.logistic.api.utils.fulfillment;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.logistic.api.model.common.RegistryType;
import ru.yandex.market.logistic.api.model.fulfillment.InboundRegistry;
import ru.yandex.market.logistic.api.model.fulfillment.MovementRegistry;
import ru.yandex.market.logistic.api.model.fulfillment.OutboundRegistry;
import ru.yandex.market.logistic.api.model.fulfillment.RegistryItem;
import ru.yandex.market.logistic.api.utils.DateTime;

import static ru.yandex.market.logistic.api.utils.common.DtoFactory.createResourceId;
import static ru.yandex.market.logistic.api.utils.common.DtoFactory.createSingleRegistryBox;
import static ru.yandex.market.logistic.api.utils.common.DtoFactory.createSingleRegistryPallet;
import static ru.yandex.market.logistic.api.utils.common.DtoFactory.createUnitInfo;

public class RegistryDtoFactory {
    private RegistryDtoFactory() {
        throw new UnsupportedOperationException();
    }

    public static MovementRegistry createMinimalMovementRegistry() {
        return MovementRegistry.builder(
            createResourceId(1),
            createResourceId(2),
            RegistryType.PLANNED
        )
            .setDate(new DateTime("2020-10-19T10:53:00"))
            .setComment("this is a test registry")
            .setPallets(createSingleRegistryPallet("test pallet"))
            .setBoxes(createSingleRegistryBox("test box"))
            .setItems(createSingleRegistryItem("TestItem3000", "test item", "8.99"))
            .build();
    }

    public static InboundRegistry createMinimalInboundRegistry() {
        return InboundRegistry.builder(
            createResourceId(1),
            createResourceId(2),
            RegistryType.PLANNED
        )
            .setDate(new DateTime("2020-10-19T10:53:00"))
            .setComment("this is a test registry")
            .setPallets(createSingleRegistryPallet("test pallet"))
            .setBoxes(createSingleRegistryBox("test box"))
            .setItems(createSingleRegistryItem("TestItem3000", "test item", "8.99"))
            .build();
    }

    public static OutboundRegistry createMinimalOutboundRegistry() {
        return OutboundRegistry.builder(
            createResourceId(1),
            createResourceId(2),
            RegistryType.PLANNED
        )
            .setDate(new DateTime("2020-10-19T10:53:00"))
            .setComment("this is a test registry")
            .setPallets(createSingleRegistryPallet("test pallet"))
            .setBoxes(createSingleRegistryBox("test box"))
            .setItems(createSingleRegistryItem("TestItem3000", "test item", "8.99"))
            .build();
    }

    public static List<RegistryItem> createSingleRegistryItem(String name, String description, String price) {
        return Collections.singletonList(
            RegistryItem.builder(createUnitInfo(description))
                .setPrice(new BigDecimal(price))
                .setName(name)
                .build()
        );
    }
}
