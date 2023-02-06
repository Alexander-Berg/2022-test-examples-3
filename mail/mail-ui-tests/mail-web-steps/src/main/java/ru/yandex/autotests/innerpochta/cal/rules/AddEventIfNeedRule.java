package ru.yandex.autotests.innerpochta.cal.rules;

import lombok.AllArgsConstructor;
import org.glassfish.jersey.internal.util.Producer;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * @author cosmopanda
 */
@AllArgsConstructor(staticName = "addEventIfNeed")
public class AddEventIfNeedRule extends ExternalResource {

    private Producer<AllureStepStorage> producer;

    @Override
    protected void before() throws Throwable {
        AllureStepStorage user = producer.call();
        if (user.apiCalSettingsSteps().getAllEvents().size() < 1) {
            Long layerID = user.apiCalSettingsSteps().getUserLayers().get(0).getId();
            user.apiCalSettingsSteps().createNewEvent(
                user.settingsCalSteps().formDefaultEvent(layerID)
            );
        }
    }
}
