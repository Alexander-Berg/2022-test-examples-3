package ru.yandex.market.pvz.internal.mvc;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.NativeWebRequest;

import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PrePickupPointRequestData;
import ru.yandex.market.pvz.core.test.TestExternalConfiguration;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;


@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PrePickupPointMethodArgumentResolverTest {

    private final PrePickupPointMethodArgumentResolver argumentResolver;

    private final TestCrmPrePickupPointFactory crmPrePickupPointFactory;

    private final LegalPartnerQueryService legalPartnerQueryService;

    @Test
    void testCrmPrePickupPoint() {
        CrmPrePickupPointParams prePickupPoint = crmPrePickupPointFactory.create();
        LegalPartnerParams legalPartner = legalPartnerQueryService.get(prePickupPoint.getLegalPartnerId());

        NativeWebRequest webRequest = createWebRequest(legalPartner.getPartnerId(), prePickupPoint.getId());
        var data = (PrePickupPointRequestData) argumentResolver.resolveArgument(null, null, webRequest, null);

        assertThat(data).isEqualTo(new PrePickupPointRequestData(
                prePickupPoint.getId(),
                legalPartner.getId(),
                null,
                prePickupPoint.getName(),
                TestExternalConfiguration.DEFAULT_UID
        ));
    }

    private NativeWebRequest createWebRequest(long partnerId, long prePickupPointId) {
        var webRequest = mock(NativeWebRequest.class);
        when(webRequest.getAttribute(eq(URI_TEMPLATE_VARIABLES_ATTRIBUTE), eq(SCOPE_REQUEST))).thenReturn(Map.of(
                "partnerId", String.valueOf(partnerId),
                "prePickupPointId", String.valueOf(prePickupPointId)
        ));
        return webRequest;
    }

}
