package ru.yandex.avia.booking.partners.gateways.aeroflot;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import ru.yandex.avia.booking.partners.gateways.aeroflot.converter.AeroflotVariantConverter;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotCategoryOffer;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotTotalOffer;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotVariant;
import ru.yandex.avia.booking.partners.gateways.model.search.CategoryPrice;
import ru.yandex.avia.booking.partners.gateways.model.search.PriceInfo;
import ru.yandex.avia.booking.partners.gateways.model.search.Variant;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.loadSampleTdRequestNdcV3MultipleTariffs;

public class AeroflotGatewaySynchronizeVariantTest {
    private final AeroflotGateway gateway = AeroflotApiStubsHelper.defaultGateway("url_not_needed");

    @Test
    void syncChangedPrice() {
        JsonNode variantData = loadSampleTdRequestNdcV3MultipleTariffs();
        AeroflotVariant variant = gateway.resolveVariantInfo(variantData);
        assertThat(variant.getOffer().getTotalPrice()).isEqualTo(Money.of(3755, "RUB"));
        assertThat(getVariantsPrices(variant)).isEqualTo(List.of(
                Money.of(2555, "RUB"),
                Money.of(3755, "RUB"),
                Money.of(6155, "RUB")
        ));
        AeroflotCategoryOffer categoryOffer = variant.getOffer().getCategoryOffers().get(0);
        assertThat(categoryOffer.getTotalPrice().getTotalPrice()).isEqualTo(Money.of(3755, "RUB"));
        assertThat(categoryOffer.getTotalPrice().getBasePrice()).isEqualTo(Money.of(1740, "RUB"));

        Variant changedVariant = AeroflotVariantConverter.convertVariant(variant);
        PriceInfo changedPriceInfo = changedVariant.getAllTariffs().get(1);
        changedPriceInfo.setTotal(addExtra(changedPriceInfo.getTotal(), 100));
        for (CategoryPrice categoryPrice : changedPriceInfo.getCategoryPrices()) {
            categoryPrice.setTotal(addExtra(categoryPrice.getTotal(), 100));
            categoryPrice.setFare(addExtra(categoryPrice.getFare(), 50));
        }
        gateway.synchronizeUpdatedVariantInfoJson(variantData, changedVariant);

        AeroflotVariant reloadedVariant = gateway.resolveVariantInfo(variantData);

        assertThat(reloadedVariant.getOffer().getTotalPrice()).isEqualTo(Money.of(3855, "RUB"));
        assertThat(getVariantsPrices(reloadedVariant)).isEqualTo(List.of(
                Money.of(2555, "RUB"),
                Money.of(3855, "RUB"),
                Money.of(6155, "RUB")
        ));
        AeroflotCategoryOffer reloadedCategoryOffer = reloadedVariant.getOffer().getCategoryOffers().get(0);
        assertThat(reloadedCategoryOffer.getTotalPrice().getTotalPrice()).isEqualTo(Money.of(3855, "RUB"));
        assertThat(reloadedCategoryOffer.getTotalPrice().getBasePrice()).isEqualTo(Money.of(1790, "RUB"));
    }

    private Money addExtra(Money money, int extraRoubles) {
        return money.add(Money.of(extraRoubles, "RUB"));
    }

    private List<Money> getVariantsPrices(AeroflotVariant variant) {
        return variant.getAllTariffs().stream()
                .map(AeroflotTotalOffer::getTotalPrice)
                .collect(toList());
    }
}
