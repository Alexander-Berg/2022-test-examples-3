package ru.yandex.market.checkout.pushapi.shop.entity;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import ru.yandex.market.checkout.pushapi.client.entity.Builder;
import ru.yandex.market.checkout.pushapi.client.entity.shop.SettingsBuilder;

import java.util.HashMap;
import java.util.Map;

import static ru.yandex.market.checkout.pushapi.client.entity.BuilderUtil.buildMap;
import static ru.yandex.market.checkout.pushapi.client.entity.shop.SettingsBuilder.sameSettings;

/**
 * @author msavelyev
 */
public class AllSettingsBuilder implements Builder<AllSettings> {

    private Map<Long, SettingsBuilder> allSettings = new HashMap<>();

    public AllSettingsBuilder add(Long shopId, SettingsBuilder settings) {
        final AllSettingsBuilder copy = copy();
        copy.allSettings.put(shopId, settings);
        return copy;
    }

    private AllSettingsBuilder copy() {
        final AllSettingsBuilder newBuilder = new AllSettingsBuilder();
        newBuilder.allSettings = new HashMap<>(allSettings);
        return newBuilder;
    }

    @Override
    public AllSettings build() {
        final AllSettings allSettings = new AllSettings();
        allSettings.putAll(buildMap(this.allSettings));
        return allSettings;
    }

    public static Matcher<AllSettings> sameAllSettings(final AllSettingsBuilder expected) {
        return sameAllSettings(expected.build());
    }

    public static Matcher<AllSettings> sameAllSettings(final AllSettings expected) {
        return new BaseMatcher<AllSettings>() {
            @Override
            public boolean matches(Object o) {
                if(!(o instanceof AllSettings)) {
                    return false;
                }

                final AllSettings actual = (AllSettings) o;
                if(actual.size() != expected.size()) {
                    return false;
                }
                for(Long shopId : expected.keySet()) {
                    if(!actual.containsKey(shopId)) {
                        return false;
                    }
                    if(!sameSettings(expected.get(shopId)).matches(actual.get(shopId))) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("same allSettings");
            }
        };
    }
}
