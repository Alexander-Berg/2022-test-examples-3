package ru.yandex.market.mbo.test;

import org.junit.rules.ExternalResource;

import ru.yandex.market.mbo.image.ImageProcessingServiceTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Helps injecting external resources into test class.
 * Make sure there aren't influence of different tests to injected content.
 * See {@link InjectResourcesTest#testNoInfluence1()} and {@link InjectResourcesTest#testNoInfluence2()}
 * Usage see {@link InjectResourcesTest}
 *
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 09.02.2018
 */
public class InjectResources extends ExternalResource {
    private static final int BUFFER_SIZE = 1024;
    private final Object testInstance;
    private List<Field> injectedFields = new ArrayList<>();

    /*
     * Passing instance is agly, but unfortunately TestRule doesn't get it.
     * Follow issue https://github.com/junit-team/junit4/issues/351
     */
    public InjectResources(Object testInstance) {
        this.testInstance = testInstance;
    }

    private static byte[] readFile(String fileName) {
        try (InputStream inputStream = ImageProcessingServiceTest.class.getResourceAsStream(fileName)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) > -1) {
                bos.write(buffer, 0, read);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void before() throws Throwable {
        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(InjectResource.class)) {
                inject(field);
            }
        }
    }

    private void inject(Field field) throws Exception {
        InjectResource annotation = field.getAnnotation(InjectResource.class);
        Object value;

        if (field.getType() == byte[].class) {
            value = readFile(annotation.value());
        } else if (field.getType() == String.class) {
            value = new String(readFile(annotation.value()));
        } else {
            throw new Exception("field " + field.getName() + " must be type byte[] or String, but " + field.getType());
        }

        field.setAccessible(true);
        field.set(testInstance, value);
        injectedFields.add(field);
    }

    @Override
    protected void after() {
        for (Field field : injectedFields) {
            try {
                field.set(testInstance, null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
