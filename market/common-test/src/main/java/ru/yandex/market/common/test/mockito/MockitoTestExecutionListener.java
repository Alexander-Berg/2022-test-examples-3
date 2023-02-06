package ru.yandex.market.common.test.mockito;

import java.lang.reflect.Field;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.mockito.Mock;
import org.mockito.MockingDetails;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ReflectionUtils;

/**
 * {@link TestExecutionListener}, который скидывает настройки моков после каждого теста.
 *
 * @author Vladislav Bauer
 */
@ParametersAreNonnullByDefault
public class MockitoTestExecutionListener extends AbstractTestExecutionListener {

    private static final int CACHE_SIZE = 100;

    private static final Cache<Class<?>, Object[]> CACHE =
            CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();


    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeTestMethod(final TestContext testContext) throws Exception {
        resetMocks(testContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTestMethod(final TestContext testContext) throws Exception {
        resetMocks(testContext);
    }

    private void resetMocks(final TestContext testContext) throws Exception {
        final Class<?> testClass = testContext.getTestClass();
        final Object[] mocks = CACHE.get(testClass, () -> calculateMocks(testContext));
        Mockito.reset(mocks);
    }

    private Object[] calculateMocks(final TestContext testContext) {
        final Class<?> testClass = testContext.getTestClass();
        final Object testInstance = testContext.getTestInstance();

        final List<Field> fields = FieldUtils.getAllFieldsList(testClass);
        return fields.stream()
                .filter(this::isPossibleMock)
                .map(field -> getFieldValue(field, testInstance))
                .filter(this::isMockitoMock)
                .toArray();
    }

    private boolean isMockitoMock(@Nullable final Object object) {
        if (object != null) {
            final MockingDetails mockingDetails = Mockito.mockingDetails(object);
            return mockingDetails.isSpy() || mockingDetails.isMock();
        }
        return false;
    }

    private Object getFieldValue(final Field field, final Object object) {
        try {
            ReflectionUtils.makeAccessible(field);
            return field.get(object);
        } catch (final IllegalAccessException ex) {
            throw new RuntimeException("Could not get value from field " + field, ex);
        }
    }

    private boolean isPossibleMock(final Field field) {
        return field.getAnnotation(Autowired.class) != null
                || field.getAnnotation(Mock.class) != null
                || field.getAnnotation(Spy.class) != null;
    }

}
