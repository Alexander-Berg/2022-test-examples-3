package ru.yandex.direct.core.entity;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reflections.Reflections;

import ru.yandex.direct.core.entity.banner.repository.type.BannerRepositoryTypeSupport;
import ru.yandex.direct.core.entity.banner.type.turboapp.BannerWithTurboAppUpdateOperationTypeSupport;
import ru.yandex.direct.core.entity.campaign.repository.type.CampaignRepositoryTypeSupport;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignWithPackageStrategyAddOperationSupport;
import ru.yandex.direct.multitype.service.type.add.AddOperationTypeSupport;
import ru.yandex.direct.multitype.service.type.update.UpdateOperationTypeSupport;
import ru.yandex.direct.multitype.service.validation.type.add.AddValidationTypeSupport;
import ru.yandex.direct.multitype.service.validation.type.update.UpdateValidationTypeSupport;
import ru.yandex.direct.multitype.typesupport.TypeSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что классы, реализующие интерфейсы,
 * перечисленные в {@link ComponentAnnotationTypeSupportTest#interfaces()},
 * размечены аннотацией {@link org.springframework.stereotype.Component}
 * Без этой аннотации бины не инджектятся и на самом деле не работают
 */
@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class ComponentAnnotationTypeSupportTest {

    /**
     * Список классов, которые нужно исключить из проверки
     */
    private static final Set<Class<? extends TypeSupport<?>>> EXCLUDED_CLASSES = Set.of(
            BannerWithTurboAppUpdateOperationTypeSupport.class,
            CampaignWithPackageStrategyAddOperationSupport.class
    );

    private static Object[] interfaces() {
        return new Object[][]{
                {AddValidationTypeSupport.class},
                {UpdateValidationTypeSupport.class},
                {AddOperationTypeSupport.class},
                {UpdateOperationTypeSupport.class},
                {BannerRepositoryTypeSupport.class},
                {CampaignRepositoryTypeSupport.class},
        };
    }

    @Test
    @Parameters(method = "interfaces")
    @TestCaseName("Classes implement {0}")
    public void checkAnnotationForTypeSupport(Class<?> clazz) {
        Reflections reflections = new Reflections("ru.yandex.direct.core.entity");
        for (var cls : reflections.getSubTypesOf(clazz)) {
            if (cls.getInterfaces().length != 0 || Modifier.isAbstract(cls.getModifiers())) {
                // Пропускаем интерфейсы и абстрактные классы
            } else if (EXCLUDED_CLASSES.contains(cls)) {
                // Пропускаем классы, которые нужно исключить из проверки
            } else {
                List<String> annotations = StreamEx.of(cls.getAnnotations())
                        .map(t -> t.annotationType().getSimpleName())
                        .toList();
                assertThat(annotations)
                        .as(cls.getSimpleName() + " is not annotated by @Component")
                        .contains(org.springframework.stereotype.Component.class.getSimpleName());
            }
        }
    }
}
