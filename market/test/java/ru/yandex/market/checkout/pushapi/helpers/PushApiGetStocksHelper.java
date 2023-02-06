package ru.yandex.market.checkout.pushapi.helpers;

import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.pushapi.client.entity.StocksRequest;
import ru.yandex.market.checkout.pushapi.client.entity.StocksResponse;
import ru.yandex.market.checkout.pushapi.providers.SettingsProvider;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;
import ru.yandex.market.checkout.util.shopapi.ShopApiConfigurer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebTestHelper
public class PushApiGetStocksHelper extends MockMvcAware {

    private final SettingsService settingsService;
    private final SettingsProvider settingsProvider;
    private final ShopApiConfigurer shopApiConfigurer;

    public PushApiGetStocksHelper(MockMvc mockMvc,
                                  PushApiTestSerializationService testSerializationService,
                                  SettingsService settingsService,
                                  SettingsProvider settingsProvider,
                                  ShopApiConfigurer shopApiConfigurer) {
        super(mockMvc, testSerializationService);
        this.settingsService = settingsService;
        this.settingsProvider = settingsProvider;
        this.shopApiConfigurer = shopApiConfigurer;
    }

    public StocksResponse queryStocks(PushApiQueryStocksParameters parameters, ResultMatcher status) throws Exception {
        setupSettings(parameters.getShopId(), parameters.isSandbox(), parameters.isPartnerInterface(),
                parameters.getDataType());
        setupMock(parameters);

        return performQueryStocks(parameters.getShopId(), parameters.getRequest(), status);
    }

    public StocksResponse queryStocksTimeout(PushApiQueryStocksParameters parameters, ResultMatcher status)
            throws Exception {
        setupSettings(parameters.getShopId(), parameters.isSandbox(), parameters.isPartnerInterface(),
                parameters.getDataType());
        setupMock(parameters);

        return performQueryStocksTimeout(parameters.getShopId(), parameters.getRequest(), status);
    }

    public StocksResponse queryStocksWithoutSettings(PushApiQueryStocksParameters parameters, ResultMatcher status) throws Exception {
        setupMock(parameters);

        return performQueryStocks(parameters.getShopId(), parameters.getRequest(), status);
    }

    private void setupSettings(long shopId, boolean sandbox, boolean partnerInterface, DataType dataType) {
        settingsService.updateSettings(
                shopId,
                settingsProvider.buildXmlSettings(partnerInterface, dataType),
                sandbox
        );
    }

    private void setupMock(PushApiQueryStocksParameters parameters) {
        shopApiConfigurer.mockStocks(parameters);
    }

    public List<ServeEvent> getServeEvents() {
        return shopApiConfigurer.getServeEvents();
    }

    private StocksResponse performQueryStocks(long shopId, StocksRequest request, ResultMatcher status) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/shops/{shopId}/stocks", shopId)
                        .content(testSerializationService.serialize(request))
                        .contentType(MediaType.APPLICATION_XML)
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult = mockMvc.perform(asyncDispatch(mvcResult))
                .andDo(log())
                .andExpect(status)
                .andReturn();

        return testSerializationService.deserialize(mvcResult.getResponse().getContentAsString(), StocksResponse.class);
    }

    private StocksResponse performQueryStocksTimeout(long shopId, StocksRequest request, ResultMatcher status)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/shops/{shopId}/stocks", shopId)
                        .content(testSerializationService.serialize(request))
                        .contentType(MediaType.APPLICATION_XML)
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult.getRequest().getAsyncContext().setTimeout(30000);
        mvcResult.getAsyncResult();

        mvcResult = mockMvc.perform(asyncDispatch(mvcResult))
                .andDo(log())
                .andExpect(status)
                .andReturn();

        return testSerializationService.deserialize(mvcResult.getResponse().getContentAsString(), StocksResponse.class);
    }

}
