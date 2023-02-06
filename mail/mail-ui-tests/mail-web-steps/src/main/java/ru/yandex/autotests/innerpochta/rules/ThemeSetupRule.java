package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;

/**
 * @author a-zoshchuk
 */
public class ThemeSetupRule extends ExternalResource {
    private InitStepsRule steps;

    private ThemeSetupRule(InitStepsRule steps) {
        this.steps = steps;
    }

    public static ThemeSetupRule themeSetupRule(InitStepsRule steps) {
        return new ThemeSetupRule(steps);
    }

    @Override
    protected void before() throws Throwable {
        if (UrlProps.urlProps().getTheme() != null)
            steps.user().apiSettingsSteps()
                .callWithListAndParams(
                    "Устанавливаем тему " + UrlProps.urlProps().getTheme(),
                    of(COLOR_SCHEME, UrlProps.urlProps().getTheme())
                );
    }
}
