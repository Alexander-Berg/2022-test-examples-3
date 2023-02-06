package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.model.callout.CalloutModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.callout.CalloutModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderation.service.receiving.CalloutModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.AdditionsItemCalloutsStatusmoderate;
import ru.yandex.direct.dbschema.ppc.tables.records.AdditionsItemCalloutsRecord;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.CALLOUT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveCalloutModerationResponseTest extends
        AbstractModerationResponseTest<CalloutModerationMeta, Verdict, CalloutModerationResponse> {

    @Autowired
    Steps steps;

    @Autowired
    TestModerationRepository testModerationRepository;

    @Autowired
    CalloutModerationReceivingService calloutModerationReceivingService;

    private ClientInfo clientInfo;
    private int shard;
    private Callout callout;

    private static final long DEFAULT_VERSION = 10L;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        callout = steps.calloutSteps().createCalloutWithText(clientInfo, RandomStringUtils.randomAlphanumeric(5));

        testModerationRepository.setCalloutStatusModerate(shard, callout.getId(), AdditionsItemCalloutsStatusmoderate.Sent);
        testModerationRepository.createCalloutVersion(shard, callout.getId(), DEFAULT_VERSION, LocalDateTime.now());
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected void checkInDbForId(long id, CalloutModerationResponse response) {
        List<AdditionsItemCalloutsRecord> callouts = testModerationRepository.getCallouts(shard,
                Collections.singleton(id));

        assumeThat(callouts, not(empty()));

        AdditionsItemCalloutsRecord dbRecord = callouts.get(0);

        String verdict = StringUtils.capitalize(response.getResult().getVerdict().toString().toLowerCase());
        var expectedStatusModerate = AdditionsItemCalloutsStatusmoderate.valueOf(verdict);

        assertEquals(dbRecord.getStatusmoderate(), expectedStatusModerate);
    }

    @Override
    protected ModerationReceivingService<CalloutModerationResponse> getReceivingService() {
        return calloutModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        Callout callout = steps.calloutSteps().createCalloutWithText(clientInfo,
                RandomStringUtils.randomAlphanumeric(5));
        testModerationRepository.setCalloutStatusModerate(shard, callout.getId(), AdditionsItemCalloutsStatusmoderate.Sent);
        testModerationRepository.createCalloutVersion(shard, callout.getId(), version, LocalDateTime.now());
        return callout.getId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.CALLOUT;
    }

    @Override
    protected long getDefaultVersion() {
        return DEFAULT_VERSION;
    }

    @Override
    protected CalloutModerationResponse createResponse(long id, ModerationDecision status,
                                                       @Nullable String language, long version,
                                                       Map<String, String> flags, List<Long> minusRegions,
                                                       ClientInfo clientInfo, List<ModerationReasonDetailed> reasons) {
        CalloutModerationResponse response = new CalloutModerationResponse();
        response.setService(ModerationServiceNames.DIRECT_SERVICE);
        response.setType(CALLOUT);

        CalloutModerationMeta meta = new CalloutModerationMeta();
        meta.setClientId(clientInfo.getClientId().asLong());
        meta.setCalloutId(id);
        meta.setUid(clientInfo.getUid());
        meta.setVersionId(version);

        response.setMeta(meta);

        Verdict v = new Verdict();
        v.setVerdict(status);

        if (status == No) {
            v.setReasons(DEFAULT_REASONS.stream().map(ModerationReasonDetailed::getId).collect(Collectors.toList()));
            v.setDetailedReasons(DEFAULT_REASONS);
        }

        response.setResult(v);

        return response;
    }

    @Override
    protected long getDefaultObjectId() {
        return callout.getId();
    }

    @Override
    protected ClientInfo getDefaultObjectClientInfo() {
        return clientInfo;
    }

    @Override
    protected void deleteDefaultObjectVersion() {
        testModerationRepository.deleteCalloutVersion(shard, callout.getId());
    }
}
