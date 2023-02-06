package ru.yandex.market.mbi.tariffs.service.dump;

import java.util.Arrays;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.mbi.tariffs.model.DumpFormatEnum;

/**
 * Тесты для {@link DumpService}
 */
@ParametersAreNonnullByDefault
public class DumpServiceTest extends FunctionalTest {

    @Autowired
    private DumpService dumpService;

    @Test
    void testAllDumpFormatsHaveGenerator() {
        Assertions.assertArrayEquals(
                DumpFormatEnum.values(),
                Arrays.stream(DumpFormatEnum.values())
                .filter(dumpService::isFormatSupported)
                .toArray(DumpFormatEnum[]::new)
        );
    }
}
