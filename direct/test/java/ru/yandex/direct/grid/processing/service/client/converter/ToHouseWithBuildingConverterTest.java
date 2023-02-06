package ru.yandex.direct.grid.processing.service.client.converter;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.service.client.converter.VcardDataConverter.BUILDING_SYMBOL;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class ToHouseWithBuildingConverterTest {

    @Parameterized.Parameter
    public String house;

    @Parameterized.Parameter(1)
    public String building;

    @Parameterized.Parameter(2)
    public String expectedResult;

    @Parameterized.Parameters(name = "house={0}, building={1}, expectedResult={2}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {null, null, null},
                {null, "bla bla", null},
                {"14", null, "14"},
                {"2", "2Щ", "2" + BUILDING_SYMBOL + "2Щ"},
                {"84", "А", "84А"},
        });
    }


    @Test
    public void toHouseWithBuildTest() {
        String result = VcardDataConverter.toHouseWithBuilding(house, building);

        assertThat(result)
                .isEqualTo(expectedResult);
    }

}
