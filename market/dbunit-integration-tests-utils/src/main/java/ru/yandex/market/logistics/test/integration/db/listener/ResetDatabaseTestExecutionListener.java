package ru.yandex.market.logistics.test.integration.db.listener;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.logistics.test.integration.db.cleaner.DatabaseCleaner;

@ParametersAreNonnullByDefault
public class ResetDatabaseTestExecutionListener extends AbstractTestExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(ResetDatabaseTestExecutionListener.class);

    @Override
    public void beforeTestMethod(TestContext testContext) {
        boolean methodHasDataset = isAnnotated(testContext.getTestMethod(), DatabaseSetup.class)
            || isAnnotated(testContext.getTestMethod(), CleanDatabase.class);
        boolean classHasDataSet = isAnnotated(testContext.getTestClass(), DatabaseSetup.class)
            || isAnnotated(testContext.getTestClass(), CleanDatabase.class);
        if (methodHasDataset || classHasDataSet) {
            getDatabaseCleaners(testContext).forEach(DatabaseCleaner::clearDatabase);
        }
    }

    private boolean isAnnotated(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass) {
        return CollectionUtils.isNonEmpty(AnnotationUtils.getRepeatableAnnotations(annotatedElement, annotationClass));
    }

    @Nonnull
    private Collection<DatabaseCleaner> getDatabaseCleaners(TestContext testContext) {
        try {
            ApplicationContext applicationContext = testContext.getApplicationContext();

            String[] names = Optional.of(applicationContext)
                .map(context -> context.getBeanNamesForType(DatabaseCleaner.class))
                .orElse(new String[0]);

            return Arrays.stream(names)
                .map(name -> applicationContext.getBean(name, DatabaseCleaner.class))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error occurred during DatabaseCleaner retrieval ", e);

            return Collections.emptyList();
        }
    }

}
