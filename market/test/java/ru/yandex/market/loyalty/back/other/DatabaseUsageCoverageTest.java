package ru.yandex.market.loyalty.back.other;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.loyalty.back.controller.CertificatesController;
import ru.yandex.market.loyalty.back.controller.DeliveryWelcomeCoinV2Controller;
import ru.yandex.market.loyalty.back.controller.LoyaltyProgramsController;
import ru.yandex.market.loyalty.back.controller.NotificationsController;
import ru.yandex.market.loyalty.back.controller.PumpkinController;
import ru.yandex.market.loyalty.back.controller.SubscriptionsController;
import ru.yandex.market.loyalty.back.controller.Top500Controller;
import ru.yandex.market.loyalty.core.config.DatabaseUsage;
import ru.yandex.market.loyalty.test.SourceScanner;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.checkAllDatabaseUsageTested;

public class DatabaseUsageCoverageTest {
    @Test
    public void checkAllDatabaseUsageTestedByAnyTest() {
        checkAllDatabaseUsageTested("ru.yandex.market.loyalty.back", SubscriptionsController.class,
                CertificatesController.class, Top500Controller.class, NotificationsController.class,
                PumpkinController.class, NotificationsController.class, LoyaltyProgramsController.class
        );
    }

    @Test
    public void checkAllRequestMethodsHasDatabaseUsage() {
        Set<Class<?>> exclusions = ImmutableSet.of(LoyaltyProgramsController.class);
        List<Method> methods = SourceScanner.requestMethods("ru.yandex.market.loyalty.back")
                .filter(m -> m.getDeclaredAnnotationsByType(DatabaseUsage.class).length == 0).filter(m -> exclusions.contains(m))
                .collect(Collectors.toList());

        assertThat("Each web request method must have @DatabaseUsage", methods, is(empty()));
    }

    @Test
    public void checkAllScheduledAnnotationsHasDatabaseUsage() {
        List<Method> methodsWithoutDatabaseUsage = SourceScanner.findSpringBeans("ru.yandex.market.loyalty")
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .filter(method -> method.getDeclaredAnnotationsByType(Scheduled.class).length != 0)
                        .filter(m -> m.getDeclaredAnnotationsByType(DatabaseUsage.class).length == 0)
                )
                .collect(Collectors.toList());

        assertThat(methodsWithoutDatabaseUsage, is(empty()));
    }

    @Test
    public void checkAllEventListenersHasDatabaseUsage() {
        List<Method> methodsWithoutDatabaseUsage = SourceScanner.findSpringBeans("ru.yandex.market.loyalty")
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .filter(method -> method.getDeclaredAnnotationsByType(EventListener.class).length != 0)
                        .filter(m -> m.getDeclaredAnnotationsByType(DatabaseUsage.class).length == 0)
                )
                .collect(Collectors.toList());

        assertThat(methodsWithoutDatabaseUsage, is(empty()));
    }

    @Test
    public void checkAllMethodsWithDatabaseUsageHasNotTransactional() {
        List<Method> methods = SourceScanner.findSpringBeans("ru.yandex.market.loyalty.admin")
                .flatMap(aClass -> Arrays.stream(aClass.getDeclaredMethods()))
                .filter(m -> m.getDeclaredAnnotationsByType(DatabaseUsage.class).length != 0)
                .filter(m -> m.getDeclaredAnnotationsByType(Transactional.class).length != 0)
                .collect(Collectors.toList());

        assertThat("Methods with @DatabaseUsage may not have @Transactional (dual order of calls)", methods, is(empty()));
    }
}
