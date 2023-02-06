package ru.yandex.market.sqb.service.config.reader.strategy;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.sqb.exception.SqbConfigurationException;

/**
 * Unit-тесты для {@link FolderScanningStrategy}.
 *
 * @author Kirill Lakhtin (klaktin@yandex-team.ru)
 */
class FolderScanningStrategyTest {

    private FolderScanningStrategy instance;

    private File file;

    @BeforeEach
    void init() {
        file = Mockito.mock(File.class);
        Mockito.when(file.exists()).thenReturn(true);
        Mockito.when(file.isDirectory()).thenReturn(true);
        instance = new FolderScanningStrategy(file);
    }

    @Test
    void testFolderScanningStrategyFileNotExists() {
        Mockito.when(file.exists()).thenReturn(false);
        Assertions.assertThrows(SqbConfigurationException.class, () -> instance = new FolderScanningStrategy(file));
    }

    @Test
    void testFolderScanningStrategyFileIsNotAFolder() {
        Mockito.when(file.exists()).thenReturn(true);
        Mockito.when(file.isDirectory()).thenReturn(false);
        Assertions.assertThrows(SqbConfigurationException.class, () -> instance = new FolderScanningStrategy(file));
    }

    @Test
    void testConfigurationReadersOK() {
        File innerFile = new File("test");
        Mockito.when(file.listFiles(Mockito.any(FileFilter.class))).thenReturn(new File[]{innerFile});

        Collection<Supplier<String>> result = instance.getConfigurationReaders();

        Assertions.assertEquals(1, result.size());
    }

}
