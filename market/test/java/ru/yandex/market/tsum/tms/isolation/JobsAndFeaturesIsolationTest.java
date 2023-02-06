package ru.yandex.market.tsum.tms.isolation;

import org.eclipse.jetty.util.MultiMap;
import org.junit.Assert;
import org.junit.Test;
import org.reflections.Reflections;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.tsum.clients.notifications.TelegramChatProvider;
import ru.yandex.market.tsum.config.ClientExtensionsConfig;
import ru.yandex.market.tsum.config.JobIsolationConfig;
import ru.yandex.market.tsum.core.auth.TsumUserDao;
import ru.yandex.market.tsum.isolation.impl.ProjectContextFactoryImpl;
import ru.yandex.market.tsum.pipe.engine.RootContextJobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.common.SourceCodeEntity;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobFeature;
import ru.yandex.market.tsum.pipe.engine.definition.job.RootContextJobFeature;
import ru.yandex.market.tsum.pipe.engine.isolation.model.SecretVersion;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.autowired_job.AutowiredJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.BadInterruptJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.SleepyJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.StuckJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.WaitingForInterruptOnceJob;
import ru.yandex.market.tsum.tms.isolation.model.NamedBean;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 22/11/2018
 */
public class JobsAndFeaturesIsolationTest {
    private Set<Class<? extends SourceCodeEntity>> ignoredJobs = new HashSet<>(
        Arrays.asList(
            AutowiredJob.class,
            SleepyJob.class,
            BadInterruptJob.class,
            WaitingForInterruptOnceJob.class,
            StuckJob.class
        )
    );

    @Test
    public void allEntitiesUsesAllowedBeans() {
        Set<Class<?>> declaredServices = IsolationHelper.loadBeansHierarchy(ClientExtensionsConfig.class)
            .stream().map(NamedBean::getClazz).collect(Collectors.toSet());

        Set<Class<? extends SourceCodeEntity>> entities = getEntities();

        Set<Class<? extends SourceCodeEntity>> usesValueAnnotations = new HashSet<>();

        MultiMap<String> services = new MultiMap<>();
        for (Class<? extends SourceCodeEntity> entity : entities) {
            Set<String> dependencies = fieldsSet(declaredServices, Arrays.stream(entity.getDeclaredFields()),
                this::hasAutowiredAnnotation);

            if (Arrays.stream(entity.getDeclaredFields())
                .map(f -> f.getAnnotation(Value.class))
                .anyMatch(value -> Objects.nonNull(value) && value.value().contains(":"))
            ) {
                usesValueAnnotations.add(entity);
            }

            dependencies.addAll(fieldsSet(declaredServices, Arrays.stream(entity.getFields()), this::hasAutowiredAnnotation));

            dependencies.addAll(
                Arrays.stream(entity.getConstructors())
                    .flatMap(constructor -> Arrays.stream(constructor.getParameters()))
                    .filter(
                        param -> param.getType().isAssignableFrom(Object.class) &&
                            !declaredServices.contains(param.getType())
                    )
                    .map(x -> x.getType().getSimpleName())
                    .collect(Collectors.toSet())
            );

            dependencies.forEach(dep -> services.add(dep, entity.getSimpleName()));
        }

        String report = services.entrySet().stream()
            .sorted(Comparator.comparing(x -> x.getValue().size()))
            .map(e -> e.getKey() + ": " + String.join(", ", e.getValue()))
            .collect(Collectors.joining("\n"));

        Assert.assertTrue(
            String.format("Found entities that use illegal bean:\n%s\n", report),
            report.isEmpty()
        );

        String usesValueAnnotationsReport = usesValueAnnotations.stream()
            .map(Class::getName)
            .collect(Collectors.joining("\n"));

        Assert.assertTrue(
            String.format("Found entities that use illegal value annotations with default value:\n%s\n",
                usesValueAnnotationsReport),
            usesValueAnnotations.isEmpty()
        );
    }

    private Set<String> fieldsSet(Set<Class<?>> declaredServices, Stream<Field> fieldStream, Predicate<Field> fieldPredicate) {
        return fieldStream
            .filter(f -> !declaredServices.contains(f.getType()))
            .filter(fieldPredicate)
            .map(x -> x.getType().getSimpleName())
            .collect(Collectors.toSet());
    }

    private boolean hasAutowiredAnnotation(Field f) {
        return f.getAnnotationsByType(Autowired.class).length > 0;
    }

    private Set<Class<? extends SourceCodeEntity>> getEntities() {
        Set<Class<? extends SourceCodeEntity>> entities = new Reflections("ru.yandex.market.tsum")
            .getSubTypesOf(SourceCodeEntity.class).stream()
            .filter(clazz -> JobFeature.class.isAssignableFrom(clazz) || JobExecutor.class.isAssignableFrom(clazz))
            .filter(this::isNotRelatedToRootContext)
            .filter(entity -> !ignoredJobs.contains(entity))
            .filter(entity -> Modifier.isPublic(entity.getModifiers()))
            .collect(Collectors.toSet());

        Assert.assertTrue(
            "Broken test",
            entities.stream().filter(JobFeature.class::isAssignableFrom).count() > 3
        );

        Assert.assertTrue(
            "Broken test",
            entities.stream().filter(JobExecutor.class::isAssignableFrom).count() > 250
        );

        return entities;
    }

    private boolean isNotRelatedToRootContext(Class<? extends SourceCodeEntity> executor) {
        return !RootContextJobExecutor.class.isAssignableFrom(executor) && !RootContextJobFeature.class.isAssignableFrom(executor);
    }

    @Test
    public void autowireAllEntities() throws IOException {
        NettyHttpClientContext nettyContext = new NettyHttpClientContext(new HttpClientConfig());

        Set<Class<? extends SourceCodeEntity>> jobs = getEntities().stream()
            .filter(x -> !Modifier.isAbstract(x.getModifiers()))
            .collect(Collectors.toSet());

        ArrayList<PropertySource> sources = new ArrayList<>(JobIsolationConfig.jobsPropertySources());
        sources.add(new ResourcePropertySource("classpath:/vault-test-secrets.properties"));
        TelegramChatProvider telegramChatProvider = new TsumUserDao();
        ProjectContextFactoryImpl factory = new ProjectContextFactoryImpl(nettyContext, sources, telegramChatProvider);

        ApplicationContext context = factory.create(SecretVersion.builder().withVersion("").build());

        Map<UUID, String> entitiesIds = new HashMap<>();
        Set<String> errors = new HashSet<>();
        jobs.forEach(requiredType -> {
            try {
                SourceCodeEntity entity = context.getAutowireCapableBeanFactory().createBean(requiredType);

                Assert.assertEquals(
                    String.format("Not persistent id, class: %s", requiredType.getName()),
                    entity.getSourceCodeId(),
                    entity.getSourceCodeId()
                );

                Assert.assertFalse(
                    String.format(
                        "Duplicate id: %s, classes: %s, %s",
                        entity.getSourceCodeId(), entitiesIds.get(entity.getSourceCodeId()), requiredType.getName()
                    ),
                    entitiesIds.containsKey(entity.getSourceCodeId())
                );

                entitiesIds.put(entity.getSourceCodeId(), requiredType.getName());
            } catch (BeanCreationException e) {
                errors.add(String.format("%s: %s", requiredType.getSimpleName(), e.getCause().getMessage()));
            }
        });

        Assert.assertTrue("\n" + errors.stream().sorted().collect(Collectors.joining("\n")), errors.isEmpty());
    }
}
