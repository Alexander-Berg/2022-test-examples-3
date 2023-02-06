package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.servicebus;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Stock;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Transfer;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

@Resource.Classpath("wms/test.properties")
@DisplayName("API: Transfer")
@Epic("API Tests")
public class TransferTest {
    private static final Logger log = LoggerFactory.getLogger(TransferTest.class);

    @Property("test.vendorId")
    private long vendorId;

    private final Item item = Item.builder().sku("AUTOTRANSFER4").vendorId(1559).article("AUTOTRANSFER4").build();

    private final Inbound TEST_INBOUND = new Inbound(1596631165L, "0000039863");
    private final Transfer TEST_TRANSFER = new Transfer(1562327467932L, "0000000123");

    @BeforeEach
    public void setUp() throws Exception {
        PropertyLoader.newInstance().populate(this);
    }

    @Test
    @DisplayName("createTransfer")
    @Disabled("Временно отключаем тест до переезда базы в Докер, т.к. приходится всё время оживлять данные для теста")
    @Description("Создание трансфера: Создаем трансфер с излишка на годный и с годного на излишек для известной поставки. " +
            "В результате после его обработки кол-во стока должно остаться неизменным, что позволит тесту выполняться и дальше.")
    public void createTransferTest() {
        log.info("Testing createTransfer");

        ApiSteps.Transfer().createTransfer(TEST_INBOUND, item, Stock.Surplus, Stock.Fit, 1);
        ApiSteps.Transfer().createTransfer(TEST_INBOUND, item, Stock.Fit, Stock.Surplus, 1);
    }

    @Test
    @DisplayName("getTransferStatus")
    public void getTransferStatusTest() {
        log.info("Testing getTransferStatus");

        ApiSteps.Transfer().verifyTransferStatusIs(TEST_TRANSFER, Transfer.STATUS_COMPLETED);
    }

    /**
     *
     *     Статусы:
     *
     *     UNKNOWN(-1),
     *     NEW(1),
     *     PROCESSING(20),
     *     ACCEPTED(30),
     *     COMPLETED(40),
     *     ERROR(50);
     *
     */
    @Step("Проверяем корректность данных в истории")
    private void verifyTransferHistory(ValidatableResponse response) {
        response
                .body("root.response.transferStatusHistory.history.transferStatusEvent.findAll " +
                                "{it.statusCode == 50}",
                        is(empty()))
                .body("root.response.transferStatusHistory.history.transferStatusEvent.find " +
                                "{it.statusCode == 1}.date",
                        is("2019-07-05T14:51:13+03:00"))
                .body("root.response.transferStatusHistory.history.transferStatusEvent.find " +
                                "{it.statusCode == 20}.date",
                        is("2019-07-05T14:55:00+03:00"))
                .body("root.response.transferStatusHistory.history.transferStatusEvent.find " +
                                "{it.statusCode == 40}.date",
                        is("2019-07-05T14:55:00+03:00"));
    }

    @Step("Проверяем корректность данных в деталях трансфера")
    public void verifyTransferDetails(ValidatableResponse response) {
        String prefix = "root.response.transferDetails.transferDetailsItems.transferDetailsItem.";
        String expectedArticle = "AUTOTRANSFER";
        String expectedVendorId = "1559";
        String expectedActualField = "1";
        String expectedDeclaredField = "1";

        response
                .body(prefix + "unitId.id",
                        is(expectedArticle))
                .body(prefix + "unitId.vendorId",
                        is(expectedVendorId))
                .body(prefix + "unitId.article",
                        is(expectedArticle))
                .body(prefix + "actual",
                        is(expectedActualField))
                .body(prefix + "declared",
                        is(expectedDeclaredField));
    }

    @Test
    @DisplayName("getTransferHistory")
    public void getTransferHistoryTest() {
        log.info("Testing getTransferHistory");

        ValidatableResponse response = ApiSteps.Transfer().getTransferHistory(TEST_TRANSFER).log().all();

        verifyTransferHistory(response);
    }

    @Test
    @DisplayName("getTransferDetails")
    public void getTransferDetailsTest() {
        log.info("Testing getTransferDetails");

        ValidatableResponse response = ApiSteps.Transfer().getTransferDetails(TEST_TRANSFER).log().all();

        verifyTransferDetails(response);
    }
}
