package ru.yandex.market.sec;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.partner.mvc.MockPartnerRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.core.campaign.model.PartnerId.datasourceId;
import static ru.yandex.market.core.campaign.model.PartnerId.fmcgId;
import static ru.yandex.market.core.campaign.model.PartnerId.supplierId;

class JavaSecAuthoritiesTest extends JavaSecFunctionalTest {

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of("SHOP_WORKFLOW_NEWBIE", null, 10, fmcgId(9), 555, true),
                Arguments.of("SHOP_WORKFLOW_NEWBIE", null, 11, fmcgId(10), 555, false),
                Arguments.of("SHOP_WORKFLOW_NEWBIE", null, 4, datasourceId(3), 555, false),
                Arguments.of("SHOP_WORKFLOW_NEWBIE", null, 5, datasourceId(4), 555, true),
                Arguments.of("SHOP_WORKFLOW_NEWBIE", null, 100000, fmcgId(999999), 555, false),
                Arguments.of("SHOP_WORKFLOW_NEWBIE", null, 100000, datasourceId(999999), 555, false),
                Arguments.of("SHOP_WORKFLOW_NOT_NEWBIE", null, 10, fmcgId(9), 555, false),
                Arguments.of("SHOP_WORKFLOW_NOT_NEWBIE", null, 11, fmcgId(10), 555, true),
                Arguments.of("SHOP_WORKFLOW_NOT_NEWBIE", null, 4, datasourceId(3), 555, true),
                Arguments.of("SHOP_WORKFLOW_NOT_NEWBIE", null, 5, datasourceId(4), 555, false),
                Arguments.of("SHOP_WORKFLOW_NOT_NEWBIE", null, 100000, fmcgId(999999), 555, false),
                Arguments.of("SHOP_WORKFLOW_NOT_NEWBIE", null, 100000, datasourceId(999999), 555, false),
                Arguments.of("DCO_ENABLED", null, 6, supplierId(5), 555, false),
                Arguments.of("DCO_ENABLED", null, 7, supplierId(6), 555, true),
                // явная роль оператор
                Arguments.of("SHOP_OPERATOR", null, 2, datasourceId(1), 556, true),
                // пользователь только оператор
                Arguments.of("SHOP_TECHNICAL", null, 2, datasourceId(1), 556, false),
                Arguments.of("SHOP_EVERYONE", null, 2, datasourceId(1), 556, true),
                // У пользователя явная роль - админ. оператор и техник неявно
                Arguments.of("SHOP_ADMIN", null, 3, datasourceId(2), 558, true),
                Arguments.of("SHOP_OPERATOR", null, 3, datasourceId(2), 558, true),
                Arguments.of("SHOP_TECHNICAL", null, 3, datasourceId(2), 558, true),
                Arguments.of("SHOP_EVERYONE", null, 3, datasourceId(2), 558, true),
                // Линк без ролей
                Arguments.of("SHOP_EVERYONE", null, 12, datasourceId(12), 558, false)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void authorities(String authority, String param, long campaignId, PartnerId partnerId, long uid, boolean expected) {
        boolean result = secManager.hasAuthority(authority, param, new MockPartnerRequest(uid, campaignId, partnerId));
        assertEquals(expected, result);
    }
}

