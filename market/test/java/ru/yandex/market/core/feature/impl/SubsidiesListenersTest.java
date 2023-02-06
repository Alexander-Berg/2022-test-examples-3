package ru.yandex.market.core.feature.impl;

import java.time.Clock;
import java.util.Collections;

import org.apache.commons.lang3.BooleanUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.api.cpa.yam.service.PrepayRequestStatusListener;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.common.balance.xmlrpc.model.PersonStructure;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.feature.precondition.listeners.PrepayPreconditionListener;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.StringParamValue;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.UIDActionContext;
import ru.yandex.market.mbi.api.client.entity.payment.PaymentCheckStatus;
import ru.yandex.market.mbi.util.Changed;
import ru.yandex.market.mbi.util.Updated;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class SubsidiesListenersTest extends FunctionalTest {

    @Autowired
    @Qualifier("subsidiesPrepayPreconditionListener")
    private PrepayPreconditionListener prepayListener;

    @Autowired
    private ParamService paramService;

    @Autowired
    private CutoffService cutoffService;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private Clock clock;

    @BeforeEach
    void init() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
        when(checkouterClient.shops()).thenReturn(mock(CheckouterShopApi.class));
    }

    @Test
    @DbUnitDataSet(
            before = "freezePrepayRequest.before.csv",
            after = "freezePrepayRequest.after.csv"
    )
    void freezePrepayRequest() {
        Changed<PrepayRequestStatusListener.StatusInfo> changed = changeStatus(1, PartnerApplicationStatus.COMPLETED,
                PartnerApplicationStatus.FROZEN);
        prepayListener.processStatusChanges(changed, 1);
    }

    @Test
    @DbUnitDataSet(
            before = "freezePrepayRequestFailedSubsidies.before.csv",
            after = "freezePrepayRequestFailedSubsidies.after.csv"
    )
    void freezePrepayRequestFailedSubsidies() {
        Changed<PrepayRequestStatusListener.StatusInfo> changed = changeStatus(1, PartnerApplicationStatus.COMPLETED,
                PartnerApplicationStatus.FROZEN);
        prepayListener.processStatusChanges(changed, 1);
    }

    @Test
    @DbUnitDataSet(
            before = "completePrepayRequest.before.csv",
            after = "completePrepayRequest.after.csv"
    )
    void completePrepayRequest() {
        Changed<PrepayRequestStatusListener.StatusInfo> changed =
                changeStatus(1, PartnerApplicationStatus.IN_PROGRESS, PartnerApplicationStatus.COMPLETED);
        prepayListener.processStatusChanges(changed, 1);
    }

    @Test
    @DbUnitDataSet(
            before = "completePrepayRequestFailedSubsidies.before.csv",
            after = "completePrepayRequestFailedSubsidies.after.csv")
    void completePrepayRequestFailedSubsidies() {
        Changed<PrepayRequestStatusListener.StatusInfo> changed =
                changeStatus(1, PartnerApplicationStatus.IN_PROGRESS, PartnerApplicationStatus.COMPLETED);
        prepayListener.processStatusChanges(changed, 100500);
    }

    @Test
    @DbUnitDataSet(
            before = "disablePrepay.before.csv",
            after = "disablePrepay.after.csv"
    )
    void disablePrepay() {
        paramService.setParam(new StringParamValue(ParamType.PAYMENT_CHECK_STATUS, 1, PaymentCheckStatus.DONT_WANT.name()), 1);
    }

    /**
     * Включаем руками магазина предоплату у магазина, размещающегося в ПИ. Субсидии включаются и отправляются на КЗ.
     */
    @Test
    @DbUnitDataSet(
            before = "enablePrepay.before.csv",
            after = "enablePrepay.after.csv"
    )
    void enablePrepay() {
        paramService.setParam(new StringParamValue(ParamType.PAYMENT_CHECK_STATUS, 1, PaymentCheckStatus.SUCCESS.name()), 1);
    }

    /**
     * Включаем руками магазина предоплату у магазина, размещающегося в ПИ. Субсидии включаются сразу в SUCCESS.
     */
    @Test
    @DbUnitDataSet(
            before = "enablePrepayPI.before.csv",
            after = "enablePrepayPI.after.csv"
    )
    void enablePrepayPI() {
        PersonStructure person = new PersonStructure();
        person.setPersonId(1001L);
        person.setIsPartner(BooleanUtils.toBoolean(PersonStructure.TYPE_GENERAL));
        when(balanceService.getClientPersons(eq(1L), eq(PersonStructure.TYPE_GENERAL)))
                .thenReturn(Collections.singletonList(person));

        protocolService.actionInTransaction(
                new UIDActionContext(ActionType.CHANGE_PARAM, 100500L),
                (transactionStatus, actionId) -> {
                    paramService.setParam(new StringParamValue(ParamType.PAYMENT_CHECK_STATUS, 1, PaymentCheckStatus.SUCCESS.name()), actionId);
                    return null;
                });
    }

    @Test
    @DbUnitDataSet(
            before = "disableCpa.before.csv",
            after = "disableCpa.after.csv"
    )
    void disableCpa() {
        protocolService.actionInTransaction(
                new UIDActionContext(ActionType.CHANGE_PARAM, 100500L),
                (status, actionId) -> cutoffService.openCutoff(1, CutoffType.CPA_PARTNER, 1)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "enableCpa.before.csv",
            after = "enableCpa.after.csv"
    )
    void enableCpa() {
        protocolService.actionInTransaction(
                new UIDActionContext(ActionType.CHANGE_PARAM, 100500L),
                (status, actionId) -> cutoffService.closeCutoff(1, CutoffType.CPA_PARTNER, 1)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "needInfoPrepayRequest.before.csv",
            after = "needInfoPrepayRequest.after.csv"
    )
    void needInfoPrepayRequest() {
        Changed<PrepayRequestStatusListener.StatusInfo> changed =
                changeStatus(1, PartnerApplicationStatus.COMPLETED, PartnerApplicationStatus.NEED_INFO);
        prepayListener.processStatusChanges(changed, 1);
    }

    private static Changed<PrepayRequestStatusListener.StatusInfo> changeStatus(
            long shopId,
            PartnerApplicationStatus from,
            PartnerApplicationStatus to
    ) {
        return Updated.fromTo(
                PrepayRequestStatusListener.StatusInfo.builder()
                        .setRequestId(1L)
                        .setDatasourceIds(Collections.singleton(shopId))
                        .setStatus(from)
                        .setPrepayType(PrepayType.YANDEX_MARKET)
                        .build(),
                PrepayRequestStatusListener.StatusInfo.builder()
                        .setRequestId(1L)
                        .setDatasourceIds(Collections.singleton(shopId))
                        .setStatus(to)
                        .setPrepayType(PrepayType.YANDEX_MARKET)
                        .build()
        );
    }
}
