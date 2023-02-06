package ru.yandex.direct.core.testing.repository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonStatusSending;
import ru.yandex.direct.core.testing.data.TestModerationDiag;
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsStatusmoderate;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.CALLOUT;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.CAMPAIGN;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.PHRASES;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.SITELINKS_SET;
import static ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonMapping.reasonsToDbFormat;
import static ru.yandex.direct.dbschema.ppc.Tables.MOD_REASONS;

public class TestModerationReasonsRepository {

    private final DslContextProvider dslContextProvider;
    public static final int SHARD = 1;

    @Autowired
    public TestModerationReasonsRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public static final Map<ModerationReasonObjectType, List<Long>> STANDART_MODERATION_REASONS_BY_OBJECT_TYPE_AND_ID =
            new EnumMap<ModerationReasonObjectType, List<Long>>(ModerationReasonObjectType.class) {{
                put(PHRASES, asList(1L, 2L, 3L));
                put(CAMPAIGN, asList(4L, 5L, 6L));
                put(SITELINKS_SET, asList(1L, 2L, 3L));
                put(CALLOUT, asList(8L, 9L, 10L));
            }};


    @QueryWithoutIndex("Очистка тестового репа")
    public void clean() {
        dslContextProvider.ppc(SHARD).deleteFrom(MOD_REASONS).execute();
    }

    public void insertReasons(Map<ModerationReasonObjectType, List<Long>> moderationReasonsByObjectTypeAndId) {
        insertReasons(SHARD, moderationReasonsByObjectTypeAndId);
    }

    public void insertReasons(int shardId, Map<ModerationReasonObjectType,
            List<Long>> moderationReasonsByObjectTypeAndId) {
        insertReasons(shardId, moderationReasonsByObjectTypeAndId,
                asList(new ModerationReasonDetailed().withId(TestModerationDiag.DIAG_ID1),
                        new ModerationReasonDetailed().withId(TestModerationDiag.DIAG_ID2)));
    }


    public void insertReasons(int shardId, Map<ModerationReasonObjectType,
            List<Long>> moderationReasonsByObjectTypeAndId, List<ModerationReasonDetailed> reasons) {
        insertReasons(shardId, moderationReasonsByObjectTypeAndId,
                reasonsToDbFormat(reasons));
    }

    public void insertReasons(int shardId, Map<ModerationReasonObjectType,
            List<Long>> moderationReasonsByObjectTypeAndId, String reasonsString) {
        for (Map.Entry<ModerationReasonObjectType, List<Long>> moderationReasonObjectTypeListEntry
                : moderationReasonsByObjectTypeAndId.entrySet()) {
            ModerationReasonObjectType type = moderationReasonObjectTypeListEntry.getKey();
            List<Long> list = moderationReasonObjectTypeListEntry.getValue();
            if (list == null) {
                continue;
            }
            for (Long aLong : list) {
                dslContextProvider.ppc(shardId)
                        .insertInto(MOD_REASONS)
                        .set(MOD_REASONS.ID, aLong)
                        .set(MOD_REASONS.TYPE, ModerationReasonObjectType.toSource(type))
                        .set(MOD_REASONS.STATUS_MODERATE, ModReasonsStatusmoderate.No)
                        .set(MOD_REASONS.REASON, reasonsString)
                        .execute();
            }
        }
    }

    public void insertReasons(int shardId, long id, ModerationReasonObjectType type, ClientId clientId, long cid,
                              ModerationReasonStatusSending statusSending) {
        dslContextProvider.ppc(shardId)
                .insertInto(MOD_REASONS)
                .set(MOD_REASONS.ID, id)
                .set(MOD_REASONS.TYPE, ModerationReasonObjectType.toSource(type))
                .set(MOD_REASONS.CLIENT_ID, clientId.asLong())
                .set(MOD_REASONS.CID, cid)
                .set(MOD_REASONS.STATUS_SENDING, ModerationReasonStatusSending.toSource(statusSending))
                .execute();
    }
}
