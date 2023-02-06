package ru.yandex.market.logistics.lom.client;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.lom.model.dto.MdsFileDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentAcceptanceCertificateDto;
import ru.yandex.market.logistics.lom.model.enums.FileType;
import ru.yandex.market.logistics.lom.model.filter.AcceptanceCertificateSearchFilter;

class AcceptanceCertificateSearchTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Поиск актов приема-передачи")
    void searchAcceptanceCertificates() {
        prepareMockRequest(
            HttpMethod.PUT,
            "/acceptance-certificates/search",
            "document/search_acceptance_certificates.json"
        );
        List<ShipmentAcceptanceCertificateDto> actual = lomClient.searchAcceptanceCertificates(
            AcceptanceCertificateSearchFilter.builder().shipmentApplicationIds(ImmutableSet.of(1L, 2L, 3L)).build()
        );

        List<ShipmentAcceptanceCertificateDto> expected = List.of(
            createShipmentAcceptanceCertificateDto(2L),
            createShipmentAcceptanceCertificateDto(3L)
        );

        softly.assertThat(actual).usingRecursiveFieldByFieldElementComparator().isEqualTo(expected);
    }

    @Nonnull
    private ShipmentAcceptanceCertificateDto createShipmentAcceptanceCertificateDto(long id) {
        return ShipmentAcceptanceCertificateDto.builder()
            .shipmentApplicationId(id)
            .shipmentAcceptanceCertificateFile(createMdsFileDto(id))
            .build();
    }

    @Nonnull
    private MdsFileDto createMdsFileDto(long id) {
        return MdsFileDto.builder()
            .id(id)
            .mimeType("application/pdf")
            .fileType(FileType.SHIPMENT_ACCEPTANCE_CERTIFICATE)
            .fileName(String.format("act-%d.pdf", id))
            .url("https://mds.url/lom-doc-test/" + id)
            .build();
    }
}
