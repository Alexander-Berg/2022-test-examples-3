package ru.yandex.market.logistics.iris.controller.testing;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("testing")
public class MeasurementAuditControllerTest extends AbstractContextualTest {

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/audit/testing/1.xml")
    public void getMeasurementAudit() throws Exception {
        httpOperationWithResult(
                get("/measurement-audit")
                        .param("partner_id", "1")
                        .param("partner_sku", "sku1"),
                status().isOk(),
                content().json(extractFileContent("fixtures/controller/response/measurement-audit/get-audit.json"))
        );
    }

}
