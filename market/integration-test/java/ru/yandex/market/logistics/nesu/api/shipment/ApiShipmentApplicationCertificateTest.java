package ru.yandex.market.logistics.nesu.api.shipment;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.MdsFileDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentAcceptanceCertificateDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentSearchDto;
import ru.yandex.market.logistics.lom.model.filter.AcceptanceCertificateSearchFilter;
import ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.nesu.api.AbstractApiTest;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.binaryContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение акта приема-передачи в Open API")
@DatabaseSetup("/controller/api/shipment/application/certificate/data.xml")
class ApiShipmentApplicationCertificateTest extends AbstractApiTest {

    private static final long SHOP_ID = 1L;
    private static final long SHOP_MARKET_ID = 1000L;
    private static final long APPLICATION_ID = 1L;
    private static final String TEST_FILE = "controller/api/shipment/application/certificate/test.gif";

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private LomClient lomClient;

    @BeforeEach
    void setup() {
        authHolder.mockAccess(mbiApiClient, SHOP_ID);

        mockShipmentsSearch(List.of(
            ShipmentSearchDto.builder().hasAcceptanceCertificate(true).build()
        ));
        mockCertificateSearch(List.of(
            ShipmentAcceptanceCertificateDto.builder()
                .shipmentAcceptanceCertificateFile(
                    MdsFileDto.builder()
                        .url(ClassLoader.getSystemResource(TEST_FILE).toString())
                        .build()
                )
                .build()
        ));
    }

    @Test
    @DisplayName("Неизвестный магазин")
    void unknownShop() throws Exception {
        getCertificate(2L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [2]"));
    }

    @Test
    @DisplayName("Недоступный магазин")
    void inaccessibleShop() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, SHOP_ID);

        getCertificate(SHOP_ID)
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Неизвестная заявка")
    void unknownShipmentApplication() throws Exception {
        mockShipmentsSearch(List.of());

        getCertificate(SHOP_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHIPMENT_APPLICATION] with ids [1]"));
    }

    @Test
    @DisplayName("Флаг наличия АПП = false")
    void hasCertificateFlagIsFalse() throws Exception {
        mockShipmentsSearch(List.of(
            ShipmentSearchDto.builder().hasAcceptanceCertificate(false).build()
        ));

        getCertificate(SHOP_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHIPMENT_ACCEPTANCE_CERTIFICATE] with ids [1]"));
    }

    @Test
    @DisplayName("Ответ из LOM пустой")
    void emptyResponse() throws Exception {
        mockCertificateSearch(List.of());

        getCertificate(SHOP_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHIPMENT_ACCEPTANCE_CERTIFICATE] with ids [1]"));
    }

    @Test
    @DisplayName("Ответ непустой, но отсутствует файл с АПП")
    void noCertificate() throws Exception {
        mockCertificateSearch(List.of(
            ShipmentAcceptanceCertificateDto.builder().shipmentAcceptanceCertificateFile(null).build()
        ));

        getCertificate(SHOP_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHIPMENT_ACCEPTANCE_CERTIFICATE] with ids [1]"));
    }

    @Test
    @DisplayName("Неправильный адрес файла")
    void invalidFileUrl() throws Exception {
        mockCertificateSearch(List.of(
            ShipmentAcceptanceCertificateDto.builder()
                .shipmentAcceptanceCertificateFile(
                    MdsFileDto.builder()
                        .url("invalid:file")
                        .build()
                )
                .build()
        ));

        getCertificate(SHOP_ID)
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("unknown protocol: invalid"));
    }

    @Test
    @DisplayName("Успех")
    void success() throws Exception {
        getCertificate(SHOP_ID)
            .andExpect(status().isOk())
            .andExpect(binaryContent(TEST_FILE));
    }

    void mockShipmentsSearch(List<ShipmentSearchDto> shipments) {
        doReturn(new PageResult<ShipmentSearchDto>().setData(shipments))
            .when(lomClient)
            .searchShipments(
                eq(
                    ShipmentSearchFilter.builder()
                        .marketIdFrom(SHOP_MARKET_ID)
                        .withApplication(true)
                        .shipmentApplicationIds(Set.of(APPLICATION_ID))
                        .build()
                ),
                any()
            );
    }

    void mockCertificateSearch(List<ShipmentAcceptanceCertificateDto> certificates) {
        doReturn(certificates)
            .when(lomClient)
            .searchAcceptanceCertificates(
                AcceptanceCertificateSearchFilter.builder()
                    .shipmentApplicationIds(Set.of(APPLICATION_ID))
                    .build()
            );
    }

    private ResultActions getCertificate(Long shopId) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.GET, "/api/shipments/applications/" + APPLICATION_ID
                + "/act", null)
                .param("cabinetId", String.valueOf(shopId))
        );
    }

}
