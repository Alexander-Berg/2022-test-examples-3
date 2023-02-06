package ru.yandex.direct.geosearch;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.geosearch.model.AddressComponent;
import ru.yandex.direct.geosearch.model.GeoObject;
import ru.yandex.direct.geosearch.model.Kind;
import ru.yandex.direct.tvm.TvmService;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(Parameterized.class)
public class GeosearchClientGetMostExactGeoObjectOfKindTest {

    @Parameterized.Parameter
    public Kind kind;
    @Parameterized.Parameter(1)
    public List<GeoObject> getGeoDataResult;
    @Parameterized.Parameter(2)
    public GeoObject expectedGeoObject;

    private GeosearchClient geosearchClient;

    private static GeoObject.Builder defaultGeoObjectBuilder() {
        return new GeoObject.Builder()
                .withX(1.0).withY(2.0)
                .withX1(3.0).withY1(4.0)
                .withX2(5.0).withY2(6.0);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                // Если геокодер вернул пустой ответ, то результатом будет пустой Optional
                {
                        Kind.HOUSE,
                        Collections.emptyList(),
                        null
                },

                // Если в ответе отсутствует адрес с компонентом заданного типа, то результатом будет пустой Optional
                {
                        Kind.HOUSE,
                        Arrays.asList(
                                defaultGeoObjectBuilder().withKind(Kind.COUNTRY).build(),
                                defaultGeoObjectBuilder().withKind(Kind.LOCALITY).build()),
                        null
                },
                {
                        Kind.STREET,
                        Arrays.asList(
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр фед округ"),
                                                new AddressComponent(Kind.PROVINCE, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.LOCALITY)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр фед округ"),
                                                new AddressComponent(Kind.PROVINCE, "Москва"),
                                                new AddressComponent(Kind.LOCALITY, "Москва")
                                        )).build()),
                        null
                },

                // Если в ответе присутствует одна иерархия адресов с компонентом заданного типа,
                // но при этом в ней нет адреса заданного типа, то результатом будет пустой Optional
                // (по идее в жизни такого не должно происходить)
                {
                        Kind.PROVINCE,
                        Arrays.asList(
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.COUNTRY)
                                        .withComponents(singletonList(
                                                new AddressComponent(Kind.COUNTRY, "Россия")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.LOCALITY)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Москва"),
                                                new AddressComponent(Kind.LOCALITY, "Москва")
                                        )).build()),
                        null
                },

                // Если в ответе присутствует одна иерархия адресов с компонентом заданного типа,
                // и в ней есть один адрес заданного типа, то результатом будет адрес заданного типа
                // (длина искомого адреса больше 1)
                {
                        Kind.PROVINCE,
                        Arrays.asList(
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.LOCALITY)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Москва"),
                                                new AddressComponent(Kind.LOCALITY, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.COUNTRY)
                                        .withComponents(singletonList(
                                                new AddressComponent(Kind.COUNTRY, "Россия")
                                        )).build()),

                        defaultGeoObjectBuilder()
                                .withKind(Kind.PROVINCE)
                                .withComponents(asList(
                                        new AddressComponent(Kind.COUNTRY, "Россия"),
                                        new AddressComponent(Kind.PROVINCE, "Москва")
                                )).build(),
                },

                // Если в ответе присутствует одна иерархия адресов с компонентом заданного типа,
                // и в ней есть один адрес заданного типа, то результатом будет адрес заданного типа
                // (длина искомого адреса равна 1)
                {
                        Kind.COUNTRY,
                        Arrays.asList(
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.LOCALITY)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Москва"),
                                                new AddressComponent(Kind.LOCALITY, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.COUNTRY)
                                        .withComponents(singletonList(
                                                new AddressComponent(Kind.COUNTRY, "Россия")
                                        )).build()),

                        defaultGeoObjectBuilder()
                                .withKind(Kind.COUNTRY)
                                .withComponents(singletonList(
                                        new AddressComponent(Kind.COUNTRY, "Россия")
                                )).build(),
                },

                // Если в ответе присутствует одна иерархия адресов с компонентом заданного типа,
                // и в ней есть один адрес заданного типа, то результатом будет адрес заданного типа
                // (длина искомого адреса равна самому длинному адресу)
                {
                        Kind.LOCALITY,
                        Arrays.asList(
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.LOCALITY)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Москва"),
                                                new AddressComponent(Kind.LOCALITY, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.COUNTRY)
                                        .withComponents(singletonList(
                                                new AddressComponent(Kind.COUNTRY, "Россия")
                                        )).build()),

                        defaultGeoObjectBuilder()
                                .withKind(Kind.LOCALITY)
                                .withComponents(asList(
                                        new AddressComponent(Kind.COUNTRY, "Россия"),
                                        new AddressComponent(Kind.PROVINCE, "Москва"),
                                        new AddressComponent(Kind.LOCALITY, "Москва")
                                )).build(),
                },

                // Если в ответе присутствует одна иерархия адресов с компонентом заданного типа,
                // и в ней есть несколько адресов заданного типа, то результатом будет
                // самый длинный адрес заданного типа
                {
                        Kind.PROVINCE,
                        Arrays.asList(
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.LOCALITY)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                                new AddressComponent(Kind.PROVINCE, "Москва"),
                                                new AddressComponent(Kind.LOCALITY, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                                new AddressComponent(Kind.PROVINCE, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.COUNTRY)
                                        .withComponents(singletonList(
                                                new AddressComponent(Kind.COUNTRY, "Россия")
                                        )).build()),

                        defaultGeoObjectBuilder()
                                .withKind(Kind.PROVINCE)
                                .withComponents(asList(
                                        new AddressComponent(Kind.COUNTRY, "Россия"),
                                        new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                        new AddressComponent(Kind.PROVINCE, "Москва")
                                )).build(),
                },

                // Если в ответе присутствует одна иерархия адресов с компонентом заданного типа,
                // и в ней есть несколько адресов заданного типа, то результатом будет
                // самый длинный адрес заданного типа
                // (дублирует предыдущий кейс, но с изменением порядка объектов)
                {
                        Kind.PROVINCE,
                        Arrays.asList(
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.LOCALITY)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                                new AddressComponent(Kind.PROVINCE, "Москва"),
                                                new AddressComponent(Kind.LOCALITY, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                                new AddressComponent(Kind.PROVINCE, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.COUNTRY)
                                        .withComponents(singletonList(
                                                new AddressComponent(Kind.COUNTRY, "Россия")
                                        )).build()),

                        defaultGeoObjectBuilder()
                                .withKind(Kind.PROVINCE)
                                .withComponents(asList(
                                        new AddressComponent(Kind.COUNTRY, "Россия"),
                                        new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                        new AddressComponent(Kind.PROVINCE, "Москва")
                                )).build(),
                },

                // Если в ответе присутствует две иерархии адресов одинаковой длины с компонентом заданного типа,
                // и в каждой из них есть по одному адресу заданного типа одинаковой длины,
                // то будет выбран адрес заданного типа из первой иерархии
                {
                        Kind.PROVINCE,
                        Arrays.asList(
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Запад. Фед. Округ")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.COUNTRY)
                                        .withComponents(singletonList(
                                                new AddressComponent(Kind.COUNTRY, "Россия")
                                        )).build()),

                        defaultGeoObjectBuilder()
                                .withKind(Kind.PROVINCE)
                                .withComponents(asList(
                                        new AddressComponent(Kind.COUNTRY, "Россия"),
                                        new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ")
                                )).build(),
                },

                // Если в ответе присутствует две иерархии адресов одинаковой длины с компонентом заданного типа,
                // и в каждой из них есть по два адреса заданного типа одинаковой длины,
                // то будет выбран адрес заданного типа из первой иерархии
                {
                        Kind.PROVINCE,
                        Arrays.asList(
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                                new AddressComponent(Kind.PROVINCE, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.COUNTRY)
                                        .withComponents(singletonList(
                                                new AddressComponent(Kind.COUNTRY, "Россия")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                                new AddressComponent(Kind.PROVINCE, "Московская область")
                                        )).build()),

                        defaultGeoObjectBuilder()
                                .withKind(Kind.PROVINCE)
                                .withComponents(asList(
                                        new AddressComponent(Kind.COUNTRY, "Россия"),
                                        new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                        new AddressComponent(Kind.PROVINCE, "Москва")
                                )).build(),
                },

                // Если в ответе присутствует две иерархии адресов разной длины с компонентом заданного типа,
                // и в каждой из них есть по одному адресу заданного типа одинаковой длины,
                // то будет выбран адрес заданного типа из самой длинной иерархии
                {
                        Kind.PROVINCE,
                        Arrays.asList(
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.LOCALITY)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                                new AddressComponent(Kind.PROVINCE, "Москва"),
                                                new AddressComponent(Kind.LOCALITY, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                                new AddressComponent(Kind.PROVINCE, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.COUNTRY)
                                        .withComponents(singletonList(
                                                new AddressComponent(Kind.COUNTRY, "Россия")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                                new AddressComponent(Kind.PROVINCE, "Московская область")
                                        )).build()),

                        defaultGeoObjectBuilder()
                                .withKind(Kind.PROVINCE)
                                .withComponents(asList(
                                        new AddressComponent(Kind.COUNTRY, "Россия"),
                                        new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                        new AddressComponent(Kind.PROVINCE, "Москва")
                                )).build(),
                },

                // Если в ответе присутствует две иерархии адресов разной длины с компонентом заданного типа,
                // и в каждой из них есть по одному адресу заданного типа одинаковой длины,
                // то будет выбран адрес заданного типа из самой длинной иерархии
                // (дублирующий кейс для проверки, что результат не зависит от порядка объектов)
                {
                        Kind.PROVINCE,
                        Arrays.asList(
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                                new AddressComponent(Kind.PROVINCE, "Московская область")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.LOCALITY)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                                new AddressComponent(Kind.PROVINCE, "Москва"),
                                                new AddressComponent(Kind.LOCALITY, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                                new AddressComponent(Kind.PROVINCE, "Москва")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.PROVINCE)
                                        .withComponents(asList(
                                                new AddressComponent(Kind.COUNTRY, "Россия"),
                                                new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ")
                                        )).build(),
                                defaultGeoObjectBuilder()
                                        .withKind(Kind.COUNTRY)
                                        .withComponents(singletonList(
                                                new AddressComponent(Kind.COUNTRY, "Россия")
                                        )).build()),

                        defaultGeoObjectBuilder()
                                .withKind(Kind.PROVINCE)
                                .withComponents(asList(
                                        new AddressComponent(Kind.COUNTRY, "Россия"),
                                        new AddressComponent(Kind.PROVINCE, "Центр. Фед. Округ"),
                                        new AddressComponent(Kind.PROVINCE, "Москва")
                                )).build(),
                },
        });
    }

    @Before
    public void setUp() {
        GeosearchClientSettings settings = new GeosearchClientSettings("http://geocoder",
                "some-origin", 3, Duration.ofSeconds(1), 1, 1);
        geosearchClient = spy(new GeosearchClient(settings, TvmService.DUMMY, mock(ParallelFetcherFactory.class), null));
    }

    @Test
    public void test() {
        doReturn(getGeoDataResult).when(geosearchClient).searchAddress((String) any());
        GeoObject actualGeoObject = geosearchClient.getMostExactGeoObjectOfKind("123,123", kind)
                .orElse(null);

        if (expectedGeoObject != null) {
            assertThat(actualGeoObject).isEqualToComparingFieldByField(expectedGeoObject);
        } else {
            assertThat(actualGeoObject).isNull();
        }
    }
}
