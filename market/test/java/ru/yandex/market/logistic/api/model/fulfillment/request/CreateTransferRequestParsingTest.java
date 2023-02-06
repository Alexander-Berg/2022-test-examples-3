package ru.yandex.market.logistic.api.model.fulfillment.request;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.request.Token;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.Transfer;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.TransferParsingTest;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class CreateTransferRequestParsingTest extends ParsingWrapperTest<RequestWrapper, CreateTransferRequest> {

    public CreateTransferRequestParsingTest() {
        super(RequestWrapper.class, CreateTransferRequest.class, "fixture/request/ff_create_transfer_request.xml");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.<String, Object>builder()
            .put("hash", "36fc8f6373206300cd2d3350611cc50c")
            .build();
    }

    @Override
    protected void performAdditionalAssertions(RequestWrapper requestWrapper) {
        assertions().assertThat(requestWrapper.getToken())
            .as("Asserting token value")
            .isEqualTo(new Token("zawr8kexa3Re7ecrusagus3estesapav4Uph7yavu5achustum4brutep2thatrE"));

        CreateTransferRequest createTransferRequest = (CreateTransferRequest) requestWrapper.getRequest();

        assertions().assertThat(createTransferRequest.getType())
            .as("Asserting content type value")
            .isEqualTo("createTransfer");

        assertions().assertThat(createTransferRequest.getTransfer())
            .as("Asserting transfer is not null")
            .isNotNull();

        ImmutableMap<String, Object> values = TransferParsingTest.TRANSFER_VALUES;

        assertTransferValues(values, createTransferRequest.getTransfer());
    }

    private void assertTransferValues(ImmutableMap<String, Object> values, Transfer transfer) {
        values.forEach((key, value) -> assertions()
            .assertThat(transfer)
            .hasFieldOrPropertyWithValue(key, value));
    }
}
