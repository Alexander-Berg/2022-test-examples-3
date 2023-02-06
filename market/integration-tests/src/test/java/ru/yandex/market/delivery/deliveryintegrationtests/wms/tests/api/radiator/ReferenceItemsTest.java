package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.radiator;

import io.qameta.allure.Epic;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.FileUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.RadiatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@Resource.Classpath({"wms/test.properties", "wms/infor.properties"})
@DisplayName("API: Reference Items")
@Epic("API Tests")
public class ReferenceItemsTest {
    private static final Logger log = LoggerFactory.getLogger(ReferenceItemsTest.class);

    private final ServiceBus serviceBus = new ServiceBus();
    private final RadiatorClient radiatorClient = new RadiatorClient();

    @Property("test.vendorId")
    private long vendorId;

    @Property("infor.token")
    private String inforToken;

    @BeforeEach
    public void setUp() throws Exception {
        PropertyLoader.newInstance().populate(this);
    }

    @Test
    @DisplayName("getReferenceItems")
    public void getReferenceItemsTest() {
        log.info("Testing getReferenceItems");

        String article = "AUTO_GET_STOCKS_TEST";

        ValidatableResponse response = radiatorClient.getReferenceItems(
                vendorId,
                article
        );

        response
                .body("root.response.itemReferences.itemReference.korobyte.width", is("23"))
                .body("root.response.itemReferences.itemReference.korobyte.height", is("34"))
                .body("root.response.itemReferences.itemReference.korobyte.length", is("12"))
                .body("root.response.itemReferences.itemReference.korobyte.weightGross", is("3.0"))
                .body("root.response.itemReferences.itemReference.korobyte.weightNet", is("3.0"))
                .body("root.response.itemReferences.itemReference.korobyte.weightTare", is("0.0"))
                .body("root.response.itemReferences.itemReference.lifeTime", is("365"))
                .body("root.response.itemReferences.itemReference.item.updated", is("2020-05-19T00:21:18+00:00"))
                .body("root.response.itemReferences.itemReference.item.barcodes.barcode.code", is("GETSTOCKSTEST"));
    }

    @Test
    @DisplayName("getReferenceItems: Multibox")
    public void getReferenceItemsMultiboxTest() {
        log.info("Testing getReferenceItemsMultibox");

        String article = "REF_MULTIBOX";

        ValidatableResponse response = radiatorClient.getReferenceItems(
                vendorId,
                article
        );

        response
                .body("root.response.itemReferences.itemReference.korobyte.width", is("20"))
                .body("root.response.itemReferences.itemReference.korobyte.height", is("30"))
                .body("root.response.itemReferences.itemReference.korobyte.length", is("10"))
                .body("root.response.itemReferences.itemReference.korobyte.weightGross", is("4.4"))
                .body("root.response.itemReferences.itemReference.korobyte.weightNet", is("4.4"))
                .body("root.response.itemReferences.itemReference.korobyte.weightTare", is("0.0"))
                .body("root.response.itemReferences.itemReference.item.updated", is("2020-05-14T12:29:27+00:00"))
                .body("root.response.itemReferences.itemReference.item.barcodes.barcode.code", is("REF_MULTIBOX"));
    }

    @Test
    @DisplayName("putReferenceItems")
    public void putReferenceItemsTest() {
        log.info("Testing putReferenceItems");

        String hash = UniqueId.getStringUUID();
        String article = UniqueId.getString();
        String body = FileUtil.bodyStringFromFile("wms/servicebus/putReferenceItems.xml",
                hash,
                vendorId,
                article,
                inforToken
        );
        serviceBus.putReferenceItems(body);

        radiatorClient.getReferenceItems(vendorId, article)
                .body("root.response.itemReferences.itemReference.unitId.id", not(emptyOrNullString()))
                .body("root.response.itemReferences.itemReference.barcodes.barcode.code",
                        containsInAnyOrder("98976876876", "4742943014930"))
                .body("root.response.itemReferences.itemReference.item.unitId.id", not(emptyOrNullString()))
                .body("root.response.itemReferences.itemReference.item.unitId.vendorId", is(Long.toString(vendorId)))
                .body("root.response.itemReferences.itemReference.item.unitId.article", is(article))
                .body("root.response.itemReferences.itemReference.item.name", is("putReferenceItems Test"))
                .body("root.response.itemReferences.itemReference.item.barcodes.barcode.code",
                        containsInAnyOrder("98976876876", "4742943014930"));
    }
}
