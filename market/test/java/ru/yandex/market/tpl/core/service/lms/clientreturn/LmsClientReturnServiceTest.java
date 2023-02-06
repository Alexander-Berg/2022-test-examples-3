package ru.yandex.market.tpl.core.service.lms.clientreturn;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.front.library.dto.Action;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.dto.grid.GridItem;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnService;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.lms.clientreturn.LmsClientReturnFilterDto;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.service.lms.usershift.LmsUserShiftService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class LmsClientReturnServiceTest extends TplAbstractTest {

    private static final String BARCODE = "VOZVRAT_SF_PS_700238586";
    private static final String RETURN_ID = "200200";
    private static final Long UID = 1L;

    private final ClientReturnRepository clientReturnRepository;
    private final LmsClientReturnService lmsClientReturnService;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final OrderGenerateService orderGenerateService;
    private final ClientReturnService clientReturnService;
    private final LmsUserShiftService lmsUserShiftService;
    private final UserShiftRepository userShiftRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    private PickupPoint lockerPickupPoint;

    @BeforeEach
    void init() {
        lockerPickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L,
                DeliveryService.DEFAULT_DS_ID);
    }

    @Test
    public void getClientReturnDetailData() {
        var clientReturn = transactionTemplate.execute(ts -> {
            var cr = clientReturnGenerator.generate();
            cr.setBarcode(BARCODE);
            cr.setExternalReturnId(RETURN_ID);
            cr.setStatus(ClientReturnStatus.READY_FOR_RECEIVED);
            return clientReturnRepository.save(cr);
        });


        DetailData detailData = lmsClientReturnService.getClientReturnById(clientReturn.getId());
        assertThat(detailData.getItem().getTitle()).isEqualTo("Клиентский возврат 200200");
        assertThat(detailData.getItem().getValues()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "barcode", BARCODE,
                "status", ClientReturnStatus.READY_FOR_RECEIVED.getDescription(),
                "createdDate", LocalDate.now(),
                "externalReturnId", RETURN_ID
        ));
        assertThat(detailData.getMeta().getActions())
                .filteredOn(this::actionIsActive)
                .extracting(Action::getSlug)
                .isEqualTo(List.of("/deliveredToSc"));
    }


    @Test
    public void getClientReturnGridData() {
        var clientReturn1 = transactionTemplate.execute(ts -> {
            var cr = clientReturnGenerator.generate();
            cr.setBarcode(BARCODE);
            cr.setExternalReturnId(RETURN_ID);
            cr.setStatus(ClientReturnStatus.READY_FOR_RECEIVED);
            return clientReturnRepository.save(cr);
        });

        var clientReturn2 = transactionTemplate.execute(ts -> {
            var cr = clientReturnGenerator.generate();
            cr.setBarcode(BARCODE + "1");
            cr.setExternalReturnId(RETURN_ID + "1");
            cr.setStatus(ClientReturnStatus.CANCELLED);
            return clientReturnRepository.save(cr);
        });

        var clientReturn3 = transactionTemplate.execute(ts -> {
            var cr = clientReturnGenerator.generate();
            cr.setBarcode(BARCODE + "2");
            cr.setExternalReturnId(RETURN_ID + "2");
            cr.setStatus(ClientReturnStatus.DELIVERED_TO_SC);
            return clientReturnRepository.save(cr);
        });

        GridData unfilteredGridData = lmsClientReturnService.getClientReturns(null, Pageable.unpaged());
        assertThat(unfilteredGridData.getTotalCount()).isEqualTo(3);

        GridData filteredByExternalReturnId = lmsClientReturnService.getClientReturns(
                new LmsClientReturnFilterDto(null, RETURN_ID, null, null),
                Pageable.unpaged()
        );
        assertThat(filteredByExternalReturnId.getItems())
                .extracting(GridItem::getId)
                .containsExactly(clientReturn1.getId());

        GridData filteredByBarcode = lmsClientReturnService.getClientReturns(
                new LmsClientReturnFilterDto(null, null, BARCODE + "2", null),
                Pageable.unpaged()
        );
        assertThat(filteredByBarcode.getItems())
                .extracting(GridItem::getId)
                .containsExactly(clientReturn3.getId());


        GridData filteredByBarcodeAndCreatedDate = lmsClientReturnService.getClientReturns(
                new LmsClientReturnFilterDto(null, null, BARCODE + "1", LocalDate.now()),
                Pageable.unpaged()
        );
        assertThat(filteredByBarcodeAndCreatedDate.getItems())
                .extracting(GridItem::getId)
                .containsExactlyInAnyOrder(clientReturn2.getId());
    }

    @Test
    public void clientReturnDeliveredToSc() {
        var clientReturn = transactionTemplate.execute(ts -> {
            var cr = clientReturnGenerator.generate();
            cr.setBarcode(BARCODE);
            cr.setExternalReturnId(RETURN_ID);
            cr.setStatus(ClientReturnStatus.READY_FOR_RECEIVED);
            cr.setPickupPoint(lockerPickupPoint);
            return clientReturnRepository.save(cr);
        });

        assertThat(clientReturn.getStatus()).isEqualTo(ClientReturnStatus.READY_FOR_RECEIVED);
        lmsClientReturnService.makeDeliveredToSc(clientReturn.getId());

        ClientReturn findReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(findReturn.getStatus()).isEqualTo(ClientReturnStatus.DELIVERED_TO_SC);
    }

    @Test
    public void clientReturnDeliveredToScWithFinishedUserShift() {
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(lockerPickupPoint)
                .build());

        var user = testUserHelper.findOrCreateUser(UID);
        var userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        var task = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        var clientReturn = transactionTemplate.execute(ts -> {
            var cr = clientReturnGenerator.generate();
            cr.setBarcode(BARCODE);
            cr.setExternalReturnId(RETURN_ID);
            cr.setStatus(ClientReturnStatus.READY_FOR_RECEIVED);
            cr.setPickupPoint(lockerPickupPoint);
            return clientReturnRepository.save(cr);
        });

        task.createPickupSubtaskClientReturn(Long.parseLong(RETURN_ID), LockerDeliverySubtaskStatus.STARTED);

        lmsUserShiftService.closeUserShift(userShift.getId());
        UserShift findShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        assertThat(findShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);

        clientReturnService.deliveredToSc(clientReturn.getExternalReturnId());
        ClientReturn findReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(findReturn.getStatus()).isEqualTo(ClientReturnStatus.DELIVERED_TO_SC);
    }

    public boolean actionIsActive(Action action) {
        return action.isActive() == null; //sic!
    }

}

