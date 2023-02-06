package ru.yandex.market.sc.core.exception;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.sc.core.configuration.LocaleConfig;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@EmbeddedDbTest
class ScErrorCodeTest {

    private static List<Runnable> supportedLocales() {
        return List.of(
                LocaleConfig::clean, // default = russian
                () -> LocaleConfig.set(Locale.ENGLISH),
                () -> LocaleConfig.set(Locale.forLanguageTag("es"))
        );
    }

    @AfterEach
    void tearDown() {
        LocaleConfig.clean();
    }

    @Test
    void testLocaleDefaultIsRussian() {
        assertThat(ScErrorCode.UNKNOWN_ERROR.getMessage())
                .isEqualTo("Неизвестная ошибка. Свяжитесь со службой поддержки");
    }

    @Test
    void testLocale() {
        LocaleConfig.set(Locale.forLanguageTag("ru"));
        assertThat(ScErrorCode.UNKNOWN_ERROR.getMessage())
                .isEqualTo("Неизвестная ошибка. Свяжитесь со службой поддержки");
        LocaleConfig.set(Locale.ENGLISH);
        assertThat(ScErrorCode.UNKNOWN_ERROR.getMessage())
                .isEqualTo("Unknown error. Contact the support service");
        LocaleConfig.set(Locale.forLanguageTag("es"));
        assertThat(ScErrorCode.UNKNOWN_ERROR.getMessage())
                .isEqualTo("Error Desconocido. Póngase en contacto con el Servicio al cliente");

        // to default
        LocaleConfig.clean();
        assertThat(ScErrorCode.UNKNOWN_ERROR.getMessage())
                .isEqualTo("Неизвестная ошибка. Свяжитесь со службой поддержки");
    }

    @ParameterizedTest(name = "ошибки бэкенда для локали [{index}]")
    @MethodSource("supportedLocales")
    void testAllErrorCodesLocalized(Runnable supportedLocaleInitializer) {
        supportedLocaleInitializer.run();
        for (ScErrorCode code : ScErrorCode.values()) {
            assertThat(code.getMessage()).isNotBlank();
        }
    }

}
