package ru.yandex.direct.core.entity.banner.model;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.reflect.Modifier.isStatic;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class TextBannerFlagsTest {

    private static final String[] BOOLEAN_FLAGS = {
            "abortion",
            "medicine",
            "med_services",
            "med_equipment",
            "pharmacy",
            "alcohol",
            "tobacco",
            "plus18",
            "dietarysuppl",
            "project_declaration",
            "tragic",
            "asocial",
            "pseudoweapon",
            "forex"
    };

    private static final Map<String, Class> ENUM_FLAGS = ImmutableMap.of(
            "age", Age.class,
            "baby_food", BabyFood.class
    );

    @Test
    public void BannerFlags_checkBooleanGetters()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        for (String flag : BOOLEAN_FLAGS) {
            BannerFlags bannerFlags = new BannerFlags();
            bannerFlags.getFlags().put(flag, null);

            FlagProperty<Boolean> flagProperty = getProperty(flag);
            Boolean isFlagEnabled = bannerFlags.get(flagProperty);
            assertThat("ожидается выставленный флаг " + flag, isFlagEnabled, is(true));
        }
    }

    @Test
    public void BannerFlags_checkBooleanSettersWith()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        for (String flag : BOOLEAN_FLAGS) {
            BannerFlags bannerFlags = new BannerFlags();

            FlagProperty<Boolean> flagProperty = getProperty(flag);
            bannerFlags.with(flagProperty, true);
            assertThat("ожидается выставленный флаг " + flag, bannerFlags.getFlags().containsKey(flag), is(true));
        }
    }

    @Test
    public void BannerFlags_checkEnumGetters()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        for (Map.Entry<String, Class> entry : ENUM_FLAGS.entrySet()) {
            String flag = entry.getKey();
            Class enumClass = entry.getValue();

            for (Object enumConstant : enumClass.getEnumConstants()) {
                BannerFlags bannerFlags = new BannerFlags();

                String enumStringValue = getStringFieldValueFromEnumConstant(enumClass, enumConstant);
                bannerFlags.getFlags().put(flag, enumStringValue);

                FlagProperty<?> flagProperty = getProperty(flag);
                Object flagValue = bannerFlags.get(flagProperty);
                assertThat("ожидается выставленный флаг " + flag, flagValue, is(enumConstant));
            }
        }
    }

    @Test
    public void BannerFlags_checkEnumSettersWith()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        for (Map.Entry<String, Class> entry : ENUM_FLAGS.entrySet()) {
            String flag = entry.getKey();
            Class enumClass = entry.getValue();

            for (Object enumConstant : enumClass.getEnumConstants()) {
                BannerFlags bannerFlags = new BannerFlags();
                Map<String, String> flagsMap = bannerFlags.getFlags();

                String enumStringValue = getStringFieldValueFromEnumConstant(enumClass, enumConstant);

                FlagProperty<Object> flagProperty = getProperty(flag);
                bannerFlags.with(flagProperty, enumConstant);
                assertThat("ожидается выставленный флаг " + flag, flagsMap.get(flag), is(enumStringValue));
            }
        }
    }

    @Test
    public void BannerFlags_checkBooleanRemove()
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        for (String flag : BOOLEAN_FLAGS) {
            BannerFlags bannerFlags = new BannerFlags();
            bannerFlags.getFlags().put(flag, null);

            FlagProperty<Boolean> flagProperty = getProperty(flag);
            Boolean isFlagEnabled = bannerFlags.get(flagProperty);
            checkState(isFlagEnabled != null && isFlagEnabled,
                    "геттер boolean флага должен работать, проверен другим тестом");

            bannerFlags.remove(flagProperty);
            isFlagEnabled = bannerFlags.get(flagProperty);
            assertThat("ожидается отсутствие флага " + flag, isFlagEnabled, is(false));
        }
    }

    @Test
    public void BannerFlags_checkEnumRemove()
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        for (Map.Entry<String, Class> entry : ENUM_FLAGS.entrySet()) {
            String flag = entry.getKey();
            Class enumClass = entry.getValue();

            BannerFlags bannerFlags = new BannerFlags();

            Object someEnumConstant = enumClass.getEnumConstants()[0];

            String enumStringValue = getStringFieldValueFromEnumConstant(enumClass, someEnumConstant);
            bannerFlags.getFlags().put(flag, enumStringValue);

            FlagProperty<?> flagProperty = getProperty(flag);
            Object flagValue = bannerFlags.get(flagProperty);

            checkState(flagValue != null && flagValue.equals(someEnumConstant),
                    "геттер enum флага должен работать, проверен другим тестом");

            bannerFlags.remove(flagProperty);

            flagValue = bannerFlags.get(flagProperty);
            assertThat("ожидается отсутствие флага " + flag, flagValue, nullValue());
        }
    }

    private String getStringFieldValueFromEnumConstant(Class enumClass, Object enumConstant)
            throws IllegalAccessException {
        Optional<Field> enumStringField = Arrays.stream(enumClass.getDeclaredFields())
                .filter(f -> !isStatic(f.getModifiers()))
                .filter(f -> f.getType().equals(String.class))
                .peek(f -> f.setAccessible(true))
                .findFirst();
        if (!enumStringField.isPresent()) {
            throw new IllegalStateException("ожидается текстовое поле значения флага в перечислимом типе");
        }

        return (String) enumStringField.get().get(enumConstant);
    }

    @SuppressWarnings("unchecked")
    private <T> FlagProperty<T> getProperty(String flagName) throws NoSuchFieldException, IllegalAccessException {
        return (FlagProperty<T>) BannerFlags.class.getDeclaredField(flagName.toUpperCase()).get(null);
    }
}
