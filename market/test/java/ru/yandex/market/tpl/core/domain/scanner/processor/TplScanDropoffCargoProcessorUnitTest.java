package ru.yandex.market.tpl.core.domain.scanner.processor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.api.model.scanner.ScannerDisplayMode;
import ru.yandex.market.tpl.api.model.scanner.ScannerOrderDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoQueryService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class TplScanDropoffCargoProcessorUnitTest {

    public static final long USER_ID = 1L;
    @Mock
    private UserShiftRepository userShiftRepository;
    @Mock
    private DropoffCargoQueryService cargoQueryService;
    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @InjectMocks
    private TplScanDropoffCargoProcessor dropoffCargoProcessor;

    @BeforeEach
    void setUp() {
        when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CARGO_DROPOFF_DIRECT_FLOW_ENABLED))
                .thenReturn(true);
    }

    @Test
    void process_when_Disabled() {
        //given
        when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CARGO_DROPOFF_DIRECT_FLOW_ENABLED))
                .thenReturn(false);

        //then
        assertThat(dropoffCargoProcessor.process(List.of(), null)).isEmpty();
    }

    @Test
    void process_when_DropoffWithoutCurrentUserShift() {
        //given
        List<String> existedBarcodes = List.of("existedBarcode");
        User user = new User();
        when(cargoQueryService.getAllByBarcodes(existedBarcodes)).thenReturn(List.of(new DropoffCargo()));
        when(userShiftRepository.findCurrentShift(user)).thenReturn(Optional.empty());

        //then
        assertThrows(TplInvalidParameterException.class, () -> dropoffCargoProcessor.process(existedBarcodes, user));
    }

    @Test
    void process_when_DropoffWithUserShiftAndNotUniqCargo() {
        //given
        List<String> existedBarcodes = List.of("existedBarcode");
        User user = new User();
        when(cargoQueryService.getAllByBarcodes(existedBarcodes)).thenReturn(List.of(new DropoffCargo()));

        UserShift us = new UserShift();
        us.setId(USER_ID);
        when(userShiftRepository.findCurrentShift(user)).thenReturn(Optional.of(us));

        when(cargoQueryService.getDropoffForPickup(USER_ID, existedBarcodes)).thenReturn(List.of(new DropoffCargo(),
                new DropoffCargo()));
        //then
        assertThrows(TplInvalidParameterException.class, () -> dropoffCargoProcessor.process(existedBarcodes, user));
    }

    @Test
    void process_when_DropoffWithCargoNotInUserShift() {
        //given
        List<String> existedBarcodes = List.of("existedBarcode", "foundedBarcode");
        User user = new User();
        DropoffCargo dropoffCargo = new DropoffCargo();
        dropoffCargo.setBarcode("foundedBarcode");
        when(cargoQueryService.getAllByBarcodes(existedBarcodes)).thenReturn(List.of(dropoffCargo));

        UserShift us = new UserShift();
        us.setId(USER_ID);
        when(userShiftRepository.findCurrentShift(user)).thenReturn(Optional.of(us));

        when(cargoQueryService.getDropoffForPickup(USER_ID, existedBarcodes)).thenReturn(List.of());

        //when
        Optional<ScannerOrderDto> scannerOrderDtoO = dropoffCargoProcessor.process(existedBarcodes, user);

        //then
        assertThat(scannerOrderDtoO).isEmpty();
    }


    @Test
    void process_when_CorrectDropoff() {
        //given
        List<String> existedBarcodes = List.of("existedBarcode", "foundedBarcode");
        User user = new User();
        DropoffCargo dropoffCargo = new DropoffCargo();
        dropoffCargo.setBarcode("foundedBarcode");
        when(cargoQueryService.getAllByBarcodes(existedBarcodes)).thenReturn(List.of(dropoffCargo));

        UserShift us = new UserShift();
        us.setId(USER_ID);
        when(userShiftRepository.findCurrentShift(user)).thenReturn(Optional.of(us));

        when(cargoQueryService.getDropoffForPickup(USER_ID, existedBarcodes)).thenReturn(List.of(dropoffCargo));

        //when
        Optional<ScannerOrderDto> scannerOrderDtoO = dropoffCargoProcessor.process(existedBarcodes, user);

        //then
        assertThat(scannerOrderDtoO).isNotEmpty();
        ScannerOrderDto scannerOrderDto = scannerOrderDtoO.get();
        assertThat(scannerOrderDto.getDisplayMode()).isEqualTo(ScannerDisplayMode.PICKUP);
        assertThat(scannerOrderDto.getExternalOrderId()).isEqualTo(dropoffCargo.getBarcode());
        assertThat(scannerOrderDto.getPlaces()).hasSize(1);
        assertThat(scannerOrderDto.getPlaces().get(0).getBarcode()).isEqualTo(dropoffCargo.getBarcode());
    }


    @Test
    void processByReferenceID_when_CorrectDropoff_notUniq() {
        //given
        List<String> existedBarcodes = List.of("existedBarcode", "foundedBarcode");
        User user = new User();
        DropoffCargo dropoffCargo = new DropoffCargo();
        dropoffCargo.setBarcode("multy-1");
        dropoffCargo.setReferenceId("multy");
        DropoffCargo dropoffCargo2 = new DropoffCargo();
        dropoffCargo2.setBarcode("multy-2");
        dropoffCargo2.setReferenceId("multy");

        UserShift us = new UserShift();
        us.setId(USER_ID);
        when(userShiftRepository.findCurrentShift(user)).thenReturn(Optional.of(us));
        when(cargoQueryService.getDropoffForPickup(USER_ID, existedBarcodes)).thenReturn(List.of());

        when(cargoQueryService.getDropoffForPickupByReference(USER_ID, existedBarcodes))
                .thenReturn(List.of(dropoffCargo, dropoffCargo2));

        //when
        Optional<ScannerOrderDto> scannerOrderDtoO = dropoffCargoProcessor.process(existedBarcodes, user);

        //then
        assertThat(scannerOrderDtoO).isNotEmpty();
        ScannerOrderDto scannerOrderDto = scannerOrderDtoO.get();
        assertThat(scannerOrderDto.getDisplayMode()).isEqualTo(ScannerDisplayMode.MULTI_PLACE_NEED_SCAN_BARCODE);
        assertThat(scannerOrderDto.getExternalOrderId()).isEqualTo(dropoffCargo.getReferenceId());
        assertThat(scannerOrderDto.getPlaces()).hasSize(2);
        assertThat(scannerOrderDto.getPlaces().stream()
                .map(PlaceForScanDto::getBarcode).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(dropoffCargo.getBarcode(), dropoffCargo2.getBarcode());
    }


    @Test
    void processByReferenceID_when_CorrectDropoff_empty() {
        //given
        List<String> existedBarcodes = List.of("existedBarcode", "foundedBarcode");
        User user = new User();
        DropoffCargo dropoffCargo = new DropoffCargo();
        dropoffCargo.setBarcode("multy-1");
        dropoffCargo.setReferenceId("multy");
        DropoffCargo dropoffCargo2 = new DropoffCargo();
        dropoffCargo2.setBarcode("multy-2");
        dropoffCargo2.setReferenceId("multy");

        UserShift us = new UserShift();
        us.setId(USER_ID);
        when(userShiftRepository.findCurrentShift(user)).thenReturn(Optional.of(us));
        when(cargoQueryService.getDropoffForPickup(USER_ID, existedBarcodes)).thenReturn(List.of());

        when(cargoQueryService.getDropoffForPickupByReference(USER_ID, existedBarcodes))
                .thenReturn(List.of());

        //when
        Optional<ScannerOrderDto> scannerOrderDtoO = dropoffCargoProcessor.process(existedBarcodes, user);

        //then
        assertThat(scannerOrderDtoO).isEmpty();
    }


    @Test
    void processByReferenceID_when_CorrectDropoff() {
        //given
        List<String> existedBarcodes = List.of("existedBarcode", "foundedBarcode");
        User user = new User();
        DropoffCargo dropoffCargo = new DropoffCargo();
        dropoffCargo.setBarcode("foundedBarcode");

        UserShift us = new UserShift();
        us.setId(USER_ID);
        when(userShiftRepository.findCurrentShift(user)).thenReturn(Optional.of(us));
        when(cargoQueryService.getDropoffForPickup(USER_ID, existedBarcodes)).thenReturn(List.of());

        when(cargoQueryService.getDropoffForPickupByReference(USER_ID, existedBarcodes))
                .thenReturn(List.of(dropoffCargo));

        //when
        Optional<ScannerOrderDto> scannerOrderDtoO = dropoffCargoProcessor.process(existedBarcodes, user);

        //then
        assertThat(scannerOrderDtoO).isNotEmpty();
        ScannerOrderDto scannerOrderDto = scannerOrderDtoO.get();
        assertThat(scannerOrderDto.getDisplayMode()).isEqualTo(ScannerDisplayMode.PICKUP);
        assertThat(scannerOrderDto.getExternalOrderId()).isEqualTo(dropoffCargo.getBarcode());
        assertThat(scannerOrderDto.getPlaces()).hasSize(1);
        assertThat(scannerOrderDto.getPlaces().get(0).getBarcode()).isEqualTo(dropoffCargo.getBarcode());
    }
}
