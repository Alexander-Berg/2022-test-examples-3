package ru.yandex.market.logistics.management;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.logistics.management.configuration.Profiles;

@ContextConfiguration
@TestPropertySource(
    properties = {
        "dynamic.validation-aspect.enabled=true",
        "dynamic.validation-aspect.prevent-commit=true",
        "lms.strategy.partner.get-pickup-holidays-with-new-schema-enabled=true"
    }
)
@ActiveProfiles(Profiles.INTEGRATION_TEST_VALIDATION)
public class AbstractContextualAspectValidationTest extends AbstractContextualTest {
}
