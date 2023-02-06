package ru.yandex.market.checkout.common.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author jkt on 28/01/2022.
 */
class LogFormatterUtilsTest {

    private static final String LOG_RECORD_TEMPLATE = "Before orderId=%s in the middle anotherIds %s after";
    private static final String UUID = "5703c480-18e0-4bd8-8f88-ff66fb7d4dd0";
    private static final String LOG_WITHOUT_IDS = "Just some log: no, id, list";

    @Test
    void shouldReplaceIds() {
        String croppedLog = LogFormatterUtils.replaceAllIds(generateLogRecord("123456"));
        Assertions.assertThat(croppedLog).isEqualTo(generateLogRecord("<id>"));
    }

    @Test
    void shouldReplaceUuids() {
        String croppedLog = LogFormatterUtils.replaceAllUuids(generateLogRecord(UUID));
        Assertions.assertThat(croppedLog).isEqualTo(generateLogRecord("<uuid>"));
    }

    @Test
    void shouldReplaceIdLists() {
        String croppedLog = LogFormatterUtils.replaceAllIdsAndIdLists(generateLogRecord("12345, 6789 "));
        Assertions.assertThat(croppedLog).isEqualTo(generateLogRecord("<id>"));
    }

    @Test
    void shouldReturnNullOnNullInput() {
        String croppedLog = LogFormatterUtils.replaceAllIds(null);
        Assertions.assertThat(croppedLog).isNull();
    }

    @Test
    void shouldReturnEmptyOnEmptyInput() {
        String croppedLog = LogFormatterUtils.replaceAllIds("");
        Assertions.assertThat(croppedLog).isEmpty();
    }

    @Test
    void shouldReturnAsIsWhenNoIdsPresent() {
        String croppedLog = LogFormatterUtils.replaceAllIds(LOG_WITHOUT_IDS);
        Assertions.assertThat(croppedLog).isEqualTo(LOG_WITHOUT_IDS);
    }

    private String generateLogRecord(String ids) {
        return String.format(LOG_RECORD_TEMPLATE, ids, ids);
    }
}
