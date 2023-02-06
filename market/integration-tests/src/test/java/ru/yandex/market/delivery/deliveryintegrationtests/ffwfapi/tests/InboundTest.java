package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.tests;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.PropertyLoader;

import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.dto.CalendaringInterval;
import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.dto.RequestDocType;
import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.step.FFWfApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

@DisplayName("FF Workflow API Inbound Test")
public class InboundTest {
    private final Logger log = LoggerFactory.getLogger(InboundTest.class);

    private final FFWfApiSteps ffWfApiSteps = new FFWfApiSteps();

    private static final String AUTO_CALENDAR = "2";
    private static final String MANUAL_CALENDAR = "0";
    private static final String second_day = "1";
    private static final String third_day = "2";

    private static final String ARTICLE1 = "00065.00026.100126174717";
    private static final String ARTICLE2 = "00065.00026.100126179065";

    private static final String CIS1 = "011002566481941121mbg:zCaRlUcpDqhRs050";
    private static final String CIS2 = "011002566481941121mbg:zCaRlUcpDqhRs070";

    private static final String WH_ID = "147";

    private static final String FILE_PATH = "ffwfapi/requests/calendaringInbound.json";

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
            .body("itemsTotalCount", is(20))
            .body("itemsTotalFactCount", is(22))
            .body("itemsWithSurplus", is(1))
            .body("itemsWithDefects", is(0));
    }

    @Step("Проверяем детали поставки с кизами")
    private void verifyInboundDetailsWithCises(Long inboundId) {
        log.info("Checking inbound details");
        ValidatableResponse response = ffWfApiSteps.getRequest(inboundId);
        List<String> cises = new ArrayList<>();
        cises.add(response.extract().path("factUnitId.parts.get(0).value").toString());
        cises.add(response.extract().path("factUnitId.parts.get(1).value").toString());

        assertThat(cises, hasItems(CIS1, CIS2));
    }

    @Test
    @DisplayName("Создание и отмена поставки")
    public void inboundCreateCancelTest() {
        log.info("Starting inboundCreateCancel...");

        Long inboundId = ffWfApiSteps.createInbound();
        ffWfApiSteps.waitRequestCreated(inboundId, true);
        ffWfApiSteps.cancelRequest(inboundId);
        ffWfApiSteps.waitRequestCanceled(inboundId);
    }

    @Test
    @DisplayName("Создание и обработка поставки, проверка документов")
    public void inboundCreateProcessTest() {
        log.info("Starting inboundCreateProcess...");

        Long inboundId = ffWfApiSteps.createInbound();
        String ffInboundId = ffWfApiSteps.waitRequestCreated(inboundId, true);

        mocksId.addAll(ffWfApiSteps.mockInboundData(
            inboundId,
            ffInboundId,
            "ffwfapi/requests/mock/mockGetInbound.xml"
        ));

        ffWfApiSteps.waitRequestHasDocumentTypes(
            inboundId,
            RequestDocType.ACT_OF_RECEPTION_TRANSFER,
            RequestDocType.PALLET_LABEL
        );

        ffWfApiSteps.waitRequestComplete(inboundId);

        verifyInboundDetails(inboundId);

        ffWfApiSteps.waitRequestHasDocumentTypes(
            inboundId,
            RequestDocType.ACT_OF_RECEPTION_TRANSFER,
            RequestDocType.PALLET_LABEL,
            RequestDocType.SECONDARY_RECEPTION_ACT,
            RequestDocType.ACT_OF_DISCREPANCY
        );
    }

    @Test
    @DisplayName("Создание календаризированной поставки с автовыбором окна")
    public void calendaringInboundAutoSelectSlotTest() {
        log.info("Starting calendaring inbound with auto select slot...");

        Long inboundId = ffWfApiSteps.createInbound(FILE_PATH, AUTO_CALENDAR);
        ffWfApiSteps.waitRequestCreated(inboundId, false);
        ffWfApiSteps.waitRequestAppearOnCalendar(
            inboundId,
            WH_ID,
            DateUtil.calendarDatePlusDays(2)
        );
        ffWfApiSteps.cancelRequest(inboundId);
    }

    @Test
    @DisplayName("Создание календаризированной поставки с ручным выбором окна")
    public void calendaringInboundManualSelectSlotTest() {
        log.info("Starting calendaring inbound with manual select slot...");

        Long inboundId = ffWfApiSteps.createInbound(FILE_PATH, MANUAL_CALENDAR);
        ffWfApiSteps.waitRequestValidated(inboundId);
        CalendaringInterval calendaringInterval = ffWfApiSteps.getFreeTimeSlot(inboundId, second_day);
        ffWfApiSteps.selectSlot(
            inboundId,
            calendaringInterval.getDay(),
            calendaringInterval.getFrom(),
            calendaringInterval.getTo()
        );
        ffWfApiSteps.waitRequestAppearOnCalendar(
            inboundId,
            WH_ID,
            calendaringInterval.getDay()
        );
        ffWfApiSteps.waitRequestCreated(inboundId, false);
        ffWfApiSteps.cancelRequest(inboundId);
    }

    @Test
    @DisplayName("Создание и отмена теневой поставки")
    public void shadowInboundCreateCancelTest() {
        log.info("Starting shadowInboundCreateCancel...");

        Long shadowInboundId = ffWfApiSteps.createShadowInbound(FILE_PATH, MANUAL_CALENDAR);
        ffWfApiSteps.waitRequestValidated(shadowInboundId);
        ffWfApiSteps.cancelRequest(shadowInboundId);
        ffWfApiSteps.waitRequestCanceled(shadowInboundId);
    }

    @Test
    @DisplayName("Создание теневой поставки и выбор окна для неё")
    public void shadowInboundSelectSlotTest() {
        log.info("Starting shadow inbound select slot...");

        Long shadowInboundId = ffWfApiSteps.createShadowInbound(FILE_PATH, MANUAL_CALENDAR);
        ffWfApiSteps.waitRequestValidated(shadowInboundId);
        CalendaringInterval calendaringInterval = ffWfApiSteps.getFreeTimeSlot(shadowInboundId, second_day);
        ffWfApiSteps.selectSlot(
            shadowInboundId,
            calendaringInterval.getDay(),
            calendaringInterval.getFrom(),
            calendaringInterval.getTo()
        );
        ffWfApiSteps.waitRequestAppearOnCalendar(
            shadowInboundId,
            WH_ID,
            calendaringInterval.getDay()
        );
        ffWfApiSteps.cancelRequest(shadowInboundId);
    }

    @Test
    @DisplayName("Создание обычной поставки на основе теневой")
    public void createInboundBasedOnShadowRequestTest() {
        log.info("Starting create inbound based on shadow request...");

        Long shadowInboundId = ffWfApiSteps.createShadowInbound(FILE_PATH, MANUAL_CALENDAR);
        ffWfApiSteps.waitRequestValidated(shadowInboundId);
        CalendaringInterval calendaringInterval = ffWfApiSteps.getFreeTimeSlot(shadowInboundId, third_day);
        ffWfApiSteps.selectSlot(
            shadowInboundId,
            calendaringInterval.getDay(),
            calendaringInterval.getFrom(),
            calendaringInterval.getTo()
        );
        Long inboundId = ffWfApiSteps.createSupplyBbasedOnShadow(shadowInboundId);
        ffWfApiSteps.waitRequestComplete(shadowInboundId);
        ffWfApiSteps.waitRequestAppearOnCalendar(
            inboundId,
            WH_ID,
            calendaringInterval.getDay()
        );
        ffWfApiSteps.waitRequestCreated(inboundId, false);
        ffWfApiSteps.cancelRequest(inboundId);
    }

    @Test
    @DisplayName("Проверка переноса окна теневой поставки на другой день")
    public void shadowInboundChangeSlotTest() {
        log.info("Starting shadow inbound select slot...");

        Long shadowInboundId = ffWfApiSteps.createShadowInbound(FILE_PATH, MANUAL_CALENDAR);
        ffWfApiSteps.waitRequestValidated(shadowInboundId);
        CalendaringInterval calendaringInterval = ffWfApiSteps.getFreeTimeSlot(shadowInboundId, second_day);
        ffWfApiSteps.selectSlot(
            shadowInboundId,
            calendaringInterval.getDay(),
            calendaringInterval.getFrom(),
            calendaringInterval.getTo()
        );
        ffWfApiSteps.waitRequestAppearOnCalendar(
            shadowInboundId,
            WH_ID,
            calendaringInterval.getDay()
        );
        CalendaringInterval newCalendaringInterval = ffWfApiSteps.getFreeTimeSlot(shadowInboundId, third_day);
        ffWfApiSteps.selectSlot(
            shadowInboundId,
            newCalendaringInterval.getDay(),
            newCalendaringInterval.getFrom(),
            newCalendaringInterval.getTo()
        );
        ffWfApiSteps.waitRequestAppearOnCalendar(
            shadowInboundId,
            WH_ID,
            newCalendaringInterval.getDay()
        );
        ffWfApiSteps.cancelRequest(shadowInboundId);
    }

    @Test
    @DisplayName("Проверка переноса окна обычной поставки на другой день")
    public void calendaringInboundChangeSlotTest() {
        log.info("Starting calendaring inbound change slot test...");

        Long inboundId = ffWfApiSteps.createInbound(FILE_PATH, MANUAL_CALENDAR);
        ffWfApiSteps.waitRequestValidated(inboundId);
        CalendaringInterval calendaringInterval = ffWfApiSteps.getFreeTimeSlot(inboundId, third_day);
        ffWfApiSteps.selectSlot(
            inboundId,
            calendaringInterval.getDay(),
            calendaringInterval.getFrom(),
            calendaringInterval.getTo()
        );
        ffWfApiSteps.waitRequestAppearOnCalendar(
            inboundId,
            WH_ID,
            calendaringInterval.getDay()
        );
        ffWfApiSteps.waitRequestCreated(inboundId, false);
        CalendaringInterval newCalendaringInterval = ffWfApiSteps.getFreeTimeSlot(inboundId, second_day);
        ffWfApiSteps.selectSlot(
            inboundId,
            newCalendaringInterval.getDay(),
            newCalendaringInterval.getFrom(),
            newCalendaringInterval.getTo()
        );
        ffWfApiSteps.waitRequestAppearOnCalendar(
            inboundId,
            WH_ID,
            newCalendaringInterval.getDay()
        );
        ffWfApiSteps.cancelRequest(inboundId);
    }

    @Test
    @DisplayName("Создание 3p поставки на основе теневой")
    public void create3pInboundBasedOnShadowRequestTest() {
        log.info("Starting create 3p inbound based on shadow request...");

        Long shadowInboundId = ffWfApiSteps.createShadowSupply();
        ffWfApiSteps.waitRequestValidated(shadowInboundId);
        CalendaringInterval calendaringInterval = ffWfApiSteps.getFreeTimeSlotByService(shadowInboundId, third_day);
        Long inboundId = ffWfApiSteps.commitShadowSupply(shadowInboundId, calendaringInterval);
        ffWfApiSteps.waitRequestComplete(shadowInboundId);
        ffWfApiSteps.waitRequestAppearOnCalendar(
            inboundId,
            calendaringInterval.getServiceId(),
            calendaringInterval.getDay()
        );
        ffWfApiSteps.waitRequestCreated(inboundId, false);
        ffWfApiSteps.cancelRequest(inboundId);
    }

    @Test
    @DisplayName("Создание поставки с КИЗ-ами 1p")
    public void create1pInboundWithCisesTest() {
        log.info("Starting create inbound with cises...");

        Long inboundId = ffWfApiSteps.createInbound("ffwfapi/requests/1pInboundWithCIS.json");
        ffWfApiSteps.waitRequestOnWaitingForConfirmation(inboundId);
        Long inboundUpdateItemsId = ffWfApiSteps.itemsUpdate(inboundId, createUpdateRequestBody());
        ffWfApiSteps.waitRequestComplete(inboundUpdateItemsId);
        ffWfApiSteps.confirmRequest(inboundId);
        String ffInboundId = ffWfApiSteps.waitRequestCreated(inboundId, true);
        mocksId.addAll(ffWfApiSteps.mockInboundData(
            inboundId,
            ffInboundId,
            "delivery/mock/mockGet1pInboundWithCis.xml"
        ));

        ffWfApiSteps.waitRequestHasDocumentTypes(
            inboundId,
            RequestDocType.ACT_OF_RECEPTION_TRANSFER,
            RequestDocType.PALLET_LABEL
        );

        ffWfApiSteps.waitRequestComplete(inboundId);
        verifyInboundDetailsWithCises(inboundId);
    }

    @Test
    @DisplayName("Создание поставки с КИЗ-ами 3p")
    public void create3pInboundWithCisesTest() {
        log.info("Starting create inbound with cises...");

        Long inboundId = ffWfApiSteps.createInbound();
        String ffInboundId = ffWfApiSteps.waitRequestCreated(inboundId, true);
        mocksId.addAll(ffWfApiSteps.mockInboundData(
            inboundId,
            ffInboundId,
            "delivery/mock/mockGet3pInboundWithCis.xml"
        ));

        ffWfApiSteps.waitRequestHasDocumentTypes(
            inboundId,
            RequestDocType.ACT_OF_RECEPTION_TRANSFER,
            RequestDocType.PALLET_LABEL
        );

        ffWfApiSteps.waitRequestComplete(inboundId);
        verifyInboundDetailsWithCises(inboundId);
    }

    private HashMap<String, Object> createUpdateRequestBody() {
        HashMap<String, Object> updateRequestItem1 = createUpdateRequestItem(ARTICLE1,
            CIS1, 1, BigDecimal.valueOf(100)
        );
        HashMap<String, Object> updateRequestItem3 = createUpdateRequestItem(ARTICLE2,
            CIS2, 1, BigDecimal.valueOf(100)
        );

        HashMap<String, Object> updateRequest = new HashMap<>() {{
            put("items", List.of(updateRequestItem1, updateRequestItem3));
        }};

        return updateRequest;
    }

    private HashMap<String, Object> createUpdateRequestItem(String article, String cis, int count, BigDecimal price) {
        HashMap<String, Object> registryUnitPartialId = new HashMap<>() {{
            put("type", "CIS");
            put("value", cis);
        }};
        HashMap<String, Object> registryUnitId = new HashMap<>() {{
            put("parts", Collections.singleton(registryUnitPartialId));
        }};
        HashMap<String, Object> updateRequestItem = new HashMap<>() {{
            put("article", article);
            put("count", count);
            put("price", price);
            put("unitId", registryUnitId);
        }};

        return updateRequestItem;
    }
}
