package ru.yandex.market.logistics.nesu.controller.document;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.MdsFileDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentAcceptanceCertificateDto;
import ru.yandex.market.logistics.lom.model.enums.FileType;
import ru.yandex.market.logistics.lom.model.filter.AcceptanceCertificateSearchFilter;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение акта приёма-передачи")
@ParametersAreNonnullByDefault
class DocumentGetAcceptanceCertificateTest extends AbstractContextualTest {
    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Успешное получение")
    void getAcceptanceCertificateOk() throws Exception {
        AcceptanceCertificateSearchFilter filter = createAcceptanceCertificateSearchFilter();
        when(lomClient.searchAcceptanceCertificates(safeRefEq(filter)))
            .thenReturn(createShipmentAcceptanceCertificateDtoList(1L));
        getAcceptanceCertificate()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/document/acceptance_certificate_1.json"));
        verify(lomClient).searchAcceptanceCertificates(safeRefEq(filter));
    }

    @Test
    @DisplayName("Ответ непустой, но отсутствует файл с АПП")
    void getAcceptanceCertificateFileNotFound() throws Exception {
        AcceptanceCertificateSearchFilter filter = createAcceptanceCertificateSearchFilter();
        when(lomClient.searchAcceptanceCertificates(safeRefEq(filter)))
            .thenReturn(List.of(createShipmentAcceptanceCertificateDto(1L, null)));
        getAcceptanceCertificate()
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/document/acceptance_certificate_not_found.json"));
        verify(lomClient).searchAcceptanceCertificates(safeRefEq(filter));
    }

    @Test
    @DisplayName("Ответ из LOM пустой")
    void getAcceptanceCertificateResponseIsEmpty() throws Exception {
        AcceptanceCertificateSearchFilter filter = createAcceptanceCertificateSearchFilter();
        when(lomClient.searchAcceptanceCertificates(safeRefEq(filter))).thenReturn(List.of());
        getAcceptanceCertificate()
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/document/acceptance_certificate_not_found.json"));
        verify(lomClient).searchAcceptanceCertificates(safeRefEq(filter));
    }

    @Nonnull
    private AcceptanceCertificateSearchFilter createAcceptanceCertificateSearchFilter() {
        return AcceptanceCertificateSearchFilter.builder()
            .shipmentApplicationIds(Set.of(1L))
            .build();
    }

    @Nonnull
    private List<ShipmentAcceptanceCertificateDto> createShipmentAcceptanceCertificateDtoList(
        Long... shipmentApplicationIds
    ) {
        return Stream.of(shipmentApplicationIds)
            .map(this::createShipmentAcceptanceCertificateDto)
            .collect(Collectors.toList());
    }

    @Nonnull
    private ShipmentAcceptanceCertificateDto createShipmentAcceptanceCertificateDto(Long shipmentApplicationId) {
        return createShipmentAcceptanceCertificateDto(shipmentApplicationId, createMdsFileDto(shipmentApplicationId));
    }

    @Nonnull
    private ShipmentAcceptanceCertificateDto createShipmentAcceptanceCertificateDto(
        Long shipmentApplicationId,
        @Nullable MdsFileDto certificateFile
    ) {
        return ShipmentAcceptanceCertificateDto.builder()
            .shipmentApplicationId(shipmentApplicationId)
            .shipmentAcceptanceCertificateFile(certificateFile)
            .build();
    }

    @Nonnull
    private MdsFileDto createMdsFileDto(long id) {
        return MdsFileDto.builder()
            .id(id)
            .fileName(String.format("act-%d.pdf", id))
            .fileType(FileType.SHIPMENT_ACCEPTANCE_CERTIFICATE)
            .mimeType("application/pdf")
            .url("https://mds.url/lom-doc-test/" + id)
            .build();
    }

    @Nonnull
    private ResultActions getAcceptanceCertificate() throws Exception {
        return mockMvc.perform(
            get("/back-office/documents/acceptance-certificate")
                .param("shipmentApplicationId", String.valueOf(1L))
        );
    }
}
