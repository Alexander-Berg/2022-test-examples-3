package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.core.entity.moderation.model.AbstractModerationResultResponse;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.ModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Maybe;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;

public abstract class AbstractModerationResponseTest<META extends ModerationMeta,
        RESULT extends Verdict,
        RESPONSE extends AbstractModerationResultResponse<META, RESULT>
        > {

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    protected static final List<Long> DEFAULT_MINUS_REGION = Arrays.asList(4L, 5L, 6L);
    protected static final List<ModerationReasonDetailed> DEFAULT_REASONS = Arrays.asList(
            new ModerationReasonDetailed().withId(2L),
            new ModerationReasonDetailed().withId(3L));

    protected abstract int getShard();

    protected void checkInDb(RESPONSE response) {
        checkInDbForId(getDefaultObjectId(), response);
    }

    protected abstract void checkInDbForId(long id, RESPONSE response);

    protected abstract ModerationReceivingService<RESPONSE> getReceivingService();

    protected abstract long createObjectInDb(long version);

    protected abstract ModerationObjectType getObjectType();

    protected abstract long getDefaultVersion();

    protected abstract RESPONSE createResponse(
            long bid,
            ModerationDecision status,
            @Nullable String language,
            long version,
            Map<String, String> flags,
            List<Long> minusRegions,
            ClientInfo clientInfo,
            List<ModerationReasonDetailed> reasons);

    protected abstract long getDefaultObjectId();

    protected abstract ClientInfo getDefaultObjectClientInfo();

    protected RESPONSE createResponse(long bid, ModerationDecision status) {
        return createResponse(bid, status, null, getDefaultVersion(),
                emptyMap(), DEFAULT_MINUS_REGION, getDefaultObjectClientInfo(), DEFAULT_REASONS);
    }

    protected RESPONSE createResponseForDefaultObject(ModerationDecision status) {
        return createResponse(getDefaultObjectId(), status, null, getDefaultVersion(),
                emptyMap(), DEFAULT_MINUS_REGION, getDefaultObjectClientInfo(), DEFAULT_REASONS);
    }

    @After
    public void clearRestrictedPropertyValue() {
        if (getRestrictedModePropertyName() != null) {
            ppcPropertiesSupport.get(getRestrictedModePropertyName()).remove();
        }
    }

    protected PpcPropertyName<Boolean> getRestrictedModePropertyName() {
        return null;
    }

    protected BannersBannerType getDirectBannerType() {
        return null;
    }

    protected void checkStatusModerateNotChanged(long id) {
        throw new NotImplementedException("");
    }

    /**
     * Сохранение Yes-No в базе
     */

    @Test
    public void moderationResponseYes_savedInDb() {
        RESPONSE response = createResponseForDefaultObject(Yes);
        var unknownVerdictCountAndSuccess =
                getReceivingService().processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response);
    }

    @Test
    public void moderationResponseNo_savedInDb() {
        RESPONSE response = createResponseForDefaultObject(No);
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response);
    }

    @Test
    public void unknownVerdict_notSavedInDb_correctReturnValue() {
        RESPONSE response = createResponseForDefaultObject(Maybe);
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));
        assertEquals(1, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(0, unknownVerdictCountAndSuccess.getRight().size());
    }

    @Test
    public void multipleModerationResponses_savedInDb() {
        AtomicReference<ModerationDecision> status = new AtomicReference<>(Yes);

        List<Long> bids = IntStreamEx.range(4).mapToObj(e -> createObjectInDb(getDefaultVersion()))
                .collect(Collectors.toList());

        List<RESPONSE> responses = StreamEx.of(bids)
                .map(bi -> createResponse(bi, status.get() == Yes ? status.getAndSet(No) :
                        status.getAndSet(Yes)))
                .toList();

        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), responses);

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(4, unknownVerdictCountAndSuccess.getRight().size());

        EntryStream.zip(bids, responses).forKeyValue(this::checkInDbForId);
    }

    @Test
    public void moderationResponseVersionDifferent_notSavedInDb() {
        RESPONSE yesResponse = createResponseForDefaultObject(Yes);
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(yesResponse));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());

        checkInDb(yesResponse);

        RESPONSE noResponse = createResponse(getDefaultObjectId(), No, null,
                yesResponse.getMeta().getVersionId() + 1,
                emptyMap(), emptyList(), getDefaultObjectClientInfo(), emptyList());


        unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(noResponse));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(0, unknownVerdictCountAndSuccess.getRight().size());

        checkInDb(yesResponse);
    }

    @Test
    public void moderationResponseVersionDifferent_NoThenYes_notSavedInDb() {
        RESPONSE yesResponse = createResponseForDefaultObject(No);
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(yesResponse));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());

        checkInDb(yesResponse);

        RESPONSE noResponse = createResponse(getDefaultObjectId(), Yes, null,
                yesResponse.getMeta().getVersionId() + 1,
                emptyMap(), emptyList(), getDefaultObjectClientInfo(), emptyList());


        unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(noResponse));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(0, unknownVerdictCountAndSuccess.getRight().size());

        checkInDb(yesResponse);
    }

    @Test
    public void moderationResponseNoLocalVersion_savedInDb() {
        RESPONSE yesResponse = createResponseForDefaultObject(Yes);

        deleteDefaultObjectVersion();

        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(yesResponse));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());

        checkInDb(yesResponse);
    }

    protected abstract void deleteDefaultObjectVersion();

    @Test
    public void firstYesThenYesVerdict_savedInDb() {
        RESPONSE response = createResponseForDefaultObject(Yes);
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());

        checkInDb(response);

        response = createResponseForDefaultObject(Yes);
        unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response);
    }

    @Test
    public void firstYesThenNoVerdict_savedInDb() {
        RESPONSE response = createResponseForDefaultObject(Yes);
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());

        checkInDb(response);

        response = createResponseForDefaultObject(No);

        unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response);

    }

    @Test
    public void firstNoThenYesVerdict_savedInDb() {
        RESPONSE response = createResponseForDefaultObject(No);
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response);

        response = createResponseForDefaultObject(Yes);

        unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response);
    }

    @Test
    public void moderationResponseRestrictedMode_notSavedInDb() {
        PpcPropertyName<Boolean> propertyName = getRestrictedModePropertyName();
        assumeTrue(propertyName != null);

        ppcPropertiesSupport.get(propertyName).set(true);

        long id = createObjectInDb(getDefaultVersion());

        RESPONSE responseNo = createResponse(id, No, null, getDefaultVersion(),
                emptyMap(), DEFAULT_MINUS_REGION, getDefaultObjectClientInfo(), DEFAULT_REASONS);
        var unknownVerdictCountAndSuccess =
                getReceivingService().processModerationResponses(getShard(), singletonList(responseNo));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());

        checkStatusModerateNotChanged(getDefaultObjectId());
    }

    @Test
    public void moderationResponseOldVersion_notApplied() {
        long id = createObjectInDb(getDefaultVersion() + 10);

        RESPONSE responseNo = createResponse(id, No, null, getDefaultVersion(),
                emptyMap(), DEFAULT_MINUS_REGION, getDefaultObjectClientInfo(), DEFAULT_REASONS);

        var unknownVerdictCountAndSuccess =
                getReceivingService().processModerationResponses(getShard(), singletonList(responseNo));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(0, unknownVerdictCountAndSuccess.getRight().size());
    }

    @Test
    public void moderationResponseMigratedVersion_appliedAndSavedInDb() {
        long id = createObjectInDb(getDefaultVersion());

        RESPONSE responseYes = createResponse(id, Yes, null, getDefaultVersion() - 1,
                emptyMap(), DEFAULT_MINUS_REGION, getDefaultObjectClientInfo(), emptyList());

        var unknownVerdictCountAndSuccess =
                getReceivingService().processModerationResponses(getShard(), singletonList(responseYes));
        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());

        checkInDbForId(id, responseYes);
    }

}
