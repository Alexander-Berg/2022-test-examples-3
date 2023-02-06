package ru.yandex.direct.core.entity.mobilecontent.converter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jooq.DSLContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.direct.core.entity.domain.service.DomainService;
import ru.yandex.direct.core.entity.mobilecontent.model.AgeLabel;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreCountry;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeTextSerializer;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.misc.io.ClassPathResourceInputStreamSource;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.mobilecontent.converter.MobileContentYtConverter.convertDownloads;
import static ru.yandex.direct.core.entity.mobilecontent.converter.MobileContentYtConverter.getScreensEx;
import static ru.yandex.direct.core.entity.mobilecontent.service.MobileContentServiceConstants.FIXED_DOMAINS;

@RunWith(MockitoJUnitRunner.class)
public class MobileContentYtConverterTest {
    @Mock
    private DomainService domainService;

    @Mock
    private DslContextProvider dslContextProvider;

    @InjectMocks
    private MobileContentYtConverter converter;

    @Test
    public void iconUrlToHash() {
        String original = "http://avatars.mds.yandex.net/get-google-play-app-icon/"
                + "24057/6942f8cd0d07d22093227ae234219879/orig";
        String expected = "24057/6942f8cd0d07d22093227ae234219879";
        assertEquals(expected, MobileContentYtConverter.iconUrlToHash(original));
    }

    @Test
    public void parseSampleYtResponse() throws Exception {
        YTreeNode sampleResponse = YTreeTextSerializer.deserialize(new ClassPathResourceInputStreamSource(
                "ru/yandex/direct/core/entity/mobilecontent/sample_yt_answer.yson").getInput());
        MobileContent content = converter.ytToMobileContent(sampleResponse.mapNode(), OsType.ANDROID,
                new HashMap<>());
        assertEquals(sampleResponse.mapNode().getString("app_id"), content.getStoreContentId());
        assertEquals(StoreCountry.RU.name(), content.getStoreCountry());
        assertEquals(AgeLabel._18_2B, content.getAgeLabel());
    }

    @Test
    public void parseSampleYtResponseWithNullAge() throws Exception {
        YTreeNode sampleResponse = YTreeTextSerializer.deserialize(new ClassPathResourceInputStreamSource(
                "ru/yandex/direct/core/entity/mobilecontent/sample_yt_answer.yson").getInput());
        sampleResponse.mapNode().put("adult", new YTreeEntityNodeImpl(Cf.map()));
        MobileContent content = converter.ytToMobileContent(sampleResponse.mapNode(), OsType.ANDROID,
                new HashMap<>());
        assertEquals(sampleResponse.mapNode().getString("app_id"), content.getStoreContentId());
        assertEquals(StoreCountry.RU.name(), content.getStoreCountry());
        assertEquals(AgeLabel._18_2B, content.getAgeLabel());
    }

    @Test
    public void parseSampleYtResponseWithNameWithSpecialChars() throws Exception {
        YTreeNode sampleResponse = YTreeTextSerializer.deserialize(new ClassPathResourceInputStreamSource(
                "ru/yandex/direct/core/entity/mobilecontent/sample_yt_answer.yson").getInput());
        sampleResponse.mapNode().put("name", new YTreeStringNodeImpl("clock " + '\u23f0', Cf.map()));

        MobileContent content = converter.ytToMobileContent(sampleResponse.mapNode(), OsType.ANDROID,
                new HashMap<>());
        assertEquals(sampleResponse.mapNode().getString("app_id"), content.getStoreContentId());
        assertEquals("clock ", content.getName());
    }

    @Test
    public void parseSampleYtResponseWithNameWithPunctuation() throws Exception {
        YTreeNode sampleResponse = YTreeTextSerializer.deserialize(new ClassPathResourceInputStreamSource(
                "ru/yandex/direct/core/entity/mobilecontent/sample_yt_answer.yson").getInput());
        sampleResponse.mapNode().put("name", new YTreeStringNodeImpl("Punctuation-.[]*", Cf.map()));

        MobileContent content = converter.ytToMobileContent(sampleResponse.mapNode(), OsType.ANDROID,
                new HashMap<>());
        assertEquals(sampleResponse.mapNode().getString("app_id"), content.getStoreContentId());
        assertEquals("Punctuation-.[]*", content.getName());
    }

    @Test
    public void parseSampleYtResponse_CheckRating() throws Exception {
        YTreeNode sampleResponse = YTreeTextSerializer.deserialize(new ClassPathResourceInputStreamSource(
                "ru/yandex/direct/core/entity/mobilecontent/sample_yt_answer.yson").getInput());
        MobileContent content = converter.ytToMobileContent(sampleResponse.mapNode(), OsType.ANDROID,
                new HashMap<>());
        assertEquals(new BigDecimal("3.44"), content.getRating());
    }

    @Test
    public void convertRating_Absent() {
        assertEquals(new BigDecimal("0.00"), MobileContentYtConverter.convertRating(Optional.empty()));
    }

    @Test
    public void convertRating_Negative() {
        assertEquals(new BigDecimal("0.00"), MobileContentYtConverter.convertRating(Optional.of(-2d)));
    }

    @Test
    public void minVersionRegexTest_correct() {
        String original = "3.4.123.432";
        String expected = "3.4";
        assertEquals(expected, converter.getMinOsVersion(original));
    }

    @Test
    public void minVersionRegexTest_wrong() {
        String original = "Зависит от устройства";
        String expected = "";
        assertEquals(expected, converter.getMinOsVersion(original));
    }

    @Test
    public void minVersionRegexTest_single_digit() {
        String original = "3 и выше";
        String expected = "3";
        assertEquals(expected, converter.getMinOsVersion(original));
    }

    @Test
    public void getGenresTest_empty() {
        YTreeMapNode node = new YTreeMapNodeImpl(Cf.map());
        node.put("genres", new YTreeStringNodeImpl("[]", Cf.map()));
        assertEquals("", converter.getGenres(node));
    }

    @Test
    public void getGenresTest_two() {
        YTreeMapNode node = new YTreeMapNodeImpl(Cf.map());
        node.put("genres", new YTreeStringNodeImpl("[\"genre\";\"other genre\"]", Cf.map()));
        assertEquals("genre,other genre", converter.getGenres(node));
    }

    @Test
    public void getDomainIdsTest_fixed() {
        Map.Entry<String, String> fixedDomain = FIXED_DOMAINS.entrySet().iterator().next();
        long domainId = 1L;
        Map<String, Optional<String>> websites = singletonMap(fixedDomain.getKey(), Optional.of("new.website.ru"));
        when(dslContextProvider.ppc(anyInt())).thenReturn(mock(DSLContext.class));
        when(domainService.getOrCreate(any(), eq(singletonList(fixedDomain.getValue()))))
                .thenReturn(singletonList(domainId));
        assertEquals(singletonMap(fixedDomain.getKey(), domainId), converter.getDomainIds(1, websites));
    }

    @Test
    public void getDomainIdsTest_fixedWithoutWebsite() {
        Map.Entry<String, String> fixedDomain = FIXED_DOMAINS.entrySet().iterator().next();
        long domainId = 1L;
        Map<String, Optional<String>> websites = singletonMap(fixedDomain.getKey(), Optional.empty());
        when(dslContextProvider.ppc(anyInt())).thenReturn(mock(DSLContext.class));
        when(domainService.getOrCreate(any(), eq(singletonList(fixedDomain.getValue()))))
                .thenReturn(singletonList(domainId));
        assertEquals(singletonMap(fixedDomain.getKey(), domainId), converter.getDomainIds(1, websites));
    }

    @Test
    public void getDomainIdsTest_correctDomain() {
        String appId = "test.app";
        String domain = "http://www.domain.ru";
        long domainId = 1L;
        Map<String, Optional<String>> websites = singletonMap(appId, Optional.of(domain));
        when(dslContextProvider.ppc(anyInt())).thenReturn(mock(DSLContext.class));
        when(domainService.getOrCreate(any(), any())).thenReturn(singletonList(domainId));
        assertEquals(singletonMap(appId, domainId), converter.getDomainIds(1, websites));
    }

    @Test
    public void getDomainIdsTest_wrongDomain() {
        String appId = "test.app";
        String domain = "wrongdomain123";
        Map<String, Optional<String>> websites = singletonMap(appId, Optional.of(domain));
        when(dslContextProvider.ppc(anyInt())).thenReturn(mock(DSLContext.class));
        when(domainService.getOrCreate(any(), eq(emptyList()))).thenReturn(emptyList());
        assertEquals(emptyMap(), converter.getDomainIds(1, websites));
    }

    @Test
    public void screenEx_valid1() {
        var s = "{\"screens_ex\"=\"[{\\\"height\\\" = 800; \\\"path\\\" = " +
                "\\\"/get-google-play-app-screens/4049603/22df38e5027766e81f4cda7ac3e2d66f/orig\\\"; \\\"width\\\" = " +
                "480}; {\\\"height\\\" = 700; \\\"path\\\" = " +
                "\\\"/get-google-play-app-screens/3506943/002186a6796eebc7d12bea6e0bc7a5d9/orig\\\"; \\\"width\\\" = " +
                "580};]\"}";
        var mn = YTreeTextSerializer.deserialize(s).mapNode();
        assertThat(getScreensEx(mn)).isEqualTo(List.of(
                Map.of(
                        "height", "800",
                        "width", "480",
                        "path", "/get-google-play-app-screens/4049603/22df38e5027766e81f4cda7ac3e2d66f/orig"
                ),
                Map.of(
                        "height", "700",
                        "width", "580",
                        "path", "/get-google-play-app-screens/3506943/002186a6796eebc7d12bea6e0bc7a5d9/orig"
                )
        ));
    }

    @Test
    public void screenEx_validWithIgnoredBoolean() {
        var s = "{\"screens_ex\"=\"[{\\\"height\\\" = %true; \\\"path\\\" = " +
                "\\\"/get-google-play-app-screens/4049603/22df38e5027766e81f4cda7ac3e2d66f/orig\\\"; \\\"width\\\" = " +
                "480};]\"}";
        var mn = YTreeTextSerializer.deserialize(s).mapNode();
        assertThat(getScreensEx(mn)).isEqualTo(List.of(
                Map.of(
                        "width", "480",
                        "path", "/get-google-play-app-screens/4049603/22df38e5027766e81f4cda7ac3e2d66f/orig"
                )
        ));
    }

    @Test
    public void screenEx_empty1() {
        var s = "{\"screens_ex\"=\"[]\"}";
        var mn = YTreeTextSerializer.deserialize(s).mapNode();
        assertThat(getScreensEx(mn)).isEmpty();
    }

    @Test
    public void screenEx_empty2() {
        var s = "{\"screens_ex\"=\"\"}";
        var mn = YTreeTextSerializer.deserialize(s).mapNode();
        assertThat(getScreensEx(mn)).isEmpty();
    }

    @Test
    public void screenEx_empty3() {
        var s = "{}";
        var mn = YTreeTextSerializer.deserialize(s).mapNode();
        assertThat(getScreensEx(mn)).isEmpty();
    }

    @Test
    public void downloads_valid1() {
        assertThat(convertDownloads("1.000+")).isEqualTo(1000L);
    }

    @Test
    public void downloads_valid2() {
        assertThat(convertDownloads("1,000+")).isEqualTo(1000L);
    }

    @Test
    public void downloads_empty() {
        assertThat(convertDownloads("")).isNull();
    }

    @Test
    public void downloads_invalid() {
        assertThat(convertDownloads("HydrometInstalações")).isNull();
    }

    @Test
    public void parseSampleYtResponse_CheckDownloadsAbsent() throws Exception {
        YTreeNode sampleResponse = YTreeTextSerializer.deserialize(new ClassPathResourceInputStreamSource(
                "ru/yandex/direct/core/entity/mobilecontent/sample_yt_answer.yson").getInput());
        MobileContent content = converter.ytToMobileContent(sampleResponse.mapNode(), OsType.ANDROID,
                new HashMap<>());
        assertThat(content.getDownloads()).isNull();
    }

    @Test
    public void parseSampleYtResponse_CheckDownloads() throws Exception {
        YTreeNode sampleResponse = YTreeTextSerializer.deserialize(new ClassPathResourceInputStreamSource(
                "ru/yandex/direct/core/entity/mobilecontent/sample_yt_answer_downloads.yson").getInput());
        MobileContent content = converter.ytToMobileContent(sampleResponse.mapNode(), OsType.ANDROID,
                new HashMap<>());
        assertThat(content.getDownloads()).isEqualTo(5000L);
    }


}
