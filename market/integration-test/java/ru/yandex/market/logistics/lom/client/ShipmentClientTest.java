package ru.yandex.market.logistics.lom.client;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.lom.model.dto.ShipmentApplicationDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentConfirmationDto;

import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.entityErrorBuilder;
import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.shipmentApplicationDtoBuilder;
import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.shipmentConfirmationDtoBuilder;
import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.shipmentDtoBuilder;

class ShipmentClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Создать заявку на отгрузку")
    void createShipmentApplication() {
        prepareMockRequest(
            HttpMethod.POST,
            "/shipments",
            "request/shipment/create_shipment_application.json",
            "response/shipment/create_shipment_application.json"
        );
        ShipmentApplicationDto actual = lomClient.createShipmentApplication(shipmentApplicationDtoBuilder().build());
        ShipmentApplicationDto expected = shipmentApplicationDtoBuilder()
            .id(1L)
            .shipment(shipmentDtoBuilder().id(1L).build())
            .build();

        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Подтвердить заявку на отгрузку")
    void confirmShipmentApplication() {
        prepareMockRequest(
            HttpMethod.PUT,
            "/shipments/1/confirm",
            null,
            "response/shipment/confirm_shipment_application.json"
        );

        ShipmentConfirmationDto expected = shipmentConfirmationDtoBuilder()
            .build();

        ShipmentConfirmationDto response = lomClient.confirmShipmentApplication(1);

        softly.assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Подтвердить заявку на отгрузку - парсинг ошибок")
    void confirmShipmentApplicationWithErrors() {
        prepareMockRequest(
            HttpStatus.BAD_REQUEST,
            HttpMethod.PUT,
            "/shipments/1/confirm",
            null,
            "response/shipment/confirm_shipment_application_errors.json"
        );

        ShipmentConfirmationDto expected = shipmentConfirmationDtoBuilder()
            .errors(List.of(entityErrorBuilder().build()))
            .build();

        ShipmentConfirmationDto response = lomClient.confirmShipmentApplication(1);

        softly.assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }
}
