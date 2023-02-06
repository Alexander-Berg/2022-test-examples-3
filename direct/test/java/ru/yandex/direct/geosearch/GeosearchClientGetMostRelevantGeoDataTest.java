package ru.yandex.direct.geosearch;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.geosearch.model.Address;
import ru.yandex.direct.geosearch.model.GeoObject;
import ru.yandex.direct.geosearch.model.Kind;
import ru.yandex.direct.geosearch.model.Precision;
import ru.yandex.direct.tvm.TvmService;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.geosearch.GeosearchClient.ALLOWABLE_KINDS;
import static ru.yandex.direct.geosearch.GeosearchClient.DEFAULT_GEO_OBJECT;


@RunWith(Parameterized.class)
public class GeosearchClientGetMostRelevantGeoDataTest {
    @Parameterized.Parameter
    public Address address;
    @Parameterized.Parameter(value = 1)
    public List<GeoObject> getGeoDataResult;
    @Parameterized.Parameter(value = 2)
    public GeoObject expectedGeoObject;
    private GeosearchClient geosearchClient;

    private static GeoObject.Builder defaultGeoObjectBuilder() {
        return new GeoObject.Builder()
                .withX(1.0).withY(2.0)
                .withX1(3.0).withY1(4.0)
                .withX2(5.0).withY2(6.0)
                .withPrecision(Precision.EXACT)
                .withKind(Kind.HOUSE);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Stream.of(
                // Проверяем, что если ничего не задано, то возвращается default-ый GeoObject
                Stream.<Object[]>of(
                        new Object[]{
                                new Address(),
                                Collections.emptyList(),
                                GeosearchClient.DEFAULT_GEO_OBJECT
                        }),

                // Проверяем что если выполняется сортировка, что выбирается точка с лучшим precision-ом
                Stream.<Object[]>of(
                        new Object[]{
                                new Address(),
                                Arrays.asList(
                                        defaultGeoObjectBuilder().withPrecision(Precision.EXACT).build(),
                                        defaultGeoObjectBuilder().withPrecision(Precision.NUMBER).build()),
                                defaultGeoObjectBuilder().withPrecision(Precision.EXACT).build()
                        }),

                // Проверяем что фильтруется по kind-у
                EnumSet.complementOf(ALLOWABLE_KINDS)
                        .stream()
                        .map(kind -> new Object[]{
                                new Address(),
                                Arrays.asList(
                                        defaultGeoObjectBuilder()
                                                .withPrecision(
                                                        Precision.EXACT) // EXACT более точнее, поэтому без фильтра по kind должен выбираться он
                                                .withKind(kind)
                                                .build(),
                                        defaultGeoObjectBuilder().withPrecision(Precision.NUMBER).build()),
                                defaultGeoObjectBuilder().withPrecision(Precision.NUMBER).build()
                        }),

                // Проверяем что учитывается город в адресе без административного округа
                Stream.of(
                        new Object[]{
                                new Address().withCity("Москва"),
                                singletonList(defaultGeoObjectBuilder().withCity("москва").build()),
                                defaultGeoObjectBuilder().withCity("москва").build()
                        },
                        new Object[]{
                                new Address().withCity("Орёл"),
                                singletonList(defaultGeoObjectBuilder().withCity("Орел").build()),
                                defaultGeoObjectBuilder().withCity("Орел").build()
                        },
                        new Object[]{
                                new Address().withCity("Орел"),
                                singletonList(defaultGeoObjectBuilder().withCity("Орёл").build()),
                                defaultGeoObjectBuilder().withCity("Орёл").build()
                        },
                        new Object[]{
                                new Address().withCity("Москва"),
                                singletonList(defaultGeoObjectBuilder().withCity("Орёл").build()),
                                DEFAULT_GEO_OBJECT
                        },
                        new Object[]{
                                new Address().withCity("Москва"),
                                singletonList(defaultGeoObjectBuilder().build()),
                                DEFAULT_GEO_OBJECT
                        }),

                // Проверяем что учитывается город в адресе с учетом административного округа
                Stream.of(
                        new Object[]{
                                new Address().withCity("Москва  (Москва)"),
                                singletonList(
                                        defaultGeoObjectBuilder().withCity("Москва").build()),
                                defaultGeoObjectBuilder().withCity("Москва").build()
                        },
                        new Object[]{
                                new Address().withCity("Москва  (Москва)"),
                                singletonList(
                                        defaultGeoObjectBuilder()
                                                .withCity("Москва").withAdministrativeArea("Москва")
                                                .build()),
                                defaultGeoObjectBuilder()
                                        .withCity("Москва").withAdministrativeArea("Москва")
                                        .build()
                        },
                        new Object[]{
                                new Address().withCity("Москва  (Москва)"),
                                singletonList(
                                        defaultGeoObjectBuilder()
                                                .withCity("Москва").withAdministrativeArea("Моск. область")
                                                .build()),
                                DEFAULT_GEO_OBJECT
                        }))
                .flatMap(s -> s)
                .collect(toList());
    }

    @Before
    public void setUp() {
        GeosearchClientSettings settings = new GeosearchClientSettings("http://geocoder",
                "some-origin", 3, Duration.ofSeconds(1), 1, 1);
        geosearchClient = spy(new GeosearchClient(settings, TvmService.DUMMY, mock(ParallelFetcherFactory.class), null));
    }

    @Test
    public void test() {
        doReturn(getGeoDataResult).when(geosearchClient).searchAddress((Address) any());

        GeoObject actualGeoObject = geosearchClient.getMostRelevantGeoData(address);

        assertThat(actualGeoObject).isEqualToComparingFieldByField(expectedGeoObject);
    }
}
