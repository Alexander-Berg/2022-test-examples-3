package ru.yandex.direct.core.entity.adgeneration;

import java.time.ZoneId;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.timetarget.model.GeoTimezone;
import ru.yandex.direct.core.entity.timetarget.repository.GeoTimezoneRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.geobasehelper.GeoBaseHelper;
import ru.yandex.direct.geobasehelper.GeoBaseHelperStub;
import ru.yandex.direct.regions.GeoTreeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.DEFAULT_TIMEZONE;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID;
import static ru.yandex.direct.regions.Region.TURKEY_REGION_ID;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TimezoneGenerationServiceTest {
    private static final Long NUR_SULTAN_REGION_ID = 163L;
    private static final GeoTimezone ALMATY_TIMEZONE = new GeoTimezone()
            .withTimezoneId(123L)
            .withTimezone(ZoneId.of("Asia/Almaty"))
            .withRegionId(KAZAKHSTAN_REGION_ID);

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    private TimezoneGenerationService timezoneGenerationService;

    @Before
    public void before() {
        var geoBaseHelper = initGeoBaseHelper();
        var geoTimezoneRepository = mock(GeoTimezoneRepository.class);
        when(geoTimezoneRepository.getByCountryIdAndTimezone(
                eq(KAZAKHSTAN_REGION_ID), eq(ALMATY_TIMEZONE.getTimezone()))).thenReturn(ALMATY_TIMEZONE);
        timezoneGenerationService = new TimezoneGenerationService(geoBaseHelper, geoTimezoneRepository);
    }

    @Test
    public void generateTimezoneId_RegionIsNull() {
        Long timezoneId = timezoneGenerationService.generateTimezoneId(null);
        assertThat(timezoneId).isEqualTo(DEFAULT_TIMEZONE);
    }

    @Test
    public void generateTimezoneId_ChiefRegion() {
        Long timezoneId = timezoneGenerationService.generateTimezoneId(NUR_SULTAN_REGION_ID);
        assertThat(timezoneId).isEqualTo(ALMATY_TIMEZONE.getTimezoneId());
    }

    @Test
    public void generateTimezoneId_CountryRegion() {
        Long timezoneId = timezoneGenerationService.generateTimezoneId(KAZAKHSTAN_REGION_ID);
        assertThat(timezoneId).isEqualTo(ALMATY_TIMEZONE.getTimezoneId());
    }

    @Test
    public void generateTimezoneId_RegionNotFound() {
        Long timezoneId = timezoneGenerationService.generateTimezoneId(TURKEY_REGION_ID);
        assertThat(timezoneId).isEqualTo(DEFAULT_TIMEZONE);
    }

    private GeoBaseHelper initGeoBaseHelper() {
        var geoBaseHelper = new GeoBaseHelperStub(geoTreeFactory);
        geoBaseHelper.addTimezone(KAZAKHSTAN_REGION_ID, StringUtils.EMPTY);
        geoBaseHelper.addTimezone(NUR_SULTAN_REGION_ID, ALMATY_TIMEZONE.getTimezone().getId());

        geoBaseHelper.addChiefRegionId(KAZAKHSTAN_REGION_ID, NUR_SULTAN_REGION_ID);
        geoBaseHelper.addChiefRegionId(NUR_SULTAN_REGION_ID, GLOBAL_REGION_ID);

        geoBaseHelper.addRegionWithParent(NUR_SULTAN_REGION_ID, List.of((int) KAZAKHSTAN_REGION_ID));
        return geoBaseHelper;
    }
}
