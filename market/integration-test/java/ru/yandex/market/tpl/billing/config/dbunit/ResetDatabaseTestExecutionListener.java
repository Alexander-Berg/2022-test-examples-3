package ru.yandex.market.tpl.billing.config.dbunit;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.tpl.billing.config.dbunit.cleaner.DatabaseCleaner;

@Slf4j
public class ResetDatabaseTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        boolean methodHasDataset = isAnnotated(testContext.getTestMethod(), DatabaseSetup.class);
        boolean classHasDataSet = isAnnotated(testContext.getTestClass(), DatabaseSetup.class);
        if (methodHasDataset || classHasDataSet) {
            cleanDatabase(testContext);
        }
    }

    private void cleanDatabase(final TestContext testContext) {
        getDatabaseCleaners(testContext).forEach(DatabaseCleaner::clearDatabase);
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

    private boolean isAnnotated(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass) {
        return CollectionUtils.isNonEmpty(AnnotationUtils.getRepeatableAnnotations(annotatedElement, annotationClass));
    }
}
