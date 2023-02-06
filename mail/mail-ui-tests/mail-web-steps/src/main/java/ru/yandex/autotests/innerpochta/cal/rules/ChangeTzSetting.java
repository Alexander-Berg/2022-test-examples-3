package ru.yandex.autotests.innerpochta.cal.rules;

import org.glassfish.jersey.internal.util.Producer;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;

/**
 * @author cosmopanda
 */
public class ChangeTzSetting extends ExternalResource {

    private Producer<AllureStepStorage> producer;

    private ChangeTzSetting(Producer<AllureStepStorage> producer) {
        this.producer = producer;
    }

    public static ChangeTzSetting changeTzSetting(Producer<AllureStepStorage> producer) {
        return new ChangeTzSetting(producer);
    }

    @Override
    protected void before() throws Throwable {
        AllureStepStorage user = producer.call();
        user.apiCalSettingsSteps().updateUserSettings(
            "Последняя предложенная таймзона",
            new Params().withLastOfferedGeoTz("Europe/Moscow")
        );
    }
}
