package ru.yandex.market.fintech.banksint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;
import ru.yandex.market.fintech.banksint.config.EmbeddedPostgresConfig;
import ru.yandex.market.fintech.banksint.config.LiquibaseConfig;
import ru.yandex.market.fintech.banksint.config.SpringApplicationConfig;
import ru.yandex.market.fintech.banksint.config.TestConfig;
import ru.yandex.market.fintech.banksint.mybatis.ScoringDataMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles(profiles = {"functionalTest"})
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        MockitoTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class
        })
@TestPropertySource(locations = {"classpath:functional-test.properties"})
@Import({
        TestConfig.class,
        EmbeddedPostgresConfig.class,
        LiquibaseConfig.class,
        PropertyPlaceholderConfigurer.class
})
public abstract class FunctionalTest extends JupiterDbUnitTest {
    @Autowired
    protected TestRestTemplate testRestTemplate;

    @Autowired
    protected ScoringDataMapper scoringDataMapper;

    @Autowired
    protected Clock clock;

    protected String readClasspathFile(String fileName) {
        try (var is = this.getClass().getResourceAsStream(fileName)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void assertStatusCode(HttpStatus expectedCode, ResponseEntity<String> response) {
        assertEquals(
                expectedCode,
                response.getStatusCode(),
                () -> "Server response: " + response.getBody()
        );
    }
}

