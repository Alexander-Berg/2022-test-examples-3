package ru.yandex.market.abo.gen.manual;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

public class ByUrlGeneratorTest extends EmptyTest {

    @Autowired
    ByUrlGenerator byUrlGenerator;

    @Test
    public void testByUrlGenerator() throws Exception {
        byUrlGenerator.generate();
    }
}
