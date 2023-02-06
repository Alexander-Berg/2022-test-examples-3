package ru.yandex.market.checkout.util;

import java.util.EnumSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.FieldDefinition;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;

import ru.yandex.common.util.language.LanguageCode;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.tariff.TariffData;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;

public class EnhancedRandomHelper {

    private EnhancedRandomHelper() {
    }

    public static EnhancedRandom createEnhancedRandom() {
        EnhancedRandom defaultRandom = EnhancedRandomBuilder.aNewEnhancedRandom();

        return EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .randomize(Address.class, (Randomizer<Address>) () -> defaultRandom.nextObject(AddressImpl.class))
                .randomize(ItemPromo.class, (Randomizer<ItemPromo>) () -> defaultRandom.nextObject(ItemPromo.class))
                .exclude(JsonNode.class, ArrayNode.class)
                .randomize(new FieldDefinition<>("customsLanguages", EnumSet.class, TariffData.class),
                        (Randomizer<EnumSet<LanguageCode>>) () ->
                                EnumSet.copyOf(EnhancedRandom.randomStreamOf(3, LanguageCode.class)
                                        .collect(Collectors.toSet()))
                )

                .build();
    }
}
