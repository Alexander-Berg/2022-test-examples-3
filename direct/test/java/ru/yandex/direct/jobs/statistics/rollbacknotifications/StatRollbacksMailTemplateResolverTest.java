package ru.yandex.direct.jobs.statistics.rollbacknotifications;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.i18n.Language;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
public class StatRollbacksMailTemplateResolverTest {

    public static final String FRAUD_WITH_WALLET = "RU_ONE_DAY_REMAIN";
    public static final String FRAUD_WITHOUT_WALLET = "FRAUD_WITHOUT_WALLET";
    public static final String CHARGEBACK_WITH_WALLET = "CHARGEBACK_WITH_WALLET";
    public static final String CHARGEBACK_WITHOUT_WALLET = "CHARGEBACK_WITHOUT_WALLET";
    public static final String ERROR_WITH_WALLET = "ERROR_WITH_WALLET";
    public static final String ERROR_WITHOUT_WALLET = "ERROR_WITHOUT_WALLET";
    public static final String PERROR_WITH_WALLET = "PERROR_WITH_WALLET";
    public static final String PERROR_WITHOUT_WALLET = "PERROR_WITHOUT_WALLET";
    public static final String CUSTOM_WITH_WALLET = "CUSTOM_WITH_WALLET";
    public static final String CUSTOM_WITHOUT_WALLET = "CUSTOM_WITHOUT_WALLET";


    private final Map<StatRollbacksMailNotificationType, String> notificationTypes = Map.of(
            StatRollbacksMailNotificationType.FRAUD_WITH_WALLET, FRAUD_WITH_WALLET,
            StatRollbacksMailNotificationType.FRAUD_WITHOUT_WALLET, FRAUD_WITHOUT_WALLET,
            StatRollbacksMailNotificationType.CHARGEBACK_WITH_WALLET, CHARGEBACK_WITH_WALLET,
            StatRollbacksMailNotificationType.CHARGEBACK_WITHOUT_WALLET, CHARGEBACK_WITHOUT_WALLET,
            StatRollbacksMailNotificationType.ERROR_WITH_WALLET, ERROR_WITH_WALLET,
            StatRollbacksMailNotificationType.ERROR_WITHOUT_WALLET, ERROR_WITHOUT_WALLET,
            StatRollbacksMailNotificationType.PERROR_WITH_WALLET, PERROR_WITH_WALLET,
            StatRollbacksMailNotificationType.PERROR_WITHOUT_WALLET, PERROR_WITHOUT_WALLET,
            StatRollbacksMailNotificationType.CUSTOM_WITH_WALLET, CUSTOM_WITH_WALLET,
            StatRollbacksMailNotificationType.CUSTOM_WITHOUT_WALLET, CUSTOM_WITHOUT_WALLET);
    private final Map<Language, Map<StatRollbacksMailNotificationType, String>> templatesMap = Map.of(Language.RU,
            notificationTypes);

    static Object[] parametrizedTestData() {
        return new Object[][]{
                // шаблон существует для языка
                {Language.RU, StatRollbacksMailNotificationType.FRAUD_WITH_WALLET, FRAUD_WITH_WALLET},
                {Language.RU, StatRollbacksMailNotificationType.FRAUD_WITHOUT_WALLET, FRAUD_WITHOUT_WALLET},
                {Language.RU, StatRollbacksMailNotificationType.CHARGEBACK_WITH_WALLET, CHARGEBACK_WITH_WALLET},
                {Language.RU, StatRollbacksMailNotificationType.CHARGEBACK_WITHOUT_WALLET, CHARGEBACK_WITHOUT_WALLET},
                {Language.RU, StatRollbacksMailNotificationType.ERROR_WITH_WALLET, ERROR_WITH_WALLET},
                {Language.RU, StatRollbacksMailNotificationType.ERROR_WITHOUT_WALLET, ERROR_WITHOUT_WALLET},
                {Language.RU, StatRollbacksMailNotificationType.PERROR_WITH_WALLET, PERROR_WITH_WALLET},
                {Language.RU, StatRollbacksMailNotificationType.PERROR_WITHOUT_WALLET, PERROR_WITHOUT_WALLET},
                {Language.RU, StatRollbacksMailNotificationType.CUSTOM_WITH_WALLET, CUSTOM_WITH_WALLET},
                {Language.RU, StatRollbacksMailNotificationType.CHARGEBACK_WITHOUT_WALLET, CHARGEBACK_WITHOUT_WALLET},

                // шаблон не существует для языка
                {Language.UK, StatRollbacksMailNotificationType.FRAUD_WITH_WALLET, FRAUD_WITH_WALLET},
                {Language.UK, StatRollbacksMailNotificationType.FRAUD_WITHOUT_WALLET, FRAUD_WITHOUT_WALLET},
                {Language.UK, StatRollbacksMailNotificationType.CHARGEBACK_WITH_WALLET, CHARGEBACK_WITH_WALLET},
                {Language.UK, StatRollbacksMailNotificationType.CHARGEBACK_WITHOUT_WALLET, CHARGEBACK_WITHOUT_WALLET},
                {Language.UK, StatRollbacksMailNotificationType.ERROR_WITH_WALLET, ERROR_WITH_WALLET},
                {Language.UK, StatRollbacksMailNotificationType.ERROR_WITHOUT_WALLET, ERROR_WITHOUT_WALLET},
                {Language.UK, StatRollbacksMailNotificationType.PERROR_WITH_WALLET, PERROR_WITH_WALLET},
                {Language.UK, StatRollbacksMailNotificationType.PERROR_WITHOUT_WALLET, PERROR_WITHOUT_WALLET},
                {Language.UK, StatRollbacksMailNotificationType.CUSTOM_WITH_WALLET, CUSTOM_WITH_WALLET},
                {Language.UK, StatRollbacksMailNotificationType.CHARGEBACK_WITHOUT_WALLET, CHARGEBACK_WITHOUT_WALLET},
        };
    }

    @ParameterizedTest(name = "check TemplateResolver for lang {0} when notificationType: {1}")
    @MethodSource("parametrizedTestData")
    void checkTemplateResolver(Language lang,
                               StatRollbacksMailNotificationType notificationType,
                               String expectedResult) {
        StatRollbacksMailTemplateResolver statRollbacksMailTemplateResolver =
                new StatRollbacksMailTemplateResolver(templatesMap);
        String result = statRollbacksMailTemplateResolver.resolveTemplateId(lang, notificationType);

        assertThat(result).isEqualTo(expectedResult);
    }
}
