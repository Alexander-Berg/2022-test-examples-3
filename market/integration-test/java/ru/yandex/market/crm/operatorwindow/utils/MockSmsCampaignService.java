package ru.yandex.market.crm.operatorwindow.utils;

import java.util.Collections;

import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.operatorwindow.informing.CreateSmsCampaignRequest;
import ru.yandex.market.crm.operatorwindow.informing.SmsCampaignService;
import ru.yandex.market.crm.operatorwindow.informing.SmsReceiverLite;
import ru.yandex.market.crm.operatorwindow.informing.ValidatedSmsCampaign;
import ru.yandex.market.crm.util.CrmCollections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;

@Component
public class MockSmsCampaignService extends AbstractMockService<SmsCampaignService> {
    private final SmsCampaignService smsCampaignService;

    public MockSmsCampaignService(SmsCampaignService smsCampaignService) {
        super(smsCampaignService);
        this.smsCampaignService = smsCampaignService;
    }

    public void mockSendUnreachableSmsClientMessage(Long orderId) {
        Mockito
                .when(smsCampaignService
                        .createValidatedSmsCampaign(
                                argThat(new CreateSmsCampaignMatcher(orderId))))
                .thenReturn(new ValidatedSmsCampaign(123456L, Collections.emptySet()));
    }

    public void mock() {
        Mockito.when(smsCampaignService.createValidatedSmsCampaign(any()))
                .thenReturn(new ValidatedSmsCampaign(123L, Collections.emptySet()));
    }


    public void verifySendUnreachableSmsClientMessage(Long orderId) {
        Mockito.verify(smsCampaignService, times(1))
                .createValidatedSmsCampaign(
                        argThat(new CreateSmsCampaignMatcher(orderId)));
    }

    public class CreateSmsCampaignMatcher implements ArgumentMatcher<CreateSmsCampaignRequest<SmsReceiverLite>> {
        private final Long expectedOrderId;


        public CreateSmsCampaignMatcher(long expectedOrderId) {
            this.expectedOrderId = expectedOrderId;
        }

        @Override
        public boolean matches(CreateSmsCampaignRequest<SmsReceiverLite> argument) {
            if (null == argument) {
                return false;
            }
            if (CrmCollections.isEmpty(argument.getSmsReceivers())) {
                return false;
            }
            return argument.getSmsReceivers().size() == 1
                    && argument.getSmsReceivers().get(0).getOrderId() == this.expectedOrderId;
        }
    }
}
