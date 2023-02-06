package ru.yandex.autotests.innerpochta.cal.rules;

import lombok.AllArgsConstructor;
import org.glassfish.jersey.internal.util.Producer;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * @author cosmopanda
 */
@AllArgsConstructor(staticName = "deleteTodayEvents")
public class DeleteTodayEventsRule extends ExternalResource {

    private Producer<AllureStepStorage> producer;

    @Override
    protected void before() throws Throwable {
        AllureStepStorage user = producer.call();
        user.apiCalSettingsSteps().deleteTodayEvents();
    }
}
