package ru.yandex.market.charon;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.reflections.ReflectionUtils;

import ru.yandex.market.charon.config.CharonConfig;
import ru.yandex.market.charon.impl.CharsetSettings;
import ru.yandex.market.charon.impl.StringData;
import ru.yandex.market.charon.impl.destinations.StorageDestination;
import ru.yandex.market.charon.impl.destinations.storage.Storage;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link StorageDestination}.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
@RunWith(MockitoJUnitRunner.class)
public class StorageDestinationTest {

    @SuppressWarnings("unused")
    @Mock
    private NamedHistoryMdsS3Client namedHistoryMdsS3Client;

    @InjectMocks
    @Spy
    private CharonConfig charonConfig;


    @Test
    public void testStorageDestination() throws IOException {
        final Storage storage = Mockito.mock(Storage.class);
        final File file = TempFileUtils.createTempFile();

        final StorageDestination<StringData> destination = new StorageDestination<>();
        destination.setStorage(storage);
        destination.setFile(file.getAbsolutePath());
        destination.setCharsetSettings(new CharsetSettings());

        try {
            TransferSession<StringData> s = destination.newSession();
            s.open();
            s.accept(new StringData("test"));
            s.accept(new StringData("test2"));
            s.close(true);
            Mockito.verify(storage).putFile(Mockito.eq(file.getAbsolutePath()));
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAllStorageDestinations() throws Exception {
        final Set<Method> factoryMethods = ReflectionUtils.getMethods(
                CharonConfig.class,
                (m) -> m != null
                        && Modifier.isPublic(m.getModifiers())
                        && m.getReturnType() == StorageDestination.class
        );

        assertThat(factoryMethods, not(empty()));

        for (final Method factoryMethod : factoryMethods) {
            final StorageDestination storage = (StorageDestination) factoryMethod.invoke(charonConfig);
            final String file = storage.getFile();

            assertThat(file, not(isEmptyOrNullString()));
        }
    }

}
