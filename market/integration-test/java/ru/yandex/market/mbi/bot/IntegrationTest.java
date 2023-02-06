package ru.yandex.market.mbi.bot;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = {IntegrationTestConfig.class})
@ExtendWith(SpringExtension.class)
@ActiveProfiles("integrationTest")
@TestPropertySource(locations = "classpath:integration-test.properties")
public abstract class IntegrationTest {


}
