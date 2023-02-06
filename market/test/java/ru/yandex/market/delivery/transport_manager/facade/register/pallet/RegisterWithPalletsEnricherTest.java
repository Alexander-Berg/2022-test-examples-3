package ru.yandex.market.delivery.transport_manager.facade.register.pallet;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.delivery.transport_manager.domain.entity.Korobyte;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationTask;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationTaskStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.dto.TransportationTaskRegisterIdDto;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.PartialId;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.IdType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterUnitMapper;
import ru.yandex.market.delivery.transport_manager.service.status_flow.flow.StatusFlowFactory;
import ru.yandex.market.delivery.transport_manager.service.status_flow.flow.StatusFlowInitialData;
import ru.yandex.market.delivery.transport_manager.service.status_flow.lambda.Runnable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RegisterWithPalletsEnricherTest {
    public static final long REGISTER_ID = 100L;
    public static final TransportationTaskRegisterIdDto TASK_ID_REGISTER_ID = new TransportationTaskRegisterIdDto()
        .setRegisterId(REGISTER_ID)
        .setTransportationTaskId(1L);
    private RegisterWithPalletsEnricher registerWithPalletsEnricher;
    private RegisterMapper registerMapper;
    private RegisterUnitMapper registerUnitMapper;
    private StatusFlowFactory statusFlowFactory;
    private StatusFlowInitialData<TransportationTaskStatus, TransportationTask> statusFlowInitialData;

    @BeforeEach
    void setUp() throws Exception {
        registerMapper = mock(RegisterMapper.class);
        registerUnitMapper = mock(RegisterUnitMapper.class);
        statusFlowFactory = mock(StatusFlowFactory.class);
        statusFlowInitialData = Mockito.mock(StatusFlowInitialData.class);
        when(statusFlowInitialData.fromStatus(any(TransportationTaskStatus.class))).thenReturn(statusFlowInitialData);
        when(statusFlowInitialData.successStatus(any())).thenReturn(statusFlowInitialData);
        when(statusFlowInitialData.skipSuccessStatus(anyBoolean())).thenReturn(statusFlowInitialData);
        when(statusFlowFactory.transportationTask(any())).thenReturn(statusFlowInitialData);

        when(statusFlowInitialData.performTransactionalResultSaving(any())).thenAnswer(invocation -> {
            Runnable savingOp = invocation.getArgument(0);
            savingOp.apply();
            return true;
        });

        registerWithPalletsEnricher = new DummyRegisterWithPalletsEnricher(
            registerUnitMapper,
            statusFlowFactory,
            new PalletingService(registerUnitMapper)
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(registerUnitMapper);
    }

    @Test
    void enrichOk() {
        Register register = new Register().setId(REGISTER_ID).setPallets(Collections.emptyList());
        Mockito.when(registerMapper.findById(REGISTER_ID)).thenReturn(Optional.of(register));
        Mockito.when(registerMapper.getRegisterWithUnits(REGISTER_ID)).thenReturn(register);

        Mockito.when(registerUnitMapper.find(
            any(),
            eq(Pageable.unpaged())
        ))
            .thenReturn(List.of(
                new RegisterUnit()
                    .setId(1L)
                    .setType(UnitType.ITEM)
                    .setKorobyte(new Korobyte().setLength(100).setWidth(80).setHeight(140)),
                new RegisterUnit()
                    .setId(2L)
                    .setType(UnitType.ITEM)
                    .setKorobyte(new Korobyte().setLength(100).setWidth(80).setHeight(140))
            ));

        // имитируем простановку id внутри маппера:
        doAnswer(invocation -> palletAnswer(invocation, 100L))
            .doAnswer(invocation -> palletAnswer(invocation, 200L))
            .when(registerUnitMapper).persist(any());

        Assertions.assertEquals(
            Optional.of(TASK_ID_REGISTER_ID),
            registerWithPalletsEnricher.enrich(TASK_ID_REGISTER_ID)
        );

        final RegisterUnit p1 = new RegisterUnit()
            .setId(100L)
            .setType(UnitType.PALLET)
            .setRegisterId(register.getId())
            .setPartialIds(List.of(new PartialId().setIdType(IdType.PALLET_ID).setValue("100")))
            .setCounts(List.of(new UnitCount().setCountType(CountType.FIT).setQuantity(1)));

        final RegisterUnit p2 = new RegisterUnit()
            .setId(200L)
            .setType(UnitType.PALLET)
            .setRegisterId(register.getId())
            .setPartialIds(List.of(new PartialId().setIdType(IdType.PALLET_ID).setValue("200")))
            .setCounts(List.of(new UnitCount().setCountType(CountType.FIT).setQuantity(1)));

        verify(registerUnitMapper).persist(eq(p1));
        verify(registerUnitMapper).persist(eq(p2));
        verify(registerUnitMapper).itemUpdate(eq(p1));
        verify(registerUnitMapper).itemUpdate(eq(p2));

        verify(registerUnitMapper).find(
            any(),
            eq(Pageable.unpaged())
        );
        verify(registerUnitMapper).getAllRelationsForRegister(REGISTER_ID);
        // в товарах проставлена паллета
        verify(registerUnitMapper).persistSingleRelation(1L, 100L);
        verify(registerUnitMapper).persistSingleRelation(2L, 200L);
    }

    private long palletAnswer(InvocationOnMock invocation, long id) {
        RegisterUnit pallet = invocation.getArgument(0);
        pallet.setId(id);
        return id;
    }
}
