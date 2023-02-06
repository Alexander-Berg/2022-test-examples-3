package ru.yandex.autotests.innerpochta.cal.rules;

import lombok.AllArgsConstructor;
import org.glassfish.jersey.internal.util.Producer;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.layer.Layer;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.USER_TYPE;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.YELLOW_COLOR;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author cosmopanda
 */
@AllArgsConstructor(staticName = "addLayerIfNeed")
public class AddLayerIfNeedRule extends ExternalResource {

    private Producer<AllureStepStorage> producer;

    @Override
    protected void before() throws Throwable {
        AllureStepStorage user = producer.call();
        if (user.apiCalSettingsSteps().getUserLayers().size() < 2) {
            user.apiCalSettingsSteps().createNewLayer(
                new Layer()
                    .withName(getRandomName())
                    .withColor(YELLOW_COLOR)
                    .withType(USER_TYPE)
                    .withIsClosed(false)
                    .withIsDefault(false)
                    .withAffectsAvailability(true)
                    .withIsOwner(true)
            );
        }
    }
}
