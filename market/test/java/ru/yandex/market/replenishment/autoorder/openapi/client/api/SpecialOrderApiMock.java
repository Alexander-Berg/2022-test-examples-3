package ru.yandex.market.replenishment.autoorder.openapi.client.api;


import ru.yandex.market.replenishment.autoorder.openapi.client.ApiException;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApproveSpecialOrderRequest;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApproveSpecialOrderResponse;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.CreateSpecialOrderRequest;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.CreateSpecialOrderResponse;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.CreatedDemandIdsResponseDTO;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.DeclineSpecialOrderRequest;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.MessageDTO;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.StarTrekTicketUpdateRequest;

public class SpecialOrderApiMock extends SpecialOrderApi {

    public SpecialOrderApiMock() {
        super(null);
    }

    @Override
    public ApproveSpecialOrderResponse approveAll(ApproveSpecialOrderRequest request) throws ApiException {
        throw new ApiException("Response in not set up or request was built in wrong way \n" + request.toString());
    }

    @Override
    public ApproveSpecialOrderResponse approveAny(ApproveSpecialOrderRequest request) throws ApiException {
        throw new ApiException("Response in not set up or request was built in wrong way \n" + request.toString());
    }

    @Override
    public CreateSpecialOrderResponse createAll(CreateSpecialOrderRequest request) throws ApiException {
        throw new ApiException("Response in not set up or request was built in wrong way: \n" + request.toString());
    }

    @Override
    public CreateSpecialOrderResponse createAny(CreateSpecialOrderRequest request) throws ApiException {
        throw new ApiException("Response in not set up or request was built in wrong way \n" + request.toString());
    }

    @Override
    public MessageDTO declineRest(DeclineSpecialOrderRequest request) throws ApiException {
        throw new ApiException("Response in not set up or request was built in wrong way \n" + request.toString());
    }

    @Override
    public CreatedDemandIdsResponseDTO finalizeProccess(StarTrekTicketUpdateRequest request) throws ApiException {
        throw new ApiException("Response in not set up or request was built in wrong way \n" + request.toString());
    }
}
