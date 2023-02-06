package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.ExternalResource;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DISABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_TODO;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TIMELINE_ENABLE;

/**
 * @author cosmopanda
 */
public class OffSettingsRule extends ExternalResource {
    private InitStepsRule steps;

    private OffSettingsRule(InitStepsRule steps) {
        this.steps = steps;
    }

    public static OffSettingsRule offSettings(InitStepsRule steps) {
        return new OffSettingsRule(steps);
    }

    @Override
    protected void before() throws Throwable {
        steps.user().apiSettingsSteps()
            .callWithListAndParams(SHOW_ADVERTISEMENT, of(SHOW_ADVERTISEMENT, DISABLED_ADV))
            .callWithListAndParams(TIMELINE_ENABLE, of(TIMELINE_ENABLE, FALSE))
            .callWithListAndParams(SHOW_TODO, of(SHOW_TODO,STATUS_FALSE));
    }
}
