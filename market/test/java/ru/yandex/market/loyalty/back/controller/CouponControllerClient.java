package ru.yandex.market.loyalty.back.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.loyalty.api.model.CouponActivationRequest;
import ru.yandex.market.loyalty.api.model.CouponDto;
import ru.yandex.market.loyalty.api.model.UserAccountCouponInfoDto;
import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.back.config.MarketLoyaltyBack;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;

import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by maratik.
 */
@Service
public final class CouponControllerClient {
    private static final TypeReference<List<UserAccountCouponInfoDto>> USER_ACC_COUPON_INFO_DTO_TYPE_REF =
            new TypeReference<List<UserAccountCouponInfoDto>>() {
            };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @MarketLoyaltyBack
    private ObjectMapper objectMapper;
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;

    public CouponDto getOrCreateCoupon(String key, Identity.Type userType, String userId, Long promoId) {
        Identity<?> identity = userType != null && userId != null ? userType.buildIdentity(userId) : null;
        return marketLoyaltyClient.generateCoupon(key, identity, promoId);
    }

    public CouponDto activateCoupon(String couponCode) {
        return marketLoyaltyClient.activateCoupons(new CouponActivationRequest(Collections.singletonList(
                new CouponActivationRequest.Item(couponCode, "source1")))).get(0);
    }

    public List<UserAccountCouponInfoDto> requestCouponsByUid(Identity.Type identityType, String userId) throws Exception {
        String response = requestCouponsByUid(identityType.getCode(), userId, status().isOk());
        return objectMapper.readValue(response, USER_ACC_COUPON_INFO_DTO_TYPE_REF);
    }

    public String requestCouponsByUid(String identityType, String userId, ResultMatcher resultMatcher) throws Exception {
        return mockMvc.perform(get("/coupon/user/" + identityType + '/' + userId))
                .andDo(log())
                .andExpect(resultMatcher)
                .andReturn().getResponse().getContentAsString();
    }
}
