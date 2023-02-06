package ru.yandex.market.abo.cpa.shops;

import java.io.File;

import com.fasterxml.jackson.core.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.expimp.storage.export.storage.DirectHistoryMdsS3Downloader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 23.11.17.
 */
public class CpaShopStatCreatorJsonTest {

    @InjectMocks
    private CpaShopStatCreator cpaShopStatCreator;
    @Mock
    private DirectHistoryMdsS3Downloader directHistoryMdsS3Downloader;

    @BeforeEach
    @SuppressWarnings("all")
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(directHistoryMdsS3Downloader.downloadLast())
                .thenReturn(new File(getClass().getClassLoader().getResource("cpa/cpaShops.json").getFile()));
    }

    @Test
    public void checkJsonParsing() throws Exception {
        JsonParser jsonParser = cpaShopStatCreator.getJsonParser();
        cpaShopStatCreator.validateJson(jsonParser);
        CpaShopStatCreator.CpaShop cpaShop = cpaShopStatCreator.parseJsonForShop(jsonParser);

        assertEquals(91, cpaShop.shopId);
        assertEquals(0, cpaShop.isEnabled);
        assertEquals(0, cpaShop.isFirstTime);
        assertEquals(null, cpaShop.paymentCheckStatus);
        assertEquals(1, cpaShop.isPartnerInterface);
        assertArrayEquals(new Integer[]{213}, cpaShop.ownRegions);
        assertArrayEquals(new Integer[]{42, 24}, cpaShop.cpaRegions);
        assertEquals(237, cpaShop.placementTimeInDays);
        assertEquals(1, cpaShop.cpaPlacementTimeInDays);

        CpaShopStatCreator.OpenCutoff openCutoff = cpaShop.openCutoffs[0];
        assertEquals(10266478, openCutoff.id);
        assertEquals(91, openCutoff.shopId);
        assertEquals(CutoffType.CPA_GENERAL, openCutoff.type);
    }
}
