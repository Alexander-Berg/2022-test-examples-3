package ru.yandex.direct.grid.processing.service.constant;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.timetarget.model.GeoTimezone;
import ru.yandex.direct.core.entity.timetarget.model.GroupType;
import ru.yandex.direct.grid.processing.model.constants.GdTimezone;
import ru.yandex.direct.grid.processing.model.constants.GdTimezoneGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(Parameterized.class)
public class ConstantsConverterTimezoneLocalizationTest {
    @Parameterized.Parameter
    public Locale locale;

    @Parameterized.Parameter(1)
    public GeoTimezone timezone;

    @Parameterized.Parameter(2)
    public GdTimezoneGroup timezoneGroup;

    @Parameterized.Parameter(3)
    public String description;

    @Test
    public void testToGdTimezoneGroup() {
        List<GdTimezoneGroup> actualGroups = ConstantsConverter.toGdTimezoneGroups(List.of(timezone), locale);
        assertThat(actualGroups).hasSize(1);

        GdTimezoneGroup actual = actualGroups.get(0);
        assertThat(actual).is(matchedBy(beanDiffer(timezoneGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Parameterized.Parameters(name = "{3}")
    public static Collection<Object[]> parameters() {
        return List.of(new Object[][] {
                {
                        new Locale("ru"),
                        getTimezone(131L, "Europe/Kaliningrad",
                                "Калининград", null, null, null),
                        getTimezoneGroup(131L, "Europe/Kaliningrad", "Калининград (MSK -01:00)",
                                GroupType.RUSSIA.getTypedValue()),
                        "Русский язык имени"
                },
                {
                        new Locale("uk"),
                        getTimezone(131L, "Europe/Kaliningrad",
                                null, null, "Калінінград", null),
                        getTimezoneGroup(131L, "Europe/Kaliningrad", "Калінінград (MSK -01:00)",
                                GroupType.RUSSIA.getTypedValue()),
                        "Украинский язык имени"
                },
                {
                        new Locale("en"),
                        getTimezone(131L, "Europe/Kaliningrad",
                                null, "Kaliningrad", null, null),
                        getTimezoneGroup(131L, "Europe/Kaliningrad", "Kaliningrad (MSK -01:00)",
                                GroupType.RUSSIA.getTypedValue()),
                        "Английский язык имени"
                },
                {
                        new Locale("tr"),
                        getTimezone(131L, "Europe/Kaliningrad",
                                null, null, null, "Kaliningrad"),
                        getTimezoneGroup(131L, "Europe/Kaliningrad", "Kaliningrad (MSK -01:00)",
                                GroupType.RUSSIA.getTypedValue()),
                        "Турецкий язык имени"
                },
                {
                        new Locale("ru"),
                        getTimezone(130L, "Europe/Moscow",
                                "Москва", null, null, null),
                        getTimezoneGroup(130L, "Europe/Moscow", "Москва",
                                GroupType.RUSSIA.getTypedValue()),
                        "Московская таймзона (имя будет без сдвига)"
                },
                {
                        new Locale("uk"),
                        getTimezone(130L, "Europe/Moscow",
                                null, null, "Москва", null),
                        getTimezoneGroup(130L, "Europe/Moscow", "Москва",
                                GroupType.RUSSIA.getTypedValue()),
                        "Московская таймзона (имя будет без сдвига, и это актуально для других языков)"
                },
                {
                        new Locale("ar"), // Arabic
                        getTimezone(130L, "Europe/Moscow",
                                "Москва", null, null, null),
                        getTimezoneGroup(130L, "Europe/Moscow", "Москва",
                                GroupType.RUSSIA.getTypedValue()),
                        "Дефолтный язык — русский"
                }
        });
    }

    private static GeoTimezone getTimezone(Long id, String timezone,
                                           @Nullable String nameRu,
                                           @Nullable String nameEn,
                                           @Nullable String nameUa,
                                           @Nullable String nameTr) {
        return new GeoTimezone()
                .withTimezoneId(id).withTimezone(ZoneId.of(timezone))
                .withGroupType(GroupType.RUSSIA)
                .withNameRu(nameRu).withNameEn(nameEn).withNameUa(nameUa).withNameTr(nameTr);
    }

    private static GdTimezoneGroup getTimezoneGroup(Long id, String timezone, String name, String groupNick) {
        GdTimezone expectedTimezone = new GdTimezone()
                .withId(id)
                .withTimezone(timezone)
                .withName(name);
        return new GdTimezoneGroup().withGroupNick(groupNick).withTimezones(List.of(expectedTimezone));
    }

}
