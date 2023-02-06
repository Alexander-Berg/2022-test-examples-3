package ru.yandex.market.checkout.checkouter.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pdf.PdfService;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * @author : poluektov
 * date: 22.05.18.
 */
public class PdfServiceTest extends AbstractWebTestBase {

    private static final String TEMP_DIR = "tmp";

    @Autowired
    private PdfService pdfService;
    @Autowired
    private ReturnHelper returnHelper;

    @BeforeAll
    public static void tempFolder() {
        new File(TEMP_DIR).mkdir();
    }

    @BeforeEach
    public void initMock() {
        returnHelper.mockShopInfo();
        checkouterProperties.setNewLogoForWarranty(false);

    }

    @Test
    public void generateDropShipPdf() throws IOException {
        File file = new File(TEMP_DIR + "/return-request-dsbs.pdf");
        file.createNewFile();

        try (OutputStream os = new FileOutputStream(file)) {
            Order order = OrderProvider.getFulfilmentOrder();
            order.setId(1234L);
            order.setShopOrderId("4321");
            order.setCreationDate(Date.from(Instant.now()));
            order.getItems().forEach(item -> {
                item.setId(Long.valueOf(item.getOfferId()));
                item.setOfferName(item.getOfferId()
                        + " Rainshower Душевая система с " +
                        "термостатом для душа с верхним душем Rainshower Cosmo 310 мм");
            });
            Return ret = ReturnProvider.generateReturn(order);
            ret.setId(555L);
            ret.getItems().get(0).setReasonType(ReturnReasonType.BAD_QUALITY);
            ret.getItems().get(1).setReasonType(ReturnReasonType.DO_NOT_FIT);
            pdfService.generateReturnPdf(order, ret, os);
        }

        assertThat(file.length(), greaterThan(0L));
    }

    @Test
    public void generatePdf() throws IOException {
        File file = new File(TEMP_DIR + "/return-request.pdf");
        file.createNewFile();

        try (OutputStream os = new FileOutputStream(file)) {
            Order order = OrderProvider.getFulfilmentOrder();
            order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
            order.setId(1234L);
            order.setShopOrderId("4321");
            order.setCreationDate(Date.from(Instant.now()));
            order.getItems().forEach(item -> {
                item.setId(Long.valueOf(item.getOfferId()));
                item.setOfferName(item.getOfferId()
                        + " Мойка высокого давления KARCHER K");
            });
            Return ret = ReturnProvider.generateReturn(order);
            ret.setId(555L);
            ret.getItems().get(0).setReasonType(ReturnReasonType.BAD_QUALITY);
            ret.getItems().get(1).setReasonType(ReturnReasonType.DO_NOT_FIT);
            pdfService.generateReturnPdf(order, ret, os);
        }

        assertThat(file.length(), greaterThan(0L));
    }

    @AfterAll
    public static void clear() throws IOException {
        FileUtils.forceDelete(new File(TEMP_DIR));
    }
}
