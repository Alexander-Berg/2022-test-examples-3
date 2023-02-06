package ru.yandex.market.checkout.util;

import java.util.Arrays;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import ru.yandex.market.checkout.pushapi.settings.Settings;

import static org.hamcrest.CoreMatchers.is;

public final class SettingsUtils {

    private SettingsUtils() {
    }

    public static Matcher<Settings> sameSettings(final Settings settings) {
        return new BaseMatcher<>() {
            @Override
            public boolean matches(Object o) {
                if (!(o instanceof Settings)) {
                    return false;
                }

                final Settings actual = (Settings) o;
                if (!is(settings.getAuthToken()).matches(actual.getAuthToken())) {
                    return false;
                }
                if (!is(settings.getDataType()).matches(actual.getDataType())) {
                    return false;
                }
                if (!is(settings.getAuthType()).matches(actual.getAuthType())) {
                    return false;
                }
                if (!is(settings.getUrlPrefix()).matches(actual.getUrlPrefix())) {
                    return false;
                }
                if (!Arrays.equals(settings.getFingerprint(), actual.getFingerprint())) {
                    return false;
                }
                if (!is(settings.isPartnerInterface()).matches(actual.isPartnerInterface())) {
                    return false;
                }

                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("same settings as " + settings);
            }
        };
    }
}
