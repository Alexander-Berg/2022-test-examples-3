package ru.yandex.market.abo.cpa.shops;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * @author kukabara
 */
public class CpaShopStatCreatorTest extends EmptyTest {

    @Autowired
    private CpaShopStatCreator cpaShopStatCreator;
    @Autowired
    private CpaShopStatService cpaShopStatService;

    @Test
    @Disabled("needed to validate results on testing/prod")
    public void testCompareJsonWithDb() throws Exception {
        compareJsonWithDb(cpaShopStatCreator);
    }

    @Test
    public void createShopsSnapshotTest() throws Exception {
        CpaShopStatCreator mockSnapshotCreator = spy(cpaShopStatCreator);
        doReturn(createParser()).doReturn(createParser()).when(mockSnapshotCreator).getJsonParser();
        mockSnapshotCreator.createShopsSnapshot();

        compareJsonWithDb(mockSnapshotCreator);
    }

    private void compareJsonWithDb(CpaShopStatCreator snapshotCreator) throws IOException, InterruptedException {
        JsonParser jsonParser = snapshotCreator.getJsonParser();
        snapshotCreator.validateJson(jsonParser);
        while (!jsonParser.isClosed()) {
            CpaShopStatCreator.CpaShop jsonShop = snapshotCreator.parseJsonForShop(jsonParser);
            if (jsonShop == CpaShopStatCreator.CpaShop.FINISH_FLAG || jsonShop == null) {
                break;
            }

            CpaShopStatInfo shopInfoFromDb = cpaShopStatService.getShop(jsonShop.shopId);
            String errorShopMsg = "fail for shop " + shopInfoFromDb.getShopId();

            assertEquals(jsonShop.isFirstTime, shopInfoFromDb.getIsFirstTime() ? 1 : 0, errorShopMsg);
            assertEquals(jsonShop.placementTimeInDays, shopInfoFromDb.getCpcPlacementDays().longValue(), errorShopMsg);
            assertEquals(jsonShop.cpaPlacementTimeInDays, shopInfoFromDb.getCpaPlacementDays().longValue(), errorShopMsg);
            assertEquals(jsonShop.isLimitedRegions, shopInfoFromDb.getLimitedRegions() ? 1 : 0, errorShopMsg);
            // не проверяем isPartnerInterface, isEnabled, т.к обновляются не всегда
        }
    }

    private JsonParser createParser() throws IOException {
        return new JsonFactory().createParser(this.getClass().getResourceAsStream("/cpa/cpaShops.json"));
    }

}
