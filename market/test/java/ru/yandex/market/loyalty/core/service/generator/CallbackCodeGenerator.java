package ru.yandex.market.loyalty.core.service.generator;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Created by maratik.
 */
public class CallbackCodeGenerator implements CodeGenerator {

    private final Supplier<Optional<String>> generateCalled;

    public CallbackCodeGenerator(Supplier<Optional<String>> generateCalled) {
        this.generateCalled = generateCalled;
    }

    @Override
    public Optional<String> generate() {
        return generateCalled.get();
    }
}
