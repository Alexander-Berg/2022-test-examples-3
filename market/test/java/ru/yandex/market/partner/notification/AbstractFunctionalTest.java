package ru.yandex.market.partner.notification;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.guava.ForgetfulSuppliersInitializer;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.notification.telegram.bot.service.TelegramBotTransportService;
import ru.yandex.market.partner.notification.config.TestTVMConfig;
import ru.yandex.market.partner.notification.config.TestableClockConfig;
import ru.yandex.market.partner.notification.service.email.EmailService;
import ru.yandex.market.partner.notification.service.providers.address.IsSendingSettingsExperimentFlag;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = {
                SpringApplicationConfig.class,
                TestTVMConfig.class,
                TestableClockConfig.class
        }
)
@SpringJUnitConfig(
        initializers = ForgetfulSuppliersInitializer.class
)
@TestExecutionListeners(
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
        listeners = {
                DbUnitTestExecutionListener.class
        }
)
@ActiveProfiles(profiles = {"functionalTest"})
@TestPropertySource({
        "classpath:test_properties/functional-test.properties",
        "classpath:test_properties/postgres_test.properties"
})
@DbUnitDataSet(
        nonTruncatedTables = {
                "notification.notification_type",
                "notification.notification_template",
                "notification.mustache_notification_template",
                "notification.notification_theme",
                "notification.type_template_transport",
                "notification.nn_alias"
        }
)
public abstract class AbstractFunctionalTest {

    @MockBean
    EmailService emailService;

    @MockBean
    TelegramBotTransportService telegramBotTransportService;

    @MockBean
    protected MbiOpenApiClient mbiOpenApiClient;

    @MockBean
    IsSendingSettingsExperimentFlag isSendingSettingsExperimentFlag;
}

