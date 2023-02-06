package ru.yandex.travel.junit;

import com.google.inject.Injector;
import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

import static ru.yandex.travel.utils.GuiceUtils.createInjectorFor;
import static ru.yandex.travel.utils.GuiceUtils.getModulesFor;
import static ru.yandex.travel.utils.GuiceUtils.injectTo;

public class GuiceParametersRunnerFactory implements ParametersRunnerFactory {

    @Override
    public Runner createRunnerForTestWithParameters(TestWithParameters testWithParameters) throws InitializationError {
        Class testClass = testWithParameters.getTestClass().getJavaClass();

        Class<?>[] modules = getModulesFor(testClass);
        final Injector injector = createInjectorFor(modules);
        return new BlockJUnit4ClassRunnerWithParameters(testWithParameters) {
            public final Object createTest() throws Exception {
                Object testObject = super.createTest();
                return injectTo(injector, testObject);
            }
        };
    }

}
