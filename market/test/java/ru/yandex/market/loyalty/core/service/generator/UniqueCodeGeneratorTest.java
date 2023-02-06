package ru.yandex.market.loyalty.core.service.generator;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.config.Default;
import ru.yandex.market.loyalty.core.dao.coupon.CodeDao;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.monitoring.PushMonitor;

import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by maratik.
 */
public class UniqueCodeGeneratorTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private CodeDao codeDao;
    @Autowired
    @Default
    private PushMonitor complicatedMonitoring;

    @Test
    public void testUniqueCodeGenerator() throws GeneratorException {
        UniqueCodeGenerator codeGenerator1 = new UniqueCodeGenerator(
                codeDao,
                new CallbackCodeGenerator(() -> Optional.of("value1")),
                complicatedMonitoring
        );
        Optional<String> generated = codeGenerator1.generate();
        assertTrue(generated.isPresent());
        assertEquals("value1", generated.get());

        class IncSupplier implements Supplier<Optional<String>> {
            private int id = 0;

            @Override
            public Optional<String> get() {
                id++;
                assertFalse("Should be stopped at the previous iteration", id >= 3);
                return Optional.of(String.format("value%d", id));
            }
        }

        UniqueCodeGenerator codeGenerator2 = new UniqueCodeGenerator(
                codeDao,
                new CallbackCodeGenerator(new IncSupplier()),
                complicatedMonitoring
        );
        generated = codeGenerator2.generate();
        assertTrue(generated.isPresent());
        assertEquals("value2", generated.get());
    }
}
