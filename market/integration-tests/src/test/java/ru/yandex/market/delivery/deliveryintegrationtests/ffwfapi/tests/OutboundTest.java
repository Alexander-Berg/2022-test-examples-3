package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.tests;


import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Step;
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

import static org.hamcrest.Matchers.is;


@DisplayName("FF Workflow API Outbound Test")
public class OutboundTest {
    private final Logger log = LoggerFactory.getLogger(OutboundTest.class);

    private static final String WH_ID = "171";
    private static final String third_day = "2";

    private final FFWfApiSteps ffWfApiSteps = new FFWfApiSteps();

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

    @Test
    @DisplayName("Создание и отмена изъятия")
    public void outboundCreateCancelTest() {
        log.info("Starting outboundCreateCancelTest...");

        Long outboundId = ffWfApiSteps.createWithdraw();
        ffWfApiSteps.waitRequestCreated(outboundId, false);
        ffWfApiSteps.cancelRequest(outboundId);
        ffWfApiSteps.waitRequestCanceled(outboundId);
    }

    @Step("Проверяем детали изъятия")
    private void verifyOutboundDetails(Long inboundId) {
        log.info("Checking outbound details");
        ffWfApiSteps.getRequest(inboundId)
                .body("itemsTotalCount", is(2))
                .body("itemsTotalFactCount", is(2));
    }

    @Test
    @DisplayName("Создание и обработка изъятия, проверка документов")
    public void outboundCreateProcessTest() {
        log.info("Starting outboundCreateProcessTest...");

        Long outboundId = ffWfApiSteps.createWithdraw();
        String ffInboundId = ffWfApiSteps.waitRequestCreated(outboundId, false);

        mocksId.addAll(ffWfApiSteps.mockOutboundData(outboundId, ffInboundId));
        ffWfApiSteps.waitRequestComplete(outboundId);

        verifyOutboundDetails(outboundId);

        ffWfApiSteps.waitRequestHasDocumentTypes(outboundId,
                RequestDocType.WITHDRAW,
                RequestDocType.ACT_OF_WITHDRAW);
    }

    @Test
    @DisplayName("Создание календаризированного изъятия")
    public void createCalendaringOutboundTest() {
        log.info("Starting create calendaring outbound test...");

        Long shadowOutboundId = ffWfApiSteps.createShadowWithdraw();
        ffWfApiSteps.waitRequestValidated(shadowOutboundId);
        CalendaringInterval calendaringInterval = ffWfApiSteps.getFreeTimeSlot(shadowOutboundId, third_day);
        Long OutboundId = ffWfApiSteps.createWithdrawBasedOnShadow(
                shadowOutboundId,
                calendaringInterval.getDay(),
                calendaringInterval.getFrom(),
                calendaringInterval.getTo());
        ffWfApiSteps.waitRequestComplete(shadowOutboundId);
        ffWfApiSteps.waitOutboundAppearOnCalendar(OutboundId,
                WH_ID,
                calendaringInterval.getDay());
        ffWfApiSteps.waitRequestCreated(OutboundId, false);
        ffWfApiSteps.cancelRequest(OutboundId);
    }
}
