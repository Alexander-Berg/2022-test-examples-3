package ru.yandex.market.application.properties.supplier;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.application.properties.source.ConfigDirectoriesSource;
import ru.yandex.market.application.properties.source.ConfigDirectoriesSourceImpl;
import ru.yandex.market.application.properties.supplier.implementation.ClassPathConfigDirectoriesSourceSupplier;
import ru.yandex.market.application.properties.supplier.implementation.EnvironmentConfigDirectoriesSourceSupplier;
import ru.yandex.market.application.properties.supplier.implementation.YaSettingsConfigDirectoriesSourceSupplier;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ConfigDirectoriesSourceSupplierChainTest {


    private static final String TEST_DIR = "testDir";
    private ClassPathConfigDirectoriesSourceSupplier classPathSupplierMock;
    private EnvironmentConfigDirectoriesSourceSupplier environmentSupplierMock;
    private YaSettingsConfigDirectoriesSourceSupplier yaSettingsSupplierMock;

    @Test
    public void verifyChainInvocations_StopWhenClassPathSourceFound() {
        ConfigDirectoriesSourceSupplierChain chain = buildChain();
        setupReturnResult(classPathSupplierMock);
        checkReturnedSourceSource(chain);
        verify(classPathSupplierMock, times(1)).getSource();
        verify(environmentSupplierMock, never()).getSource();
        verify(yaSettingsSupplierMock, never()).getSource();
    }

    @Test
    public void verifyChainInvocations_StopWhenEnvironmentSourceFound() {
        ConfigDirectoriesSourceSupplierChain chain = buildChain();

        doCallRealMethod().when(classPathSupplierMock).getSource();
        setupReturnResult(environmentSupplierMock);

        checkReturnedSourceSource(chain);

        verify(classPathSupplierMock, times(1)).getSource();
        verify(environmentSupplierMock, times(1)).getSource();
        verify(yaSettingsSupplierMock, never()).getSource();
    }

    @Test
    public void verifyChainInvocations_StopWhenYaSettingsFound() {
        ConfigDirectoriesSourceSupplierChain chain = buildChain();

        doCallRealMethod().when(classPathSupplierMock).getSource();
        doCallRealMethod().when(environmentSupplierMock).getSource();
        setupReturnResult(yaSettingsSupplierMock);

        checkReturnedSourceSource(chain);

        verify(classPathSupplierMock, times(1)).getSource();
        verify(environmentSupplierMock, times(1)).getSource();
        verify(yaSettingsSupplierMock, times(1)).getSource();
    }


    private void setupReturnResult(ConfigDirectoriesSourceSupplier supplier) {
        ConfigDirectoriesSource testSource = ConfigDirectoriesSourceImpl.fromSingleDir(TEST_DIR);
        Mockito.when(supplier.getSource()).thenReturn(testSource);
    }

    private void checkReturnedSourceSource(ConfigDirectoriesSourceSupplierChain chain) {
        ConfigDirectoriesSource source = chain.getSource();
        assertEquals(1, source.getDirectoriesPaths().size());
        assertEquals(TEST_DIR, Lists.newLinkedList(source.getDirectoriesPaths()).remove());
    }

    private ConfigDirectoriesSourceSupplierChain buildChain() {

        classPathSupplierMock = Mockito.mock(
                ClassPathConfigDirectoriesSourceSupplier.class
        );
        environmentSupplierMock = Mockito.mock(
                EnvironmentConfigDirectoriesSourceSupplier.class
        );
        yaSettingsSupplierMock = Mockito.mock(
                YaSettingsConfigDirectoriesSourceSupplier.class
        );
        return ConfigDirectoriesSourceSupplierChain
                .first(classPathSupplierMock)
                .addNext(environmentSupplierMock)
                .addNext(yaSettingsSupplierMock)
                .create();
    }

}
