package ru.yandex.direct.grid.processing.service.tools;

import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.geobasehelper.GeoBaseHelper;
import ru.yandex.direct.grid.processing.model.GdPhoneCodes;
import ru.yandex.direct.grid.processing.model.GdRegion;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class GeoToolsServiceGetPhoneCodesTest {
    private final static Integer CODE = RandomNumberUtils.nextPositiveInteger();
    private final static Integer OTHER_CODE = RandomNumberUtils.nextPositiveInteger();
    private final static Integer ANOTHER_CODE = RandomNumberUtils.nextPositiveInteger();

    @Mock
    public GeoBaseHelper geoBaseHelper;

    @InjectMocks
    public GeoToolsService geoToolsService;

    private static GdRegion region = new GdRegion();

    @Test
    public void getPhoneCodes_noCodes() {
        doReturn("").when(geoBaseHelper).getPhoneCodeByRegionId(any());
        GdPhoneCodes actual = geoToolsService.getPhoneCodeByRegionId(region.withRegionId(1));
        GdPhoneCodes expected = new GdPhoneCodes().withCodes(List.of());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getPhoneCodes_nullCodes() {
        doReturn(null).when(geoBaseHelper).getPhoneCodeByRegionId(any());
        GdPhoneCodes actual = geoToolsService.getPhoneCodeByRegionId(region.withRegionId(1));
        GdPhoneCodes expected = new GdPhoneCodes().withCodes(List.of());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getPhoneCodes_singleCode() {
        doReturn(String.valueOf(CODE)).when(geoBaseHelper).getPhoneCodeByRegionId(any());
        GdPhoneCodes actual = geoToolsService.getPhoneCodeByRegionId(region.withRegionId(1));
        GdPhoneCodes expected = new GdPhoneCodes().withCodes(List.of(CODE));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getPhoneCodes_severalCodes() {
        doReturn(StreamEx.of(CODE, OTHER_CODE, ANOTHER_CODE).map(String::valueOf).joining(" "))
                .when(geoBaseHelper).getPhoneCodeByRegionId(any());
        GdPhoneCodes actual = geoToolsService.getPhoneCodeByRegionId(region.withRegionId(1));
        GdPhoneCodes expected = new GdPhoneCodes().withCodes(List.of(CODE, OTHER_CODE, ANOTHER_CODE));
        assertThat(actual).isEqualTo(expected);
    }

}
