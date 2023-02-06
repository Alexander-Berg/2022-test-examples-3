package ru.yandex.market.mbi.api.controller.operation.replication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.ReplicateRequest;
import ru.yandex.market.mbi.open.api.client.model.ShipmentDateReplicationResponse;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ShipmentDateCopyControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Успешное копирование правила")
    @DbUnitDataSet(before = "PartnerReplicationController.ShipmentsDateCopy.before.csv",
            after = "PartnerReplicationController.ShipmentsDateCopy.after.csv")
    public void shipmentDataCopySuccess() {
        ShipmentDateReplicationResponse response =
                getMbiOpenApiClient().requestPartnerShipmentDateCopy(123L, new ReplicateRequest()
                        .sourcePartnerId(501L)
                        .newPartnerId(502L));

        assertThat(response)
                .returns(true, ShipmentDateReplicationResponse::getCopied);
    }

    @Test
    @DisplayName("Нет правила-донора")
    public void shipmentDataCopyNoRule() {
        assertThatExceptionOfType(MbiOpenApiClientResponseException.class)
                .isThrownBy(() ->
                        getMbiOpenApiClient().requestPartnerShipmentDateCopy(123L, new ReplicateRequest()
                                .sourcePartnerId(501L)
                                .newPartnerId(502L)))
                .satisfies(e -> assertThat(e.getApiError().getMessage())
                        .isEqualTo("Partner with id 501 was not found"));
    }
}
