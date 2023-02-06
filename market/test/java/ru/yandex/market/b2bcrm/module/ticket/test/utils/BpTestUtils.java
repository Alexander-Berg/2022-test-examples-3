package ru.yandex.market.b2bcrm.module.ticket.test.utils;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import ru.yandex.market.b2bcrm.module.business.process.Bp;
import ru.yandex.market.b2bcrm.module.business.process.BpState;
import ru.yandex.market.b2bcrm.module.business.process.BpStatus;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.utils.Maps;

@Component
public class BpTestUtils {
    @Inject
    BcpService bcpService;

    public BpStatus createBpStatus() {
        return bcpService.create(Fqn.of("bpStatus"), Maps.of(
                BpStatus.CODE, Randoms.string(),
                BpStatus.TITLE, Randoms.string()
        ));
    }

    public BpStatus createBpStatus(String code) {
        return bcpService.create(Fqn.of("bpStatus"), Maps.of(
                BpStatus.CODE, code,
                BpStatus.TITLE, code
        ));
    }

    public BpState createBpState() {
        return bcpService.create(Fqn.of("bpState"), Maps.of(
                BpState.TITLE, Randoms.string(),
                BpState.START_STATUS, createBpStatus(),
                BpState.NEXT_STATUSES, createBpStatus()
        ));
    }

    public BpState createBpState(BpStatus startStatus, List<BpStatus> nextStatuses) {
        return bcpService.create(Fqn.of("bpState"), Maps.of(
                BpState.TITLE, Randoms.string(),
                BpState.START_STATUS, startStatus,
                BpState.NEXT_STATUSES, nextStatuses
        ));
    }

    public Bp createBp() {
        List<BpState> states = Arrays.asList(createBpState(), createBpState());

        return bcpService.create(Fqn.of("bp"), Maps.of(
                Bp.CODE, Randoms.string(),
                Bp.TITLE, Randoms.string(),
                Bp.STATES, states
        ));
    }

    public Bp createBp(String code, List<BpState> states) {
        return bcpService.create(Fqn.of("bp"), Maps.of(
                Bp.CODE, code,
                Bp.TITLE, code,
                Bp.STATES, states
        ));
    }
}
