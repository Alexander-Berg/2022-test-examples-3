package step;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import client.ReportClient;
import dto.Item;
import dto.requests.report.OfferItem;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;

@Slf4j
public class ReportSteps {

    private static final ReportClient REPORT = new ReportClient();

    @Nonnull
    public List<OfferItem> getValidOffer(
        long fesh,
        long feedId,
        String offerIdMask,
        Integer count,
        String rearrfactor,
        boolean isDifferentOffers
    ) {
        return getValidOffer(fesh, feedId, offerIdMask, count, rearrfactor, isDifferentOffers, false);
    }

    @Nonnull
    @Step("Выбираем валидный оффер в Репорте, задаём кол-во айтемов для заказа")
    public List<OfferItem> getValidOffer(
        long fesh,
        long feedId,
        String offerIdMask,
        Integer count,
        String rearrfactor,
        boolean isDifferentOffers,
        boolean onlyCourierOptions
    ) {
        log.debug("Searching valid item for order...");
        return Retrier.clientRetry(() -> {
            List<FoundOffer> foundOffers = REPORT.offerInfo(fesh, feedId, offerIdMask, rearrfactor);

            Assertions.assertFalse(foundOffers.isEmpty(), "Пришли пустые результаты от репорта");
            Assertions.assertTrue(foundOffers.size() >= count, "Не пришло достаточное количество офферов от репорта");
            List<OfferItem> offers = new ArrayList<>();
            if (isDifferentOffers) {
                Assertions.assertTrue(foundOffers.size() >= count, "Пришло меньше офферов чем ожидали");
                foundOffers.stream()
                    .filter(foundOffer -> !onlyCourierOptions || foundOffer.getDelivery())
                    .limit(count)
                    .forEach(foundOffer -> {
                            OfferItem offerItem = offers
                                .stream()
                                .filter(offer -> offer.getShopId().equals(foundOffer.getShopId()))
                                .findAny()
                                .orElse(null);
                            if (offerItem == null) {
                                offerItem = new OfferItem(foundOffer.getShopId(), new ArrayList<>());
                                offers.add(offerItem);
                            }
                            List<Item> items = offerItem.getItems();
                            items.add(buildItem(feedId, 1, foundOffer));
                        }
                    );
            } else {
                FoundOffer foundOffer = foundOffers.get(0);
                OfferItem offerItem = new OfferItem(foundOffer.getShopId(), new ArrayList<>());
                List<Item> items = offerItem.getItems();
                items.add(buildItem(feedId, count, foundOffer));
            }
            return offers;
        });
    }

    @Nonnull
    private Item buildItem(long feedId, int count, FoundOffer foundOffer) {
        return Item.builder()
            .feedId(feedId)
            .offerId(foundOffer.getShopOfferId())
            .buyerPrice(foundOffer.getPrice().floatValue())
            .count(count)
            .warehouseId(foundOffer.getWarehouseId())
            .shopId(foundOffer.getShopId())
            .showInfo(foundOffer.getFeeShow())
            .wareMd5(foundOffer.getWareMd5())
            .weight(foundOffer.getWeight() == null ? null : foundOffer.getWeight().longValue())
            .height(foundOffer.getHeight() == null ? null : foundOffer.getHeight().longValue())
            .width(foundOffer.getWidth() == null ? null : foundOffer.getWidth().longValue())
            .depth(foundOffer.getDepth() == null ? null : foundOffer.getDepth().longValue())
            .build();
    }

    @Nonnull
    private Integer getShipmentDay(Long regionId, OfferItem item, Long partnerId, DeliveryType deliveryOptions) {
        String itemMd5 = item.getItems()
            .stream()
            .map(Item::getWareMd5)
            .findAny()
            .orElseThrow();
        ActualDelivery actualDelivery = REPORT.actualDelivery(regionId, itemMd5);
        Assertions.assertFalse(actualDelivery.getResults().isEmpty(), "Пришли пустые результаты от репорта");
        ActualDeliveryResult actualDeliveryResult = actualDelivery.getResults().get(0);

        if (deliveryOptions.equals(DeliveryType.PICKUP)) {
            return actualDeliveryResult.getPickup()
                .stream()
                .filter(option -> option.getDeliveryServiceId().equals(partnerId))
                .findAny().orElseThrow(() -> new AssertionError("Отсутствует подходящий option в delivery"))
                .getShipmentDay();
        } else {
            return actualDeliveryResult.getDelivery()
                .stream()
                .filter(option -> option.getDeliveryServiceId().equals(partnerId))
                .findAny().orElseThrow(() -> new AssertionError("Отсутствует подходящий option в delivery"))
                .getShipmentDay();
        }

    }

    @Step("Ждем, когда в Репорте увеличится день отгрузки для заказа")
    public void verifyNewShipmentDayIsGreaterThanOld(
        Integer oldShipmentDay,
        long regionId,
        OfferItem item,
        Long partnerId,
        DeliveryType deliveryOptions
    ) {
        log.debug("Wait new shipment day from report");

        Retrier.retry(() -> {
                Integer newShipmentDay = getShipmentDay(regionId, item, partnerId, deliveryOptions);
                Assertions.assertTrue(newShipmentDay > oldShipmentDay, "Новый день отгрузки заказа меньше чем старый");
            },
            Retrier.RETRIES_BIG,
            1,
            TimeUnit.MINUTES
        );
    }

    @Step("Ждем, когда в Репорте уменьшится день отгрузки для заказа")
    public void verifyNewShipmentDayLessThanOld(
        Integer oldShipmentDay,
        long regionId,
        OfferItem item,
        Long partnerId,
        DeliveryType deliveryOptions
    ) {
        log.debug("Wait new shipment day from report");

        Retrier.retry(() -> {
                Integer newShipmentDay = getShipmentDay(regionId, item, partnerId, deliveryOptions);
                Assertions.assertTrue(newShipmentDay < oldShipmentDay, "Новый день отгрузки заказа больше чем старый");
            },
            Retrier.RETRIES_BIG,
            1,
            TimeUnit.MINUTES
        );
    }
}
