package ru.yandex.direct.api.v5.entity.dictionaries;

import java.util.List;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.dictionaries.GetRequest;
import com.yandex.direct.api.v5.dictionaries.GetResponse;
import com.yandex.direct.api.v5.dictionaries.MetroStationsItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.qatools.allure.annotations.Description;

import static com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum.METRO_STATIONS;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@Api5Test
@RunWith(SpringRunner.class)
@Description("Получение станций метро")
public class GetMetroStationsTest {

    @Autowired
    private Steps steps;

    private List<MetroStationsItem> metroStations;

    @Before
    public void before() {
        openMocks(this);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        DictionariesService dictionariesService =
                new DictionariesServiceBuilder(steps.applicationContext())
                        .withClientAuth(clientInfo)
                        .build();

        GetResponse response =
                dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(METRO_STATIONS)));
        metroStations = response.getMetroStations();

        assumeThat("получен словарь станций метро", metroStations, hasSize(greaterThan(0)));
    }

    @Test
    @Description("Идентификаторы регионов присутствуют")
    public void get_idRegionsAreFilledTest() {
        List<Long> values =
                metroStations.stream().map(MetroStationsItem::getGeoRegionId).collect(Collectors.toList());

        assertThat(values, everyItem(notNullValue(Long.class)));
    }

    @Test
    @Description("Идентификаторы станций метро присутствуют")
    public void get_idsAreFilledTest() {
        List<Long> values =
                metroStations.stream().map(MetroStationsItem::getMetroStationId).collect(Collectors.toList());

        assertThat(values, everyItem(notNullValue(Long.class)));
    }

    @Test
    @Description("Наименования станций метро присутствуют")
    public void get_namesAreFilledTest() {
        List<String> values =
                metroStations.stream().map(MetroStationsItem::getMetroStationName).collect(Collectors.toList());

        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("Cтанция метро присутствует")
    public void get_metroFoundTest() {
        MetroStationsItem metroStation = new MetroStationsItem();
        metroStation.setGeoRegionId(2L);
        metroStation.setMetroStationId(20331L);
        metroStation.setMetroStationName("Площадь Ленина");

        assertThat(metroStations, hasItem(beanDiffer(metroStation)));
    }
}
