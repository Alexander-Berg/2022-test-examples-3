package ru.yandex.direct.regions;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;

@RunWith(Parameterized.class)
public class MetroGetCityGeoIdTest {
    private static final Long NOT_MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID = MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID + 1;

    @Parameterized.Parameter
    public Long metroId;
    @Parameterized.Parameter(value = 1)
    public Region region;
    @Parameterized.Parameter(value = 2)
    public Long expectedCityGeoId;

    private static Region region(Long id) {
        return new Region(id, 0, "", "", "", "", false);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{
                        1L,
                        region(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                        MOSCOW_REGION_ID},
                new Object[]{
                        2L,
                        region(NOT_MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                        NOT_MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID});
    }

    @Test
    public void test() {
        long actualCityCeoId = new Metro(metroId, region, "Metro-" + metroId).getCityGeoId();

        assertThat(actualCityCeoId, equalTo(expectedCityGeoId));
    }
}
