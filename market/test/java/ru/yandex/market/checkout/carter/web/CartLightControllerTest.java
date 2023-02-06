package ru.yandex.market.checkout.carter.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.report.model.OfferInfoWithPromo;
import ru.yandex.market.checkout.carter.storage.dao.ydb.YdbDao;
import ru.yandex.market.checkout.common.report.ColoredGenericMarketReportService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.carter.model.Color.WHITE;
import static ru.yandex.market.checkout.carter.model.UserIdType.YANDEXUID;
import static ru.yandex.market.checkout.carter.web.CarterWebParam.PARAM_RGB;
import static ru.yandex.market.checkout.carter.web.CarterWebParam.PARAM_UPDATE_ACTUAL_STATE;

@MockBean(ColoredGenericMarketReportService.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CartLightControllerTest extends CarterMockedDbTestBase {

    @Autowired
    private YdbDao ydbDao;
    @Autowired
    private ColoredGenericMarketReportService reportService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Новые поля сериализуются/десериализуются")
    void actualInfoTest() throws Exception {
        String userId = RandomStringUtils.randomNumeric(10);
        createCartList(userId, 10, 3, 0, true);

        var itemOffers = ydbDao.loadItemsWithUserAndColor(UserContext.of(WHITE, userId, YANDEXUID));
        List<OfferInfoWithPromo> offerInfos = itemOffers.stream()
                .map(item -> new OfferInfoWithPromo(
                        item.getActualizedObjId(),
                        new BigDecimal(RandomUtils.nextInt(100, 1000)),
                        generatePromo(RandomUtils.nextInt(1, 5)),
                        generateDiscount()
                ))
                .collect(Collectors.toList());
        when(reportService.executeSearchAndParse(Mockito.any(), Mockito.any())).thenReturn(offerInfos);

        mockMvc.perform(get(String.format("/cart/YANDEXUID/%s/light-list", userId))
                .param(PARAM_RGB, "WHITE")
                .param(PARAM_UPDATE_ACTUAL_STATE, "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items[*].actualPrice")
                        .value(Matchers.containsInAnyOrder(offerInfos.stream()
                                .map(OfferInfoWithPromo::getPrice)
                                .map(BigDecimal::intValue)
                                .toArray())))
                .andExpect(jsonPath("$[0].items[*].promos[*].endDate")
                        .value(Matchers.containsInAnyOrder(extractPromoAttribute(offerInfos, "endDate"))))
                .andExpect(jsonPath("$[0].items[*].promos[*].startDate")
                        .value(Matchers.containsInAnyOrder(extractPromoAttribute(offerInfos, "startDate"))))
                .andExpect(jsonPath("$[0].items[*].promos[*].promoKey")
                        .value(Matchers.containsInAnyOrder(extractPromoAttribute(offerInfos, "promoKey"))))
                .andExpect(jsonPath("$[0].items[*].promos[*].promoType")
                        .value(Matchers.containsInAnyOrder(extractPromoAttribute(offerInfos, "promoType"))))
                .andExpect(jsonPath("$[0].items[*].actualDiscount.oldMin")
                        .value(Matchers.containsInAnyOrder(extractDiscountAttribute(offerInfos, "oldMin"))))
                .andExpect(jsonPath("$[0].items[*].actualDiscount.percent")
                        .value(Matchers.containsInAnyOrder(extractDiscountAttribute(offerInfos, "percent"))))
                .andExpect(jsonPath("$[0].items[*].actualDiscount.isBestDeal")
                        .value(Matchers.containsInAnyOrder(extractDiscountAttribute(offerInfos, "isBestDeal"))))
                .andExpect(jsonPath("$[0].items[*].actualDiscount.absolute")
                        .value(Matchers.containsInAnyOrder(extractDiscountAttribute(offerInfos, "absolute"))));
    }

    @Nonnull
    private String[] extractPromoAttribute(List<OfferInfoWithPromo> offerInfos, String name) {
        List<String> values = new ArrayList<>();
        offerInfos.forEach(info -> {
            JSONArray promos = info.getPromo();
            for (Object obj : promos) {
                JSONObject promo = (JSONObject) obj;
                values.add(promo.getString(name));
            }
        });

        return values.toArray(new String[]{});
    }

    @Nonnull
    private Object[] extractDiscountAttribute(List<OfferInfoWithPromo> offerInfos, String name) {
        List<Object> values = new ArrayList<>();
        offerInfos.forEach(info -> {
            JSONObject discount = info.getDiscount();
            values.add(discount.get(name));
        });

        return values.toArray(new Object[]{});
    }

    @Nonnull
    private JSONArray generatePromo(int count) {
        var promos = new JSONArray();
        for (int i = 0; i < count; i++) {
            promos.put(Map.of(
                    "promoKey", RandomStringUtils.randomAlphabetic(10),
                    "promoType", RandomStringUtils.randomAlphabetic(10),
                    "endDate", LocalDateTime.now().plus(RandomUtils.nextInt(1, 10), ChronoUnit.HOURS),
                    "startDate", LocalDateTime.now().minus(RandomUtils.nextInt(1, 10), ChronoUnit.HOURS)
            ));
        }

        return promos;
    }

    @Nonnull
    private JSONObject generateDiscount() {
        var discount = new JSONObject();

        discount.put("oldMin", RandomUtils.nextInt(100, 1000));
        discount.put("percent", RandomUtils.nextInt(0, 100));
        discount.put("isBestDeal", RandomUtils.nextBoolean());
        discount.put("absolute", RandomUtils.nextInt(100, 1000));

        return discount;
    }
}
