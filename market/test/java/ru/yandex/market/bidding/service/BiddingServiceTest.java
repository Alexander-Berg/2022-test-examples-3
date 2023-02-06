package ru.yandex.market.bidding.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.bidding.engine.BasicBiddingEngine;
import ru.yandex.market.bidding.engine.model.OfferBid;
import ru.yandex.market.bidding.engine.model.OfferBidBuilder;
import ru.yandex.market.bidding.model.CategoryBid;
import ru.yandex.market.bidding.model.CategoryShopBid;
import ru.yandex.market.bidding.model.ChangeBidsRequest;
import ru.yandex.market.common.test.util.StringTestUtil;

@RunWith(MockitoJUnitRunner.class)
public class BiddingServiceTest {
    public static final String CHANGE_BIDS_REQUEST_PATH = "changeDuplicateBidsRequest.json";

    @Mock
    BasicBiddingEngine basicBiddingEngine;

    @InjectMocks
    BiddingService biddingService = new BiddingService();

    @Test//(expected = BadRequestException.class) TODO: REMOVE KOSTYL AFTER https://st.yandex-team.ru/MBI-28902
    public void testUpdateBids() throws IOException {
        String str = StringTestUtil.getString(getClass(), CHANGE_BIDS_REQUEST_PATH);
        ChangeBidsRequest request = new ObjectMapper().readValue(str, ChangeBidsRequest.class);
        biddingService.updateShopBids(774, request, 1L);
    }

    @Test
    public void testConvertCategoryBids() {
        Mockito.when(basicBiddingEngine.getCategoryBids(Mockito.anyLong())).
                thenReturn(Collections.singletonList(
                        new OfferBidBuilder().setId("111").feedId(111).build(OfferBid.class)));
        List<CategoryShopBid> categoryBids = biddingService.getCategoryBids(774);
        Assert.assertEquals(1, categoryBids.size());
    }

    @Test
    public void testConvertVendorCategoryBids() {
        Mockito.when(basicBiddingEngine.getCategoryBids(Mockito.anyLong())).
                thenReturn(Collections.singletonList(
                        new OfferBidBuilder().setId("111").feedId(111).build(OfferBid.class)));
        List<CategoryBid> categoryBids = biddingService.getVendorCategoryBids(774);
        Assert.assertEquals(1, categoryBids.size());
    }

}
