package ru.yandex.test.util;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class FilterJUnitRunner extends BlockJUnit4ClassRunner {
    public FilterJUnitRunner(final Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        List<FrameworkMethod> testMethods = super.computeTestMethods();
        String testMethodName = System.getenv("TEST_METHOD");
        if (testMethodName != null) {
            System.out.println("Looking for methods: " + testMethodName);

            testMethods = testMethods.stream()
                    .filter(fm -> fm.getName().equals(testMethodName))
                    .collect(Collectors.toList());

            if (testMethods.isEmpty()) {
                testMethods =
                    testMethods.stream()
                        .filter(fm -> fm.getName()
                            .toLowerCase(Locale.ENGLISH)
                            .contains(
                                testMethodName.toLowerCase(Locale.ENGLISH)))
                        .collect(Collectors.toList());
            }
        }

        return testMethods;
    }
}
