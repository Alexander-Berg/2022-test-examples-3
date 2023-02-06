package ru.yandex.direct.api.v5.entity.bids.delegate;

import com.yandex.direct.api.v5.bids.SetRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.bids.converter.BidsHelperConverter;
import ru.yandex.direct.api.v5.entity.bids.service.validation.SetBidsRequestValidationService;
import ru.yandex.direct.api.v5.entity.bids.validation.BidsInternalValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.core.entity.bids.service.BidService;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class SetBidsDelegateTest {
    @Mock
    SetBidsRequestValidationService requestValidationService;

    @Mock
    BidsHelperConverter requestConverter;

    @Mock
    ApiAuthenticationSource authenticationSource;

    @Mock
    BidService service;

    @Mock
    BidsInternalValidationService unsupportedIdsValidationService;

    @Mock
    ResultConverter resultConverter;

    @Mock
    ApiContextHolder contextHolder;

    @InjectMocks
    public SetBidsDelegate delegate;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void validateRequest_validatesWithValidationService() {
        SetRequest request = new SetRequest();
        delegate.validateRequest(request);

        verify(requestValidationService).validate(request);
    }
}
