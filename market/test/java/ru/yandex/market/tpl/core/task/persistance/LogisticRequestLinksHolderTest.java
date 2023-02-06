package ru.yandex.market.tpl.core.task.persistance;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequest;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.task.flow.LogisticRequestLinksHolder;
import ru.yandex.market.tpl.core.task.persistence.FlowTaskEntity;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.task.service.LogisticRequestLinkService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class LogisticRequestLinksHolderTest extends TplAbstractTest {

    private final LogisticRequestLinkService logisticRequestLinkService;
    private final LogisticRequestRepository logisticRequestRepository;
    private final SpecialRequestGenerateService specialRequestGenerateService;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final Clock clock;

    private LogisticRequest specialRequest1;
    private LogisticRequest specialRequest2;
    private FlowTaskEntity flowTask;

    @BeforeEach
    void setup() {
        var pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER,
                333L, DeliveryService.DEFAULT_DS_ID);
        specialRequest1 = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .pickupPointId(pickupPoint.getId())
                        .build()
        );
        specialRequest2 = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .pickupPointId(pickupPoint.getId())
                        .build()
        );

        var user = testUserHelper.findOrCreateUser(1234L);
        var userShift = testUserHelper.createEmptyShift(user, LocalDate.of(2000, 1, 1));
        flowTask = testDataFactory.addFlowTask(
                userShift.getId(),
                TaskFlowType.LOCKER_INVENTORY,
                List.of(specialRequest1, specialRequest2)
        );
    }

    @Test
    void getLinksTest() {
        var linksHolder = new LogisticRequestLinksHolder(flowTask.getId(), logisticRequestLinkService,
                logisticRequestRepository, clock, 1L);

        var linkedLogisticRequestIds = linksHolder.getLinkedLogisticRequestIds();
        assertThat(linkedLogisticRequestIds).hasSize(2);
        assertThat(linkedLogisticRequestIds).contains(specialRequest1.getId(), specialRequest2.getId());

        var linkedLogisticRequests = linksHolder.getLinkedLogisticRequests();
        assertThat(linkedLogisticRequests).hasSize(2);
        assertThat(linkedLogisticRequests).contains(specialRequest1, specialRequest2);
    }

}
