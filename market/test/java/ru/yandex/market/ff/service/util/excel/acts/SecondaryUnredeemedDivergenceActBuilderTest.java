package ru.yandex.market.ff.service.util.excel.acts;

import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.exception.http.BadRequestException;
import ru.yandex.market.ff.exception.http.RequestNotFoundException;
import ru.yandex.market.ff.repository.FulfillmentInfoRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.util.excel.acts.misc.Utils;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SecondaryUnredeemedDivergenceActBuilderTest extends IntegrationTest {

    private static final String TEMPLATE_FILE_NAME = "service/xlsx-report/unredeemed_secondary_act.xlsx";
    private static final String TEMPLATE_FILE_NAME_WITH_NO_CONSIGNOR_ORDER_ID =
            "service/xlsx-report/unredeemed_secondary_act_with_no_consignor_order_id.xlsx";

    @Autowired
    private ShopRequestRepository shopRequestRepository;

    @Autowired
    private FulfillmentInfoRepository fulfillmentInfoRepository;

    @Autowired
    private SecondaryUnredeemedDivergenceActBuilder service;

//    @Autowired
//    private DivergenceActMailService divergenceActMailService;

    @Test
    @Disabled
    @Order(1)
    @DatabaseSetup("classpath:service/xlsx-report/secondary-act.xml")
    @Transactional
    public void testGetSecondaryDivergenceAct() throws Exception {
        Workbook templateExcel = new XSSFWorkbook(
                Objects.requireNonNull(getSystemResourceAsStream(TEMPLATE_FILE_NAME)));
        service.setShopRequestData(196149);
        Workbook excel = service.getDivergenceAct();
//        excel.write(new FileOutputStream("out.xlsx"));
//        divergenceActMailService.sendEmail(excel, service.getGeneratedActName(), "106");
        assertEquals(excel.getNumberOfSheets(), templateExcel.getNumberOfSheets());
        assertEquals(excel.getSheetAt(0).getLastRowNum(), templateExcel.getSheetAt(0).getLastRowNum());
        assertTrue(Utils.excelContentIsEqual(excel, templateExcel, 1));
    }

    @Test
    @Order(2)
    @DatabaseSetup("classpath:service/xlsx-report/secondary-act.xml")
    @Transactional
    public void testGetActFileName() {
        service.setShopRequestData(340060);
        String generatedActName = service.getGeneratedActName();
        Assertions.assertEquals(generatedActName,
                "Акт расхождений (Вторичная приемка) к Акту No 4267153 ОТ 2020-07-10.xlsx");
    }

    @Test
    @Disabled
    @Order(3)
    @DatabaseSetup("classpath:service/xlsx-report/secondary-act.xml")
    @Transactional
    public void testGetSecondaryDivergenceActWithEmptyConsignorOrderId() throws Exception {
        Workbook templateExcel = new XSSFWorkbook(
                Objects.requireNonNull(getSystemResourceAsStream(TEMPLATE_FILE_NAME_WITH_NO_CONSIGNOR_ORDER_ID)));
        service.setShopRequestData(340060);
        Workbook excel = service.getDivergenceAct();
//        excel.write(new FileOutputStream("out.xlsx"));
        assertEquals(excel.getNumberOfSheets(), templateExcel.getNumberOfSheets());
        assertEquals(excel.getSheetAt(0).getLastRowNum(), templateExcel.getSheetAt(0).getLastRowNum());
        assertTrue(Utils.excelContentIsEqual(excel, templateExcel, 1));
    }

    @Test
    @Order(4)
    @DatabaseSetup("classpath:service/xlsx-report/secondary-act.xml")
    @Transactional
    public void testNoDivergencesFoundException() {
        boolean divergencesFound = service.setShopRequestData(1111);
        assertFalse(divergencesFound);
    }

    @Test
    @Order(5)
    @DatabaseSetup("classpath:service/xlsx-report/secondary-act.xml")
    @Transactional
    public void testGetActWithNoShopRequestFoundException() {
        assertThrows(RequestNotFoundException.class, () -> service.setShopRequestData(789));
    }

    @Test
    @Order(6)
    @DatabaseSetup("classpath:service/xlsx-report/secondary-act.xml")
    @Transactional
    public void testGetActWithNoConsignorIdFoundException() {
        service.setShopRequestData(432);
        assertThrows(BadRequestException.class, () -> service.getDivergenceAct());
    }
}
