package ru.yandex.market.ff.service;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RequestType;

public class ConcreteEnvironmentParamServiceTest extends IntegrationTest {

    @Autowired
    private ConcreteEnvironmentParamService concreteEnvironmentParamService;

    @Test
    @DatabaseSetup("classpath:service/environment-param/document-types-for-update/setup.xml")
    void documentTypesForUpdateTest() {

        Set<DocumentType> documentTypes = concreteEnvironmentParamService.documentTypesForUpdate();
        assertions.assertThat(documentTypes)
                .contains(
                        DocumentType.ACT_OF_RECEPTION_TRANSFER,
                        DocumentType.PALLET_LABEL,
                        DocumentType.DRIVER_BOOKLET,
                        DocumentType.DRIVER_BOOKLET_OLD);
    }

    @Test
    @DatabaseSetup("classpath:service/environment-param/random-params-filled-setup.xml")
    void getRandomParameters() {
        Set<RegistryUnitIdType> types =
                concreteEnvironmentParamService.getRegistryUnitIdTypesSupportedForReturns();
        assertions.assertThat(types)
                .contains(
                        RegistryUnitIdType.BOX_ID,
                        RegistryUnitIdType.ORDER_ID,
                        RegistryUnitIdType.PALLET_ID);

        Set<RequestType> requestTypes =
                concreteEnvironmentParamService.getRequestDailyReportSupplyTypes();
        assertions.assertThat(requestTypes)
                .contains(
                        RequestType.SUPPLY, RequestType.X_DOC_PARTNER_SUPPLY_TO_FF);
    }

}
