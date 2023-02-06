package ru.yandex.direct.core.entity.mailnotification.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SuppressWarnings("ConstantConditions")
public class EventAdGroupParamsTest {

    @Test
    public void jsonValue_ResultStringIsEqualToExpectedString() throws Exception {
        EventAdGroupParams<String> adGroupParams =
                new EventAdGroupParams<>(1L, "SuperGroup", "old txt", "new txt");
        String expectedJsonValue = "{\"pid\":1,\"group_name\":\"SuperGroup\","
                + "\"old_text\":\"old txt\",\"new_text\":\"new txt\"}";

        assertThat(adGroupParams.jsonValue()).isEqualTo(expectedJsonValue);
    }

    @Test
    public void fromJson_ResultObjectIsEqualToExpectedObject() throws Exception {
        String jsonValue = "{\"pid\":1,\"group_name\":\"SuperGroup\","
                + "\"old_text\":\"old txt\",\"new_text\":\"new txt\"}";
        //noinspection unchecked
        EventAdGroupParams<String> actual =
                (EventAdGroupParams<String>) EventParams.fromJson(jsonValue, EventAdGroupParams.class);
        EventAdGroupParams<String> expected =
                new EventAdGroupParams<>(1L, "SuperGroup", "old txt", "new txt");

        assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void constructor_AdGroupIdIsNull_ThrowsException() throws Exception {
        Throwable throwable =
                catchThrowable(() -> new EventAdGroupParams<>(null, "SuperGroup", "ef", "ed"));
        assertThat(throwable)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("AdGroup ID");
    }

    @Test
    public void constructor_AdGroupNameIsNull_ThrowsException() throws Exception {
        Throwable throwable =
                catchThrowable(() -> new EventAdGroupParams<>(1L, null, "ef", "ed"));
        assertThat(throwable)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("AdGroup name");
    }

    @Test
    public void constructor_OldValueIdNull_ThrowsException() throws Exception {
        Throwable throwable =
                catchThrowable(() -> new EventAdGroupParams<>(1L, "SuperGroup", null, "ed"));
        assertThat(throwable)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Old value");
    }

    @Test
    public void constructor_NewValueIsNull_ThrowsException() throws Exception {
        Throwable throwable =
                catchThrowable(() -> new EventAdGroupParams<>(1L, "SuperGroup", "ef", null));
        assertThat(throwable)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("New value");
    }

    @Test
    public void constructor_NewValueEqualsOld_ThrowsException() throws Exception {
        Throwable throwable = catchThrowable(() -> new EventAdGroupParams<>(1L, "SuperGroup", "txt", "txt"));
        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class);
    }
}
