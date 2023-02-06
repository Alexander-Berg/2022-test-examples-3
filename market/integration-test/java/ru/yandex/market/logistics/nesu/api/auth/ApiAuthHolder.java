package ru.yandex.market.logistics.nesu.api.auth;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.http.HttpHeaders;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.common.services.auth.blackbox.OAuthInfo;
import ru.yandex.market.core.contact.InnerRole;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiAuthHolder {

    @Getter
    private final String token = UUID.randomUUID().toString();
    private final Long userId = new Random().nextLong();
    private final ObjectMapper objectMapper;

    public ApiAuthHolder(BlackboxService blackboxService, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        when(blackboxService.oauth(token)).thenReturn(createInfo(Map.of(
            "error", "OK",
            "scope", Set.of("delivery:partner-api"),
            "uid", userId
        )));
    }

    public HttpHeaders authHeaders() {
        HttpHeaders result = new HttpHeaders();
        result.add(HttpHeaders.AUTHORIZATION, "OAuth " + token);
        return result;
    }

    public void mockAccess(MbiApiClient mbiApiClient, Long partnerId) {
        mockHasAccess(mbiApiClient, partnerId, true);
    }

    public void mockNoAccess(MbiApiClient mbiApiClient, Long partnerId) {
        mockHasAccess(mbiApiClient, partnerId, false);
    }

    private void mockHasAccess(MbiApiClient mbiApiClient, Long partnerId, boolean hasAccess) {
        when(mbiApiClient.checkContactRole(userId, partnerId, InnerRole.SHOP_ADMIN)).thenReturn(hasAccess);
    }

    public void verifyCheckAccess(MbiApiClient mbiApiClient, Long partnerId) {
        verify(mbiApiClient).checkContactRole(userId, partnerId, InnerRole.SHOP_ADMIN);
    }

    // Обходим package-private доступ к полям
    OAuthInfo createInfo(Map<String, ?> fields) {
        return objectMapper.convertValue(fields, OAuthInfo.class);
    }

}
