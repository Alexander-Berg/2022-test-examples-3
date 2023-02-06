package ru.yandex.market.delivery.partnerapimock.api;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.delivery.partnerapimock.PartnerApiMockApplication;

@SpringBootTest(
    classes = {PartnerApiMockApplication.class}
)
@Sql(scripts = "/data/clean-tables.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@AutoConfigureMockMvc
abstract class AbstractIntegrationTest {

}
