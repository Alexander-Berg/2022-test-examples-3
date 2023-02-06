package ru.yandex.market.wms.common.spring.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.DatabaseSchema;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Transporter;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class TransporterServiceTest extends IntegrationTest {

    @Autowired
    private TransporterService transporterService;

    @Test
    @DatabaseSetup(value = "/db/service/transporter/create-putaway-and-transporter/before.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/service/transporter/create-putaway-and-transporter/after-create.xml",
            assertionMode = NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/service/transporter/create-putaway-and-transporter/before.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/db/service/transporter/create-putaway-and-transporter/after-create.xml",
            assertionMode = NON_STRICT_UNORDERED, connection = "enterpriseConnection")
    public void createPutawayAndTransporter() {
        Transporter transporter1 = Transporter.transporterBuilder()
                .putawayZone("SECTOR A")
                .addwho("AD_TEST")
                .editwho("AD_TEST1")
                .enabled(true)
                .transporterId("FFA-11025")
                .build();

        transporterService.create(transporter1, DatabaseSchema.WMWHSE1);
        transporterService.create(transporter1, DatabaseSchema.ENTERPRISE);

        Transporter transporter2 = Transporter.transporterBuilder()
                .putawayZone("SECTOR B")
                .addwho("AD_TEST")
                .editwho("AD_TEST2")
                .enabled(false)
                .transporterId("FEU-2000")
                .build();

        transporterService.create(transporter2, DatabaseSchema.WMWHSE1);
        transporterService.create(transporter2, DatabaseSchema.ENTERPRISE);
    }
}
