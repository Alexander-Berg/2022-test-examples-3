package ru.yandex.market.adv.content.manager.database.repository.template;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Date: 08.02.2022
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
class PreTemplateRepositoryTest extends AbstractContentManagerTest {

    @Autowired
    private PreTemplateRepository preTemplateRepository;

    @DisplayName("Поиск последнего шаблона по идентификатору бизнеса и типа завершился успешно.")
    @DbUnitDataSet(
            before = "PreTemplateRepository/csv/delete_unknown_nothing.before.csv",
            after = "PreTemplateRepository/csv/delete_unknown_nothing.after.csv"
    )
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "business",
            "express"
    })
    void delete_unknown_nothing(String type) {
        preTemplateRepository.delete(554L, type);
    }

    @DisplayName("Поиск последнего шаблона по идентификатору бизнеса и типа завершился успешно.")
    @DbUnitDataSet(
            before = "PreTemplateRepository/csv/delete_exist_oneRow.before.csv",
            after = "PreTemplateRepository/csv/delete_exist_oneRow.after.csv"
    )
    @Test
    void delete_exist_oneRow() {
        preTemplateRepository.delete(773L, "business");
    }

    @DisplayName("Обновление документа. Добавляем комментарий и обновляем статус модерируемого документа.")
    @DbUnitDataSet(
            before = "PreTemplateRepository/csv/insert_new_success.before.csv",
            after = "PreTemplateRepository/csv/insert_new_success.after.csv"
    )
    @Test
    void insert_new_success() {
        preTemplateRepository.insert(994L, "business");
    }

    @DisplayName("Обновление документа. Добавляем комментарий и обновляем статус модерируемого документа.")
    @DbUnitDataSet(
            before = "PreTemplateRepository/csv/insert_exist_exception.before.csv",
            after = "PreTemplateRepository/csv/insert_exist_exception.after.csv"
    )
    @Test
    void insert_exist_exception() {
        Assertions.assertThatThrownBy(() -> preTemplateRepository.insert(392L, "business"))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
