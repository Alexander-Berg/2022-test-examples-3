package ru.yandex.direct.api.v5.entity.dictionaries;

import java.util.List;
import java.util.stream.Collectors;

import com.yandex.direct.api.v5.dictionaries.GeoRegionsItem;
import com.yandex.direct.api.v5.dictionaries.GetRequest;
import com.yandex.direct.api.v5.dictionaries.GetResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.qatools.allure.annotations.Description;

import static com.yandex.direct.api.v5.dictionaries.DictionaryNameEnum.GEO_REGIONS;
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
@Description("Получение регионов")
public class GetGeoRegionsTest {

    @Autowired
    private Steps steps;

    private List<GeoRegionsItem> regions;

    @Before
    public void before() {
        openMocks(this);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        DictionariesService dictionariesService =
                new DictionariesServiceBuilder(steps.applicationContext())
                        .withClientAuth(clientInfo)
                        .build();

        GetResponse response =
                dictionariesService.get(new GetRequest().withDictionaryNames(singletonList(GEO_REGIONS)));
        regions = response.getGeoRegions();

        assumeThat("Получен словарь регионов", regions, hasSize(greaterThan(0)));
    }

    @Test
    @Description("Идентификаторы регионов присутствуют")
    public void get_idsAreFilledTest() {
        List<Long> values = regions.stream().map(GeoRegionsItem::getGeoRegionId).collect(Collectors.toList());

        assertThat(values, everyItem(notNullValue(Long.class)));
    }

    @Test
    @Description("Наименования регионов присутствуют")
    public void get_namesAreFilledTest() {
        List<String> values = regions.stream().map(GeoRegionsItem::getGeoRegionName).collect(Collectors.toList());

        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("Типы регионов присутствуют")
    public void get_typesAreFilledTest() {
        List<String> values = regions.stream().map(GeoRegionsItem::getGeoRegionType).collect(Collectors.toList());

        assertThat(values, everyItem(not(isEmptyOrNullString())));
    }

    @Test
    @Description("Идентификаторы родителей у всех регионов кроме Мира заполнены")
    public void get_parentsAreFilledTest() {
        List<Long> values = regions.stream()
                .filter(region -> region.getGeoRegionId() != 0)
                .map(GeoRegionsItem::getParentId)
                .collect(Collectors.toList());

        assertThat(values, everyItem(notNullValue(Long.class)));
    }

    @Test
    @Description("Регион Мир присутствует")
    public void get_worldFound() {
        GeoRegionsItem region = new GeoRegionsItem()
                .withGeoRegionId(0L)
                .withParentId(null)
                .withGeoRegionType("World")
                .withGeoRegionName("All");

        assertThat(regions, hasItem(beanDiffer(region)));
    }

    @Test
    @Description("Прочий регион присутствует")
    public void get_regionFoundTest() {
        GeoRegionsItem region = new GeoRegionsItem()
                .withGeoRegionId(14L)
                .withParentId(10819L)
                .withGeoRegionType("City")
                .withGeoRegionName("Tver");

        assertThat(regions, hasItem(beanDiffer(region)));
    }
}
