package ru.yandex.market.partner.content.common.service.mock;

import ru.yandex.market.ir.autogeneration.common.helpers.MboMappingsServiceHelper;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MboMappingsServiceHelperMock extends MboMappingsServiceHelper {

    private final Map<ShopIdSskuKey, SupplierOffer.Offer> offers = new HashMap<>();
    private final Map<ShopIdSskuKey, MboMappings.ReprocessOffersClassificationResponse.Status> offerToStatus =
        new HashMap<>();

    public MboMappingsServiceHelperMock() {
        super(null);
    }

    @Override
    public List<SupplierOffer.Offer> searchMappingsByKeys(Collection<String> shopSkus, int shopId) {
        return shopSkus.stream()
            .map(shopSku -> new ShopIdSskuKey(shopId, shopSku))
            .map(offers::get)
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, MboMappings.ReprocessOffersClassificationResponse.Status> sendReclassificationRequest(
        Collection<String> shopSkus, int shopId
    ) {
        return shopSkus.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                shopSku -> offerToStatus.get(new ShopIdSskuKey(shopId, shopSku))
            ));
    }

    public void addOffer(int shopId, String shopSku, SupplierOffer.Offer offer) {
        addOffer(shopId, shopSku, offer, MboMappings.ReprocessOffersClassificationResponse.Status.OK);
    }
    public void addOffer(int shopId,
                         String shopSku,
                         SupplierOffer.Offer offer,
                         MboMappings.ReprocessOffersClassificationResponse.Status status) {
        ShopIdSskuKey key = new ShopIdSskuKey(shopId, shopSku);
        offers.put(key, offer);
        offerToStatus.put(key, status);
    }

    public void changeOffersInternalStatus(Iterable<GcSkuTicket> tickets,
                                           SupplierOffer.Offer.InternalProcessingStatus status) {
        tickets.forEach(ticket -> {
            ShopIdSskuKey key = new ShopIdSskuKey(ticket.getPartnerShopId(), ticket.getShopSku());
            SupplierOffer.Offer offer = offers.get(key);
            offers.put(key, offer.toBuilder()
                .setInternalProcessingStatus(status)
                .build());
        });
    }

    //todo-k-semeon бахнуть статический метод
    private static class ShopIdSskuKey {
        private final int shopId;
        private final String shopSku;

        private ShopIdSskuKey(int shopId, String shopSku) {
            this.shopId = shopId;
            this.shopSku = shopSku;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ShopIdSskuKey that = (ShopIdSskuKey) o;
            return shopId == that.shopId &&
                Objects.equals(shopSku, that.shopSku);
        }

        @Override
        public int hashCode() {
            return Objects.hash(shopId, shopSku);
        }
    }
}
