package ru.yandex.market.liquibase;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplyNotificationTypeTaskTest {
    @Test
    void correctParameters() {
        var actual = ApplyNotificationTypeTask.parseAliasPlaceCodesString(
                "cc:ShopAdmins;to:ShopAdmins,ShopSupports,BusinessContacts;from:PublicPartnerAddress;reply_to:YaManager"
        );
        assertEquals(Map.of(
                "cc", Set.of("ShopAdmins"),
                "to", Set.of("ShopAdmins", "ShopSupports", "BusinessContacts"),
                "from", Set.of("PublicPartnerAddress"),
                "reply_to", Set.of("YaManager")
        ), actual);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "cc;ShopAdmins",
            "cc:ShopAdmins:ShopSupports",
    })
    void incorrectParameters(String aliasPlaceCodesString) {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> ApplyNotificationTypeTask.parseAliasPlaceCodesString(aliasPlaceCodesString));
        assertEquals(
                String.format("Incorrect aliasPlaceCodes format: %s ", aliasPlaceCodesString),
                exception.getMessage()
        );
    }
}
