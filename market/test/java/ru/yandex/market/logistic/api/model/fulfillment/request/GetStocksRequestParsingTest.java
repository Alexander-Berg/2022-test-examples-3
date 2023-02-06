package ru.yandex.market.logistic.api.model.fulfillment.request;

import java.util.List;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.request.Token;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class GetStocksRequestParsingTest extends ParsingWrapperTest<RequestWrapper, GetStocksRequest> {

    public GetStocksRequestParsingTest() {
        super(RequestWrapper.class, GetStocksRequest.class, "fixture/request/get_stocks_request.xml");
    }

    @Override
    protected void performAdditionalAssertions(RequestWrapper requestWrapper) {
        assertions().assertThat(requestWrapper.getHash())
            .as("Asserting hash value")
            .isEqualTo("36fc8f6373206300cd2d3350611cc50c");

        assertions().assertThat(requestWrapper.getToken())
            .as("Asserting token value")
            .isEqualTo(new Token("zawr8kexa3Re7ecrusagus3estesapav4Uph7yavu5achustum4brutep2thatrE"));

        GetStocksRequest getStocksRequest = (GetStocksRequest) requestWrapper.getRequest();

        assertions().assertThat(getStocksRequest.getType())
            .as("Assertint type value")
            .isEqualTo("getStocks");

        assertions().assertThat(getStocksRequest.getLimit())
            .as("Asserting limit value")
            .isEqualTo(10);

        assertions().assertThat(getStocksRequest.getOffset())
            .as("Asserting offset value")
            .isEqualTo(10);

        List<UnitId> unitIds = getStocksRequest.getUnitIds();
        assertions().assertThat(unitIds)
            .as("Asserting unit ids have exactly 1 element")
            .hasSize(1);

        UnitId unitId = unitIds.get(0);

        assertions().assertThat(unitId.getId())
            .as("Asserting unit id ID value")
            .isEqualTo("1");

        assertions().assertThat(unitId.getVendorId())
            .as("Asserting unit id VENDOR ID value")
            .isEqualTo(2);

        assertions().assertThat(unitId.getArticle())
            .as("Asserting unit id ARTICLE value")
            .isEqualTo("AAA");

    }
}
