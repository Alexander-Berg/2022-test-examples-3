package ru.yandex.direct.core.entity.banner.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class FlagPropertyTest {
    private static final String ALCOHOL_KEY = "alcohol";
    private FlagProperty<Boolean> alcoholFlagProperty = FlagProperty.booleanFlag(ALCOHOL_KEY);

    private static final String AGE_KEY = "age";
    private FlagProperty<Age> ageFlagProperty =
            FlagProperty.enumFlag(AGE_KEY, Age.AGE_18, Age::fromSource, Age::getValue);

    @Test
    public void extract_BooleanFlag() {
        Map<String, String> map = new HashMap<>();
        map.put(ALCOHOL_KEY, null);
        assertThat(alcoholFlagProperty.extract(map), is(true));
    }

    @Test
    public void extract_EnumFlag() {
        Map<String, String> map = new HashMap<>();
        map.put(AGE_KEY, "12");

        FlagProperty flagProperty = FlagProperty.enumFlag(AGE_KEY, Age.AGE_18, Age::fromSource, Age::getValue);

        assertThat(flagProperty.extract(map), is(Age.AGE_12));
    }

    @Test
    public void remove_BooleanFlag() {
        Map<String, String> map = new HashMap<>();
        map.put(ALCOHOL_KEY, null);
        alcoholFlagProperty.remove(map);
        assertThat(alcoholFlagProperty.extract(map), is(false));
    }

    @Test
    public void remove_EnumFlag() {
        Map<String, String> map = new HashMap<>();
        map.put(AGE_KEY, "12");
        ageFlagProperty.remove(map);
        assertThat(ageFlagProperty.extract(map), nullValue());
    }

    @Test
    public void store_BooleanFlag() {
        Map<String, String> map = new HashMap<>();
        alcoholFlagProperty.store(true, map);
        assertThat(map.containsKey(ALCOHOL_KEY), is(true));
    }

    @Test
    public void store_EnumFlag() {
        Map<String, String> map = new HashMap<>();
        ageFlagProperty.store(Age.AGE_6, map);
        assertThat(map.get(AGE_KEY), is("6"));
    }

    @Test
    public void enumFlag_withDefaultValue() {
        Map<String, String> map = new HashMap<>();
        map.put(AGE_KEY, null);

        assertThat(ageFlagProperty.extract(map), is(Age.AGE_18));
    }

    @Test
    public void isBannerGeoLegalFlag() {
        HashSet<String> bannerGeoLegalFlags = new HashSet<>();
        for (BannerGeoLegalFlags c : BannerGeoLegalFlags.values()) {
            bannerGeoLegalFlags.add(c.getValue());
        }

        assertThat(bannerGeoLegalFlags.contains(ALCOHOL_KEY), is(true));
    }
}
