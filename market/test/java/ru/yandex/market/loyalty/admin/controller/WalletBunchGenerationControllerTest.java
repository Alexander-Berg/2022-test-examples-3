package ru.yandex.market.loyalty.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.bunch.wallet.WalletBunchSaveRequest;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.model.GenericParam;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.rule.ParamsContainer;
import ru.yandex.market.loyalty.core.service.BunchGenerationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@TestFor(WalletBunchGenerationController.class)
public class WalletBunchGenerationControllerTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String DEFAULT_REQUEST_ID = "someTestKey";

    @Autowired
    private MockMvc mockMvc;
    @Qualifier("objectMapper")
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private BunchGenerationService bunchGenerationService;
    @Autowired
    private BunchGenerationRequestDao bunchGenerationRequestDao;
    private Long testPromoId;
    private Long testRequestId;

    @Before
    public void setUp() {
        testPromoId = promoManager.createAccrualPromo(
                PromoUtils.WalletAccrual.defaultModelAccrual()
        ).getId();
        var paramsContainer = new ParamsContainer<BunchGenerationRequestParamName<?>>();

        ParamsContainer.addParam(
                paramsContainer,
                BunchGenerationRequestParamName.INPUT_TABLE,
                GenericParam.of("//tmp/input_table")
        );
        String key = bunchGenerationService.saveRequestAsScheduledReturningBunchId(
                BunchGenerationRequest.builder()
                        .setKey(DEFAULT_REQUEST_ID)
                        .setPromoId(testPromoId)
                        .setSource("Some source")
                        .setCount(100)
                        .setGeneratorType(GeneratorType.YANDEX_WALLET)
                        .setEmail("email")
                        .setParamsContainer(paramsContainer)
                        .build()
        );
        final BunchGenerationRequest request = bunchGenerationService.getBunchGenerationRequest(key);
        testRequestId = request.getId();
    }

    @Test
    public void saveBunchRequest() throws Exception {
        final WalletBunchSaveRequest request = new WalletBunchSaveRequest(
                DEFAULT_REQUEST_ID,
                testPromoId,
                null,
                false,
                100,
                "table/input",
                "table/output",
                null,
                null,
                "test_campaign",
                "productId"
        );

        mockMvc.perform(
                post("/api/bunchRequest/wallet/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void cancelBunchRequest() throws Exception {
        bunchGenerationRequestDao.updateRequestStatus(testRequestId, BunchGenerationRequestStatus.PROCESSED);
        mockMvc.perform(
                put("/api/bunchRequest/wallet/refund")
                        .param("requestId", testRequestId.toString())
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
