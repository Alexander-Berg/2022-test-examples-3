package ru.yandex.market.core.feature.impl.subsidies;

import java.util.Collections;

import org.apache.commons.lang3.BooleanUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.balance.xmlrpc.model.ClientContractsStructure;
import ru.yandex.market.common.balance.xmlrpc.model.ContractType;
import ru.yandex.market.common.balance.xmlrpc.model.OfferStructure;
import ru.yandex.market.common.balance.xmlrpc.model.PersonStructure;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientContractInfo;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.ProtocolFunction;
import ru.yandex.market.core.protocol.model.UIDActionContext;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class SubsidiesBalanceHelperTest extends FunctionalTest {

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private ProtocolService protocolService;

    /**
     * Магазин подключается первый раз. Плательщика и оферты ещё нет.
     */
    @Test
    @DbUnitDataSet(before = "firstEnable.before.csv")
    void firstEnable() {
        // given
        mockBalanceGetClientPersons(1L, 1001, PersonStructure.TYPE_GENERAL);
        doReturn(1L).when(balanceService).createOrUpdatePerson(any(), anyLong());

        // when
        protocolService.actionInTransaction(
                new UIDActionContext(ActionType.FEATURE_MANAGEMENT, 100500L),
                (ProtocolFunction<Void>) (transactionStatus, actionId) -> {
                    featureService.changeStatus(actionId, ShopFeature.of(1, FeatureType.SUBSIDIES,
                            ParamCheckStatus.SUCCESS));
                    return null;
                });

        // then
        var personCaptor = ArgumentCaptor.forClass(PersonStructure.class);
        verify(balanceService).createOrUpdatePerson(personCaptor.capture(), anyLong());
        assertThat(personCaptor.getValue(), allOf(
                not(hasKey("ID")), // см PersonStructure#FIELD_PERSON_ID javadoc
                not(hasKey("PERSON_ID")), // аналогично
                hasEntry("IS_PARTNER", "1")
        ));

        var offerCaptor = ArgumentCaptor.forClass(OfferStructure.class);
        verify(balanceService).createOffer(offerCaptor.capture(), anyLong());
        assertThat(offerCaptor.getValue(), hasEntry(equalTo("person_id"), allOf(
                notNullValue(),
                not(0)
        )));
    }

    /**
     * Магазин подключается второй раз. Плательщик и оферта уже есть, пересоздавать не надо.
     */
    @Test
    @DbUnitDataSet(before = "secondEnable.before.csv")
    void secondEnable() {
        mockBalanceGetClientPersons(1L, 1001, PersonStructure.TYPE_GENERAL);
        mockBalanceGetClientPersons(1L, 1002, PersonStructure.TYPE_PARTNER);
        when(balanceService.getClientContracts(eq(1L), eq(ContractType.SPENDABLE)))
                .thenReturn(Collections.singletonList(
                        ClientContractInfo.fromBalanceStructure(new ClientContractsStructure() {{
                            setIsActive(1);
                            setIsSuspended(0);
                            setCurrency("RUR");
                        }})));

        protocolService.actionInTransaction(
                new UIDActionContext(ActionType.FEATURE_MANAGEMENT, 100500L),
                (ProtocolFunction<Void>) (transactionStatus, actionId) -> {
                    featureService.changeStatus(actionId, ShopFeature.of(3, FeatureType.SUBSIDIES,
                            ParamCheckStatus.SUCCESS));
                    return null;
                });

        verify(balanceService, never()).createOrUpdatePerson(any(), anyLong());
        verify(balanceService, never()).createOffer(any(), anyLong());
    }

    /**
     * Магазин подключается оферта приостановлена.
     */
    @Test
    @DbUnitDataSet(before = "enableWhileSuspended.before.csv")
    void enableWhileSuspended() {
        mockBalanceGetClientPersons(1L, 1001, PersonStructure.TYPE_GENERAL);
        mockBalanceGetClientPersons(1L, 1002, PersonStructure.TYPE_PARTNER);
        when(balanceService.getClientContracts(eq(4L), eq(ContractType.SPENDABLE)))
                .thenReturn(Collections.singletonList(ClientContractInfo.fromBalanceStructure(
                        new ClientContractsStructure() {{
                            setIsActive(0);
                            setIsSuspended(1);
                            setCurrency("USD");
                        }})));

        protocolService.actionInTransaction(
                new UIDActionContext(ActionType.FEATURE_MANAGEMENT, 100500L),
                (ProtocolFunction<Void>) (transactionStatus, actionId) -> {
                    featureService.changeStatus(actionId, ShopFeature.of(4, FeatureType.SUBSIDIES,
                            ParamCheckStatus.SUCCESS));
                    return null;
                });

        verify(balanceService, never()).createOrUpdatePerson(any(), anyLong());

        var offerCaptor = ArgumentCaptor.forClass(OfferStructure.class);
        verify(balanceService).createOffer(offerCaptor.capture(), anyLong());
        assertThat(offerCaptor.getValue(), hasEntry(equalTo("person_id"), allOf(
                notNullValue(),
                not(0)
        )));
    }

    private void mockBalanceGetClientPersons(long clientId, long personId, int personType) {
        PersonStructure person = new PersonStructure();
        person.setPersonId(personId);
        person.setIsPartner(BooleanUtils.toBoolean(personType));
        when(balanceService.getClientPersons(eq(clientId), eq(personType)))
                .thenReturn(Collections.singletonList(person));
    }
}
