package ru.yandex.direct.core.entity.moderationreason.repository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.moderationreason.model.ModerationReason;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonStatusSending;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestModerationReasonsRepository;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.BANNER;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.CAMPAIGN;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.PHRASES;
import static ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType.SITELINKS_SET;
import static ru.yandex.direct.core.testing.repository.TestModerationReasonsRepository.SHARD;
import static ru.yandex.direct.core.testing.repository.TestModerationReasonsRepository.STANDART_MODERATION_REASONS_BY_OBJECT_TYPE_AND_ID;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationReasonRepositoryTest {

    @Autowired
    private ModerationReasonRepository moderationReasonRepository;

    @Autowired
    private TestModerationReasonsRepository testModerationReasonsRepository;

    @Before
    public void setUp() {
        testModerationReasonsRepository.clean();
        testModerationReasonsRepository.insertReasons(STANDART_MODERATION_REASONS_BY_OBJECT_TYPE_AND_ID);
    }

    @Test
    public void fetchRejected_fetchConcreteTypeAndId_Successful() {
        List<ModerationReason> moderationReasons = moderationReasonRepository
                .fetchRejected(SHARD, PHRASES, singletonList(1L));
        assertThat(moderationReasons, hasSize(1));
    }


    @Test
    public void fetchRejected_fetchConcreteTypeAndListIds_Successful() {
        List<ModerationReason> moderationReasons = moderationReasonRepository
                .fetchRejected(SHARD, SITELINKS_SET, asList(1L, 2L, 3L));
        assertThat(moderationReasons, hasSize(3));
    }

    @Test
    public void fetchRejected_wholeTable_Successful() {
        List<ModerationReason> moderationReasons = moderationReasonRepository
                .fetchRejected(SHARD, STANDART_MODERATION_REASONS_BY_OBJECT_TYPE_AND_ID);
        assertThat(moderationReasons, hasSize(12));
    }

    @Test
    public void fetchRejected_emptyMap_ReturnEmptyList() {
        List<ModerationReason> moderationReasons = moderationReasonRepository
                .fetchRejected(SHARD, emptyMap());
        assertThat(moderationReasons, hasSize(0));
    }

    @Test
    public void fetchRejected_mapWithEmptyLists_ReturnEmptyList() {
        List<ModerationReason> moderationReasons = moderationReasonRepository
                .fetchRejected(SHARD,
                        new EnumMap<>(ModerationReasonObjectType.class) {{
                            put(PHRASES, emptyList());
                            put(SITELINKS_SET, emptyList());
                        }});
        assertThat(moderationReasons, hasSize(0));
    }

    @Test
    public void fetchRejected_mapWithOneEmptyList_ReturnCorrect() {
        List<ModerationReason> moderationReasons = moderationReasonRepository
                .fetchRejected(SHARD,
                        new EnumMap<>(ModerationReasonObjectType.class) {{
                            put(PHRASES, asList(1L, 2L));
                            put(SITELINKS_SET, emptyList());
                        }});
        assertThat(moderationReasons, hasSize(2));
    }

    @Test
    public void fetchRejected_Subset_ReturnsCorrectResult() {
        Map<ModerationReasonObjectType, List<Long>> table =
                new EnumMap<>(ModerationReasonObjectType.class) {{
                    put(PHRASES, asList(1L, 2L));
                    put(SITELINKS_SET, asList(2L, 3L));
                }};

        List<ModerationReason> moderationReasons = moderationReasonRepository.fetchRejected(SHARD, table);
        assertThat(moderationReasons, hasSize(4));
    }

    @Test
    public void fetchRejected_ReasonsParsedCorrectly() {
        List<ModerationReason> moderationReasons = moderationReasonRepository
                .fetchRejected(SHARD, STANDART_MODERATION_REASONS_BY_OBJECT_TYPE_AND_ID);
        ModerationReason firstReason = moderationReasons.get(0);
        assertNotNull(firstReason.getReasons());
        assertThat(firstReason.getReasons(), hasSize(2));
    }

    @Test
    public void deleteModerationReasons() {
        ClientId ourClient = ClientId.fromLong(200);
        ClientId otherClient = ClientId.fromLong(300);
        testModerationReasonsRepository
                .insertReasons(SHARD, 300, CAMPAIGN, ourClient, 300, ModerationReasonStatusSending.YES);
        testModerationReasonsRepository
                .insertReasons(SHARD, 301, BANNER, ourClient, 300, ModerationReasonStatusSending.NO);
        testModerationReasonsRepository
                .insertReasons(SHARD, 302, PHRASES, ourClient, 300, ModerationReasonStatusSending.SENDING);
        testModerationReasonsRepository
                .insertReasons(SHARD, 303, CAMPAIGN, ourClient, 400, ModerationReasonStatusSending.YES);
        testModerationReasonsRepository
                .insertReasons(SHARD, 304, BANNER, ourClient, 400, ModerationReasonStatusSending.NO);
        testModerationReasonsRepository
                .insertReasons(SHARD, 305, PHRASES, ourClient, 400, ModerationReasonStatusSending.SENDING);
        testModerationReasonsRepository
                .insertReasons(SHARD, 306, CAMPAIGN, otherClient, 300, ModerationReasonStatusSending.YES);
        testModerationReasonsRepository
                .insertReasons(SHARD, 307, BANNER, otherClient, 300, ModerationReasonStatusSending.NO);
        testModerationReasonsRepository
                .insertReasons(SHARD, 308, PHRASES, otherClient, 300, ModerationReasonStatusSending.SENDING);
        Integer recordsDeleted = moderationReasonRepository.deleteUnsentModerationReasons(SHARD, ourClient, 300);
        assertThat("Удалено правильное количество записей", recordsDeleted, is(equalTo(2)));
    }
}
