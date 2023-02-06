package ru.yandex.market.adv.content.manager.database.repository.template;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.adv.content.manager.database.entity.template.TemplateEntity;
import ru.yandex.market.adv.content.manager.database.entity.template.TemplateStatusEntity;
import ru.yandex.market.adv.content.manager.model.error.ErrorInfo;
import ru.yandex.market.adv.content.manager.model.template.TemplateInfo;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Date: 05.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
class TemplateRepositoryTest extends AbstractContentManagerTest {

    @Autowired
    private TemplateRepository templateRepository;

    @DisplayName("Поиск последнего шаблона по идентификатору бизнеса и типа завершился успешно.")
    @DbUnitDataSet(
            before = "TemplateRepository/csv/" +
                    "findByBusinessIdAndTypeAndIdAndRevisionId_correctData_oneRow.before.csv"
    )
    @Test
    void findByBusinessIdAndTypeAndIdAndRevisionId_correctData_oneRow() {
        Assertions.assertThat(
                        templateRepository.findByBusinessIdAndTypeAndIdAndRevisionId(
                                443L,
                                5233L,
                                "BUSINESS",
                                95231L
                        )
                ).isPresent()
                .get()
                .isEqualTo(
                        getTemplateEntity(
                                95231L,
                                "Главная страница 2",
                                TemplateStatusEntity.DRAFT,
                                LocalDateTime.of(2021, 10, 6, 12, 49, 50),
                                new TemplateInfo("URA", List.of())
                        )
                );
    }

    @DisplayName("Поиск последнего шаблона в статусе PUBLISHED по идентификатору бизнеса и типа завершился успешно.")
    @DbUnitDataSet(
            before = "TemplateRepository/csv/" +
                    "findByBusinessIdAndTypeAndStatuses_correctData_twoRow.before.csv"
    )
    @Test
    void findByBusinessIdAndTypeAndStatuses_correctData_oneRow() {
        Assertions.assertThat(
                        templateRepository.findByBusinessIdAndTypeAndStatuses(
                                443L,
                                "BUSINESS",
                                List.of(TemplateStatusEntity.PUBLISHED.name(), TemplateStatusEntity.BASIC.name())
                        )
                )
                .containsExactlyInAnyOrder(
                        getTemplateEntity(
                                99231L,
                                "Главная страница",
                                TemplateStatusEntity.PUBLISHED,
                                LocalDateTime.of(2021, 10, 7, 23, 49, 50),
                                null
                        ),
                        getTemplateEntity(
                                95232L,
                                "Главная страница 2",
                                TemplateStatusEntity.PUBLISHED,
                                LocalDateTime.of(2021, 10, 6, 12, 49, 50),
                                new TemplateInfo("URA", List.of(new ErrorInfo("invalid")))
                        )
                );
    }

    @DisplayName("Поиск последнего шаблона каждого типа по идентификатору бизнеса завершился успешно.")
    @DbUnitDataSet(
            before = "TemplateRepository/csv/" +
                    "findLastByBusinessIdGroupByType_correctData_twoRow.before.csv"
    )
    @Test
    void findLastByBusinessIdGroupByType_correctData_twoRow() {
        Assertions.assertThat(
                        templateRepository.findLastByBusinessIdGroupByType(
                                443L
                        )
                )
                .containsExactlyInAnyOrder(
                        getTemplateEntity(
                                99231L,
                                "Главная страница",
                                TemplateStatusEntity.PUBLISHED,
                                LocalDateTime.of(2021, 10, 7, 23, 49, 50),
                                null
                        ),
                        getTemplateEntity(
                                7233L,
                                99231L,
                                "EXPRESS",
                                "Страница экспресс магазина",
                                TemplateStatusEntity.DRAFT,
                                LocalDateTime.of(2021, 10, 6, 15, 49, 50),
                                new TemplateInfo("AGA", null),
                                true
                        )
                );
    }

    @DisplayName("Шаблон успешно сохранился в БД.")
    @DbUnitDataSet(
            after = "TemplateRepository/csv/insert_correctData_oneRow.before.csv"
    )
    @Test
    void insert_correctData_oneRow() {
        templateRepository.insert(
                443L,
                5233L,
                "BUSINESS",
                99231L,
                "Главная страница",
                TemplateStatusEntity.DRAFT.name(),
                LocalDateTime.of(2021, 10, 5, 23, 49, 50)
                        .atZone(ZoneOffset.systemDefault())
                        .toInstant(),
                new TemplateInfo("Неверная категория на маркете.", List.of())
        );
    }

    @DisplayName("Обновление документа. Добавляем комментарий и обновляем статус модерируемого документа.")
    @DbUnitDataSet(
            before = "TemplateRepository/csv/" +
                    "updateStatusAndCommentByBusinessIdAndIdAndRevisionIdAndType_correctData_oneRowChange.before.csv",
            after = "TemplateRepository/csv/" +
                    "updateStatusAndCommentByBusinessIdAndIdAndRevisionIdAndType_correctData_oneRowChange.after.csv"
    )
    @Test
    void updateStatusAndCommentByBusinessIdAndIdAndRevisionIdAndType_correctData_oneRowChange() {
        Assertions.assertThat(
                        templateRepository.updateStatusAndInfoByBusinessIdAndIdAndRevisionIdAndType(
                                443L,
                                5233L,
                                "BUSINESS",
                                99231L,
                                TemplateStatusEntity.MODERATION_ERROR.name(),
                                new TemplateInfo("Неверная категория на маркете.", List.of(new ErrorInfo("invalid")))
                        )
                )
                .isEqualTo(1L);
    }

    @DisplayName("Удаление всех ревизий шаблона по идентификатору бизнеса и документа.")
    @DbUnitDataSet(
            before = "TemplateRepository/csv/" +
                    "deleteAllByBusinessIdAndIdAndType_correctBusinessAndId_removeFourRows.before.csv",
            after = "TemplateRepository/csv/" +
                    "deleteAllByBusinessIdAndIdAndType_correctBusinessAndId_removeFourRows.after.csv"
    )
    @Test
    void deleteAllByBusinessIdAndIdAndType_correctBusinessAndId_removeFourRows() {
        Assertions.assertThat(templateRepository.deleteAllByBusinessIdAndIdAndType(443L, 5233L, "BUSINESS"))
                .isEqualTo(4L);
    }

    @DisplayName("Обновление информации по стенду прошло успешно.")
    @DbUnitDataSet(
            before = "TemplateRepository/csv/" +
                    "updateStandByBusinessIdAndIdAndRevisionIdAndType_correctBusinessAndId_updateOneRow.before.csv",
            after = "TemplateRepository/csv/" +
                    "updateStandByBusinessIdAndIdAndRevisionIdAndType_correctBusinessAndId_updateOneRow.after.csv"
    )
    @Test
    void updateStandByBusinessIdAndIdAndRevisionIdAndType_correctBusinessAndId_updateOneRow() {
        Assertions.assertThat(templateRepository.updateStandByBusinessIdAndIdAndRevisionIdAndType(
                                443L,
                                5233L,
                                "BUSINESS",
                                95231,
                                true
                        )
                )
                .isEqualTo(1L);
    }

    @Nonnull
    private TemplateEntity getTemplateEntity(long revisionId,
                                             String name,
                                             TemplateStatusEntity status,
                                             LocalDateTime created,
                                             TemplateInfo info) {
        return getTemplateEntity(5233L, revisionId, "BUSINESS", name, status, created, info, null);
    }

    @Nonnull
    @SuppressWarnings("checkstyle:parameterNumber")
    private TemplateEntity getTemplateEntity(long id,
                                             long revisionId,
                                             @Nonnull String type,
                                             @Nonnull String name,
                                             @Nonnull TemplateStatusEntity status,
                                             @Nonnull LocalDateTime created,
                                             TemplateInfo info,
                                             Boolean stand) {
        TemplateEntity templateEntity = new TemplateEntity();
        templateEntity.setBusinessId(443L);
        templateEntity.setId(id);
        templateEntity.setRevisionId(revisionId);
        templateEntity.setType(type);
        templateEntity.setName(name);
        templateEntity.setStatus(status);
        templateEntity.setCreated(
                created.atZone(ZoneOffset.systemDefault())
                        .toInstant()
        );
        templateEntity.setInfo(info);
        templateEntity.setStand(stand);

        return templateEntity;
    }
}
