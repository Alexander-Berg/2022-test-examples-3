package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.tests;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.PropertyLoader;

import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.dto.RequestDocType;
import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.step.FFWfApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;

import static org.hamcrest.Matchers.is;

@DisplayName("FF Workflow API X-DOC Inbound Test")
public class XdocInboundTest extends AbstractWMSTest {
    private final Logger log = LoggerFactory.getLogger(XdocInboundTest.class);

    private final FFWfApiSteps ffWfApiSteps = new FFWfApiSteps();

    private final long REAL_PARTNER = 47723L;
    private final long MOCK_PARTNER = 999106L;

    private final long YANDEX_RND = 147L;

    private final Item xdocItem = Item.builder().sku("100438892408").vendorId(10264281)
        .article("100438892408").build();

    private List<Integer> mocksId = new ArrayList<>();

    @BeforeEach
    public void setUp() throws Exception {
        PropertyLoader.newInstance().populate(this);
    }

    @AfterEach
    @Step("Чистка моков после теста")
    public void tearDown() {
        ffWfApiSteps.deleteMocks(mocksId);
    }

    @Step("Проверяем детали поставки")
    private void verifyInboundDetails(Long inboundId) {
        log.info("Checking inbound details");
        ffWfApiSteps.getRequest(inboundId)
            .body("itemsTotalCount", is(10))
            .body("itemsTotalFactCount", is(1))
            .body("itemsTotalDefectCount", is(0))
            .body("itemsTotalSurplusCount", is(0));
    }

    @Disabled("Технические работы в Axapta 24.01.2020")
    @Test
    @DisplayName("Создание x-doc поставки с реальным партнером, проверка документов")
    public void xdocInboundCreateIntegrationTest() {
        log.info("Starting xdocInboundCreateIntegrationTest...");

        Long xdocInboundId = ffWfApiSteps.createXdocInbound(YANDEX_RND, REAL_PARTNER);
        ffWfApiSteps.waitRequestCreated(xdocInboundId, true);
        ffWfApiSteps.waitXdocRequestCreatedOrShippedByPartner(xdocInboundId);

        ffWfApiSteps.waitRequestHasDocumentTypes(
            xdocInboundId,
            RequestDocType.ACT_OF_RECEPTION_TRANSFER,
            RequestDocType.PALLET_LABEL
        );
    }

    @Disabled("Процесс не используется")
    @Test
    @DisplayName("Создание x-doc и флоу поставки с моковым партнером, проверка документов")
    public void xdocInboundFlowTest() {
        log.info("Starting xdocInboundFlowTest...");

        mocksId.addAll(ffWfApiSteps.mockXdocInboundData());

        Long xdocInboundId = ffWfApiSteps.createXdocInbound(YANDEX_RND, MOCK_PARTNER);
        String ffInboundId = ffWfApiSteps.waitRequestCreated(xdocInboundId, true);
        String partnerInboundId = ffWfApiSteps.waitXdocRequestCreatedOrShippedByPartner(xdocInboundId);
        ffWfApiSteps.waitForPalletNumberToBe(xdocInboundId, 10);
        ffWfApiSteps.waitForBoxNumberToBe(xdocInboundId, 5);

        wmsSteps.acceptItemAndMoveToPickingCell(ffInboundId, xdocItem);
        wmsSteps.closeInbound(ffInboundId);

        ffWfApiSteps.waitRequestComplete(xdocInboundId);
        verifyInboundDetails(xdocInboundId);

        ffWfApiSteps.waitRequestHasDocumentTypes(
            xdocInboundId,
            RequestDocType.ACT_OF_RECEPTION_TRANSFER,
            RequestDocType.PALLET_LABEL,
            RequestDocType.SECONDARY_RECEPTION_ACT,
            RequestDocType.ACT_OF_DISCREPANCY
        );
    }

    @Disabled("Аксапта не отвечает")
    @Test
    @DisplayName("X-doc поставка приехала сразу на конечный склад в обход промежуточного, проверка документов")
    public void xdocInboundNoParnterFlowTest() {
        log.info("Starting xdocInboundNoParnterFlowTest...");

        Long xdocInboundId = ffWfApiSteps.createXdocInbound(YANDEX_RND, REAL_PARTNER);
        String ffInboundId = ffWfApiSteps.waitRequestCreated(xdocInboundId, true);

        wmsSteps.acceptItemAndMoveToPickingCell(ffInboundId, xdocItem);
        wmsSteps.closeInbound(ffInboundId);

        ffWfApiSteps.waitRequestComplete(xdocInboundId);
        verifyInboundDetails(xdocInboundId);

        ffWfApiSteps.waitRequestHasDocumentTypes(
            xdocInboundId,
            RequestDocType.ACT_OF_RECEPTION_TRANSFER,
            RequestDocType.PALLET_LABEL,
            RequestDocType.SECONDARY_RECEPTION_ACT,
            RequestDocType.ACT_OF_DISCREPANCY
        );
    }

}
