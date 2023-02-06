package ru.yandex.autotests.innerpochta.rules.enviroment;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import ru.yandex.autotests.innerpochta.rules.enviroment.EnvironmentInfoHolder;
import ru.yandex.autotests.innerpochta.rules.enviroment.EnvironmentInfoSerializer;
import ru.yandex.qatools.commons.model.Parameter;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author a-zoshchuk
 */
public class EnvironmentInfoRule implements TestRule {
    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } finally {
                    EnvironmentInfoSerializer serializer = EnvironmentInfoSerializer.getInstance();
                    Map<String, String> environmentInfo = EnvironmentInfoHolder.getInstance().getEnvironmentInfo();
                    for (Entry<String, String> entry : environmentInfo.entrySet()) {
                        Parameter parameter = new Parameter();
                        parameter.setName(entry.getKey());
                        parameter.setValue(entry.getValue());
                        parameter.setKey(toPropertiesKey(entry.getKey()));
                        serializer.addEnvParameter(parameter);
                    }
                }
            }
        };
    }

    private static String toPropertiesKey(String name) {
        String[] parts = name.replaceAll("[\"']*", "").toLowerCase().split(" ");
        return Arrays.stream(parts).collect(Collectors.joining("."));
    }
}
