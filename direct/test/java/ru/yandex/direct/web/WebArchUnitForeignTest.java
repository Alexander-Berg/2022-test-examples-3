package ru.yandex.direct.web;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchUnitRunner;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.testing.architecture.ArchitectureTests;
import ru.yandex.direct.core.testing.architecture.Predefined;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(
        packages = "ru.yandex.direct.web",
        importOptions = {Predefined.IgnoreTestClasses.class})
public class WebArchUnitForeignTest extends ArchitectureTests {
}
