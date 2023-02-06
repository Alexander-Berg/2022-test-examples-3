package ru.yandex.direct.jobs.placements;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.entity.placements.model1.PlacementBlockKey;
import ru.yandex.direct.geosearch.GeosearchClient;
import ru.yandex.direct.geosearch.model.AddressComponent;
import ru.yandex.direct.geosearch.model.GeoObject;
import ru.yandex.direct.geosearch.model.Kind;
import ru.yandex.direct.geosearch.model.Lang;
import ru.yandex.direct.i18n.Language;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithCoordinates;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;

public class EnrichPlacementsAddressDetectorTest {

    private GeosearchClient geosearchClient;

    private EnrichPlacementsAddressDetector addressDetector;

    @BeforeEach
    public void before() {
        geosearchClient = mock(GeosearchClient.class);
        defaultGeocoderResponse(geosearchClient);
        addressDetector = new EnrichPlacementsAddressDetector(geosearchClient);
    }

    @Test
    public void detectAddresses_NullBlockCoordinates() {
        OutdoorBlock block = outdoorBlockWithCoordinates(1L, 2L, null);
        Map<PlacementBlockKey, Map<Language, String>> translations = addressDetector.detectAddressesTranslations(singleton(block));
        assertThat(translations).isEmpty();
    }

    @Test
    public void detectAddresses_HouseResponseNotEmpty() {
        doAnswer(invocation -> {
            Lang lang = invocation.getArgument(2);
            return singletonList(defaultGeoObject(Kind.HOUSE)
                    .withName("Address house " + lang.getLocale())
                    .build());
        }).when(geosearchClient).searchReverse(any(), eq(Kind.HOUSE), any());

        OutdoorBlock block = outdoorBlockWithOneSize(1L, 2L);
        Map<Language, String> translations = addressDetector.detectAddressesTranslations(singleton(block))
                .get(PlacementBlockKey.of(1L, 2L));

        assertThat(translations).isEqualTo(ImmutableMap.of(
                Language.RU, "Address house ru_RU",
                Language.EN, "Address house en_RU",
                Language.UK, "Address house uk_UA",
                Language.TR, "Address house tr_TR"
        ));
    }

    @Test
    public void detectAddresses_StreetResponseNotEmpty() {
        doReturn(emptyList()).when(geosearchClient).searchReverse(any(), eq(Kind.HOUSE), any());
        doAnswer(invocation -> {
            Lang lang = invocation.getArgument(2);
            return singletonList(defaultGeoObject(Kind.STREET)
                    .withName("Address street " + lang.getLocale())
                    .build());
        }).when(geosearchClient).searchReverse(any(), eq(Kind.STREET), any());

        OutdoorBlock block = outdoorBlockWithOneSize(1L, 2L);
        Map<Language, String> translations = addressDetector.detectAddressesTranslations(singleton(block))
                .get(PlacementBlockKey.of(1L, 2L));

        assertThat(translations).isEqualTo(ImmutableMap.of(
                Language.RU, "Address street ru_RU",
                Language.EN, "Address street en_RU",
                Language.UK, "Address street uk_UA",
                Language.TR, "Address street tr_TR"
        ));
    }

    @Test
    public void detectAddresses_NoKindResponseNotEmpty() {
        doReturn(emptyList()).when(geosearchClient).searchReverse(any(), eq(Kind.HOUSE), any());
        doReturn(emptyList()).when(geosearchClient).searchReverse(any(), eq(Kind.STREET), any());
        doAnswer(invocation -> {
            Lang lang = invocation.getArgument(2);
            return singletonList(defaultGeoObject(Kind.COUNTRY)
                    .withName("Address no kind " + lang.getLocale())
                    .build());
        }).when(geosearchClient).searchReverse(any(), isNull(), any());

        OutdoorBlock block = outdoorBlockWithOneSize(1L, 2L);
        Map<Language, String> translations = addressDetector.detectAddressesTranslations(singleton(block))
                .get(PlacementBlockKey.of(1L, 2L));

        assertThat(translations).isEqualTo(ImmutableMap.of(
                Language.RU, "Address no kind ru_RU",
                Language.EN, "Address no kind en_RU",
                Language.UK, "Address no kind uk_UA",
                Language.TR, "Address no kind tr_TR"
        ));
    }

    @Test
    public void detectAddresses_EmptyResponse() {
        doAnswer(invocation -> {
            Kind kind = invocation.getArgument(1);
            Lang lang = invocation.getArgument(2);
            return singletonList(defaultGeoObject(kind)
                    .withName("Address " + lang.getLocale())
                    .build());
        }).when(geosearchClient).searchReverse(any(), any(), any());
        doReturn(emptyList()).when(geosearchClient).searchReverse(any(), any(), eq(Lang.TR));

        OutdoorBlock block = outdoorBlockWithOneSize(1L, 2L);
        Map<Language, String> translations = addressDetector.detectAddressesTranslations(singleton(block))
                .get(PlacementBlockKey.of(1L, 2L));

        assertThat(translations).isEqualTo(ImmutableMap.of(
                Language.RU, "Address ru_RU",
                Language.EN, "Address en_RU",
                Language.UK, "Address uk_UA"
        ));
    }

    @Test
    public void detectAddresses_StreetWithoutHousesIsNotPicked() {
        doReturn(emptyList()).when(geosearchClient).searchReverse(any(), eq(Kind.HOUSE), any());
        doReturn(List.of(
                defaultGeoObject(Kind.STREET)
                        .withHouses(0L)
                        .withName("Address street without houses")
                        .build(),
                defaultGeoObject(Kind.STREET)
                        .withHouses(100L)
                        .withName("Address street with houses")
                        .build()
        )).when(geosearchClient).searchReverse(any(), eq(Kind.STREET), any());

        OutdoorBlock block = outdoorBlockWithOneSize(1L, 2L);
        Map<Language, String> translations = addressDetector.detectAddressesTranslations(singleton(block))
                .get(PlacementBlockKey.of(1L, 2L));
        String ruTranslation = translations.get(Language.RU);

        assertThat(ruTranslation).isEqualTo("Address street with houses");
    }

    @Test
    public void detectAddresses_HouseStreetAndStreetDiffer() {
        doReturn(singletonList(defaultGeoObject(Kind.HOUSE)
                .withName("Address house")
                .withComponents(singletonList(
                        new AddressComponent(Kind.STREET, "House street")
                ))
                .build()
        )).when(geosearchClient).searchReverse(any(), eq(Kind.HOUSE), any());
        doReturn(singletonList(defaultGeoObject(Kind.STREET)
                .withName("Address street")
                .withComponents(singletonList(
                        new AddressComponent(Kind.STREET, "Street street")
                ))
                .build()
        )).when(geosearchClient).searchReverse(any(), eq(Kind.STREET), any());

        OutdoorBlock block = outdoorBlockWithOneSize(1L, 2L);
        Map<Language, String> translations = addressDetector.detectAddressesTranslations(singleton(block))
                .get(PlacementBlockKey.of(1L, 2L));
        String ruTranslation = translations.get(Language.RU);

        assertThat(ruTranslation).isEqualTo("Address street (Address house)");
    }

    private void defaultGeocoderResponse(GeosearchClient geosearchClient) {
        doAnswer(invocation -> {
            Kind kind = invocation.getArgument(1);
            return singletonList(defaultGeoObject(kind).build());
        }).when(geosearchClient).searchReverse(any(), any(), any());
    }

    private GeoObject.Builder defaultGeoObject(Kind kind) {
        return new GeoObject.Builder()
                .withKind(kind)
                .withName("Address name")
                .withHouses(100L)
                .withComponents(singletonList(
                        new AddressComponent(Kind.STREET, "Address street")
                ));
    }
}
