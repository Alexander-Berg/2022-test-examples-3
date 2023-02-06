package ru.yandex.market.loyalty.admin.utils;

import com.google.common.collect.ImmutableSet;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.junit.Test;
import org.springframework.scheduling.annotation.Scheduled;

import ru.yandex.market.loyalty.admin.controller.AdminTrustController;
import ru.yandex.market.loyalty.admin.controller.DocumentationController;
import ru.yandex.market.loyalty.admin.controller.ExpectationController;
import ru.yandex.market.loyalty.admin.controller.ProfilingController;
import ru.yandex.market.loyalty.admin.controller.Promo3pController;
import ru.yandex.market.loyalty.admin.controller.SecretSalesController;
import ru.yandex.market.loyalty.admin.controller.TriggerController;
import ru.yandex.market.loyalty.admin.event.promostorage.PromoStorageMessagesListener;
import ru.yandex.market.loyalty.admin.security.AccessController;
import ru.yandex.market.loyalty.admin.service.bunch.export.YtExporter;
import ru.yandex.market.loyalty.admin.tms.ArchivationProcessor;
import ru.yandex.market.loyalty.admin.tms.BlacklistUpdateProcessor;
import ru.yandex.market.loyalty.admin.tms.CouponNotificationProcessor;
import ru.yandex.market.loyalty.admin.tms.LogbrokerPromoChangesWatcherProcessor;
import ru.yandex.market.loyalty.admin.tms.PromoStorageUpdateProcessor;
import ru.yandex.market.loyalty.admin.tms.StaffUsersUpdateProcessor;
import ru.yandex.market.loyalty.core.config.DatabaseUsage;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.monitoring.beans.JugglerEventsPushExecutor;
import ru.yandex.market.loyalty.test.SourceScanner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.checkAllDatabaseUsageTested;

public class DatabaseUsageCoverageTest {
    @Test
    public void checkAllDatabaseUsageTestedByAnyTest() {
        checkAllDatabaseUsageTested("ru.yandex.market.loyalty.admin",
                DocumentationController.class, ProfilingController.class, AccessController.class,//infrastructure
                CouponNotificationProcessor.class, //tested in core and back
                BlacklistUpdateProcessor.class, //tested by parts
                ExpectationController.class,
                YtExporter.class,
                TriggerController.class, //так исторически сложилось
                JugglerEventsPushExecutor.class,
                ArchivationProcessor.class,
                StaffUsersUpdateProcessor.class, // Tested as service
                AdminTrustController.class,
                PromoStorageUpdateProcessor.class,
                LogbrokerPromoChangesWatcherProcessor.class,
                SecretSalesController.class,
                Promo3pController.class,
                PromoStorageMessagesListener.class //A functional tested in PromoStoragePromocodeImporterTest
        );
    }

    @Test
    public void checkAllScheduledAnnotationsHasDatabaseUsage() {
        List<Method> methodsWithoutDatabaseUsage = SourceScanner.findSpringBeans("ru.yandex.market.loyalty")
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .filter(method -> method.getDeclaredAnnotationsByType(Scheduled.class).length != 0)
                        .filter(method -> method.getDeclaredAnnotationsByType(SchedulerLock.class).length == 0)
                        .filter(m -> m.getDeclaredAnnotationsByType(DatabaseUsage.class).length == 0)
                )
                .collect(Collectors.toList());

        assertThat(methodsWithoutDatabaseUsage, is(empty()));
    }


    @Test
    public void checkAllRequestMethodsHasDatabaseUsage() {
        Set<Class<?>> exclusions = ImmutableSet.of(SecretSalesController.class);
        List<Method> methods = SourceScanner.requestMethods("ru.yandex.market.loyalty.admin")
                .filter(m -> m.getDeclaredAnnotationsByType(DatabaseUsage.class).length == 0).filter(m -> exclusions.contains(m.getClass()))
                .collect(Collectors.toList());

        assertThat("Each web request method must have @DatabaseUsage", methods, is(empty()));
    }

    @Test
    public void checkAllSchedulerLockMethodsUseOnlyMethodsWithDatabaseUsage() {
        Set<Class<?>> exclusions = ImmutableSet.of(Clock.class, ConfigurationService.class,
                SecretSalesController.class);

        List<Field> dependenciesWithoutDatabaseUsage = SourceScanner.findSpringBeans("ru.yandex.market.loyalty.admin")
                .filter(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .anyMatch(method -> method.getDeclaredAnnotationsByType(SchedulerLock.class).length != 0)
                )
                .flatMap(DatabaseUsageCoverageTest::dependenciesWithoutDatabaseUsage)
                .filter(f -> !exclusions.contains(f.getType()))
                .collect(Collectors.toList());

        assertThat(dependenciesWithoutDatabaseUsage, is(empty()));
    }

    private static Stream<Field> dependenciesWithoutDatabaseUsage(Class<?> bean) {
        return Arrays.stream(bean.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> !allPublicMethodsHasDatabaseUsage(resolveType(field)));
    }

    private static Class<?> resolveType(Field field) {
        if (List.class.isAssignableFrom(field.getType())) {
            Type genericArgument = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (genericArgument instanceof Class<?>) {
                return (Class<?>) genericArgument;
            } else if (genericArgument instanceof WildcardType) {
                return (Class<?>) ((WildcardType) genericArgument).getUpperBounds()[0];
            } else {
                throw new UnsupportedOperationException();
            }
        }
        return field.getType();
    }

    private static boolean allPublicMethodsHasDatabaseUsage(Class<?> type) {
        return Stream.concat(
                Arrays.stream(type.getDeclaredMethods()),
                Arrays.stream(type.getMethods())
        )
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.getDeclaringClass() != Object.class)
                .allMatch(m -> m.getDeclaredAnnotationsByType(DatabaseUsage.class).length != 0);
    }
}
