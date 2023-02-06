package ru.yandex.market.pers.qa;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.yandex.market.pers.qa.config.CoreConfig;

/**
 * @author vvolokh
 * 12.12.2018
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    CoreMockConfiguration.class,
    CoreConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource("classpath:/test-application.properties")
public class PersQATest extends PersQACoreTest {
    @Override
    protected void resetMocks() {
        // not required
    }
}
