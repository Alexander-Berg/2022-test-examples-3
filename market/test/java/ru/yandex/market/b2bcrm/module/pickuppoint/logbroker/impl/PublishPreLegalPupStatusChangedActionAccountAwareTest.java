package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointOwner;
import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.model.PreLegalPartnerPupEvent;
import ru.yandex.market.crm.lb.writer.LbWriter;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.logic.wf.bcp.WfConstants;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.pvz.client.crm.dto.PreLegalPartnerCrmDto;
import ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.client.crm.dto.CrmPayloadType.PRE_LEGAL_PARTNER;
import static ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus.CHECKING;
import static ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus.REJECTED;

@B2bPickupPointTests
@ExtendWith(SpringExtension.class)
public class PublishPreLegalPupStatusChangedActionAccountAwareTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
    }

    @Inject
    private BcpService bcpService;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private LbWriter lbWriter;

    private PickupPointOwner account;

    private ArgumentCaptor<byte[]> lbWriteCaptor;

    @BeforeEach
    public void setUp() throws Exception {
        configurationService.setValue("processPreLegalPartnerAsTicket", false);
        account = bcpService.create(PickupPointOwner.FQN, Maps.of(
                PickupPointOwner.TITLE, "someTitle",
                PickupPointOwner.PUP_ID, 4L
        ));
        lbWriteCaptor = ArgumentCaptor.forClass(byte[].class);
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(lbWriter);
    }

    @Test
    public void shouldPublishStatusChangedEvent() throws IOException {
        changeAccountStatus("checking");
        Mockito.verify(lbWriter, Mockito.times(1)).write(lbWriteCaptor.capture());
        assertEvent(lbWriteCaptor.getValue(), CHECKING, null);
    }

    @Test
    public void shouldPublishStatusChangedEventWithRefusalReason() throws IOException {
        changeAccountStatus("rejected");
        Mockito.verify(lbWriter, Mockito.times(1)).write(lbWriteCaptor.capture());
        assertEvent(lbWriteCaptor.getValue(), REJECTED, "Отклонен СБ");
    }

    @Test
    public void shouldNotPublishStatusChangedEvent() {
        changeAccountStatus("documentsSigning");
        Mockito.verify(lbWriter, Mockito.times(0)).write(Mockito.any(byte[].class));
    }

    private void changeAccountStatus(String status) {
        bcpService.edit(
                account,
                Map.of(PickupPointOwner.STATUS, status),
                Map.of(WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true)
        );
    }

    private void assertEvent(
            byte[] bytes,
            PreLegalPartnerApproveStatus status,
            String refusalReason
    ) throws IOException {
        PreLegalPartnerPupEvent event = MAPPER.readValue(bytes, PreLegalPartnerPupEvent.class);
        assertThat(event.getEventDateTime()).isNotNull();
        assertThat(event.getType()).isEqualTo(PRE_LEGAL_PARTNER);
        PreLegalPartnerCrmDto value = event.getValue();
        assertThat(value.getId()).isEqualTo(4L);
        assertThat(value.getApproveStatus()).isEqualTo(status);
        assertThat(value.getRefusalReason()).isEqualTo(refusalReason);
    }
}
