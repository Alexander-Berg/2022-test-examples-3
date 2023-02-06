package ru.yandex.market.tpl.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UsersUtilTest {
    @Test
    void shouldNormalyzePhoneNumbers() {
        assertThat("+79998887766").isEqualTo(UsersUtil.normalyzePhoneNumber("79998887766"));
        assertThat("+79998887766").isEqualTo(UsersUtil.normalyzePhoneNumber("89998887766"));
        assertThat("+79998887766").isEqualTo(UsersUtil.normalyzePhoneNumber("9998887766"));
        assertThat("+79998887766").isEqualTo(UsersUtil.normalyzePhoneNumber("(999) 888-77-66"));
        assertThat("+79998887766").isEqualTo(UsersUtil.normalyzePhoneNumber("(999) 888 77 66"));
        assertThat("+79998887766").isEqualTo(UsersUtil.normalyzePhoneNumber("999 888 77 66"));
        assertThat("Такой формат я не знаю").isEqualTo(UsersUtil.normalyzePhoneNumber("Такой формат я не знаю"));
        assertNull(UsersUtil.normalyzePhoneNumber(null));
    }
}
