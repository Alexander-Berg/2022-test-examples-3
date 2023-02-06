package ru.yandex.market.wms.transportation.client.config;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

@SpringBootTest(classes = TransportationIntegrationTestConfig.class)
@ComponentScan("ru.yandex.market.wms.transportation.client")
@ActiveProfiles(Profiles.TEST)
@AutoConfigureMockMvc
public class IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected SoftAssertions assertions;

    @BeforeEach
    public void setup() {
        assertions = new SoftAssertions();
    }
}
