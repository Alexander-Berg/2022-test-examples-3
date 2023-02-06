package ru.yandex.direct.jobs.resendblockeddomains.repository;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.dbschema.ppcdict.enums.BadDomainsTitlesStatus;
import ru.yandex.direct.dbschema.ppcdict.tables.records.BadDomainsTitlesRecord;
import ru.yandex.direct.dbutil.exception.RollbackException;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.resendblockeddomains.model.BadDomainsTitle;
import ru.yandex.direct.jobs.resendblockeddomains.model.BadDomainsTitleStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppcdict.tables.BadDomainsTitles.BAD_DOMAINS_TITLES;

/**
 * Тесты на репозиторий {@link ru.yandex.direct.jobs.resendblockeddomains.repository.BadDomainsTitlesRepository}
 */
@JobsTest
@ExtendWith(SpringExtension.class)
class BadDomainsTitlesRepositoryTest {

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private BadDomainsTitlesRepository badDomainsTitlesRepository;

    /**
     * Тестируем, что домен в статусе processed не возвращается данным методом.
     */
    @Test
    void getNotProcessedBadDomainsTitles_statusIsProcessed_recordNotReturned() {
        runWithEmptyBadDomainsTtlesTable(dslContext -> {
            BadDomainsTitlesRecord record = makeRecord();
            dslContext.insertInto(BAD_DOMAINS_TITLES)
                    .set(record)
                    .execute();

            var actual = badDomainsTitlesRepository.getNotProcessedBadDomainsTitles(dslContext, 2);

            assertThat(actual).isEmpty();
        });
    }

    /**
     * Тестируем, что домен не в статусе processed возвращается данным методом.
     */
    @Test
    void getNotProcessedBadDomainsTitles_statusIsNotProcessed_recordReturned() {
        runWithEmptyBadDomainsTtlesTable(dslContext -> {
            BadDomainsTitlesRecord record = makeRecord();
            record.setStatus(BadDomainsTitlesStatus.for_disabling);
            dslContext.insertInto(BAD_DOMAINS_TITLES)
                    .set(record)
                    .execute();

            var actual = badDomainsTitlesRepository.getNotProcessedBadDomainsTitles(dslContext, 2);

            assertThat(actual).containsOnly(new BadDomainsTitle(1, "a.com", BadDomainsTitleStatus.FOR_DISABLING));
        });
    }

    /**
     * Тестируем, что у домена не в статусе for_disabling статус не обновляется.
     */
    @Test
    void markDisablingDomainAsProcessed_statusIsNotForDisabling_recordIsNotUpdated() {
        runWithEmptyBadDomainsTtlesTable(dslContext -> {
            BadDomainsTitlesRecord record = makeRecord();
            dslContext.insertInto(BAD_DOMAINS_TITLES)
                    .set(record)
                    .execute();

            int actual = badDomainsTitlesRepository.markDisablingDomainAsProcessed(dslContext, 1L);

            assertThat(actual).isEqualTo(0);
        });
    }

    /**
     * Тестируем, что у домена в статусе for_disabling статус обновляется.
     */
    @Test
    void markDisablingDomainAsProcessed_statusIsForDisabling_recordIsUpdated() {
        runWithEmptyBadDomainsTtlesTable(dslContext -> {
            BadDomainsTitlesRecord record = makeRecord();
            record.setStatus(BadDomainsTitlesStatus.for_disabling);
            dslContext.insertInto(BAD_DOMAINS_TITLES)
                    .set(record)
                    .execute();

            int actual = badDomainsTitlesRepository.markDisablingDomainAsProcessed(dslContext, 1L);

            assertThat(actual).isEqualTo(1);
        });
    }

    /**
     * Тестируем, что домен в статусе for_disabling не обновляется, если ключ не совпадает.
     */
    @Test
    void markDisablingDomainAsProcessed_idIsUnknown_recordIsNotUpdated() {
        runWithEmptyBadDomainsTtlesTable(dslContext -> {
            BadDomainsTitlesRecord record = makeRecord();
            record.setStatus(BadDomainsTitlesStatus.for_disabling);
            dslContext.insertInto(BAD_DOMAINS_TITLES)
                    .set(record)
                    .execute();

            int actual = badDomainsTitlesRepository.markDisablingDomainAsProcessed(dslContext,2L);

            assertThat(actual).isEqualTo(0);
        });
    }

    /**
     * Тестируем, что при отсутствии подходящих данных метод ничего не удаляет.
     */
    @Test
    void deleteInStatusForEnabling_idNotFound_noDelete() {
        runWithEmptyBadDomainsTtlesTable(dslContext -> {
            BadDomainsTitlesRecord record = makeRecord();
            dslContext.insertInto(BAD_DOMAINS_TITLES)
                    .set(record)
                    .execute();

            int deleted = badDomainsTitlesRepository.deleteInStatusForEnabling(dslContext, 2L);
            assertThat(deleted).isEqualTo(0);
        });
    }

    /**
     * Тестируем, что домен не в статусе for_enabling не удаляется.
     */
    @Test
    void deleteInStatusForEnabling_statusIsNotForEnabling_noDelete() {
        runWithEmptyBadDomainsTtlesTable(dslContext -> {
            BadDomainsTitlesRecord record = makeRecord();
            record.setStatus(BadDomainsTitlesStatus.for_disabling);
            dslContext.insertInto(BAD_DOMAINS_TITLES)
                    .set(record)
                    .execute();
            int deleted = badDomainsTitlesRepository.deleteInStatusForEnabling(dslContext, 1L);
            assertThat(deleted).isEqualTo(0);
        });
    }

    /**
     * Тестируем, что домен в статусе for_enabling и совпадающим ключом удаляется.
     */
    @Test
    void deleteInStatusForEnabling_statusIsForEnabling_recordDeleted() {
        runWithEmptyBadDomainsTtlesTable(dslContext -> {
            BadDomainsTitlesRecord record = makeRecord();
            record.setStatus(BadDomainsTitlesStatus.for_enabling);
            dslContext.insertInto(BAD_DOMAINS_TITLES)
                    .set(record)
                    .execute();
            int deleted = badDomainsTitlesRepository.deleteInStatusForEnabling(dslContext, 1L);
            assertThat(deleted).isEqualTo(1);
        });
    }

    private void runWithEmptyBadDomainsTtlesTable(Consumer<DSLContext> test) {
        try {
            dslContextProvider.ppcdictTransaction(configuration -> {
                DSLContext dsl = configuration.dsl();
                dsl.deleteFrom(BAD_DOMAINS_TITLES).execute();

                test.accept(dsl);

                throw new RollbackException();
            });
        } catch (RollbackException ignored) {
        }
    }

    private BadDomainsTitlesRecord makeRecord() {
        BadDomainsTitlesRecord record = new BadDomainsTitlesRecord();
        record.setId(1L);
        record.setDomain("a.com");
        record.setStatus(BadDomainsTitlesStatus.processed);
        record.setCreateTime(LocalDateTime.now());
        return record;
    }
}
