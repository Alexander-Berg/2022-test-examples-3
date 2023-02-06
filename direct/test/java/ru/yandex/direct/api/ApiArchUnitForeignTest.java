package ru.yandex.direct.api;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchUnitRunner;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.testing.architecture.ArchitectureTests;
import ru.yandex.direct.core.testing.architecture.Predefined;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(
        packages = "ru.yandex.direct.api",
        importOptions = {Predefined.IgnoreTestClasses.class})
public class ApiArchUnitForeignTest extends ArchitectureTests {
}
