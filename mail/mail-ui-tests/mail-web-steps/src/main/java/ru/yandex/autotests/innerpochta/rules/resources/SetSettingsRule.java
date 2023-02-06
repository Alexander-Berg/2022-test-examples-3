package ru.yandex.autotests.innerpochta.rules.resources;

import org.glassfish.jersey.internal.util.Producer;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;

/**
 * @author marchart
 */
public class SetSettingsRule extends ExternalResource {

    private Producer<AllureStepStorage> producer;
    private Map<String, ?> settings;

    private SetSettingsRule(Producer<AllureStepStorage> producer, Map<String, ?> settings) {
        this.producer = producer;
        this.settings = settings;
    }

    public static SetSettingsRule setSettings(Producer<AllureStepStorage> producer, Map<String, ?> settings) {
        return new SetSettingsRule(producer, settings);
    }

    @Override
    protected void before() throws Throwable {
        AllureStepStorage user = producer.call();
        for (Map.Entry<String, ?> entry : settings.entrySet()) {
            user.apiSettingsSteps().callWithListAndParams(
                entry.getKey(),
                of(entry.getKey(), entry.getValue())
            );
        }
    }
}
