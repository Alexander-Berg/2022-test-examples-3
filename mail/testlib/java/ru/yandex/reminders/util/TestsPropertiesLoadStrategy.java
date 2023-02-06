package ru.yandex.reminders.util;

import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.property.load.strategy.PropertiesBuilder;
import ru.yandex.misc.property.load.strategy.PropertiesLoadStrategy;
import ru.yandex.misc.version.AppName;


public class TestsPropertiesLoadStrategy implements PropertiesLoadStrategy {
    private final AppName appName;
    private final String propertiesClasspath;
    private final boolean forTests;

    public TestsPropertiesLoadStrategy(AppName appName, String propertiesClasspath, boolean forTests) {
        this.appName = appName;
        this.propertiesClasspath = propertiesClasspath;
        this.forTests = forTests;
    }

    @Override
    public final void load(PropertiesBuilder builder, EnvironmentType environmentType) {
        builder.set("app.name", appName.appName());
        builder.setDefault(EnvironmentType.YANDEX_ENVIRONMENT_TYPE_PROPERTY, environmentType.getValue());

        builder.includeSystemProperties();

        builder.include("classpath:" + propertiesClasspath + "/application-default.properties");
        builder.include("classpath:" + propertiesClasspath + "/application-" + environmentType.name().toLowerCase()
                + ".properties");

        builder.includeIfExists("/etc/yandex/" + appName.serviceName() + "/" + appName.appName() + "/application.properties");

        if (forTests) {
            builder.includeIfExists("classpath:" + propertiesClasspath + "/application-tests.properties");
        }

        builder.includeCmdLineProperties();

        loadSystemEnvVariables(builder, "reminders_");
        builder.copyCutPrefix(appName.appName() + ".");

        builder.dump();
    }

    private void loadSystemEnvVariables(PropertiesBuilder builder, String prefix) {
        Map<String, String> env = normalizeSysEnvs(System.getenv(), prefix);
        Map<String, String> result = env.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue()));
        builder.include(result);
    }

    private static Map<String, String> normalizeSysEnvs(Map<String, String> envs, String prefix) {
        return envs.entrySet().stream()
                .filter(s -> s.getKey().toLowerCase().startsWith(prefix))
                .collect(Collectors.toMap(
                        e -> normalizeSysEnvKey(cutPrefix(e.getKey(), prefix)),
                        e -> e.getValue()));
    }

    private static String normalizeSysEnvKey(String key) {
        String dotted = key.replaceAll("_", ".");
        if (dotted.toUpperCase().equals(dotted)) {
            return dotted.toLowerCase();
        }
        return dotted;
    }

    private static String cutPrefix(String key, String prefix) {
        return key.substring(prefix.length());
    }
}
