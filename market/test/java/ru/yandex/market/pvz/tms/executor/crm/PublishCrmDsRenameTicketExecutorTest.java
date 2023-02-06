package ru.yandex.market.pvz.tms.executor.crm;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryService;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryServiceCommandService;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryServiceRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.tpl.common.startrek.StartrekService;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicketData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParamsBuilder;

@TransactionlessEmbeddedDbTest
@Import({PublishCrmDsRenameTicketExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PublishCrmDsRenameTicketExecutorTest {

    private static final String TICKET = "TICKET-123";

    private final DeliveryServiceCommandService deliveryServiceCommandService;
    private final DeliveryServiceRepository deliveryServiceRepository;
    private final TestDeliveryServiceFactory deliveryServiceFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TransactionTemplate transactionTemplate;

    private final PublishCrmDsRenameTicketExecutor executor;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private StartrekService startrekService;

    @BeforeEach
    void setup() {
        deliveryServiceRepository.deleteAll();

        reset(startrekService);
        when(startrekService.createTicket(any(StartrekTicketData.class)).getKey())
                .thenReturn(TICKET);
    }

    @Test
    @SneakyThrows
    void testPublish() {
        DeliveryService ds = createDsWithPartner(null);
        executor.doRealJob(null);
        assertThat(deliveryServiceRepository.findByIdOrThrow(ds.getId()).getTicketRenamedIn()).isEqualTo(TICKET);
        verify(startrekService, times(1)).createTicket(any(StartrekTicketData.class));
    }

    @Test
    @SneakyThrows
    void testNotPublish() {
        DeliveryService ds = createDsWithPartner(TICKET);
        executor.doRealJob(null);
        verify(startrekService, times(0)).createTicket(any(StartrekTicketData.class));
    }

    private DeliveryService createDsWithPartner(String ticketRenamedIn) {
        return transactionTemplate.execute(s -> {
            DeliveryService ds = deliveryServiceFactory.createDeliveryService();

            if (ticketRenamedIn != null) {
                deliveryServiceCommandService.markRenamedInCrm(List.of(ds.getId()), ticketRenamedIn);
            }

            legalPartnerFactory.createLegalPartner(LegalPartnerTestParamsBuilder.builder()
                    .deliveryService(ds)
                    .build());

            return ds;
        });
    }

}
