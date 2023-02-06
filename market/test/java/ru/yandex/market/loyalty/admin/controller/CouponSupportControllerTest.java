package ru.yandex.market.loyalty.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.controller.dto.support.CouponEntriesResponse;
import ru.yandex.market.loyalty.admin.support.service.CouponEntry;
import ru.yandex.market.loyalty.admin.support.service.CouponHistoryEntry;
import ru.yandex.market.loyalty.admin.support.service.CouponSupportService;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestFor(CouponSupportController.class)
public class CouponSupportControllerTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PromoManager promoManager;

    @Test
    public void shouldSucceedIfCouponNotExist() throws Exception {
        final CouponEntriesResponse couponEntriesResponse = apiGetCouponHistory("ABC");
        assertThat(couponEntriesResponse.getCouponEntries(), Matchers.empty());
    }

    @Test
    public void shouldFindCoupon() throws Exception {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode("ABC"));

        final CouponEntriesResponse couponEntriesResponse = apiGetCouponHistory("ABC");
        assertThat(couponEntriesResponse.getCouponEntries(), Matchers.hasSize(1));
    }

    @Test
    public void shouldFindPromocode() throws Exception {
        promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setCode("ABC"));

        final CouponEntriesResponse couponEntriesResponse = apiGetCouponHistory("ABC");
        assertThat(couponEntriesResponse.getCouponEntries(), Matchers.hasSize(1));
    }

    @Test
    public void shouldDepriveCouponAndGetProperHistory() throws Exception {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode("ABC"));

        mockMvc
                .perform(put("/api/support/coupon/deprive?couponCode=ABC").with(csrf()))
                .andExpect(status().isOk());

        final CouponEntriesResponse couponEntriesResponse = apiGetCouponHistory("ABC");
        // check coupon deprived
        final CouponEntry couponEntry = couponEntriesResponse.getCouponEntries().get(0);
        assertEquals(CouponStatus.DEPRIVED.name(), couponEntry.getStatus());
        // check history and its reason
        final CouponHistoryEntry historyEntry = couponEntry.getHistory().get(couponEntry.getHistory().size() - 1);
        assertEquals(CouponStatus.DEPRIVED.name(), historyEntry.getStatus());
        assertEquals(DiscountHistoryRecordType.DEACTIVATION, historyEntry.getRecordType());
        assertThat(historyEntry.getSourceKey(), Matchers.endsWith(CouponSupportService.DEPRIVE_REASON));
    }

    private CouponEntriesResponse apiGetCouponHistory(String couponCode) throws Exception {
        final String contentAsString = mockMvc
                .perform(get("/api/support/coupon/history?term=" + couponCode))
                .andDo(log())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(contentAsString, CouponEntriesResponse.class);
    }
}
