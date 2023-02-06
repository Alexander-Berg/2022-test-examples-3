package ru.yandex.market.mbo.mdm.common.service.stuff;

import java.io.IOException;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mboc.common.utils.JavaResourceUtils;

public class StuffServiceBaseTestClass {
    protected final ComplexMonitoring complexMonitoring = new ComplexMonitoring();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected void copyFromResources(String resourceName, String tempName) {
        try {
            JavaResourceUtils.copyFromResources(resourceName, tempFolder.newFile(tempName));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
