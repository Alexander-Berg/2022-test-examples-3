package ru.yandex.market.core.samovar;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.samovar.model.SamovarFeedDownloadError;

/**
 * Тесты для {@link SamovarNotificationFilter}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class SamovarNotificationFilterTest {

    private static final int ERROR_THRESHOLD = 5;

    private SamovarNotificationFilter samovarNotificationFilter;

    @BeforeEach
    void init() {
        samovarNotificationFilter = new SamovarNotificationFilter(ERROR_THRESHOLD);
    }

    @ParameterizedTest
    @MethodSource("testFilterErrorNotificationData")
    @DisplayName("Фильтр отправки нотификации о поломке")
    void testFilterErrorNotification(String name, int errorsNumber, boolean expected) {
        SamovarFeedDownloadError error = SamovarFeedDownloadError.builder()
                .externalErrorCount(errorsNumber)
                .build();
        boolean actual = samovarNotificationFilter.filterErrorNotification(error);
        Assertions.assertEquals(expected, actual);
    }

    @MethodSource
    private static Stream<Arguments> testFilterErrorNotificationData() {
        return Stream.of(
                Arguments.of(
                        "Меньше порога. Отправки быть не должно",
                        ERROR_THRESHOLD - 1,
                        false
                ),
                Arguments.of(
                        "Число ошибок достигло порога. Отправка должна быть",
                        ERROR_THRESHOLD,
                        true
                ),
                Arguments.of(
                        "Больше порога. Отправки быть не должно",
                        ERROR_THRESHOLD + 1,
                        false
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testFilterRecoveryNotificationData")
    @DisplayName("Фильтр отправки нотификации о починке")
    void testFilterRecoveryNotification(String name, int errorsNumber, boolean expected) {
        SamovarFeedDownloadError error = SamovarFeedDownloadError.builder()
                .externalErrorCount(errorsNumber)
                .build();
        boolean actual = samovarNotificationFilter.filterRecoveryNotification(error);
        Assertions.assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("testFilterRecoveryNotificationData")
    @DisplayName("Фильтр отправки нотификации о починке")
    void testfilterErrorForReport(String name, int errorsNumber, boolean expected) {
        SamovarFeedDownloadError error = SamovarFeedDownloadError.builder()
                .externalErrorCount(errorsNumber)
                .build();
        boolean actual = samovarNotificationFilter.filterErrorForReport(error);
        Assertions.assertEquals(expected, actual);
    }

    @MethodSource
    private static Stream<Arguments> testFilterRecoveryNotificationData() {
        return Stream.of(
                Arguments.of(
                        "Меньше порога. Отправки быть не должно",
                        ERROR_THRESHOLD - 1,
                        false
                ),
                Arguments.of(
                        "Число ошибок достигло порога. Отправка должна быть",
                        ERROR_THRESHOLD,
                        true
                ),
                Arguments.of(
                        "Больше порога. Отправка должна быть",
                        ERROR_THRESHOLD + 1,
                        true
                )
        );
    }
}
