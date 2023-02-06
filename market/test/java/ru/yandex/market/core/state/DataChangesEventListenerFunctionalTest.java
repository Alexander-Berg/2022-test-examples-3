package ru.yandex.market.core.state;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.state.event.BusinessChangesProtoLBEvent;
import ru.yandex.market.core.state.event.ContactChangesProtoLBEvent;
import ru.yandex.market.core.state.event.PartnerAppChangesProtoLBEvent;
import ru.yandex.market.core.state.event.PartnerChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Проверяем работу слушателя {@link DataChangesEventListener}.
 */
@DbUnitDataSet(before = "DataChangesEventListenerTest.before.csv")
class DataChangesEventListenerFunctionalTest extends FunctionalTest {
    private static final Instant EVENT_TIME = Instant.ofEpochSecond(1633517987000L);

    @Autowired
    private DataChangesEventListener dataChangesEventListener;
    @Autowired
    private LogbrokerEventPublisher<BusinessChangesProtoLBEvent> logbrokerBusinessChangesEventPublisher;
    @Autowired
    private LogbrokerEventPublisher<PartnerChangesProtoLBEvent> logbrokerPartnerChangesEventPublisher;
    @Autowired
    private LogbrokerEventPublisher<PartnerAppChangesProtoLBEvent> logbrokerPartnerAppChangesEventPublisher;
    @Autowired
    private LogbrokerEventPublisher<ContactChangesProtoLBEvent> logbrokerContactChangesEventPublisher;

    @BeforeEach
    void setUp() {
        when(logbrokerBusinessChangesEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
        when(logbrokerPartnerChangesEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
        when(logbrokerPartnerAppChangesEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
        when(logbrokerContactChangesEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
    }

    @Test
    void checkCoreBusinessFunctionality() {
        dataChangesEventListener.onApplicationEvent(new DataChangesEvent(EVENT_TIME, 100L,
                DataChangesEvent.PartnerDataType.BUSINESS_DATA, DataChangesEvent.PartnerDataOperation.CREATE));
        var businessEventsCaptor = ArgumentCaptor.forClass(BusinessChangesProtoLBEvent.class);
        verify(logbrokerBusinessChangesEventPublisher, atLeastOnce()).publishEventAsync(businessEventsCaptor.capture());
        assertThat(businessEventsCaptor.getValue().getPayload().getBusinessId()).isEqualTo(100L);
    }

    @Test
    void checkCorePartnerFunctionality() {
        Instant changesTime = Instant.ofEpochSecond(1633517987000L);
        dataChangesEventListener.onApplicationEvent(new DataChangesEvent(changesTime,
                775L, DataChangesEvent.PartnerDataType.PARTNER_DATA,
                DataChangesEvent.PartnerDataOperation.UPDATE));
        var eventCaptor = ArgumentCaptor.forClass(PartnerChangesProtoLBEvent.class);
        verify(logbrokerPartnerChangesEventPublisher, atLeastOnce()).publishEventAsync(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPayload().getPartnerId()).isEqualTo(775L);
        assertThat(eventCaptor.getValue().getPayload().getBusinessId()).isEqualTo(100L);
        assertThat(eventCaptor.getValue().getPayload().getInternalName()).isEqualTo("test.ru");
    }

    @Test
    void checkCorePartnerAppFunctionality() {
        dataChangesEventListener.onApplicationEvent(new DataChangesEvent(EVENT_TIME,
                103L, DataChangesEvent.PartnerDataType.PARTNER_APP_DATA,
                DataChangesEvent.PartnerDataOperation.UPDATE));
        var partnerAppEventsCaptor = ArgumentCaptor.forClass(PartnerAppChangesProtoLBEvent.class);
        verify(logbrokerPartnerAppChangesEventPublisher, atLeastOnce()).publishEventAsync(partnerAppEventsCaptor.capture());
        assertThat(new HashSet<>(partnerAppEventsCaptor.getValue().getPayload().getPartnerIdsList())).isEqualTo(Set.of(773L, 774L));
    }

    @Test
    void checkCoreContactFunctionality() {
        dataChangesEventListener.onApplicationEvent(new DataChangesEvent(EVENT_TIME,
                10L, DataChangesEvent.PartnerDataType.CONTACT_DATA,
                DataChangesEvent.PartnerDataOperation.UPDATE));
        var contactEventsCaptor = ArgumentCaptor.forClass(ContactChangesProtoLBEvent.class);
        verify(logbrokerContactChangesEventPublisher, atLeastOnce()).publishEventAsync(contactEventsCaptor.capture());
        assertThat(contactEventsCaptor.getValue().getPayload().getContactLinkCount()).isEqualTo(2);
    }
}
