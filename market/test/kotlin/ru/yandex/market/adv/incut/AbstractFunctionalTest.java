package ru.yandex.market.adv.incut;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class
        }
)
@TestPropertySource("classpath:/test.properties")
public abstract class AbstractFunctionalTest extends JupiterDbUnitTest {

    @LocalServerPort
    protected int serverPort;

    protected String baseUrl() {
        return "http://localhost:" + serverPort;
    }

    protected String getStringResource(String resource) throws IOException {
        String resName = getClass().getSimpleName() + "/" + resource;
        return new String(
                getClass().getResourceAsStream(resName).readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    protected File getFile(String resource) {
        String resName = getClass().getSimpleName() + "/" + resource;
        return new File(getClass().getResource(resName).getFile());
    }

}

