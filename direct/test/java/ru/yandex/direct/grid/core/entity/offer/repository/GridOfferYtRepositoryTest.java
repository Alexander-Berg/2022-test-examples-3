package ru.yandex.direct.grid.core.entity.offer.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.jooq.Select;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOfferFilter;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOfferId;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOfferOrderBy;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOfferOrderByField;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionGroupByDate;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.ytcomponents.service.OfferStatDynContextProvider;
import ru.yandex.direct.ytwrapper.dynamic.context.YtDynamicContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.core.util.RepositoryUtil.buildOfferStatsFilter;
import static ru.yandex.direct.multitype.entity.LimitOffset.limited;
import static ru.yandex.direct.test.utils.QueryUtils.compareQueries;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

public class GridOfferYtRepositoryTest {
    private static final GdiOfferId OFFER_ID = new GdiOfferId()
            .withBusinessId(15L)
            .withShopId(14L)
            .withOfferYabsId(23756482394L);

    private static final Long AD_GROUP_ID = 4L;
    private static final Long CAMPAIGN_ID = 3L;
    private static final Long ORDER_ID = 13L;
    private static final Long SUB_ORDER_ID = 14L;

    private static final LocalDate DATE = LocalDate.of(2021, 11, 1);
    private static final int LIMIT = 1000;

    private static final String FULL_QUERY_PATH = "classpath:///offers/offers-full.query";
    private static final String SIMPLE_QUERY_PATH = "classpath:///offers/offers-simple.query";
    private static final String ORDERS_QUERY_PATH = "classpath:///offers/offers-orders.query";
    private static final String ORDER_STAT_BY_DATE_QUERY_PATH = "classpath:///offers/offers-order-stat-by-date.query";

    private AutoCloseable mocks;

    @Mock
    private YtDynamicContext ytDynamicContext;

    @Mock
    private OfferStatDynContextProvider dynContextProvider;

    @Mock
    private CampaignService campaignService;

    @InjectMocks
    private GridOfferYtRepository gridOfferYtRepository;

    @Captor
    private ArgumentCaptor<Select<?>> argumentCaptor;

    @Before
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);

        when(ytDynamicContext.executeSelect(any(Select.class))).thenReturn(rowsetBuilder().build());

        when(dynContextProvider.getContext()).thenReturn(ytDynamicContext);

        when(campaignService.getOrderIdByCampaignId(anyCollection())).thenReturn(Map.of(CAMPAIGN_ID, ORDER_ID));
    }

    @After
    public void releaseMocks() throws Exception {
        mocks.close();
    }

    @Test
    public void getOffers() {
        GdiOfferFilter filter = new GdiOfferFilter()
                .withCampaignIdIn(Set.of(CAMPAIGN_ID))
                .withAdGroupIdIn(Set.of(AD_GROUP_ID))
                .withUrlContains("a")
                .withUrlNotContains("b")
                .withNameContains("c")
                .withNameNotContains("d")
                .withStats(buildOfferStatsFilter());

        List<GdiOfferOrderBy> orderByList = StreamEx.of(GdiOfferOrderByField.values())
                .map(field -> new GdiOfferOrderBy().withField(field).withOrder(Order.DESC))
                .toList();

        gridOfferYtRepository.getOffers(filter, orderByList, DATE, DATE, limited(LIMIT));

        checkQuery(FULL_QUERY_PATH);
    }

    @Test
    public void getOfferById() {
        gridOfferYtRepository.getOfferById(List.of(OFFER_ID));

        checkQuery(SIMPLE_QUERY_PATH);
    }

    @Test
    public void getOrderIdsByOfferId() {
        gridOfferYtRepository.getOrderIdsByOfferId(List.of(ORDER_ID), List.of(OFFER_ID), DATE, DATE);

        checkQuery(ORDERS_QUERY_PATH);
    }

    @Test
    public void getOfferStatsByDateByOrderId() {
        gridOfferYtRepository.getOfferStatsByDateByOrderId(List.of(ORDER_ID),
                Map.of(SUB_ORDER_ID, ORDER_ID), DATE, DATE, ReportOptionGroupByDate.DAY);

        checkQuery(ORDER_STAT_BY_DATE_QUERY_PATH);
    }

    private void checkQuery(String queryPath) {
        verify(ytDynamicContext).executeSelect(argumentCaptor.capture());
        String query = argumentCaptor.getValue().toString();
        String expectedQuery = LiveResourceFactory.get(queryPath).getContent();
        compareQueries(expectedQuery, query);
    }
}
