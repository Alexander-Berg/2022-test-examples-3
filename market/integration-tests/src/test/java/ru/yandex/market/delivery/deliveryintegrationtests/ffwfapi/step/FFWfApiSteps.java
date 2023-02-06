package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.step;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.client.CalendaringServiceApi;
import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.client.FFWfApi;
import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.dto.CalendaringInterval;
import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.dto.RequestDocType;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;

import client.DsFfMockClient;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

@Resource.Classpath({"delivery/report.properties"})
public class FFWfApiSteps {

    private final Logger log = LoggerFactory.getLogger(FFWfApiSteps.class);

    private final FFWfApi ffWfApi = new FFWfApi();
    private final CalendaringServiceApi csApi = new CalendaringServiceApi();
    private final DsFfMockClient dsFfMock = new DsFfMockClient();

    @Property("reportblue.crossdockShopId")
    private long blueCrossdockShopId;

    public FFWfApiSteps() {
        PropertyLoader.newInstance().populate(this);
    }

    @Step("Создаем поставку в FF Workflow AP на послезавтра")
    public Long createInbound() {
        return ffWfApi.uploadSupply(DateUtil.todayPlusXDateTime(2))
            .body("id", not(emptyOrNullString()))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Step("Создаем поставку в FF Workflow AP на послезавтра")
    public Long createInbound(String requestFilePath) {
        return ffWfApi.uploadSupply(DateUtil.todayPlusXDateTime(2), requestFilePath)
            .body("id", not(emptyOrNullString()))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Step("Создаем календаризированную поставку в FF Workflow AP на послезавтра")
    public Long createInbound(String requestFilePath, String calendaringMode) {
        //Если передавать время 00:00, то будет больше свободных окон на складе.
        return ffWfApi.uploadSupply(DateUtil.todayPlusXDateZeroTime(2), requestFilePath, calendaringMode)
            .body("id", not(emptyOrNullString()))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Step("Создаем теневую поставку в FF Workflow AP на послезавтра")
    public Long createShadowInbound(String requestFilePath, String calendaringMode) {
        return ffWfApi.uploadShadowSupply(DateUtil.todayPlusXDateZeroTime(2), requestFilePath, calendaringMode)
            .body("id", not(emptyOrNullString()))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Step("Создаем x-doc поставку в FF Workflow AP на послезавтра")
    public Long createXdocInbound(long warehouseId, long xDocPartnerId) {
        return ffWfApi.uploadXdocSupply(warehouseId, xDocPartnerId, DateUtil.todayPlusXDateTime(2))
            .body("id", not(emptyOrNullString()))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Step("Создаем изъятие в FF Workflow AP")
    public Long createWithdraw() {
        return ffWfApi.uploadWithdraw(DateUtil.todayPlusXDateTime(6))
            .body("id", not(emptyOrNullString()))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Step("Создаем теневое изъятие в FF Workflow API")
    public Long createShadowWithdraw() {
        return ffWfApi.uploadShadowWithdraw(DateUtil.todayPlusXDateTime(6))
            .body("id", not(emptyOrNullString()))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Step("Создаем теневую поставку в FF Workflow AP")
    public Long createShadowSupply() {
        return ffWfApi.uploadShadowSupply()
            .body("id", not(emptyOrNullString()))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Step("Создаем трансфер")
    public Long createTransfer(
        long inboundId,
        String article,
        int count,
        long serviceId,
        int stockTypeFrom,
        int stockTypeTo,
        long supplierId
    ) {
        return ffWfApi.createTransfer(inboundId, article, count, serviceId, stockTypeFrom, stockTypeTo, supplierId)
            .body("transferId", not(emptyOrNullString()))
            .extract()
            .jsonPath()
            .getLong("transferId");
    }

    @Step("Создаем заявку на обновление поставки КИЗ-ами")
    public Long itemsUpdate(Long requestId, HashMap<String, Object> updateRequestBody) {
        return ffWfApi.itemsUpdate(requestId, updateRequestBody)
            .body("id", not(emptyOrNullString()))
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Step("Подтверждаем заявку")
    public void confirmRequest(Long requestId) {
        ffWfApi.confirmRequest(requestId);
    }

    @Step("Отменяем заявку")
    public void cancelRequest(Long requestId) {
        ffWfApi.cancelRequest(requestId);
    }

    @Step("Проверяем, что заявка успешно создалась")
    private String verifyRequestCreated(Long requestId) {
        log.info("Verifying request is created");
        return ffWfApi.getRequest(requestId)
            .body("status", is(oneOf(3, 200, 210, 23)))
            .body("serviceRequestId", not(emptyOrNullString()))
            .extract()
            .path("serviceRequestId");
    }

    @Step("Проверяем, что x-doc заявка успешно создалась или уже отгружена на конечный склад")
    private String verifyXdocRequestCreatedOrShippedByPartner(Long requestId, Integer... statuses) {
        log.info("Verifying x-doc request is created");
        return ffWfApi.getRequest(requestId)
            .body("status", is(oneOf(statuses)))
            .body("serviceRequestId", not(emptyOrNullString()))
            .body("xDocRequestedDate", not(emptyOrNullString()))
            .body("xDocServiceRequestId", not(emptyOrNullString()))
            .extract()
            .path("xDocServiceRequestId");
    }

    @Step("Проверяем, что заявка успешно отменилась")
    private void verifyRequestCanceled(Long requestId) {
        log.info("Verifying request is canceled");
        ffWfApi.getRequest(requestId)
            .body("status", is(8));
    }

    @Step("Проверяем, что заявка обработана и данные пришли в FF Workflow API")
    private void verifyRequestComplete(Long requestId) {
        log.info("Verifying request is complete");
        ffWfApi.getRequest(requestId)
            .body("status", is(10));
    }

    private void verifyRequestOnWaitingForConfirmation(Long requestId) {
        log.info("Verifying request is confirmed");
        ffWfApi.getRequest(requestId)
            .body("status", is(12));
    }

    @Step("Ждем когда заявка получит один из статусов")
    public void waitRequestStatusIsIn(Long requestId, Integer... statuses) {
        log.info("Waiting for request to be one of: {}", statuses);
        Retrier.retry(
            () -> ffWfApi.getRequest(requestId)
                .body("status", is(oneOf(statuses))),
            Retrier.RETRIES_BIG,
            1,
            TimeUnit.MINUTES
        );
    }

    @Step("Ждем когда заявка получит статус {status}")
    public void waitRequestStatusIs(Long requestId, int status) {
        log.info("Waiting for request to receive status");

        Retrier.retry(
            () -> ffWfApi.getRequest(requestId)
                .body("status", is(status)),
            Retrier.RETRIES_BIGGEST,
            1,
            TimeUnit.MINUTES
        );
    }

    @Step("Получаем данные заявки")
    public ValidatableResponse getRequest(Long requestId) {
        log.info("Getting request data");

        return ffWfApi.getRequest(requestId);
    }

    @Step("Получаем данные трансфера")
    public ValidatableResponse getTransfer(Long transferId) {
        log.info("Getting transfer data");

        return ffWfApi.getTransfer(transferId);
    }

    @Step("Ждём, когда в календаре появится созданная поставка")
    public void waitRequestAppearOnCalendar(Long requestId, String warehouseId, String supplyDate) {
        log.info("Waiting that the created request {} receive in the calendar", requestId);
        Retrier.retry(
            () -> verifyCalendarHasRequest(requestId, warehouseId, supplyDate),
            Retrier.RETRIES_SMALL
        );
    }

    @Step("Проверяем, что в календаре появилась созданная поставка")
    public void verifyCalendarHasRequest(Long requestId, String warehouseId, String supplyDate) {

        String searchString = String.format("gates.find {it.bookings.find {it.idFromSource == \"%s\"}}", requestId);

        csApi.getCalendaringRequest(warehouseId, supplyDate).body(searchString, not(emptyOrNullString()));
    }

    @Step("Ждём, когда в календаре появится созданное изъятие")
    public void waitOutboundAppearOnCalendar(Long requestId, String warehouseId, String supplyDate) {
        log.info("Waiting that the created outbound {} receive in the calendar", requestId);
        Retrier.retry(
            () -> verifyCalendarHasOutbound(requestId, warehouseId, supplyDate),
            Retrier.RETRIES_SMALL
        );
    }

    @Step("Проверяем, что в календаре появилось созданное изъятие")
    public void verifyCalendarHasOutbound(Long requestId, String warehouseId, String supplyDate) {

        String searchString = String.format("gates.find {it.bookings.find {it.idFromSource == \"%s\"}}", requestId);

        csApi.getCalendaringWithdrawRequest(warehouseId, supplyDate).body(searchString, not(emptyOrNullString()));
    }

    @Step("Получаем данные по календарю поставок для конкретного склада на конкретный день")
    public ValidatableResponse getCalendaringRequest(String warehouseId, String supplyDate) {
        log.info("Getting calendar request data");

        return csApi.getCalendaringRequest(warehouseId, supplyDate);
    }

    @Step("Получаем документы заявки")
    public ValidatableResponse getRequestDocuments(Long requestId) {
        log.info("Getting request docs");

        return ffWfApi.getRequestDocuments(requestId);
    }

    @Step("Ждем, когда у заявки появятся документы")
    public void waitRequestHasDocumentTypes(Long requestId, RequestDocType... docTypes) {
        Retrier.retry(
            () -> verifyRequestHasDocumentTypes(requestId, docTypes),
            Retrier.RETRIES_BIG,
            1,
            TimeUnit.MINUTES
        );
    }

    @Step("Проверяем, что у заявки есть документы")
    public void verifyRequestHasDocumentTypes(Long requestId, RequestDocType... docTypes) {

        ValidatableResponse response = getRequestDocuments(requestId);

        for (RequestDocType docType : docTypes
        ) {
            String searchString = String.format("find {it.type == %s}.fileUrl", docType.getId());
            response.body(searchString, not(emptyOrNullString()));
        }
    }

    @Step("Ждем, когда заявка создастся на складе")
    public String waitRequestCreated(Long requestId, boolean isMigratedToPutXXX) {
        if (isMigratedToPutXXX) {
            waitRequestStatusIsIn(requestId, 4, 5, 8, 200, 210, 220, 23);
        } else {
            waitRequestStatusIsIn(requestId, 3, 4, 5, 8, 200, 210, 220, 23);
        }
        return verifyRequestCreated(requestId);
    }

    @Step("Ждем, когда заявка провалидируется")
    public void waitRequestValidated(Long requestId) {
        waitRequestStatusIs(requestId, 1);
    }

    @Step("Ждем, когда заявка создастся на складе партнера и конечном складе" +
        " или поставка будет отгружена со склада партнера")
    public String waitXdocRequestCreatedOrShippedByPartner(Long requestId) {
        waitRequestStatusIsIn(requestId, 4, 5, 100, 210, 220, 240);
        return verifyXdocRequestCreatedOrShippedByPartner(requestId, 210, 240);
    }

    @Step("Ждем, когда заявка отменится")
    public void waitRequestCanceled(Long requestId) {
        waitRequestStatusIsIn(requestId, 8, 101);
        verifyRequestCanceled(requestId);
    }

    @Step("Ждем, когда будет принята и обработана")
    public void waitRequestComplete(Long requestId) {
        waitRequestStatusIsIn(requestId, 7, 10, 23);
        waitRequestStatusIs(requestId, 10);
        verifyRequestComplete(requestId);
    }

    public void waitRequestOnWaitingForConfirmation(Long requestId) {
        waitRequestStatusIsIn(requestId, 5, 12);
        verifyRequestOnWaitingForConfirmation(requestId);
    }

    @Step("Ждем появления данных о паллетах и проверяем их количество")
    public void waitForPalletNumberToBe(Long inboundId, int pallets) {
        Retrier.retry(() -> verifyInboundPallets(inboundId, pallets));
    }

    private void verifyInboundPallets(Long inboundId, int pallets) {
        log.info("Checking inbound details");
        getRequest(inboundId)
            .body("actualPalletAmount", is(pallets));
    }

    @Step("Ждем появления данных о коробках и проверяем их количество")
    public void waitForBoxNumberToBe(Long inboundId, int boxes) {
        Retrier.retry(() -> verifyInboundBoxes(inboundId, boxes));
    }

    private void verifyInboundBoxes(Long inboundId, int boxes) {
        log.info("Checking inbound details");
        getRequest(inboundId)
            .body("actualBoxAmount", is(boxes));
    }

    @Step("Проверяем, что в деталях есть данные о перемещении 1 товара")
    public void verifyTransferDetails(long transferId, int transferedCount) {
        log.info("Checking transfer details");
        getRequest(transferId)
            .body("itemsTotalCount", is(transferedCount))
            .body("itemsTotalFactCount", is(transferedCount))
            .body("itemsTotalDefectCount", is(0))
            .body("itemsTotalSurplusCount", is(0));

        getTransfer(transferId)
            .body("status", is(10))
            .body("items.first().validationErrors", is(emptyIterable()))
            .body("items.first().actualCount", is(transferedCount))
            .body("items.first().declaredCount", is(transferedCount));
    }

    @Step("Задаем данные мока для поставки")
    public List<Integer> mockInboundData(Long requestId, String serviceRequestId, String mockFilePath) {
        List<Integer> mocksId = new ArrayList<>();
        mocksId.add(dsFfMock.mockGetInbound(requestId, serviceRequestId, mockFilePath));
        mocksId.add(dsFfMock.mockGetInboundHistory(requestId, serviceRequestId));
        mocksId.add(dsFfMock.mockGetInboundsStatus(requestId, serviceRequestId));
        return mocksId;
    }

    @Step("Задаем данные мока для изъятия")
    public List<Integer> mockOutboundData(Long requestId, String serviceRequestId) {
        List<Integer> mocksId = new ArrayList<>();
        mocksId.add(dsFfMock.mockGetOutboundDetails(requestId, serviceRequestId));
        mocksId.add(dsFfMock.mockGetOutbound(requestId, serviceRequestId));
        mocksId.add(dsFfMock.mockGetOutboundHistory(requestId, serviceRequestId));
        mocksId.add(dsFfMock.mockGetOutboundsStatus(requestId, serviceRequestId));
        return mocksId;
    }

    @Step("Задаем данные мока поставки через партнерский склад")
    public List<Integer> mockXdocInboundData() {
        List<Integer> mocksId = new ArrayList<>();
        mocksId.add(dsFfMock.mockXdocCreateInbound());
        mocksId.add(dsFfMock.mockXdocGetInboundDetailsXDoc());
        mocksId.add(dsFfMock.mockXdocGetInboundHistory());
        mocksId.add(dsFfMock.mockXdocGetInboundsStatus());
        return mocksId;
    }

    @Step("Получаем первое доспупное для выбора окно в конкретный день: {day}")
    public CalendaringInterval getFreeTimeSlot(Long requestId, String day) {
        log.info("Getting free time slot for request {}", requestId);

        CalendaringInterval calendaringInterval = new CalendaringInterval();

        ValidatableResponse resp = ffWfApi.getFreeTimeSlots(requestId);

        calendaringInterval.setDay(resp
            .extract()
            .path("day[%s]", day)
            .toString());

        calendaringInterval.setFrom(resp
            .extract()
            .path("[%s].slots[0].from", day)
            .toString());

        calendaringInterval.setTo(resp
            .extract()
            .path("[%s].slots[0].to", day)
            .toString());

        return calendaringInterval;
    }

    @Step("Получаем первое доспупное для выбора окно в конкретный день: {day} для первого доступного склада")
    public CalendaringInterval getFreeTimeSlotByService(Long requestId, String day) {
        log.info("Getting free time slot by service for request {}", requestId);

        CalendaringInterval calendaringIntervalByService = new CalendaringInterval();

        ValidatableResponse resp = ffWfApi.getFreeTimeSlotsByService(requestId);

        long i = findSlotForService(resp, 172L);

        calendaringIntervalByService.setServiceId(resp
            .extract()
            .path("[" + i + "].serviceId")
            .toString());

        calendaringIntervalByService.setDay(resp
            .extract()
            .path("[" + i + "].freeSlots[%s].day", day)
            .toString());

        calendaringIntervalByService.setFrom(resp
            .extract()
            .path("[" + i + "].freeSlots[%s].slots[0].from", day)
            .toString());

        calendaringIntervalByService.setTo(resp
            .extract()
            .path("[" + i + "].freeSlots[%s].slots[0].to", day)
            .toString());

        return calendaringIntervalByService;
    }

    private long findSlotForService(ValidatableResponse resp, Long serviceId) {
        long i = 0L;
        while (true) {
            String s = resp.extract()
                .path("[" + i + "].serviceId")
                .toString();
            if (s.isEmpty()) {
                break;
            }

            if (Long.parseLong(s) == serviceId) {
                break;
            }
            i = i + 1;
        }
        return i;
    }

    @Step("Бронируем окно для поставки {requestId} на {day}")
    public void selectSlot(Long requestId, String day, String from, String to) {
        ffWfApi.selectSlot(requestId, day, from, to);
    }

    @Step("Создаем заявку на поставку на основе теневой {requestId}, бронируя для нее слот на складе {serviceId} " +
        "на {day} и получаем ее id")
    public Long commitShadowSupply(Long shadowInboundId, CalendaringInterval calendaringInterval) {
        return ffWfApi.commitShadowSupply(
                shadowInboundId,
                calendaringInterval.getDay(),
                calendaringInterval.getFrom(),
                calendaringInterval.getTo(),
                calendaringInterval.getServiceId()
            )
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Step("Создаем обычную поставку на основе теневой и получаем ее id")
    public Long createSupplyBbasedOnShadow(Long shadowInboundId) {
        return ffWfApi.createSupplyBbasedOnShadow(shadowInboundId)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Step("Создаем обычное изъятие на основе теневого и получаем его id")
    public Long createWithdrawBasedOnShadow(Long shadowOutboundId, String date, String from, String to) {
        return ffWfApi.createWithdrawBasedOnShadow(shadowOutboundId, date, from, to)
            .extract()
            .jsonPath()
            .getLong("id");
    }

    @Step("Получаем список поставок, привязанных к айтемам поставщика")
    private List<Long> getInboundsToCancel() {
        return ffWfApi.getCrossdockItemsByDate(blueCrossdockShopId, DateUtil.currentDateIso())
            .extract()
            .jsonPath()
            .getList("items.findAll {it.draftFinalizationDate.startsWith('" +
                DateUtil.currentDate()
                + "') && it.shopRequestId != null}" +
                ".collect {it.shopRequestId.toLong()}.unique()");
    }

    @Step("Удаляем все неотмененные поставки поставщика за сегодня")
    public void cancelExistingInbounds() {

        List<Long> requestsToCancel = getInboundsToCancel();

        log.info("Inbounds to cancel: {}", requestsToCancel);

        for (Long request : requestsToCancel
        ) {
            cancelRequest(request);
        }

        for (Long request : requestsToCancel
        ) {
            waitRequestCanceled(request);
        }
    }

    @Step("Удаляем моки")
    public void deleteMocks(List<Integer> mocksId) {
        for (Integer mockId : mocksId) {
            if (mockId != null) {
                try {
                    dsFfMock.deleteMockById(mockId);
                } catch (AssertionFailedError e) {
                    log.info("Got assertion failed error", e);
                }
            }
        }
        mocksId.clear();
    }
}

