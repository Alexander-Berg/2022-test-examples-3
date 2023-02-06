package ru.yandex.market.loyalty.core.test;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.core.config.BusinessRulesEngine;
import ru.yandex.market.loyalty.core.trigger.restrictions.segments.UserSegmentsResponse;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Service
public class BREMockUtils {
    private final RestTemplate restTemplate;

    public BREMockUtils(@BusinessRulesEngine RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void mockBREGetUserSegmentsResponse(String... segments) {
        when(restTemplate.getForObject(anyString(), eq(UserSegmentsResponse.class)))
                .thenReturn(createUserSegmentsResponse(segments));
    }

    public void mockBREGetUserSegmentsError() {
        when(restTemplate.getForObject(anyString(), eq(UserSegmentsResponse.class)))
                .thenThrow(RestClientException.class);
    }

    @NotNull
    private static UserSegmentsResponse createUserSegmentsResponse(String... segments) {
        final UserSegmentsResponse response = new UserSegmentsResponse();
        for (String segment : segments) {
            response.putSegment(segment, true);
        }
        return response;
    }
}
