package ru.yandex.market.pers.qa.controller.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.qa.controller.QAControllerTest;
import ru.yandex.market.pers.qa.controller.dto.ProductIdDto;
import ru.yandex.market.pers.qa.controller.dto.VersusDto;
import ru.yandex.market.pers.qa.mock.mvc.VersusMvcMocks;
import ru.yandex.market.pers.qa.model.Versus;
import ru.yandex.market.pers.qa.service.VersusService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 30.08.2019
 */
public class VersusControllerTest extends QAControllerTest {
    public static final int HID = 123;
    public static final int MODEL_ID = 999;
    public static final int MODEL_ID_2 = MODEL_ID + 1;
    public static final int MODEL_ID_3 = MODEL_ID + 2;
    public static final int MODEL_ID_OTHER = 777;

    @Autowired
    private VersusMvcMocks versusMvc;

    @Autowired
    private VersusService versusService;

    @Test
    public void testReadSingleVersus() throws Exception {
        long versusId = versusService.createVersus(Versus.byModels(HID, MODEL_ID, MODEL_ID_OTHER));

        VersusDto versus = versusMvc.getVersus(versusId);
        assertVersus(versus, versusId, HID, MODEL_ID_OTHER, MODEL_ID);
    }

    private void assertVersus(VersusDto versus, long versusId, long hid, long modelId1, long modelId2) {
        assertEquals(hid, versus.getHid());
        assertEquals(VersusDto.VERSUS, versus.getEntity());
        assertEquals(versusId, versus.getId());
        assertEquals(2, versus.getProducts().size());
        assertEquals(ProductIdDto.PRODUCT, versus.getProducts().get(0).getEntity());
        assertEquals(modelId1, versus.getProducts().get(0).getId());
        assertEquals(ProductIdDto.PRODUCT, versus.getProducts().get(1).getEntity());
        assertEquals(modelId2, versus.getProducts().get(1).getId());
    }

    @Test
    public void testReadByModel() throws Exception {
        long versusId = versusService.createVersus(Versus.byModels(HID, MODEL_ID, MODEL_ID_OTHER));
        long versusId2 = versusService.createVersus(Versus.byModels(HID, MODEL_ID_OTHER, MODEL_ID_2));
        long versusId3 = versusService.createVersus(Versus.byModels(HID, MODEL_ID_2, MODEL_ID_3));

        setVersusCount(versusId, 1);
        setVersusCount(versusId2, 2);

        List<VersusDto> versusList = versusMvc.getVersusByModel(MODEL_ID_OTHER, DEF_PAGE_SIZE);
        assertEquals(2, versusList.size());
        assertVersus(versusList.get(0), versusId2, HID, MODEL_ID_OTHER, MODEL_ID_2);
        assertVersus(versusList.get(1), versusId, HID, MODEL_ID_OTHER, MODEL_ID);
    }

    @Test
    public void testReadByModelPaging() throws Exception {
        long pageSize = 2;

        long versusId = versusService.createVersus(Versus.byModels(HID, MODEL_ID, MODEL_ID_OTHER));
        long versusId2 = versusService.createVersus(Versus.byModels(HID, MODEL_ID_OTHER, MODEL_ID_2));
        long versusId3 = versusService.createVersus(Versus.byModels(HID, MODEL_ID_OTHER, MODEL_ID_3));

        setVersusCount(versusId, 1);
        setVersusCount(versusId2, 10);
        setVersusCount(versusId3, 2);

        List<VersusDto> versusList = versusMvc.getVersusByModel(MODEL_ID_OTHER, pageSize);
        assertEquals(2, versusList.size());
        assertVersus(versusList.get(0), versusId2, HID, MODEL_ID_OTHER, MODEL_ID_2);
        assertVersus(versusList.get(1), versusId3, HID, MODEL_ID_OTHER, MODEL_ID_3);
    }

    @Test
    public void testReadByHid() throws Exception {
        long versusId = versusService.createVersus(Versus.byModels(HID, MODEL_ID, MODEL_ID_OTHER));
        long versusId2 = versusService.createVersus(Versus.byModels(HID + 1, MODEL_ID_OTHER, MODEL_ID_2));
        long versusId3 = versusService.createVersus(Versus.byModels(HID, MODEL_ID_2, MODEL_ID_3));

        setVersusCount(versusId, 3);
        setVersusCount(versusId3, 1);

        List<VersusDto> versusList = versusMvc.getVersusByCategory(HID, DEF_PAGE_SIZE);
        assertEquals(2, versusList.size());
        assertVersus(versusList.get(0), versusId, HID, MODEL_ID_OTHER, MODEL_ID);
        assertVersus(versusList.get(1), versusId3, HID, MODEL_ID_2, MODEL_ID_3);
    }

    @Test
    public void testReadByHidPaging() throws Exception {
        long pageSize = 2;
        long versusId = versusService.createVersus(Versus.byModels(HID, MODEL_ID, MODEL_ID_OTHER));
        long versusId2 = versusService.createVersus(Versus.byModels(HID, MODEL_ID_OTHER, MODEL_ID_2));
        long versusId3 = versusService.createVersus(Versus.byModels(HID, MODEL_ID_2, MODEL_ID_3));

        setVersusCount(versusId, 3);
        setVersusCount(versusId3, 1);
        setVersusCount(versusId3, 235);

        List<VersusDto> versusList = versusMvc.getVersusByCategory(HID, pageSize);
        assertEquals(2, versusList.size());
        assertVersus(versusList.get(0), versusId3, HID, MODEL_ID_2, MODEL_ID_3);
        assertVersus(versusList.get(1), versusId, HID, MODEL_ID_OTHER, MODEL_ID);
    }

    private void setVersusCount(long versusId, long count) {
        qaJdbcTemplate.update(
            "update qa.exp_versus set cnt = ? where id = ?", count, versusId
        );
    }
}
