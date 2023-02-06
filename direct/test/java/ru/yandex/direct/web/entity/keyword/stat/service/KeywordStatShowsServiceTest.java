package ru.yandex.direct.web.entity.keyword.stat.service;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.advq.AdvqClient;
import ru.yandex.direct.advq.AdvqSearchOptions;
import ru.yandex.direct.advq.Device;
import ru.yandex.direct.advq.SearchKeywordResult;
import ru.yandex.direct.advq.SearchRequest;
import ru.yandex.direct.advq.search.AdvqRequestKeyword;
import ru.yandex.direct.advq.search.SearchItem;
import ru.yandex.direct.advq.search.Statistics;
import ru.yandex.direct.core.entity.showcondition.model.ShowStatRequest;
import ru.yandex.direct.core.entity.showcondition.service.ShowStatService;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.keyword.stat.model.KeywordStatShowsBulkRequest;
import ru.yandex.direct.web.validation.kernel.ValidationResultConversionService;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.advq.Device.DESKTOP;
import static ru.yandex.direct.advq.Device.PHONE;
import static ru.yandex.direct.core.entity.showcondition.service.ShowStatService.getAdvqSearchOptions;
import static ru.yandex.direct.core.entity.showcondition.service.ShowStatService.getAdvqSearchOptionsWithoutSearchedWith;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class KeywordStatShowsServiceTest {

    private static final String DEFAULT_GEO = String.valueOf(Region.RUSSIA_REGION_ID);
    private static final Set<Device> DEFAULT_DEVICE_TYPES = Set.of(PHONE, DESKTOP);
    private static final String PHRASE = "some phrase";
    private static final String MINUS_PHRASE = "minus";
    private static final String SEARCH_PHRASE = PHRASE + " -" + MINUS_PHRASE;
    private static final GdAdGroupType GD_AD_GROUP_TYPE = GdAdGroupType.TEXT;

    @Mock
    private AdvqClient advqClient;
    @Mock
    private ValidationResultConversionService validationResultConversionService;
    @Mock
    private KeywordStatShowsValidationService keywordStatShowsValidationService;
    @Mock
    private KeywordStatShowsConverter keywordStatShowsConverter;
    @InjectMocks
    private ShowStatService showStatService;
    @Captor
    private ArgumentCaptor<AdvqSearchOptions> advqSearchOptionsArgumentCaptor;
    @Captor
    private ArgumentCaptor<List<SearchRequest>> searchRequestsArgumentCaptor;

    private KeywordStatShowsService keywordStatShowsService;

    private KeywordStatShowsBulkRequest bulkRequest;
    private ImmutableMap<AdvqRequestKeyword, SearchKeywordResult> searchValue;

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);
        keywordStatShowsService = new KeywordStatShowsService(validationResultConversionService,
                keywordStatShowsValidationService, keywordStatShowsConverter, mock(StopWordService.class),
                mock(KeywordWithLemmasFactory.class), showStatService);

        bulkRequest = new KeywordStatShowsBulkRequest()
                .withPhrases(singletonList(PHRASE))
                .withCommonMinusPhrases(singletonList(MINUS_PHRASE))
                .withGeo(DEFAULT_GEO)
                .withAdGroupType(GD_AD_GROUP_TYPE)
                .withNeedSearchedWith(false);

        SearchRequest advqSearchRequest = SearchRequest.fromPhrases(singletonList(SEARCH_PHRASE),
                singletonList(Long.valueOf(DEFAULT_GEO)))
                .withDeviceTypes(DEFAULT_DEVICE_TYPES);

        when(keywordStatShowsConverter.convertRequest(any(KeywordStatShowsBulkRequest.class))).thenCallRealMethod();
        when(keywordStatShowsConverter.convertRequest(any(ShowStatRequest.class))).thenReturn(advqSearchRequest);
        when(keywordStatShowsConverter.convertToResponse(anyList(), anyMap(), any(), any())).thenCallRealMethod();
        when(keywordStatShowsValidationService.validate(bulkRequest)).thenReturn(ValidationResult.success(bulkRequest));

        SearchKeywordResult keywordResult = SearchKeywordResult.success(new SearchItem().withStat(new Statistics()));
        searchValue = ImmutableMap.of(new AdvqRequestKeyword(SEARCH_PHRASE), keywordResult);
        Map<SearchRequest, Map<AdvqRequestKeyword, SearchKeywordResult>> advqResults = new IdentityHashMap<>();
        advqResults.put(advqSearchRequest, searchValue);
        doReturn(advqResults)
                .when(advqClient).search(eq(singletonList(advqSearchRequest)), any(AdvqSearchOptions.class));
        doReturn(advqResults)
                .when(advqClient).searchVideo(eq(singletonList(advqSearchRequest)), any(AdvqSearchOptions.class));
    }

    @Test
    public void checkGetKeywordsStatShows() {
        WebResponse statBulkResponse = keywordStatShowsService.getKeywordStatShowsBulk(bulkRequest);

        assertThat(statBulkResponse.isSuccessful()).isTrue();

        verify(keywordStatShowsConverter)
                .convertToResponse(eq(singletonList(new AdvqRequestKeyword(SEARCH_PHRASE))), eq(searchValue), eq(GD_AD_GROUP_TYPE), any());

        verify(keywordStatShowsValidationService).validate(bulkRequest);
        verifyZeroInteractions(validationResultConversionService);
        verify(advqClient).search(anyList(), any());
    }

    @Test
    public void checkGetKeywordsStatShows_ContentPromotionVideoGroup() {
        GdAdGroupType gdAdGroupType = GdAdGroupType.CONTENT_PROMOTION_VIDEO;
        bulkRequest.setAdGroupType(gdAdGroupType);
        WebResponse statBulkResponse = keywordStatShowsService.getKeywordStatShowsBulk(bulkRequest);

        assertThat(statBulkResponse.isSuccessful()).isTrue();

        verify(keywordStatShowsConverter)
                .convertToResponse(eq(singletonList(new AdvqRequestKeyword(SEARCH_PHRASE))), eq(searchValue), eq(gdAdGroupType), any());

        verify(keywordStatShowsValidationService).validate(bulkRequest);
        verifyZeroInteractions(validationResultConversionService);
        verify(advqClient).searchVideo(anyList(), any());
    }

    @Test
    public void checkNeedSearchedWithOption() {
        bulkRequest.withNeedSearchedWith(true);
        keywordStatShowsService.getKeywordStatShowsBulk(bulkRequest);
        verify(advqClient).search(anyList(), advqSearchOptionsArgumentCaptor.capture());
        AdvqSearchOptions searchOptions = advqSearchOptionsArgumentCaptor.getValue();
        assertThat(searchOptions).as("searchOptions").is(matchedBy(beanDiffer(getAdvqSearchOptions())));
    }

    @Test
    public void checkNotNeedSearchedWithOption() {
        bulkRequest.withNeedSearchedWith(false);
        keywordStatShowsService.getKeywordStatShowsBulk(bulkRequest);
        verify(advqClient).search(anyList(), advqSearchOptionsArgumentCaptor.capture());
        AdvqSearchOptions searchOptions = advqSearchOptionsArgumentCaptor.getValue();
        assertThat(searchOptions).as("searchOptions")
                .is(matchedBy(beanDiffer(getAdvqSearchOptionsWithoutSearchedWith())));
    }

    @Test
    public void checkDeviceTypes() {
        bulkRequest.withDeviceTypes(DEFAULT_DEVICE_TYPES);
        keywordStatShowsService.getKeywordStatShowsBulk(bulkRequest);

        verify(advqClient).search(searchRequestsArgumentCaptor.capture(), any());
        List<SearchRequest> searchRequests = searchRequestsArgumentCaptor.getValue();

        assertThat(searchRequests).as("searchRequests").hasSize(1);
        assertThat(searchRequests.get(0).getDeviceTypes()).as("searchRequest").isEqualTo(DEFAULT_DEVICE_TYPES);
    }
}
