package ru.yandex.direct.core.testing.architecture;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.runner.RunWith;
import org.springframework.stereotype.Component;

import ru.yandex.direct.multitype.typesupport.TypeSupport;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(
        packages = "ru.yandex.direct.core",
        importOptions = {Predefined.IgnoreTestClasses.class})
@SuppressWarnings("checkstyle:VisibilityModifier")
public class CoreArchitectureTests extends ArchitectureTests {

    @ArchTest
    public static ArchRule typeSupportShouldHaveSpringAnnotation =
            ArchRuleDefinition.classes()
                    .that().implement(TypeSupport.class)
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .should(ArchConditions.beMetaAnnotatedWith(Component.class))
                    .because("descendants of TypeSupport should be annotated with Spring annotation like @Component")
                    .allowEmptyShould(true);

}
