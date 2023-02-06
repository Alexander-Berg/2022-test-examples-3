package ru.yandex.market.ff.dbqueue.service;

import javax.annotation.Nonnull;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.dbqueue.exceptions.ValidationOfRequestInWrongStatusException;
import ru.yandex.market.ff.dbqueue.exceptions.ValidationShouldNotBeDone;
import ru.yandex.market.ff.model.TypeSubtype;
import ru.yandex.market.ff.model.dbqueue.ValidateRequestPayload;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.RequestPostProcessService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.RequestValidationService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ValidateShopRequestProcessingServiceTest extends IntegrationTest {

    private static final long REQUEST_ID = 123;
    private static final ShopRequest CREATED_REQUEST = createShopRequest(RequestStatus.CREATED, RequestType.SUPPLY);
    private final RequestSubTypeEntity requestSubTypeEntity = mock(RequestSubTypeEntity.class);

    private ShopRequestFetchingService shopRequestFetchingService;
    private RequestValidationService requestValidationService;
    private ValidateShopRequestProcessingService validateShopRequestProcessingService;
    private SoftAssertions assertions;
    private RequestSubTypeService requestSubTypeService;
    private ValidateWithRegistryProcessingService validateWithRegistryProcessingService;
    private RequestPostProcessService requestPostProcessService;

    @BeforeEach
    public void init() {
        shopRequestFetchingService = mock(ShopRequestFetchingService.class);
        requestValidationService = mock(RequestValidationService.class);
        requestSubTypeService = mock(RequestSubTypeService.class);
        validateWithRegistryProcessingService = mock(ValidateWithRegistryProcessingService.class);
        requestPostProcessService = mock(RequestPostProcessService.class);
        validateShopRequestProcessingService = new ValidateShopRequestProcessingService(
                shopRequestFetchingService,
                validateWithRegistryProcessingService,
                requestValidationService,
                requestSubTypeService,
                requestPostProcessService
        );
        assertions = new SoftAssertions();

        when(requestSubTypeService.getEntityByRequestTypeAndSubtype(new TypeSubtype(RequestType.SUPPLY, "DEFAULT")))
            .thenReturn(requestSubTypeEntity);

        when(shopRequestFetchingService.getRequestWithShippersAndReceiversOrThrow(REQUEST_ID))
                .thenReturn(CREATED_REQUEST);
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    @Test
    public void processWhenShouldBeProcessed() {
        when(requestSubTypeEntity.getRegistryBasedValidation()).thenReturn(Boolean.FALSE);
        when(requestSubTypeEntity.isToBeValidated()).thenReturn(false);
        assertThrows(ValidationShouldNotBeDone.class, () ->
            validateShopRequestProcessingService.processPayload(createPayload()));
        verify(shopRequestFetchingService).getRequestWithShippersAndReceiversOrThrow(REQUEST_ID);
        verify(requestValidationService, never()).validateAndPrepare(any());
    }

    @Test
    public void processPayloadForOrderNotInCreatedStatus() {
        when(requestSubTypeEntity.getRegistryBasedValidation()).thenReturn(Boolean.FALSE);
        when(requestSubTypeEntity.isToBeValidated()).thenReturn(true);
        when(requestSubTypeService.getEntityByRequestTypeAndSubtype(any())).thenReturn(requestSubTypeEntity);
        for (RequestStatus status : RequestStatus.values()) {
            if (status == RequestStatus.CREATED) {
                continue;
            }
            when(shopRequestFetchingService.getRequestWithShippersAndReceiversOrThrow(REQUEST_ID))
                .thenReturn(createShopRequest(status, RequestType.SUPPLY));
            assertThrows(ValidationOfRequestInWrongStatusException.class, () ->
                validateShopRequestProcessingService.processPayload(createPayload()));
        }
        verify(requestValidationService, never()).validateAndPrepare(any());
    }

    @Test
    public void processPayloadWithRegistryBasedValidation() {
        when(requestSubTypeEntity.getRegistryBasedValidation()).thenReturn(Boolean.TRUE);
        validateShopRequestProcessingService.processPayload(createPayload());
        verify(validateWithRegistryProcessingService).processShopRequest(any());
    }

    @Test
    public void processPayloadForCorrectOrder() {
        when(requestSubTypeEntity.getRegistryBasedValidation()).thenReturn(Boolean.FALSE);
        when(requestSubTypeEntity.isToBeValidated()).thenReturn(true);
        validateShopRequestProcessingService.processPayload(createPayload());
        verify(requestValidationService).validateAndPrepare(CREATED_REQUEST);
    }

    @Nonnull
    private ValidateRequestPayload createPayload() {
        return new ValidateRequestPayload(REQUEST_ID);
    }

    @Nonnull
    private static ShopRequest createShopRequest(@Nonnull RequestStatus requestStatus,
                                                 @Nonnull RequestType requestType) {
        ShopRequest request = new ShopRequest();
        request.setId(REQUEST_ID);
        request.setStatus(requestStatus);
        request.setType(requestType);
        return request;
    }
}
