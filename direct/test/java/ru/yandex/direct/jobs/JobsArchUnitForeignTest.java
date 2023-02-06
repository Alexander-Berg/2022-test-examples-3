package ru.yandex.direct.jobs;

import com.tngtech.archunit.junit.AnalyzeClasses;

import ru.yandex.direct.core.testing.architecture.ArchitectureTests;
import ru.yandex.direct.core.testing.architecture.Predefined;

@AnalyzeClasses(
        packages = "ru.yandex.direct.jobs",
        importOptions = {Predefined.IgnoreTestClasses.class})
public class JobsArchUnitForeignTest extends ArchitectureTests {
}
