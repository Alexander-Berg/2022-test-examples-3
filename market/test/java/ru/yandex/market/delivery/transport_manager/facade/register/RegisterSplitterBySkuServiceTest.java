package ru.yandex.market.delivery.transport_manager.facade.register;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.register.PartialId;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.IdType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportMetadataMapper;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RegisterSplitterBySkuServiceTest {

    private static final long REGISTER_ID = 1;

    private RegisterSplitterBySkuService registerSplitterBySkuService;
    private RegisterService registerService;
    private TransportMetadataMapper transportMetadataMapper;

    @BeforeEach
    void setUp() {

        registerService = mock(RegisterService.class);

        transportMetadataMapper = mock(TransportMetadataMapper.class);
        when(transportMetadataMapper.getMaxTransportByRoute(1L, 2L)).thenReturn(33);

        registerSplitterBySkuService = new RegisterSplitterBySkuService(registerService, transportMetadataMapper);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(registerService);
    }

    @Test
    void testNoPallets() {
        Register noPallets = noPallets();
        when(registerService.getByRegisterIdWithUnits(REGISTER_ID)).thenReturn(noPallets);

        List<Register> registers = registerSplitterBySkuService.split(REGISTER_ID, 1L, 2L);
        verify(registerService).getByRegisterIdWithUnits(eq(REGISTER_ID));

        Assertions.assertEquals(registers.size(), 1);
        Assertions.assertEquals(registers.get(0), noPallets);
    }

    @Test
    void testOneHugePallet() {
        when(registerService.getByRegisterIdWithUnits(REGISTER_ID)).thenReturn(oneHugePalletRegister());

        List<Register> registers = registerSplitterBySkuService.split(REGISTER_ID, 1L, 2L);
        verify(registerService).getByRegisterIdWithUnits(eq(REGISTER_ID));

        Assertions.assertEquals(registers.size(), 1);
        Assertions.assertEquals(registers.get(0).getPallets().size(), 1);
        Assertions.assertEquals(registers.get(0).getItems().size(), 1);
    }

    @Test
    void testDividedIntoTwoRegisters() {
        when(registerService.getByRegisterIdWithUnits(REGISTER_ID)).thenReturn(twoHugePallets());

        List<Register> registers = registerSplitterBySkuService.split(REGISTER_ID, 1L, 2L);
        verify(registerService).getByRegisterIdWithUnits(eq(REGISTER_ID));

        Assertions.assertEquals(registers.size(), 2);

    }

    @Test
    void testThreePalletsTwoRegisters() {
        when(registerService.getByRegisterIdWithUnits(REGISTER_ID)).thenReturn(threePalletsTwoRegisters());

        List<Register> registers = registerSplitterBySkuService.split(REGISTER_ID, 1L, 2L);
        verify(registerService).getByRegisterIdWithUnits(eq(REGISTER_ID));

        Assertions.assertEquals(registers.size(), 2);
        Assertions.assertEquals(
            registers.get(0).getPallets().stream().map(RegisterUnit::getId).collect(Collectors.toList()),
            List.of(10L)
        );

        Assertions.assertEquals(
            registers.get(0).getItems().stream().map(RegisterUnit::getId).collect(Collectors.toList()),
            List.of(100L)
        );

        /*  Изначальный состав паллет, который схлопнется до 650 FIT, 200 DEFECT
            SKU одинаковые.

            P1: [[UnitCount(countType=FIT, quantity=400, unitIds=null), UnitCount(countType=DEFECT, quantity=200,
            unitIds=null)],
            P2: [UnitCount(countType=FIT, quantity=100, unitIds=null)],
            P3: [UnitCount(countType=FIT, quantity=150, unitIds=null)]]
         */
        Assertions.assertEquals(
            registers.get(0)
                .getItems()
                .stream()
                .flatMap((RegisterUnit registerUnit) -> registerUnit.getCounts().stream())
                .collect(Collectors.toList()),
            List.of(
                new UnitCount().setCountType(CountType.FIT).setQuantity(650),
                new UnitCount().setCountType(CountType.DEFECT).setQuantity(200)
            )
        );

        Assertions.assertEquals(
            registers.get(1).getPallets().stream().map(RegisterUnit::getId).collect(Collectors.toList()),
            List.of(11L, 12L)
        );
        Assertions.assertEquals(
            registers.get(1).getItems().stream().map(RegisterUnit::getId).collect(Collectors.toList()),
            List.of(110L)
        );
    }

    @Test
    void testPalletsOutOfOrder() {
        when(registerService.getByRegisterIdWithUnits(REGISTER_ID)).thenReturn(palletsOutOfOrder());

        List<Register> registers = registerSplitterBySkuService.split(REGISTER_ID, 1L, 2L);
        verify(registerService).getByRegisterIdWithUnits(eq(REGISTER_ID));

        Assertions.assertEquals(registers.size(), 4);
        Assertions.assertEquals(
            registers.get(0).getPallets().stream().map(RegisterUnit::getId).collect(Collectors.toList()),
            List.of(10L, 12L)
        );
        Assertions.assertEquals(
            registers.get(1).getPallets().stream().map(RegisterUnit::getId).collect(Collectors.toList()),
            List.of(11L, 13L)
        );
        Assertions.assertEquals(
            registers.get(2).getPallets().stream().map(RegisterUnit::getId).collect(Collectors.toList()),
            List.of(14L, 17L)
        );
        Assertions.assertEquals(
            registers.get(3).getPallets().stream().map(RegisterUnit::getId).collect(Collectors.toList()),
            List.of(15L, 16L)
        );
    }

    @Test
    void testLimitByMaxAvailableCar() {
        when(transportMetadataMapper.getMaxTransportByRoute(1L, 2L)).thenReturn(2);
        when(registerService.getByRegisterIdWithUnits(REGISTER_ID)).thenReturn(palletsLimitedByMaxCarCapacity());

        List<Register> registers = registerSplitterBySkuService.split(REGISTER_ID, 1L, 2L);
        verify(registerService).getByRegisterIdWithUnits(eq(REGISTER_ID));

        Assertions.assertEquals(registers.size(), 2);
    }

    private Register noPallets() {
        return new Register()
            .setItems(List.of(unit(1, UnitType.ITEM, List.of(count(1500)), 1L)));
    }

    private Register oneHugePalletRegister() {
        return new Register()
            .setPallets(List.of(unit(1, UnitType.PALLET)))
            .setItems(List.of(unit(1, UnitType.ITEM, List.of(count(1500)), 1L)));
    }

    private Register twoHugePallets() {
        return new Register()
            .setPallets(List.of(unit(10, UnitType.PALLET), unit(11, UnitType.PALLET)))
            .setItems(
                List.of(
                    unit(100, UnitType.ITEM, List.of(count(800)), 10L),
                    unit(110, UnitType.ITEM, List.of(count(900)), 11L)
                )
            );
    }

    private Register threePalletsTwoRegisters() {
        return new Register()
            .setPallets(
                List.of(unit(10, UnitType.PALLET), unit(11, UnitType.PALLET), unit(12, UnitType.PALLET))
            )
            .setItems(
                List.of(
                    unit(100, UnitType.ITEM, List.of(count(CountType.FIT, 400), count(CountType.DEFECT, 200)), 10L),
                    unit(101, UnitType.ITEM, List.of(count(100)), 10L),
                    unit(102, UnitType.ITEM, List.of(count(150)), 10L),
                    unit(110, UnitType.ITEM, List.of(count(400)), 11L),
                    unit(120, UnitType.ITEM, List.of(count(250)), 12L),
                    unit(121, UnitType.ITEM, List.of(count(CountType.DEFECT, 250)), 12L)
                )
            );
    }

    private Register palletsOutOfOrder() {
        return new Register()
            .setPallets(
                List.of(
                    unit(10, UnitType.PALLET), unit(11, UnitType.PALLET), unit(12, UnitType.PALLET),
                    unit(13, UnitType.PALLET), unit(14, UnitType.PALLET), unit(15, UnitType.PALLET),
                    unit(16, UnitType.PALLET), unit(17, UnitType.PALLET)
                )
            )
            .setItems(
                List.of(
                    unit(101, UnitType.ITEM, List.of(count(600)), 10L),
                    unit(111, UnitType.ITEM, List.of(count(800)), 11L),
                    unit(121, UnitType.ITEM, List.of(count(400)), 12L),
                    unit(131, UnitType.ITEM, List.of(count(200)), 13L),
                    unit(141, UnitType.ITEM, List.of(count(700)), 14L),
                    unit(151, UnitType.ITEM, List.of(count(500)), 15L),
                    unit(161, UnitType.ITEM, List.of(count(500)), 16L),
                    unit(171, UnitType.ITEM, List.of(count(300)), 17L)
                )
            );
    }

    private Register palletsLimitedByMaxCarCapacity() {
        return new Register()
            .setPallets(
                List.of(
                    unit(10, UnitType.PALLET),
                    unit(11, UnitType.PALLET),
                    unit(12, UnitType.PALLET)
                )
            )
            .setItems(
                List.of(
                    unit(101, UnitType.ITEM, List.of(count(100)), 10L),
                    unit(111, UnitType.ITEM, List.of(count(100)), 11L),
                    unit(121, UnitType.ITEM, List.of(count(100)), 12L)
                )
            );
    }

    private RegisterUnit unit(long id, UnitType type, List<UnitCount> counts, Long parentId) {
        return new RegisterUnit()
            .setId(id)
            .setType(type)
            .setCounts(counts)
            .setPartialIds(List.of(new PartialId().setIdType(IdType.ARTICLE).setValue("SKU1")))
            .setParentIds(Optional.ofNullable(parentId).map(Sets::newHashSet).orElse(null));
    }

    private RegisterUnit unit(long id, UnitType type) {
        return unit(id, type, null, null);
    }

    private UnitCount count(CountType type, int quantity) {
        return new UnitCount()
            .setCountType(type)
            .setQuantity(quantity);
    }

    private UnitCount count(int quantity) {
        return count(CountType.FIT, quantity);
    }

}
