package ru.yandex.market.checkout.checkouter.pay.bnpl;

import java.io.IOException;
import java.util.Collections;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplPlanCheckRequestBody;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplPlanCheckResponse;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserIds;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId.YANDEX_UID_HEADER;

class BnplAPICacheTest extends AbstractServicesTestBase {

    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;

    @Autowired
    private BnplAPI bnplAPI;

    @BeforeEach
    public void setup() throws IOException {
        bnplMockConfigurer.bnplMock.resetAll();
        bnplMockConfigurer.mockEmptyPlanCheck(YANDEX_UID_HEADER);
    }

    @Test
    void planCheckCacheTest() {
        BnplPlanCheckResponse expected = new BnplPlanCheckResponse();
        expected.setPlans(Collections.emptyList());

        BnplPlanCheckRequestBody request = new BnplPlanCheckRequestBody();
        BnplUserId userId = BnplUserId.createBnplIdHeaderByUid("231323213");
        BnplUserIds bnplUserIds = new BnplUserIds();
        bnplUserIds.addBnplUserId(userId);

        BnplPlanCheckResponse actual1 = bnplAPI.planCheck(request, bnplUserIds);
        BnplPlanCheckResponse actual2 = bnplAPI.planCheck(request, bnplUserIds);

        // Данный метод вызывается только во время запроса, если он был вызван 2 раза или более, то это может значить,
        // что бин не был спроксирован и кэш не работает
        bnplMockConfigurer.bnplMock.verify(1, RequestPatternBuilder.allRequests());

        assertEquals(expected, actual1, "Должен вернуться объект, который мы передали в Mock");
        assertEquals(expected, actual2);
        assertNotSame(expected, actual2, "Должен вернуться объект из кэша");
    }
}
