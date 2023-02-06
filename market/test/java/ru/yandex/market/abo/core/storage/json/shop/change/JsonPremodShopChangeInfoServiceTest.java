package ru.yandex.market.abo.core.storage.json.shop.change;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.premod.shopdata.model.PremodShopChangeInfo;
import ru.yandex.market.abo.core.premod.shopdata.model.PremodShopDataType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 30.03.2020
 */
class JsonPremodShopChangeInfoServiceTest extends EmptyTest {

    private static final long TICKET_ID = 123L;
    private static final String CURRENT_INFO = "current_info";
    private static final String LAST_PREMODERATION_INFO = "last_premoderation_info";

    @Autowired
    private JsonPremodShopChangeInfoService jsonPremodShopChangeInfoService;
    @Autowired
    private JsonPremodShopChangeInfoRepo jsonPremodShopChangeInfoRepo;

    @Test
    void saveNotEmptyChangesTest() {
        var shopChangesByType = Map.of(
                PremodShopDataType.REQUISITES, new PremodShopChangeInfo(LAST_PREMODERATION_INFO, CURRENT_INFO)
        );
        jsonPremodShopChangeInfoService.saveIfNotEmpty(TICKET_ID, shopChangesByType);
        flushAndClear();
        assertEquals(shopChangesByType, jsonPremodShopChangeInfoService.load(TICKET_ID));
    }

    @Test
    void saveEmptyChangesTest() {
        var shopChangesByType = Collections.<PremodShopDataType, PremodShopChangeInfo>emptyMap();
        jsonPremodShopChangeInfoService.saveIfNotEmpty(TICKET_ID, shopChangesByType);
        flushAndClear();
        assertTrue(jsonPremodShopChangeInfoService.load(TICKET_ID).isEmpty());
        assertEquals(0, jsonPremodShopChangeInfoRepo.count());
    }
}
