package ru.yandex.direct.jobs.moderationreason;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonRepository;
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.dbschema.ppc.tables.ModReasons.MOD_REASONS;
import static ru.yandex.direct.jobs.moderationreason.ModerationReasonsCleanerJob.DAYS_COUNT;

@JobsTest
@ExtendWith(SpringExtension.class)
public class ModerationReasonsCleanerJobTest {

    public static final ModReasonsType CAMPAIGN = ModReasonsType.campaign;
    private static final int SHARD = 1;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ModerationReasonRepository moderationReasonRepository;


    private List<Long> modReasonsIds;
    private Long maxId = 0L;
    private ModerationReasonsCleanerJob jobUnderTest;

    @BeforeEach
    void init() {
        initMocks(this);
        modReasonsIds = new ArrayList<>();
        jobUnderTest = new ModerationReasonsCleanerJob(SHARD, moderationReasonRepository);
    }

    /**
     * Заполняет таблицу ppc.mod_reasons данными для тестов.
     *
     * @param statusModerate - статус модерации
     * @param timeCreated    - время добавления записи
     */
    private Long fillModReasonsWith(ModReasonsStatusmoderate statusModerate, LocalDateTime timeCreated) {
        Long id = dslContextProvider.ppc(SHARD)
                .insertInto(MOD_REASONS)
                .columns(MOD_REASONS.ID, MOD_REASONS.TYPE, MOD_REASONS.STATUS_MODERATE, MOD_REASONS.TIME_CREATED)
                .values(maxId++, CAMPAIGN, statusModerate, timeCreated)
                .returning(MOD_REASONS.ID)
                .fetchOne()
                .getId();
        modReasonsIds.add(id);
        return id;
    }

    /**
     * Получаем список id записей из ppc.mod_reasons ограниченный списком созданных в тесте записей
     * (modReasonsIds)
     *
     * @return список всех id в таблице (из созданных в текущем тесте)
     */
    private List<Long> getModReasonsIds() {
        return dslContextProvider.ppc(SHARD)
                .select(MOD_REASONS.ID)
                .from(MOD_REASONS)
                .where(MOD_REASONS.ID.in(modReasonsIds))
                .and(MOD_REASONS.TYPE.eq(CAMPAIGN))
                .fetch(MOD_REASONS.ID);
    }

    private void executeJob() {
        assertThatCode(() -> jobUnderTest.execute())
                .doesNotThrowAnyException();
    }

    /**
     * Проверка, что записи с statusModerate = "Yes" и возрастом > DAYS_COUNT дней удалятся
     */
    @Test
    void testDeletingRecords() {
        LocalDateTime addTime = LocalDateTime.now().minusDays(DAYS_COUNT);
        fillModReasonsWith(ModReasonsStatusmoderate.Yes, addTime.minusMinutes(1));
        fillModReasonsWith(ModReasonsStatusmoderate.Yes, addTime.minusDays(1));

        executeJob();

        assertThat("проверяем что записи удалились",
                getModReasonsIds().size(),
                equalTo(0));
    }

    /**
     * Проверка, что записи с statusModerate = "Yes" и возрастом < DAYS_COUNT дней не удалятся
     */
    @Test
    void testNotDeletingRecords() {
        LocalDateTime addTime = LocalDateTime.now().minusDays(DAYS_COUNT);
        fillModReasonsWith(ModReasonsStatusmoderate.Yes, addTime.plusMinutes(1));
        fillModReasonsWith(ModReasonsStatusmoderate.Yes, addTime.plusDays(1));

        executeJob();

        assertThat("проверяем что записи не удалились",
                getModReasonsIds().size(),
                equalTo(2));
    }

    /**
     * Проверка, что записи с statusModerate = "No" и возрастом > DAYS_COUNT дней не удалятся
     */
    @Test
    void testNotDeletingRecordsWithStatusModerateNo() {
        LocalDateTime addTime = LocalDateTime.now().minusDays(DAYS_COUNT);
        fillModReasonsWith(ModReasonsStatusmoderate.No, addTime.minusMinutes(1));
        fillModReasonsWith(ModReasonsStatusmoderate.No, addTime.minusDays(1));

        executeJob();

        assertThat("проверяем что записи не удалились",
                getModReasonsIds().size(),
                equalTo(2));
    }

    @AfterEach
    void cleanRecords() {
        moderationReasonRepository.deleteFromModReasons(SHARD, modReasonsIds, CAMPAIGN);
    }
}
