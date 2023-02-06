package ru.yandex.direct.core.entity.mobilecontent.service;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.mobilecontent.container.MobileAppStoreUrl;
import ru.yandex.direct.core.entity.mobilecontent.model.ContentType;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentFetchQueueRepository;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository;
import ru.yandex.direct.core.entity.mobilecontent.util.MobileAppStoreUrlParser;
import ru.yandex.direct.core.service.storeurlchecker.StoreUrlInstantChecker;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MobileContentServiceTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=contentId";
    private static final MobileAppStoreUrl PARSED_STORE_URL = MobileAppStoreUrlParser.parseStrict(STORE_URL);

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private MobileContentRepository mobileContentRepository;

    @Mock
    private MobileContentYtHelper mobileContentYtHelper;

    @Mock
    private StoreUrlInstantChecker storeUrlInstantChecker;

    @Mock
    private MobileContentFetchQueueRepository mobileContentFetchQueueRepository;

    @InjectMocks
    private MobileContentService service;

    @Before
    public void setUp() throws Exception {
        when(shardHelper.getShardByClientIdStrictly(any())).thenReturn(1);
    }

    @Test
    public void getMobileContent_hasInDb() {
        when(mobileContentRepository.getMobileContent(anyInt(), any(ClientId.class),
                anyString(), any(ContentType.class), any(OsType.class), anyString()))
                .thenReturn(singletonList(new MobileContent()));

        assertTrue(service.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, true, false).isPresent());

        verify(mobileContentRepository, atLeastOnce()).getMobileContent(anyInt(), any(ClientId.class),
                anyString(), any(ContentType.class), any(OsType.class), anyString());
        verify(mobileContentYtHelper, never()).getMobileContentFromYt(anyInt(), any(), any());
        verify(mobileContentRepository, never()).addMobileContent(anyInt(), any(MobileContent.class));
    }

    @Test
    public void getMobileContent_doesntHaveAnywhere() {
        dbReturnsNothing();
        ytReturnsNothing();

        assertFalse(service.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, true, false).isPresent());

        verify(mobileContentRepository, atLeastOnce()).getMobileContent(anyInt(), any(ClientId.class),
                anyString(), any(ContentType.class), any(OsType.class), anyString());
        verify(mobileContentYtHelper, atLeastOnce()).getMobileContentFromYt(anyInt(), any(), any());
        verify(mobileContentRepository, never()).addMobileContent(anyInt(), any(MobileContent.class));
    }

    @Test
    public void getMobileContent_doesntHaveAnywhereAndSetInstantCheckStoreUrl_ShouldCallStoreUrlInstantChecker() {
        dbReturnsNothing();
        ytReturnsNothing();

        service.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, true, true);

        verify(storeUrlInstantChecker, only()).isStoreUrlReachable(STORE_URL);
    }

    @Test
    public void getMobileContent_doesntHaveAnywhereAndUnsetInstantCheckStoreUrl_ShouldNotCallStoreUrlInstantChecker() {
        dbReturnsNothing();
        ytReturnsNothing();

        service.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, true, false);

        verify(storeUrlInstantChecker, never()).isStoreUrlReachable(STORE_URL);
    }

    @Test
    public void getMobileContent_doesntHaveAnywhereAndInstantCheckStoreUrlReturnsTrue_ReturnsResult() {
        dbReturnsNothing();
        ytReturnsNothing();
        storeUrlInstantCheckerReturns(true);

        Optional<MobileContent> res = service.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, true, true);
        assertTrue(res.isPresent());
        assertThat(res.get(), equalTo(PARSED_STORE_URL.toMobileContent().withIsAvailable(true)));
    }

    @Test
    public void getMobileContent_doesntHaveAnywhereAndInstantCheckStoreUrlReturnsFalse_ReturnsResult() {
        dbReturnsNothing();
        ytReturnsNothing();
        storeUrlInstantCheckerReturns(false);
        assertFalse(service.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, true, true).isPresent());
    }

    @Test
    public void getMobileContent_hasInYt_DryRun() {
        dbReturnsNothing();
        when(mobileContentYtHelper.getMobileContentFromYt(anyInt(), any(), any()))
                .thenReturn(singletonList(new MobileContent()));

        assertTrue(service.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, true, false).isPresent());

        verify(mobileContentRepository, atLeastOnce()).getMobileContent(anyInt(), any(ClientId.class),
                anyString(), any(ContentType.class), any(OsType.class), anyString());
        verify(mobileContentYtHelper, atLeastOnce()).getMobileContentFromYt(anyInt(), any(), any());
        verify(mobileContentRepository, never()).addMobileContent(anyInt(), any(MobileContent.class));
    }

    @Test
    public void getMobileContent_hasInYt_NotDryRun() {
        dbReturnsNothing();
        when(mobileContentYtHelper.getMobileContentFromYt(anyInt(), any(), any()))
                .thenReturn(singletonList(new MobileContent()));
        when(mobileContentRepository.addMobileContent(anyInt(), anyCollection()))
                .thenReturn(singletonList(1L));

        assertTrue(service.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, false, false).isPresent());

        verify(mobileContentRepository, atLeastOnce()).getMobileContent(anyInt(), any(ClientId.class),
                anyString(), any(ContentType.class), any(OsType.class), anyString());
        verify(mobileContentYtHelper, atLeastOnce()).getMobileContentFromYt(anyInt(), any(), any());
        verify(mobileContentRepository, atLeastOnce()).addMobileContent(anyInt(), anyCollection());
    }

    @Test
    public void getMobileContent_HasInYt_UrlNotAddedToFetchQueue() {
        dbReturnsNothing();

        when(mobileContentYtHelper.getMobileContentFromYt(anyInt(), any(), any()))
                .thenReturn(singletonList(new MobileContent()));
        when(mobileContentRepository.addMobileContent(anyInt(), anyCollection()))
                .thenReturn(singletonList(1L));

        service.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, false, false);

        verify(mobileContentFetchQueueRepository, never()).addUrl(anyString());
    }

    @Test
    public void getMobileContent_HasInMobileContent_UrlNotAddedToFetchQueue() {
        when(mobileContentRepository.getMobileContent(anyInt(), any(ClientId.class),
                anyString(), any(ContentType.class), any(OsType.class), anyString()))
                .thenReturn(singletonList(new MobileContent()));

        service.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, true, false);

        verify(mobileContentFetchQueueRepository, never()).addUrl(anyString());
    }

    @Test
    public void getMobileContent_DoesNotHaveAnywhere_InstantCheckStoreUrlReturnsFalse_UrlAddedToFetchQueue() {
        dbReturnsNothing();
        ytReturnsNothing();
        storeUrlInstantCheckerReturns(false);

        service.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, true, true);

        verify(mobileContentFetchQueueRepository, only()).addUrl(STORE_URL);
    }

    @Test
    public void getMobileContent_DoesNotHaveAnywhere_InstantCheckStoreUrlReturnsTrue_UrlAddedToFetchQueue() {
        dbReturnsNothing();
        ytReturnsNothing();
        storeUrlInstantCheckerReturns(true);

        service.getMobileContent(CLIENT_ID, STORE_URL, PARSED_STORE_URL, true, true);

        verify(mobileContentFetchQueueRepository, only()).addUrl(STORE_URL);
    }

    private void ytReturnsNothing() {
        when(mobileContentYtHelper.getMobileContentFromYt(anyInt(), any(), any()))
                .thenReturn(emptyList());
    }

    private void dbReturnsNothing() {
        when(mobileContentRepository.getMobileContent(anyInt(), any(ClientId.class),
                anyString(), any(ContentType.class), any(OsType.class), anyString()))
                .thenReturn(emptyList());
    }

    private void storeUrlInstantCheckerReturns(boolean value) {
        when(storeUrlInstantChecker.isStoreUrlReachable(anyString())).thenReturn(value);
    }

}
