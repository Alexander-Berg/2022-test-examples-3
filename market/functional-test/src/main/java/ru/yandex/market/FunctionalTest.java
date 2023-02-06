package ru.yandex.market;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;

/**
 * Базовый класс для написания функциональных тестов.
 */
@DbUnitDataSet(
        before = "/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@ActiveProfiles({"functionalTest", "development"})
public abstract class FunctionalTest extends JupiterDbUnitTest {
    protected InputStreamResource getInputStreamResource(String filename) {
        return new InputStreamResource(getClass().getResourceAsStream(
                getClass().getSimpleName() + filename));
    }

    protected String getStringResource(String name) {
        Class<? extends FunctionalTest> cls = getClass();
        return readFromFile(cls, cls.getSimpleName() + name);
    }

    private static String readFromFile(Class<?> contextClass, String jsonFileName) {
        try (InputStream in = contextClass.getResourceAsStream(jsonFileName)) {
            final String resource = IOUtils.toString(in, StandardCharsets.UTF_8);
            return StringUtils.trimToNull(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
