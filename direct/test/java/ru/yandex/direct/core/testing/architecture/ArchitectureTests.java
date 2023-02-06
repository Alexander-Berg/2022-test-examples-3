package ru.yandex.direct.core.testing.architecture;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.springframework.stereotype.Component;

import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.regions.GeoTree;

import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethod;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;


/**
 * Общие для всех модулей архитектурные правила.
 * <p/>
 * <ul>
 *     <li>Модуле-специфичные тесты стоит добавлять в классах-наследниках.</li>
 *     <li>Исключения можно задавать регулярками в файле {@code archunit_ignore_patterns.txt} в ресурсах</li>
 * </ul>
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
public abstract class ArchitectureTests {

    @ArchTest
    public static ArchRule noGeoTreeFieldsInSpringComponents =
            ArchRuleDefinition.noFields()
                    .that().areDeclaredInClassesThat().areMetaAnnotatedWith(Component.class)
                    .should().haveRawType(GeoTree.class)
                    .because("GeoTree must be obtained in runtime from GeoTreeFactory");

    @ArchTest
    public static ArchRule neverCallFeatureNameGetName =
            ArchRuleDefinition.classes()
                    .should(never(callMethod(FeatureName.class, "name")))
                    .because("you probably want to use FeatureName.getName() instead of Enum.name()");

}
