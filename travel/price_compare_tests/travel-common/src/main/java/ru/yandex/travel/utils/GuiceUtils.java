package ru.yandex.travel.utils;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiceUtils {

    private GuiceUtils() {
    }

    public static Object injectTo(Injector injector, Object obj) {
        injector.injectMembers(obj);
        return obj;
    }

    public static Injector createInjectorFor(Class<?>... classes) throws InitializationError {
        List<Module> modules = new ArrayList<>();
        for (Object module : Arrays.asList(classes)) {
            Class moduleClass = (Class) module;
            try {
                modules.add((Module) moduleClass.newInstance());
            } catch (ReflectiveOperationException var6) {
                throw new InitializationError(var6);
            }
        }
        Injector injector = Guice.createInjector(modules);
        return injector;
    }

    public static Class<?>[] getModulesFor(Class<?> testClass) throws InitializationError {
        GuiceModules annotation = testClass.getAnnotation(GuiceModules.class);
        if (annotation == null) {
            String message = String.format("Missing @GuiceModules annotation for unit test \'%s\'", testClass.getName());
            throw new InitializationError(message);
        } else {
            return annotation.value();
        }
    }

}
