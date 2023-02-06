package ru.yandex.market.jmf.telephony.voximplant.test.utils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.telephony.voximplant.PhoneBook;
import ru.yandex.market.jmf.telephony.voximplant.VoximplantCall;
import ru.yandex.market.jmf.telephony.voximplant.VoximplantInboundCall;
import ru.yandex.market.jmf.telephony.voximplant.VoximplantOutboundCall;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;

@Component
public class VoximplantCallTestUtils {

    private static final Fqn TICKET_FQN = Fqn.of("ticket$simple");

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private BcpService bcpService;

    @Inject
    private ObjectSerializeService serializeService;

    public VoximplantCall createOutboundCall(Map<String, Object> parameters) {
        Ticket ticket = ticketTestUtils.createTicket(TICKET_FQN, Collections.emptyMap());
        Map<String, Object> defaultParams = Map.of(
                VoximplantCall.SESSION_ID, Randoms.string(),
                VoximplantCall.CALLER_ID, Randoms.string(),
                VoximplantCall.DESTINATION_ID, Randoms.string(),
                VoximplantCall.SEND_DATA_TO_VOXIMPLANT_CALL_SESSION_URL, Randoms.string(),
                VoximplantCall.TICKET, ticket.getGid()
        );
        return bcpService.create(VoximplantOutboundCall.FQN, mergeParams(defaultParams, parameters));
    }

    public VoximplantCall createOutboundCall() {
        return createOutboundCall(Map.of());
    }

    public VoximplantInboundCall createInboundCall(Map<String, Object> parameters) {
        Map<String, Object> defaultParams = Map.of(
                VoximplantInboundCall.SESSION_ID, Randoms.string(),
                VoximplantInboundCall.CALLER_ID, Randoms.string(),
                VoximplantInboundCall.DESTINATION_ID, Randoms.string(),
                VoximplantInboundCall.SIP_HEADERS, new String(
                        serializeService.serialize(Map.of(Randoms.string(), Randoms.string())),
                        StandardCharsets.UTF_8
                ),
                VoximplantInboundCall.SEND_DATA_TO_VOXIMPLANT_CALL_SESSION_URL, Randoms.url()
        );
        return bcpService.create(VoximplantInboundCall.FQN, mergeParams(defaultParams, parameters));
    }

    public VoximplantInboundCall createInboundCall() {
        return createInboundCall(Map.of());
    }

    public PhoneBook createPhoneBookItem() {
        return bcpService.create(PhoneBook.FQN, Map.of(
                PhoneBook.TITLE, Randoms.string(),
                PhoneBook.CODE, Randoms.string(),
                PhoneBook.PHONE, Randoms.phoneNumber()
        ));
    }

    private Map<String, Object> mergeParams(Map<String, Object> defaultParams, Map<String, Object> parameters) {
        Map<String, Object> result = new HashMap<>();
        result.putAll(defaultParams);
        result.putAll(parameters);
        return result;
    }
}
