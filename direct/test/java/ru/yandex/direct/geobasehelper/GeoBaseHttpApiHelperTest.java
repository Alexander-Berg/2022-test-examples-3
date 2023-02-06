package ru.yandex.direct.geobasehelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Ходит в реальную Геобазу")
public class GeoBaseHttpApiHelperTest {

    private GeoBaseHelper geoBaseHelper;

    @Before
    public void setUp() throws Exception {
        geoBaseHelper = new GeoBaseHttpApiHelper(null);
    }

    @Test
    public void getRegionName() {
        String regionTrName = geoBaseHelper.getRegionName(213L, "TR");
        //noinspection SpellCheckingInspection
        assertThat(regionTrName).isEqualTo("Moskova");
    }
}
