package ru.yandex.market.checkout.util.loyalty;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import ru.yandex.market.loyalty.api.model.discount.RecalculateCashbackItemRequest;
import ru.yandex.market.loyalty.api.model.discount.RecalculateCashbackItemResponse;
import ru.yandex.market.loyalty.api.model.discount.RecalculateCashbackOrderResponse;
import ru.yandex.market.loyalty.api.model.discount.RecalculateCashbackRequest;
import ru.yandex.market.loyalty.api.model.discount.RecalculateCashbackResponse;

import static ru.yandex.common.util.ObjectUtils.avoidNull;

public class ReCalcResponseTranformer extends AbstractLoyaltyBundleResponseTransformer {

    public ReCalcResponseTranformer(ObjectMapper marketLoyaltyObjectMapper) {
        super(marketLoyaltyObjectMapper);
    }

    @Override
    public ResponseDefinition transform(
            Request request, ResponseDefinition responseDefinition, FileSource files,
            Parameters parameters
    ) {
        LoyaltyParameters loyaltyParameters = (LoyaltyParameters) parameters.get("loyaltyParameters");
        RecalculateCashbackRequest recalculateCashbackRequest;
        try {
            recalculateCashbackRequest = getRequest(request, RecalculateCashbackRequest.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RecalculateCashbackResponse response = RecalculateCashbackResponse.builder()
                .setCashback(
                        avoidNull(
                                loyaltyParameters.getCalcsExpectedCashbackResponse(),
                                defaultCashback(loyaltyParameters.getSelectedCashbackOption())
                        )
                )
                .setOrders(createRecaluclateCashbackOrderResponse(recalculateCashbackRequest, loyaltyParameters))
                .build();

        try {
            return buildResponse(responseDefinition, response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<RecalculateCashbackOrderResponse> createRecaluclateCashbackOrderResponse(
            RecalculateCashbackRequest request,
            LoyaltyParameters loyaltyParameters
    ) {
        return request.getOrders().stream().map(
                order -> RecalculateCashbackOrderResponse.builder()
                        .setCartId(order.getCartId())
                        .setOrderId(order.getOrderId())
                        .setCashback(avoidNull(
                                loyaltyParameters.getCalcsExpectedCashbackResponse(),
                                defaultCashback(loyaltyParameters.getSelectedCashbackOption())
                        ))
                        .setItems(createRecalculateCashbackItemResponse(order.getItems(), loyaltyParameters))
                        .build()
        ).collect(Collectors.toList());
    }

    private List<RecalculateCashbackItemResponse> createRecalculateCashbackItemResponse(
            List<RecalculateCashbackItemRequest> items,
            LoyaltyParameters loyaltyParameters
    ) {
        return items.stream().map(
                item -> RecalculateCashbackItemResponse.builder()
                        .setCashback(avoidNull(
                                loyaltyParameters.getCalcsExpectedCashbackResponse(),
                                defaultCashback(loyaltyParameters.getSelectedCashbackOption())
                        ))
                        .setFeedId(item.getFeedId())
                        .setIsDownloadable(item.isDownloadable())
                        .setOfferId(item.getOfferId())
                        .setPrice(item.getPrice())
                        .setQuantity(item.getQuantity())
                        .build()
        ).collect(Collectors.toList());
    }


    @Override
    public String getName() {
        return "loyalty-recalc-cashback-transformer";
    }
}
