package ru.yandex.direct.core.entity.vcard.service.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.GeoTreeType;
import ru.yandex.direct.regions.SimpleGeoTreeFactory;
import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.vcard.service.validation.MetroIdValidator.DefectDefinitions.invalidMetro;
import static ru.yandex.direct.core.entity.vcard.service.validation.MetroIdValidator.DefectDefinitions.metroCityIsRequired;

@RunWith(Parameterized.class)
public class MetroIdValidatorTest {
    private static final String SOME_CITY = "city";
    private static final Long VALID_METRO_ID = 1L;
    private static final Long INVALID_METRO_ID = 2L;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    public GeoTree geoTree;

    private GeoTreeFactory geoTreeFactory;

    @Parameterized.Parameter
    public Long metroId;
    @Parameterized.Parameter(value = 1)
    public String city;
    @Parameterized.Parameter(value = 2)
    public Defect expectedDefect;

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{0L, SOME_CITY, null},
                new Object[]{VALID_METRO_ID, SOME_CITY, null},
                new Object[]{VALID_METRO_ID, null, metroCityIsRequired()},
                new Object[]{INVALID_METRO_ID, SOME_CITY, invalidMetro()});
    }

    @Before
    public void setUp() {
        when(geoTree.isCityHasMetro(eq(SOME_CITY), eq(VALID_METRO_ID)))
                .thenReturn(true);
        when(geoTree.isCityHasMetro(eq(SOME_CITY), eq(INVALID_METRO_ID)))
                .thenReturn(false);
        geoTreeFactory = new SimpleGeoTreeFactory(Map.of(GeoTreeType.GLOBAL, geoTree));
    }

    @Test
    public void test() {
        Defect actualDefect = new MetroIdValidator(geoTreeFactory)
                .createConstraintFor(city)
                .apply(metroId);
        assertThat(actualDefect).isEqualTo(expectedDefect);
    }
}
