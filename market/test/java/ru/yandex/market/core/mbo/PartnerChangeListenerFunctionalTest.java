package ru.yandex.market.core.mbo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.mbo.model.PartnerChangeEvent;
import ru.yandex.market.core.mbo.model.PartnerChangeLogbrokerEvent;
import ru.yandex.market.core.mbo.model.PartnerChangeRecord;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.partner.event.PartnerInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.partner.event.PartnerInfo.UpdateType.SERVICE_LINK;

@DbUnitDataSet(before = "PartnerChangeEventListenerFunctionalTest.before.csv")
class PartnerChangeListenerFunctionalTest extends FunctionalTest {

    @Autowired
    private PartnerChangeListener partnerChangeListener;

    @Autowired
    @Qualifier("mboPartnerExportLogbrokerService")
    private LogbrokerService logbrokerService;

    @Test
    @DisplayName("Проверяем работу в целом")
    void testWrite() {
        partnerChangeListener.onApplicationEvent(new PartnerChangeEvent(
                10, PartnerChangeRecord.UpdateType.SERVICE_LINK, null));

        ArgumentCaptor<PartnerChangeLogbrokerEvent> captor = ArgumentCaptor.forClass(PartnerChangeLogbrokerEvent.class);
        verify(logbrokerService).publishEvent(captor.capture());
        PartnerChangeLogbrokerEvent expectedEvent = new PartnerChangeLogbrokerEvent(
                PartnerInfo.PartnerInfoEvent.newBuilder()
                        .setId(10)
                        .setName("business")
                        .setUpdateType(SERVICE_LINK)
                        .setTimestamp(captor.getValue().getPayload().getTimestamp())
                        .build());

        assertThat(captor.getValue()).isEqualTo(expectedEvent);

        verifyNoMoreInteractions(logbrokerService);
    }
}
