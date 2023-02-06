package ru.yandex.direct.grid.processing.service.offer;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import one.util.streamex.StreamEx;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.dynamictextadtarget.container.DynamicTextAdTargetSelectionCriteria;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTargetState;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;
import ru.yandex.direct.core.entity.dynamictextadtarget.service.DynamicTextAdTargetService;
import ru.yandex.direct.core.entity.performancefilter.container.PerformanceFilterSelectionCriteria;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.schema.FilterSchema;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOffer;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.offer.mutation.GdFilterOffers;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.ytcomponents.service.OfferStatDynContextProvider;
import ru.yandex.direct.ytwrapper.dynamic.context.YtDynamicContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed;
import static ru.yandex.direct.grid.processing.service.offer.converter.OfferDataConverter.toGdOfferId;
import static ru.yandex.direct.grid.processing.util.OfferTestDataUtils.defaultGdiOffer;
import static ru.yandex.direct.grid.processing.util.OfferTestDataUtils.toOffersRowset;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class OfferMutationServiceTest {
    @Autowired
    private Steps steps;

    @Autowired
    private OfferMutationService offerMutationService;

    @Autowired
    private DynamicTextAdTargetService dynamicTextAdTargetService;

    @Autowired
    private PerformanceFilterService performanceFilterService;

    @Autowired
    private OfferStatDynContextProvider dynContextProvider;

    private ClientId clientId;
    private long operatorUid;

    private FeedInfo feedInfo;

    private GdiOffer offer;

    @Before
    public void before() {
        offer = defaultGdiOffer();

        YtDynamicContext ytDynamicContext = mock(YtDynamicContext.class);
        when(ytDynamicContext.executeSelect(any(Select.class))).thenReturn(toOffersRowset(List.of(offer)));
        doReturn(ytDynamicContext).when(dynContextProvider).getContext();

        feedInfo = steps.feedSteps().createFeed(new FeedInfo()
                .withFeed(defaultFeed()
                        .withMarketBusinessId(offer.getId().getBusinessId())
                        .withMarketShopId(offer.getId().getShopId())));

        clientId = feedInfo.getClientId();
        operatorUid = feedInfo.getUid();
    }

    @Test
    public void filterOffers_dynamicTextAdTarget() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(feedInfo.getClientInfo());
        steps.dynamicTextAdTargetsSteps().createDefaultDynamicTextAdTarget(adGroupInfo);

        GdFilterOffers input = new GdFilterOffers()
                .withCampaignIdIn(Set.of(adGroupInfo.getCampaignId()))
                .withAdGroupIdIn(Set.of(adGroupInfo.getAdGroupId()))
                .withOfferIds(List.of(toGdOfferId(offer.getId())));
        offerMutationService.filterOffers(clientId, operatorUid, input);

        List<DynamicTextAdTarget> dynamicTextAdTargets =
                dynamicTextAdTargetService.getDynamicTextAdTargets(clientId, operatorUid,
                        new DynamicTextAdTargetSelectionCriteria()
                                .withCampaignIds(adGroupInfo.getCampaignId())
                                .withAdGroupIds(adGroupInfo.getAdGroupId())
                                .withStates(DynamicTextAdTargetState.ON, DynamicTextAdTargetState.OFF,
                                        DynamicTextAdTargetState.SUSPENDED), LimitOffset.maxLimited());

        assertThat(dynamicTextAdTargets)
                .isNotEmpty()
                .allSatisfy(offerIsFilteredOnce(offer));
    }

    @Test
    public void filterOffers_dynamicFeedAdTarget() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo);
        steps.dynamicTextAdTargetsSteps().createDefaultDynamicFeedAdTarget(adGroupInfo);

        GdFilterOffers input = new GdFilterOffers()
                .withCampaignIdIn(Set.of(adGroupInfo.getCampaignId()))
                .withAdGroupIdIn(Set.of(adGroupInfo.getAdGroupId()))
                .withOfferIds(List.of(toGdOfferId(offer.getId())));
        offerMutationService.filterOffers(clientId, operatorUid, input);

        List<DynamicFeedAdTarget> dynamicFeedAdTargets =
                dynamicTextAdTargetService.getDynamicFeedAdTargets(clientId, operatorUid,
                        new DynamicTextAdTargetSelectionCriteria()
                                .withCampaignIds(adGroupInfo.getCampaignId())
                                .withAdGroupIds(adGroupInfo.getAdGroupId())
                                .withStates(DynamicTextAdTargetState.ON, DynamicTextAdTargetState.OFF,
                                        DynamicTextAdTargetState.SUSPENDED), LimitOffset.maxLimited());

        assertThat(dynamicFeedAdTargets)
                .isNotEmpty()
                .allSatisfy(offerIsFilteredOnce(DynamicFeedAdTarget::getCondition, offer));
    }

    @Test
    public void filterOffers_performanceFilter() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(feedInfo);
        steps.performanceFilterSteps().addPerformanceFilter(adGroupInfo);

        GdFilterOffers input = new GdFilterOffers()
                .withCampaignIdIn(Set.of(adGroupInfo.getCampaignId()))
                .withAdGroupIdIn(Set.of(adGroupInfo.getAdGroupId()))
                .withOfferIds(List.of(toGdOfferId(offer.getId())));
        offerMutationService.filterOffers(clientId, operatorUid, input);

        List<PerformanceFilter> performanceFilters =
                performanceFilterService.getPerfFiltersBySelectionCriteria(clientId, operatorUid,
                        new PerformanceFilterSelectionCriteria()
                                .withCampaignIds(List.of(adGroupInfo.getCampaignId()))
                                .withAdGroupIds(List.of(adGroupInfo.getAdGroupId()))
                                .withoutDeleted());

        assertThat(performanceFilters)
                .isNotEmpty()
                .allSatisfy(offerIsFilteredOnce(PerformanceFilter::getConditions, offer));
    }

    private Consumer<DynamicTextAdTarget> offerIsFilteredOnce(GdiOffer offer) {
        return filter -> {
            Collection<String> filteredUrls = StreamEx.of(filter.getCondition())
                    .filterBy(WebpageRule::getType, WebpageRuleType.URL_PRODLIST)
                    .filterBy(WebpageRule::getKind, WebpageRuleKind.NOT_EQUALS)
                    .flatCollection(WebpageRule::getValue)
                    .toList();

            assertThat(filteredUrls).containsOnlyOnce(offer.getUrl());
        };
    }

    private <S, T extends PerformanceFilterCondition<?>> Consumer<S> offerIsFilteredOnce(
            Function<? super S, ? extends Collection<? extends T>> getter, GdiOffer offer) {
        return filter -> {
            Collection<Object> filteredUrls = StreamEx.of(getter.apply(filter))
                    .filterBy(PerformanceFilterCondition::getFieldName, FilterSchema.URL_FIELD_NAME)
                    .filterBy(PerformanceFilterCondition::getOperator, Operator.NOT_CONTAINS)
                    .map(PerformanceFilterCondition::getParsedValue)
                    .select(Collection.class)
                    .<Object>flatMap(Collection::stream)
                    .toList();

            assertThat(filteredUrls).containsOnlyOnce(offer.getUrl());
        };
    }
}
