package ru.yandex.market.util.report;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.util.report.generators.offerinfo.OfferInfoGeneratorParameters;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.LocalDeliveryOption;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.Item;
import ru.yandex.market.shopadminstub.model.ItemDeliveryOption;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class ReportParameters implements OfferInfoGeneratorParameters {
    private final long shopId;
    private CartRequest cartRequest;
    private Set<FeedOfferId> hiddenItems = Collections.emptySet();
    private Consumer<LocalDeliveryOption> deliveryOptionModifier;
    private Consumer<FoundOffer> offerModifier;

    public ReportParameters(long shopId) {
        this.shopId = shopId;
    }

    public CartRequest getCartRequest() {
        return cartRequest;
    }

    public void setCartRequest(CartRequest cartRequest) {
        this.cartRequest = cartRequest;
    }

    public Set<FeedOfferId> getHiddenItems() {
        return hiddenItems;
    }

    public void setHiddenItems(Set<FeedOfferId> hiddenItems) {
        this.hiddenItems = hiddenItems;
    }

    @Override
    public long getShopId() {
        return shopId;
    }

    @Override
    public List<FoundOffer> getFoundOffers() {
        return cartRequest.getItems().entrySet().stream()
                .filter(e -> !hiddenItems.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .map(this::mapItemToFoundOffer)
                .collect(Collectors.toList());
    }

    public void setDeliveryOptionModifier(Consumer<LocalDeliveryOption> deliveryOptionModifier) {
        this.deliveryOptionModifier = deliveryOptionModifier;
    }

    private FoundOffer mapItemToFoundOffer(Item item) {
        FoundOffer foundOffer = new FoundOffer();
        foundOffer.setFeedId(item.getFeedId());
        foundOffer.setShopOfferId(item.getOfferId());
        foundOffer.setShopCurrency(Currency.findByName(item.getCurrency()));
        foundOffer.setOnStock(item.isPickupPossible());
        foundOffer.setIsGlobal(item.isGlobal());
        foundOffer.setShopPrice(item.getPrice());
        foundOffer.setWareMd5(item.getWareMd5());
        foundOffer.setShopSku(item.getShopSku());
        foundOffer.setSku(item.getSku());
        foundOffer.setSupplierId(item.getFulfilmentShopId());
        foundOffer.setCpa(item.getCpa());

        List<LocalDeliveryOption> localDeliveryOptions = item.getDeliveryOptions()
                .stream()
                .map(this::mapLocalDeliveryOption)
                .collect(Collectors.toList());

        foundOffer.setLocalDelivery(localDeliveryOptions);
        ofNullable(offerModifier).ifPresent(modifier -> modifier.accept(foundOffer));
        return foundOffer;
    }

    private LocalDeliveryOption mapLocalDeliveryOption(ItemDeliveryOption ido) {
        LocalDeliveryOption localDeliveryOption = new LocalDeliveryOption();
        localDeliveryOption.setCost(ido.getPrice());
        localDeliveryOption.setCurrency(Currency.findByName(ido.getCurrency()));
        localDeliveryOption.setDayFrom(ido.getFromDay());
        localDeliveryOption.setDayTo(ido.getToDay());
        localDeliveryOption.setOrderBefore(ido.getOrderBefore());
        Set<String> paymentMethods = ido.getPaymentMethods().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        localDeliveryOption.setPaymentMethods(paymentMethods);
        ofNullable(deliveryOptionModifier).ifPresent(modifier -> modifier.accept(localDeliveryOption));
        return localDeliveryOption;
    }

    public void setOfferModifier(Consumer<FoundOffer> offerModifier) {
        this.offerModifier = offerModifier;
    }
}
