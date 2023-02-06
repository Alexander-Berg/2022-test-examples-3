package ru.yandex.direct.jobs.walletswarnings;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.core.entity.eventlog.model.DaysLeftNotificationType;
import ru.yandex.direct.i18n.Language;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
public class WalletsWarningsMailTemplateResolverTest {

    public static final String ONE_DAY_REMAIN = "RU_ONE_DAY_REMAIN";
    public static final String THREE_DAYS_REMAIN = "RU_THREE_DAYS_REMAIN";
    private final Map<DaysLeftNotificationType, String> notificationTypes = Map.of(
            DaysLeftNotificationType.ONE_DAY_REMAIN, ONE_DAY_REMAIN,
            DaysLeftNotificationType.THREE_DAYS_REMAIN, THREE_DAYS_REMAIN);
    private final Map<Language, Map<DaysLeftNotificationType, String>> templatesMap = Map.of(Language.RU,
            notificationTypes);

    static Object[] parametrizedTestData() {
        return new Object[][]{
                // шаблон существует для языка
                {Language.RU, DaysLeftNotificationType.THREE_DAYS_REMAIN, THREE_DAYS_REMAIN},
                {Language.RU, DaysLeftNotificationType.ONE_DAY_REMAIN, ONE_DAY_REMAIN},
                // шаблон не существует для языка
                {Language.UK, DaysLeftNotificationType.THREE_DAYS_REMAIN, THREE_DAYS_REMAIN},
                {Language.UK, DaysLeftNotificationType.ONE_DAY_REMAIN, ONE_DAY_REMAIN},
        };
    }

    @ParameterizedTest(name = "check TemplateResolver for lang {0} when notificationType: {1}")
    @MethodSource("parametrizedTestData")
    void checkTemplateResolver(Language lang,
                               DaysLeftNotificationType notificationType,
                               String expectedResult) {
        WalletsWarningsMailTemplateResolver walletsWarningsMailTemplateResolver =
                new WalletsWarningsMailTemplateResolver(templatesMap);
        String result = walletsWarningsMailTemplateResolver.resolveTemplateId(lang, notificationType);

        assertThat(result).isEqualTo(expectedResult);
    }
}
