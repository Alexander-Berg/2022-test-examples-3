package ru.yandex.market.api.integration;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.AddressV2;
import ru.yandex.market.api.domain.v2.GeoSuggest;
import ru.yandex.market.api.domain.v2.RegionField;
import ru.yandex.market.api.geo.GeoAddressService;
import ru.yandex.market.api.geo.domain.AddressSuggestV2;
import ru.yandex.market.api.geo.domain.RegionType;
import ru.yandex.market.api.matchers.GeoSuggestMatcher;
import ru.yandex.market.api.matchers.RegionV2Matcher;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.MapsSuggestTestClient;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;

/**
 * Created by tesseract on 04.10.16.
 */
public class GeoAddressServiceTest extends BaseTest {

    @Inject
    GeoAddressService mapsService;

    @Inject
    MapsSuggestTestClient mapsSuggestTestClient;

    /**
     * Проверяем правильнось работы v2.0.1/geo/addresses/suggest?name_part=екатеринбург блюхера 12
     * <p>
     * Проверяем, что в результате содержится единственный адрес и поля regionId, country, region, subRegion, locality,
     * premiseNumber заполнены верно (как в ответе от GeoAddress) и то,
     * что адрес вернулся в формате v2.0.1
     */
    @Test
    public void getSuggest_V2_0_1() {
        // настройка системы
        context.setVersion(Version.V2_0_1);
        configureBluhera12WithSuggest();
        // вызов системы
        List<AddressV2> result = (List<AddressV2>) Futures.waitAndGet(mapsService.getSuggests("екатеринбург блюхера 12", null));
        // проверка утверждений
        Assert.assertEquals(1, result.size());

        AddressV2 address = result.get(0);
        Assert.assertEquals(0, address.getRegionId());
        Assert.assertEquals("улица Блюхера, 12, Екатеринбург, Свердловская область, Россия", address.getFullAddress());
        Assert.assertEquals("Екатеринбург, Свердловская область, Россия", ((AddressSuggestV2) address).getDesc());
        Assert.assertEquals("улица Блюхера, 12", ((AddressSuggestV2) address).getShortAddress());
        // очистка
    }

    @Test
    public void shouldEnrichRegion() {
        // настройка системы
        context.setVersion(Version.V2_0_1);
        mapsSuggestTestClient.getSuggest("Советск Киров", "suggest-maps__tune.json");
        List<GeoSuggest> suggests = Futures.waitAndGet(mapsService.getGeoSuggests("Советск Киров", 225, null,
            Collections.singletonList(RegionType.CITY), Collections.singletonList(RegionField.PARENT)));

        Assert.assertThat(suggests, hasSize(1));
        Assert.assertThat(
            suggests,
            contains(
                GeoSuggestMatcher.suggest(
                    GeoSuggestMatcher.id(11074),
                    GeoSuggestMatcher.name("Советск"),
                    GeoSuggestMatcher.fullName("Советск (Кировская область, Россия)"),
                    GeoSuggestMatcher.type(RegionType.CITY),
                    GeoSuggestMatcher.childCount(0),
                    GeoSuggestMatcher.countryInfo(
                        RegionV2Matcher.regionV2(
                            RegionV2Matcher.id(225),
                            RegionV2Matcher.name("Россия"),
                            RegionV2Matcher.type(RegionType.COUNTRY),
                            RegionV2Matcher.childCount(11)
                        )
                    ),
                    GeoSuggestMatcher.parent(
                        RegionV2Matcher.regionV2(
                            RegionV2Matcher.id(99507),
                            RegionV2Matcher.name("Советский район"),
                            RegionV2Matcher.type(RegionType.SUBJECT_FEDERATION_DISTRICT),
                            RegionV2Matcher.childCount(1),
                            RegionV2Matcher.countryInfo(
                                RegionV2Matcher.regionV2(
                                    RegionV2Matcher.id(225),
                                    RegionV2Matcher.name("Россия"),
                                    RegionV2Matcher.type(RegionType.COUNTRY),
                                    RegionV2Matcher.childCount(11)
                                )
                            )
                        )
                    )
                )
            )
        );
    }


    private void configureBluhera12WithSuggest() {
        mapsSuggestTestClient.getSuggest("екатеринбург блюхера 12", "mapsSuggest_Bluhera12.json");
    }
}
