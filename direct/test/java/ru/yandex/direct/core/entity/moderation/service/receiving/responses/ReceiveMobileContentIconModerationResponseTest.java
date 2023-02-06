package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.model.mobilecontenticon.MobileContentIconModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.mobilecontenticon.MobileContentIconModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderation.service.receiving.MobileContentIconModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.MobileContentStatusiconmoderate;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.mobilecontent.model.StatusIconModerate.SENT;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.testing.data.TestMobileContents.androidMobileContent;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveMobileContentIconModerationResponseTest
        extends AbstractModerationResponseTest<MobileContentIconModerationMeta, Verdict, MobileContentIconModerationResponse> {
    private static final long DEFAULT_VERSION = 10L;

    @Autowired
    TestModerationRepository testModerationRepository;

    @Autowired
    Steps steps;

    @Autowired
    MobileContentIconModerationReceivingService mobileContentIconModerationReceivingService;

    private ClientInfo clientInfo;
    private int shard;
    private MobileContentInfo mobileContentInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        mobileContentInfo = steps.mobileContentSteps().createMobileContent(
                new MobileContentInfo()
                        .withClientInfo(clientInfo)
                        .withMobileContent(androidMobileContent().withStatusIconModerate(SENT)));

        testModerationRepository.createMobileContentIconVersion(shard, mobileContentInfo.getMobileContentId(),
                DEFAULT_VERSION, LocalDateTime.now());
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected void checkInDbForId(long id, MobileContentIconModerationResponse response) {
        var actualStatusModerate = testModerationRepository.getMobileContentIconStatusModerate(shard, id);

        String verdict = StringUtils.capitalize(response.getResult().getVerdict().toString().toLowerCase());
        var expectedStatusModerate = MobileContentStatusiconmoderate.valueOf(verdict);

        assertEquals(actualStatusModerate, expectedStatusModerate);
    }

    @Override
    protected ModerationReceivingService<MobileContentIconModerationResponse> getReceivingService() {
        return mobileContentIconModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        MobileContentInfo info = steps.mobileContentSteps().createMobileContent(
                new MobileContentInfo()
                        .withClientInfo(clientInfo)
                        .withMobileContent(androidMobileContent().withStatusIconModerate(SENT)));
        testModerationRepository.createMobileContentIconVersion(shard, info.getMobileContentId(),
                version, LocalDateTime.now());
        return info.getMobileContentId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.MOBILE_CONTENT_ICON;
    }

    @Override
    protected long getDefaultVersion() {
        return DEFAULT_VERSION;
    }

    @Override
    protected MobileContentIconModerationResponse createResponse(long id, ModerationDecision status,
                                                                 @Nullable String language, long version,
                                                                 Map<String, String> flags, List<Long> minusRegions,
                                                                 ClientInfo clientInfo,
                                                                 List<ModerationReasonDetailed> reasons) {
        MobileContentIconModerationResponse response = new MobileContentIconModerationResponse();
        response.setService(ModerationServiceNames.DIRECT_SERVICE);
        response.setType(ModerationObjectType.MOBILE_CONTENT_ICON);

        MobileContentIconModerationMeta meta = new MobileContentIconModerationMeta();
        meta.setClientId(clientInfo.getClientId().asLong());
        meta.setMobileContentId(id);
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
        return mobileContentInfo.getMobileContentId();
    }

    @Override
    protected ClientInfo getDefaultObjectClientInfo() {
        return clientInfo;
    }

    @Override
    protected void deleteDefaultObjectVersion() {
        testModerationRepository.deleteMobileContentIconVersion(shard, mobileContentInfo.getMobileContentId());
    }
}
