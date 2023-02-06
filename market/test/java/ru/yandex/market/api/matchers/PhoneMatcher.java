package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.offer.Phone;

public class PhoneMatcher {
    public static Matcher<Phone> phone(String number, String sanitizedNumber) {
        return Matchers.allOf(
            ApiMatchers.map(
                Phone::getNumber,
                "'number'",
                Matchers.is(number),
                PhoneMatcher::toStr
            ),
            ApiMatchers.map(
                Phone::getSanitizedNumber,
                "'sanitizedNumber'",
                Matchers.is(sanitizedNumber),
                PhoneMatcher::toStr
            )
        );
    }

    public static String toStr(Phone phone) {
        if (null == phone) {
            return "null";
        }
        return MoreObjects.toStringHelper(Phone.class)
            .add("number", phone.getNumber())
            .add("sanitizedNumber", phone.getSanitizedNumber())
            .toString();

    }
}
