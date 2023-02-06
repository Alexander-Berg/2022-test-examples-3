package ru.yandex.market.logistics.management.util;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.error.AssertionErrorCreator;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@ParametersAreNonnullByDefault
public class DynamicBuilderJUnitSoftAssertions extends DynamicBuilderSoftAssert implements AfterEachCallback {
    private final AssertionErrorCreator assertionErrorCreator;

    public DynamicBuilderJUnitSoftAssertions(AssertionErrorCreator assertionErrorCreator) {
        this.assertionErrorCreator = assertionErrorCreator;
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        List<Throwable> errors = errorsCollected();
        if (!errors.isEmpty()) {
            assertionErrorCreator.tryThrowingMultipleFailuresError(errors);
        }
    }
}
