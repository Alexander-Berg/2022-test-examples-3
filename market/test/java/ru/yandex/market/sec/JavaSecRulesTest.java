package ru.yandex.market.sec;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.partner.mvc.MockPartnerRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.core.campaign.model.PartnerId.datasourceId;

class JavaSecRulesTest extends JavaSecFunctionalTest {

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of("register-shop@POST", -1, null, 1234, true),
                Arguments.of("enableFeature@POST", 2, datasourceId(1), 555, true),
                Arguments.of("enableFeature@POST", 2, datasourceId(1), 556, false),
                Arguments.of("cabinet/{cabinet}/config@GET", 2, datasourceId(1), 556, true),
                Arguments.of("cabinet/{cabinet}/pages@GET", 2, datasourceId(1), 556, true),
                Arguments.of("/dco-enabled", 6, datasourceId(5), 555, false),
                Arguments.of("/dco-enabled", 7, datasourceId(6), 555, true)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void rules(String operation, long campaignId, PartnerId partnerId, long uid, boolean expected) {
        boolean result = secManager.canDo(operation, new MockPartnerRequest(uid, campaignId, partnerId));
        assertEquals(expected, result);
    }

}

