package ru.yandex.direct.api.v5.entity.dictionaries;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.yandex.direct.api.v5.dictionaries.GetRequest;
import com.yandex.direct.api.v5.dictionaries.GetResponse;
import com.yandex.direct.api.v5.dictionaries.TimeZonesItem;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.timetarget.model.GeoTimezone;
import ru.yandex.direct.core.entity.timetarget.repository.GeoTimezoneRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.qatools.allure.annotations.Description;

import static com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum.TIME_ZONES;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.timetarget.model.GroupType.CIS;
import static ru.yandex.direct.core.entity.timetarget.model.GroupType.RUSSIA;
import static ru.yandex.direct.core.entity.timetarget.model.GroupType.WORLD;
import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;
import static ru.yandex.direct.utils.DateTimeUtils.MSK;

@Api5Test
@RunWith(Parameterized.class)
@Description("Получение таймзон")
public class GetTimezonesTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public GeoTimezone geoTimezone;

    @Parameterized.Parameter(2)
    public TimeZonesItem timeZonesItem;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Регион Москва",
                        new GeoTimezone()
                                .withTimezone(MSK)
                                .withNameEn("Moscow")
                                .withGroupType(RUSSIA),
                        new TimeZonesItem()
                                .withTimeZone(MOSCOW_TIMEZONE)
                                .withTimeZoneName("Moscow")
                                .withUtcOffset(10800)
                },
                {"Регион Россия",
                        new GeoTimezone()
                                .withTimezone(ZoneId.of("Asia/Yekaterinburg"))
                                .withNameEn("Yekaterinburg")
                                .withGroupType(RUSSIA),
                        new TimeZonesItem()
                                .withTimeZone("Asia/Yekaterinburg")
                                .withTimeZoneName("Yekaterinburg (MSK +02:00)")
                                .withUtcOffset(18000)
                },
                {"Регион СНГ",
                        new GeoTimezone()
                                .withTimezone(ZoneId.of("Europe/Minsk"))
                                .withNameEn("Belarus")
                                .withGroupType(CIS),
                        new TimeZonesItem()
                                .withTimeZone("Europe/Minsk")
                                .withTimeZoneName("Belarus (MSK +00:00, GMT +03:00)")
                                .withUtcOffset(10800)
                },
                {"Регион Мир",
                        new GeoTimezone()
                                .withTimezone(ZoneId.of("Asia/Tokyo"))
                                .withNameEn("Japan")
                                .withGroupType(WORLD),
                        new TimeZonesItem()
                                .withTimeZone("Asia/Tokyo")
                                .withTimeZoneName("Japan (GMT +09:00)")
                                .withUtcOffset(32400)
                },
        };
        return Arrays.asList(data);
    }

    private DictionariesService dictionariesService;

    @Before
    public void before() {
        openMocks(this);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        GeoTimezoneRepository geoTimezoneRepository = mock(GeoTimezoneRepository.class);

        when(geoTimezoneRepository.getGeoTimezonesByTimezoneIds(emptyList()))
                .thenReturn(Collections.singleton(geoTimezone));

        dictionariesService =
                new DictionariesServiceBuilder(steps.applicationContext())
                        .withClientAuth(clientInfo)
                        .withGeoTimezoneRepository(geoTimezoneRepository)
                        .build();
    }

    @Test
    @Description("Таймзона присутствует")
    public void get_timezoneFoundTest() {
        GetResponse response = dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(TIME_ZONES)));
        List<TimeZonesItem> timeZones = response.getTimeZones();

        assertThat(timeZones, contains(beanDiffer(timeZonesItem)));
    }
}
