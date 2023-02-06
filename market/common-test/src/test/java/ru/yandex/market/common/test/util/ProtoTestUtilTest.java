package ru.yandex.market.common.test.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import Market.NEntityMapper.EntityMapper;
import com.google.protobuf.ExtensionRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import yandex.maps.proto.common2.geo_object.GeoObjectOuterClass;
import yandex.maps.proto.common2.geometry.GeometryOuterClass;
import yandex.maps.proto.common2.metadata.MetadataOuterClass;
import yandex.maps.proto.common2.response.ResponseOuterClass;
import yandex.maps.proto.search.address.AddressOuterClass;
import yandex.maps.proto.search.geocoder.Geocoder;
import yandex.maps.proto.search.kind.KindOuterClass;
import yandex.maps.proto.search.precision.PrecisionOuterClass;
import yandex.maps.proto.search.search.Search;
import ru.yandex.market.proto.indexer.v2.FeedLog;

/**
 * Тесты для {@link ProtoTestUtil}.
 */
class ProtoTestUtilTest {

    @Test
    @DisplayName("Получение прото модели по json представлению")
    void testGetProtoMessageByJson() {
        FeedLog.Feed feedlog = getFeedProtoFromFile();

        Assertions.assertThat(feedlog.getFeedId() == 10
                && feedlog.getShopId() == 100
                && feedlog.getDownloadStatus().equals("200 OK")
                && feedlog.getFeedProcessingType() == FeedLog.FeedProcessingType.PULL
                && feedlog.getLastSession().getParseRetcode() == 3
                && feedlog.getLastSession().getParsing().getStatus() == FeedLog.Status.ERROR
                && feedlog.getCachedSession().getParsing().getStatus() == FeedLog.Status.OK
                // проверяем подстановку макроса timestamp
                && feedlog.getCachedSession().getStartDate() == 1544898840
                && feedlog.getIndexation().getStatistics().getTotalOffers() == 11
        ).isTrue();
    }

    @Test
    @DisplayName("Сравнение протобуфок через AssertJ")
    void testAssertThat() {
        FeedLog.Feed actual = getFeedProtoFromFile();
        FeedLog.Feed expected = getFeedProtoFromCode();

        ProtoTestUtil.assertThat(actual)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Сравнение протобуфок через AssertJ. Можно добавить дополнительные поля для игнорирования")
    void testAssertThatIgnoreFields() {
        FeedLog.Feed actual = getFeedProtoFromFile();
        FeedLog.Feed expected = getFeedProtoFromCode().toBuilder()
                .setFeedId(999)
                .build();

        ProtoTestUtil.assertThat(actual)
                .ignoringFieldsMatchingRegexes(".*feedId.*")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Сравнение протобуфок через AssertJ. Расширения должны быть распознаны")
    void testAssertThatExtensionsRecognized() {

        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        registry.add(Search.sEARCHRESPONSEMETADATA);
        registry.add(Geocoder.gEOOBJECTMETADATA);

        ResponseOuterClass.Response actualGeoObjectResponse = ProtoTestUtil.getProtoMessageByJson(
                ResponseOuterClass.Response.class,
                registry,
                "json/ProtoTestUtilTest.testGetProtoMessageByJsonWithExtensions.json",
                getClass()
        );
        ResponseOuterClass.Response expectedGeoObjectResponse = getResponseGeocoderProtoFromCode();

        ProtoTestUtil.assertThat(actualGeoObjectResponse).isEqualTo(expectedGeoObjectResponse);
    }

    @Test
    @DisplayName("Мапы в протобуфе сравниваются как маппы. Не зависимо от способа хранения")
    void testMapComparing() {
        // В прото из кода мапа хранится в режиме StorageMode#MAP
        Map<Long, Long> map = new HashMap<>();
        map.put(123L, 456L);
        EntityMapper.TEntityMapper protoFromCode = EntityMapper.TEntityMapper.newBuilder()
                .putAllNewIdToOldId(map)
                .build();

        // В прото из файла мапа хранится в режиме StorageMode#BOTH
        EntityMapper.TEntityMapper protoFromFile = ProtoTestUtil.getProtoMessageByJson(
                EntityMapper.TEntityMapper.class,
                "json/ProtoTestUtilTest.testMapComparing.json",
                getClass()
        );

        ProtoTestUtil.assertThat(protoFromCode)
                .isEqualTo(protoFromFile);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Получение полей для игнорирования")
    @MethodSource("testGetIgnoredFieldsData")
    void testGetIgnoredFields(String name, List<String> extra, List<String> expected) {
        String[] actual = ProtoTestUtil.getIgnoredFields(extra.toArray(new String[0]));

        Assertions.assertThat(actual)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    private static Stream<Arguments> testGetIgnoredFieldsData() {
        return Stream.of(
                Arguments.of(
                        "Без дополнительных полей",
                        Arrays.asList(),
                        Arrays.asList(".*bitField0_.*", ".*memoizedHashCode.*", ".*memoizedIsInitialized.*")
                ),
                Arguments.of(
                        "С дополнительными полями",
                        Arrays.asList("abc", "dd"),
                        Arrays.asList(".*bitField0_.*", ".*memoizedHashCode.*", ".*memoizedIsInitialized.*", "abc",
                                "dd")
                )
        );
    }

    private FeedLog.Feed getFeedProtoFromFile() {
        return ProtoTestUtil.getProtoMessageByJson(
                FeedLog.Feed.class,
                "json/ProtoTestUtilTest.testGetProtoMessageByJson.json",
                getClass()
        );
    }

    private FeedLog.Feed getFeedProtoFromCode() {
        return FeedLog.Feed.newBuilder()
                .setFeedId(10)
                .setShopId(100)
                .setLastSession(FeedLog.RobotFeedSession.newBuilder()
                        .setSessionName("20191212_0341")
                        .setUrlInArchive("url_in_archive")
                        .setParseRetcode(3)
                        .setStartDate(1576122004)
                        .setDownloadDate(1576122104)
                        .setYmlDate("yml_date")
                        .setParsing(FeedLog.ProcessingSummary.newBuilder()
                                .setStatistics(FeedLog.ParseStats.newBuilder()
                                        .setTotalOffers(4)
                                        .setValidOffers(5)
                                        .setWarnOffers(6)
                                        .setErrorOffers(7)
                                        .build())
                                .setStatus(FeedLog.Status.ERROR)
                                .build())
                        .build())
                .setCachedSession(FeedLog.RobotFeedSession.newBuilder()
                        .setSessionName("20181215_2134")
                        .setUrlInArchive("url")
                        .setParseRetcode(0)
                        .setStartDate(1544898840)
                        .setDownloadDate(1546292042)
                        .setYmlDate("yml_date")
                        .setParsing(FeedLog.ProcessingSummary.newBuilder()
                                .setStatistics(FeedLog.ParseStats.newBuilder()
                                        .setTotalOffers(4)
                                        .setValidOffers(5)
                                        .setWarnOffers(6)
                                        .setErrorOffers(7)
                                        .build())
                                .setStatus(FeedLog.Status.OK)
                                .build())
                        .build())
                .setIndexation(FeedLog.ProcessingSummary.newBuilder()
                        .setStatistics(FeedLog.ParseStats.newBuilder()
                                .setTotalOffers(11)
                                .setValidOffers(13)
                                .setWarnOffers(17)
                                .setErrorOffers(19)
                                .build())
                        .setStatus(FeedLog.Status.OK)
                        .build())
                .setOffersHosts("yandex.ru,yandex-team.ru,yandex.net")
                .setDownloadRetcode(0)
                .setDownloadStatus("200 OK")
                .setIndexedStatus("cached")
                .setFeedProcessingType(FeedLog.FeedProcessingType.PULL)
                .build();
    }

    private ResponseOuterClass.Response getResponseGeocoderProtoFromCode() {
        return ResponseOuterClass.Response.newBuilder()
                .setReply(GeoObjectOuterClass.GeoObject.newBuilder()
                        .addMetadata(
                                MetadataOuterClass.Metadata.newBuilder().setExtension(
                                        Search.sEARCHRESPONSEMETADATA, Search.SearchResponseMetadata.newBuilder()
                                                .setFound(1)
                                                .build()
                                )
                        )
                        .addGeoObject(GeoObjectOuterClass.GeoObject.newBuilder()
                                .addMetadata(MetadataOuterClass.Metadata.newBuilder().setExtension(
                                        Geocoder.gEOOBJECTMETADATA,
                                        Geocoder.GeoObjectMetadata.newBuilder()
                                                .setHousePrecision(PrecisionOuterClass.Precision.EXACT)
                                                .setAddress(AddressOuterClass.Address.newBuilder()
                                                        .setFormattedAddress("here/formatted_address")
                                                        .addComponent(AddressOuterClass.Component.newBuilder()
                                                                .setName("Russia")
                                                                .addKind(KindOuterClass.Kind.COUNTRY)
                                                        )
                                        ).build()
                                ))
                                .setName("Name")
                                .addGeometry(GeometryOuterClass.Geometry.newBuilder()
                                        .setPoint(GeometryOuterClass.Point.newBuilder().setLon(1.2).setLat(2.1))
                                )
                        )
                )
                .build();
    }

}
