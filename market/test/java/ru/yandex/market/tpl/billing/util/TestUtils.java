package ru.yandex.market.tpl.billing.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.transaction.support.TransactionCallback;

import static java.lang.ClassLoader.getSystemResourceAsStream;

@UtilityClass
public class TestUtils {

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public Object doInTransaction(InvocationOnMock invocation) {
        return ((TransactionCallback<Object>) invocation.getArguments()[0]).doInTransaction(null);
    }

    @Nonnull
    public static String extractFileContent(String relativePath) {
        try (InputStream inputStream = inputStreamFromResource(relativePath)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error during reading from file " + relativePath, e);
        }
    }

    @Nonnull
    public static InputStream inputStreamFromResource(String relativePath) {
        return Objects.requireNonNull(getSystemResourceAsStream(relativePath));
    }
}
