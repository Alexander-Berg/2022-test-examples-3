package ru.yandex.direct.core.testing.mock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jooq.TransactionalCallable;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService;
import ru.yandex.direct.core.entity.mobilecontent.container.MobileAppStoreUrl;
import ru.yandex.direct.core.entity.mobilecontent.model.ContentType;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentService;
import ru.yandex.direct.core.entity.uac.model.Store;
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Map.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MobileBannerRatingMockUtils {
    public static final int SHARD = 19;

    public static final Long BANNER_ID_1 = 1L;
    public static final String RATING_1 = null;
    public static final String URL_1 = "1";
    public static final RedirectCheckResult REDIRECTED_URL_1 = RedirectCheckResult.createSuccessResult(
            "https://play.google.com/store/apps/details?id=com.yandex.mobile" +
                    ".realty&referrer=appmetrica_tracking_id%3D962757581464476054%26ym_tracking_id" +
                    "%3D885845959306947380", "play.google.com");
    public static final MobileAppStoreUrl PARSED_URL_1 = new MobileAppStoreUrl(
            OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "RU", "RU", "com.yandex.mobile.realty", true);
    public static final MobileContent MOBILE_CONTENT_1 = new MobileContent().withRating(BigDecimal.valueOf(3.87777));
    public static final InternalBanner BANNER_1 = new InternalBanner()
            .withId(BANNER_ID_1)
            .withTemplateId(3103L)
            .withStatusBsSynced(StatusBsSynced.NO)
            .withTemplateVariables(List.of(
                    new TemplateVariable().withTemplateResourceId(4059L).withInternalValue(URL_1),
                    new TemplateVariable().withTemplateResourceId(4060L).withInternalValue(RATING_1)));


    public static final Long BANNER_ID_2 = 2L;
    public static final String RATING_2 = "4.5";
    public static final String URL_2 = "2";
    public static final RedirectCheckResult REDIRECTED_URL_2 = RedirectCheckResult.createSuccessResult(
            "https://itunes.apple.com/ru/app/id1020247568?mt=8&amp%3Bat=11l9Wx&amp%3Bct=own&referrer" +
                    "=appmetrica_tracking_id%3D98066449569973923%26ym_tracking_id%3D18355380167709923026",
            "itunes.apple.com");
    public static final MobileAppStoreUrl PARSED_URL_2 = new MobileAppStoreUrl(
            OsType.IOS, Store.ITUNES, ContentType.APP, "RU", "RU", "id1020247568", false);
    public static final MobileContent MOBILE_CONTENT_2 = new MobileContent().withRating(BigDecimal.valueOf(5));
    public static final InternalBanner BANNER_2 = new InternalBanner()
            .withId(BANNER_ID_2)
            .withTemplateId(3103L)
            .withStatusBsSynced(StatusBsSynced.YES)
            .withTemplateVariables(List.of(
                    new TemplateVariable().withTemplateResourceId(4059L).withInternalValue(URL_2),
                    new TemplateVariable().withTemplateResourceId(4060L).withInternalValue(RATING_2)));


    public static final Long BANNER_ID_3 = 3L;
    public static final String RATING_3 = "3.9";
    public static final String URL_3 = "1?1";
    public static final InternalBanner BANNER_3 = new InternalBanner()
            .withId(BANNER_ID_3)
            .withTemplateId(3103L)
            .withStatusBsSynced(StatusBsSynced.NO)
            .withTemplateVariables(List.of(
                    new TemplateVariable().withTemplateResourceId(4059L).withInternalValue(URL_3),
                    new TemplateVariable().withTemplateResourceId(4060L).withInternalValue(RATING_3)));


    private static final Map<String, RedirectCheckResult> urlsToRedirect = Map.ofEntries(
            entry(URL_1, REDIRECTED_URL_1),
            entry(URL_2, REDIRECTED_URL_2)
    );

    private static final Map<MobileAppStoreUrl, Optional<MobileContent>> parsedUrlsToMobileContent = Map.ofEntries(
            entry(PARSED_URL_1, Optional.of(MOBILE_CONTENT_1)),
            entry(PARSED_URL_2, Optional.of(MOBILE_CONTENT_2))
    );


    public static BannerUrlCheckService createBannerUrlCheckServiceMock() {
        var mock = mock(BannerUrlCheckService.class);

        when(mock.getRedirect(eq(URL_1))).thenReturn(urlsToRedirect.get(URL_1));
        when(mock.getRedirect(eq(URL_2))).thenReturn(urlsToRedirect.get(URL_2));

        return mock;
    }

    public static MobileContentService createMobileContentServiceMock() {
        var mock = mock(MobileContentService.class);

        when(mock.getMobileContentFromYt(eq(SHARD), eq(Set.of(PARSED_URL_1, PARSED_URL_2))))
                .thenReturn(parsedUrlsToMobileContent);

        when(mock.getMobileContentFromYt(eq(SHARD), eq(Set.of(PARSED_URL_1))))
                .thenReturn(Map.ofEntries(entry(PARSED_URL_1, Optional.of(MOBILE_CONTENT_1))));

        when(mock.getMobileContentFromYt(eq(SHARD), eq(Set.of(PARSED_URL_2))))
                .thenReturn(Map.ofEntries(entry(PARSED_URL_2, Optional.of(MOBILE_CONTENT_2))));

        when(mock.getMobileContentFromYt(eq(SHARD), eq(Set.of())))
                .thenReturn(Map.of());

        return mock;
    }

    public static DslContextProvider createDslContextProviderMock() {
        var mock = mock(DslContextProvider.class);
        when(mock.ppcTransactionResult(eq(SHARD), any(TransactionalCallable.class)))
                .thenAnswer(a -> ((TransactionalCallable<Integer>) a.getArgument(1)).run(null));
        return mock;
    }
}
