package ru.yandex.market.mbi.api.controller.operation.replication;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.api.controller.operation.partner.replication.PartnerReplicationController;
import ru.yandex.market.mbi.open.api.client.model.ApiError;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerResponse;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты на {@link PartnerReplicationController#replicateDbsPartner}.
 */
public class DbsReplicationControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Репликация партнера. Успешная")
    @DbUnitDataSet(before = "PartnerReplicationController.DbsPartnerReplication.before.csv",
            after = "PartnerReplicationController.DbsPartnerReplication.after.csv")
    public void replicationPartnerSuccessTests() {
        final ReplicatePartnerRequest partnerRequest = new ReplicatePartnerRequest();
        partnerRequest.setPartnerDonorId(1237L);
        partnerRequest.setRegionId(213L);
        partnerRequest.setWarehouseName("test Warehouse name");
        partnerRequest.setPartnerWarehouseId("ABCabc012-_/\\");
        ReplicatePartnerResponse response = getMbiOpenApiClient().replicateDbsPartner(123L, partnerRequest);
        Assertions.assertNotNull(response.getPartnerId());
    }

    @Test
    @DisplayName("Репликация партнера. Ошибка при неподтвержденной заявке")
    @DbUnitDataSet(before = "PartnerReplicationController.appNotCompleted.before.csv")
    public void replicationPartnerShouldThrowAppNotCompletedTests() {
        final ReplicatePartnerRequest partnerRequest = new ReplicatePartnerRequest();
        partnerRequest.setPartnerDonorId(1237L);
        partnerRequest.setRegionId(213L);
        partnerRequest.setWarehouseName("test Warehouse name");
        MbiOpenApiClientResponseException exception = Assertions.assertThrows(MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().replicateDbsPartner(123L, partnerRequest));

        assertThat(exception.getApiError())
                .returns("Application for partner 1237 is not in 'COMPLETED' status", ApiError::getMessage)
                .returns(ApiError.MessageCodeEnum.APP_NOT_COMPLETED, ApiError::getMessageCode);
    }

    @Test
    @DisplayName("Репликация партнера. Ошибка для региона уровня 7")
    @DbUnitDataSet(before = "PartnerReplicationController.DbsPartnerReplication.before.csv")
    public void replicationPartnerShouldThrowRegionTests() {
        final ReplicatePartnerRequest partnerRequest = new ReplicatePartnerRequest();
        partnerRequest.setPartnerDonorId(1237L);
        partnerRequest.setRegionId(222L);
        partnerRequest.setWarehouseName("test Warehouse name");
        MbiOpenApiClientResponseException exception = Assertions.assertThrows(MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().replicateDbsPartner(123L, partnerRequest));

        assertThat(exception.getApiError())
                .returns("wrong-region", ApiError::getMessage)
                .returns(ApiError.MessageCodeEnum.INCORRECT_LOCAL_DELIVERY_REGION, ApiError::getMessageCode);
    }

    @ParameterizedTest
    @DisplayName("Репликация партнера. Ошибка при невалидном externalId")
    @CsvSource({"абвгд", "bcd.,!?*", "white space"})
    @DbUnitDataSet(before = "PartnerReplicationController.DbsPartnerReplication.before.csv")
    public void replicationPartnerShouldThrowIncorrectExternalIdTests(String externalId) {
        final ReplicatePartnerRequest partnerRequest = new ReplicatePartnerRequest();
        partnerRequest.setPartnerDonorId(1237L);
        partnerRequest.setRegionId(213L);
        partnerRequest.setWarehouseName("test Warehouse name");
        partnerRequest.setPartnerWarehouseId(externalId);
        MbiOpenApiClientResponseException exception = Assertions.assertThrows(MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().replicateDbsPartner(123L, partnerRequest));

        assertThat(exception.getApiError())
                .returns("Incorrect externalId " + externalId + ". Must be alphanumeric + symbols _-\\/",
                        ApiError::getMessage)
                .returns(ApiError.MessageCodeEnum.INCORRECT_EXTERNAL_ID, ApiError::getMessageCode);
    }
}
