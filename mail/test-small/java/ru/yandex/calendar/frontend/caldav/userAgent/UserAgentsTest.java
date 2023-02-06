package ru.yandex.calendar.frontend.caldav.userAgent;

import java.util.stream.Stream;

import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;

public class UserAgentsTest {
    @ParameterizedTest
    @MethodSource("getAgents")
    public void check(String userAgent, UserAgentType expected) {
        assertThat(UserAgents.userAgentType(userAgent)).isEqualTo(expected);
    }

    private static Stream<Arguments> getAgents() {
        String macintoshMozilla = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; en-US; rv:1.9.1.9) "
                + "Gecko/20100317 Lightning/1.0b1 Thunderbird/3.0.4";
        String calendarStoreDavKit = "DAVKit/4.0.3 (732); CalendarStore/4.0.4 (997); "
                + "iCal/4.0.4 (1395); Mac OS X/10.6.6 (10J567)";
        String calDavSyncAdapter = "CalDAV Sync Adapter (Android) https://github.com/gggard/AndroidCaldavSyncAdapater Version:1.8.1";

        return Stream.of(
                of(macintoshMozilla, UserAgentType.LIGHTNING),
                of(calendarStoreDavKit, UserAgentType.ICAL),
                of("Mac+OS+X/10.12.5 (16F73) CalendarAgent/386", UserAgentType.ICAL),
                of("iOS/7.0.2 (11A501) dataaccessd/1.0", UserAgentType.IOS_8_0_AND_LOWER),
                of("iOS/8.2 (12D5461b) dataaccessd/1.0", UserAgentType.IOS_8_1_AND_HIGHER),
                of("ru.yandex.mail/4.49.2.69887 (Yandex YNDX-000SB; Android 9)",
                        UserAgentType.YANDEX_MOBILE_MAIL_CLIENT),
                of("DAVKit/5.0 (767); iCalendar/5.0 (79); iPhone/4.2.1 8C148", UserAgentType.IPHONE),
                of("Mozilla/5.0 (Android 2.3.3) CalDAVSync/Beta",UserAgentType.HYPERMATIX_ANDROID),
                of("CalDAV-Sync (Android) (like iOS/5.0.1 (9A405) dataaccessd/1.0) gzip",
                        UserAgentType.CALDAVSYNC_ANDROID),
                of(calDavSyncAdapter, UserAgentType.CALDAVSYNCADAPTER_ANDROID)
        );
    }

    @Disabled("This test case shows that the ios matcher is not working correctly. TODO fix that")
    public void iOSTricky() {
        val ua = "iOS/10.0 How could you miss such an obvious case?";
        assertThat(UserAgents.userAgentType(ua)).isEqualTo(UserAgentType.IOS_8_1_AND_HIGHER);
    }
}
