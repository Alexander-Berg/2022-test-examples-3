package ru.yandex.market.global.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.global.checkout.config.ExternalConfig;
import ru.yandex.market.global.checkout.config.InternalConfig;
import ru.yandex.market.global.checkout.config.TestServicesConfig;
import ru.yandex.market.global.checkout.util.TestDataService;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                InternalConfig.class,
                SpringApplicationConfig.class,
                TestServicesConfig.class,
                ExternalConfig.class,
        }
)
@TestPropertySource({
        "file:src/main/properties.d/00_tvm.properties",
        "file:src/main/properties.d/00_application.properties",
        "file:src/main/properties.d/local/00_application.properties",
        "file:src/main/properties.d/local/99_local-application.properties",
})
@ActiveProfiles({"local", "development"})
public abstract class BaseLocalTest {
    @Autowired
    protected TestDataService testDataService;

    protected TestDataService.TestData testData;

    @BeforeEach
    public final void prepareTestData() {
        testDataService.cleanTestData();
        testData = testDataService.saveTestData();
    }
}
