package ru.yandex.market.marketId;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.marketId.config.SolomonTestJvmConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {Marketid.class, SolomonTestJvmConfig.class}
)
@ActiveProfiles(profiles = {"functionalTest", "development"})
@DbUnitDataSet(nonTruncatedTables = {
        "market_id.legal_info_type",
        "market_id.legal_info_status",
        "market_id.partner_type",
}, nonRestartedSequences = "market_id.market_id_seq")
@TestPropertySource("classpath:functional-test.properties")
public abstract class FunctionalTest extends JupiterDbUnitTest {

    private static final String BASE_URL = "http://localhost:";

    @LocalServerPort
    private int port;

    protected String baseUrl() {
        return BASE_URL + port;
    }

}
