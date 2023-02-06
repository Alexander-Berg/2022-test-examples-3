package ru.yandex.market.common.mds.s3.client.test;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.pushtorefresh.private_constructor_checker.PrivateConstructorChecker;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.apache.commons.io.IOUtils;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator.DELIMITER_FOLDER;

/**
 * Утилитный класс для unit-тестов.
 *
 * @author Vladislav Bauer
 */
public final class TestUtils {

    public static final String TEST_DATA = "test data " + RandUtils.randomText();


    private TestUtils() {
        throw new UnsupportedOperationException();
    }


    public static void checkConstructor(@Nonnull final Class<?> utilsClass) {
        PrivateConstructorChecker
            .forClass(utilsClass)
            .expectedTypeOfException(UnsupportedOperationException.class)
            .check();

        assertThat(Modifier.isFinal(utilsClass.getModifiers()), equalTo(true));
    }

    public static void checkProvider(@Nonnull final ContentProvider provider, @Nonnull final String expectedData) {
        try (InputStream inputStream = provider.getInputStream()) {
            assertThat(inputStream, notNullValue());

            final String data = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            assertThat(data, equalTo(expectedData));
        } catch (final Exception ex) {
            throw new RuntimeException("Could not provide data", ex);
        }
    }

    public static void checkEqualsAndHashCodeContract(@Nonnull final Class<?> pojoClass) {
        EqualsVerifier.forClass(pojoClass)
            .suppress(
                Warning.STRICT_INHERITANCE,
                Warning.NONFINAL_FIELDS,
                Warning.ALL_FIELDS_SHOULD_BE_USED
            )
            .verify();
    }

    public static String todaysRandomFolder() {
        return ZonedDateTime.now().toLocalDate().toString() + DELIMITER_FOLDER + UUID.randomUUID();
    }

}
