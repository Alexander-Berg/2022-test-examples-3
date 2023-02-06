package ru.yandex.market.fulfillment.wrap.marschroute.service.outbound;

import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.entity.OutboundDispatchInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.repository.OutboundDispatchInfoRepository;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundDispatchInfoServiceTest {

    @Mock
    private OutboundDispatchInfoRepository repository;

    @InjectMocks
    private OutboundDispatchInfoService service;

    @Test
    void registerMapping() {

        service.registerMapping(new ResourceId("ya-id", "partner-id"), "dispatch-id");

        ArgumentMatcher<OutboundDispatchInfo> matcher = toArgumentMatcher(info ->
            "ya-id".equals(info.getYandexId()) &&
                "partner-id".equals(info.getPartnerId()) &&
                "dispatch-id".equals(info.getDispatchPartnerId()) &&
                info.getCreated() != null);

        verify(repository, times(1)).insertIfMissing(argThat(matcher));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findUsingCache() {
        ResourceId outboundId = new ResourceId("ya-id", "partner-id");
        Optional<OutboundDispatchInfo> expected = Optional.of(OutboundDispatchInfo.of(outboundId, "partner-id"));

        when(repository.findByOutboundId(outboundId)).thenReturn(expected);

        assertEquals(expected, service.find(outboundId));
        verify(repository, times(1)).findByOutboundId(outboundId);

        assertEquals(expected, service.find(outboundId));
        assertEquals(expected, service.find(outboundId));
        verifyNoMoreInteractions(repository);
    }


    private static <T> ArgumentMatcher<T> toArgumentMatcher(Predicate<T> predicate) {
        return new ArgumentMatcher<T>() {
            @Override
            public boolean matches(Object argument) {
                return predicate.test((T) argument);
            }
        };
    }

}
