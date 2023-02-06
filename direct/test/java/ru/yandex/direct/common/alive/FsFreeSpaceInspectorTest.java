package ru.yandex.direct.common.alive;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class FsFreeSpaceInspectorTest {
    private final File inspectedPartition;
    private final boolean expectedHealth;

    public FsFreeSpaceInspectorTest(String path, long totalSpace, long freeSpace, boolean expectedHealth) {
        this.expectedHealth = expectedHealth;
        inspectedPartition = mockFile(path, totalSpace, freeSpace);
    }

    @Test
    public void inspectPartition() throws Exception {
        FsFreeSpaceInspector inspector = new FsFreeSpaceInspector(Collections.singleton(inspectedPartition), 0.1);
        HealthStatus status = inspector.inspectHealth();
        assertThat(status.isHealthy()).isEqualTo(expectedHealth);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> generateData() {
        return Arrays.asList(new Object[][]{
                {"/brokenPartition", 0L, 4096L, false},
                {"/fullPartition", 4096L, 409L, false},
                {"/almostFullPartition", 4096L, 410L, true}
        });
    }

    private File mockFile(String path, long totalSpace, long freeSpace) {
        File result = mock(File.class);
        doReturn(path).when(result).getPath();
        doReturn(freeSpace).when(result).getFreeSpace();
        doReturn(freeSpace).when(result).getUsableSpace();
        doReturn(totalSpace).when(result).getTotalSpace();
        doReturn(true).when(result).exists();
        return result;
    }
}
