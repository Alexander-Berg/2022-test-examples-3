package ru.yandex.market.global.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.global.checkout.config.InternalConfig;
import ru.yandex.market.global.checkout.config.TestServicesConfig;
import ru.yandex.market.global.checkout.config.TestsExternalConfig;
import ru.yandex.market.global.checkout.util.TestDataService;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                InternalConfig.class,
                SpringApplicationConfig.class,
                TestServicesConfig.class,
                TestsExternalConfig.class,
        }
)
@TestPropertySource({"classpath:test_properties/test.properties"})
@ActiveProfiles("functionalTest")
public abstract class BaseFunctionalTest {
    @Autowired
    @Deprecated
    protected TestDataService testDataService;

    @Deprecated
    protected TestDataService.TestData testData;

    @BeforeEach
    public final void prepareTestData() {
        testDataService.cleanTestData();
        testData = testDataService.saveTestData();
    }
}
