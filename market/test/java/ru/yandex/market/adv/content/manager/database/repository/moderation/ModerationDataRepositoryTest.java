package ru.yandex.market.adv.content.manager.database.repository.moderation;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.adv.content.manager.database.entity.moderation.ModerationDataEntity;
import ru.yandex.market.adv.content.manager.model.moderation.ModerationDataInfoModel;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Date: 30.09.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
class ModerationDataRepositoryTest extends AbstractContentManagerTest {

    @Autowired
    private ModerationDataRepository moderationDataRepository;

    @DisplayName("Поиск по данным для модерации вернул корректный результат в виде одной строчки.")
    @DbUnitDataSet(
            before = "ModerationDataRepository/csv/findByData_correctData_oneRow.before.csv"
    )
    @Test
    void findByData_correctData_oneRow() {
        Assertions.assertThat(moderationDataRepository.findByDataAndType("https://s3.yandex.net/gif.png", "IMAGE"))
                .isPresent()
                .get()
                .isEqualTo(
                        getModerationEntity("https://s3.yandex.net/gif.png", "IMAGE", false,
                                new ModerationDataInfoModel(Map.of("WRONG", "FAIL")))
                );
    }

    @DisplayName("Поиск по данным, которые не прошли модерацию, вернул корректный результат.")
    @DbUnitDataSet(
            before = "ModerationDataRepository/csv/findByDataCollection_twoElementList_fourRow.before.csv"
    )
    @Test
    void findByDataCollection_twoElementList_fourRow() {
        Assertions.assertThat(moderationDataRepository.findByDataInAndTypeIn(
                        List.of(
                                "https://s3.yandex.net/gif.png",
                                "https://s3.yandex.net/banner.png"
                        ),
                        List.of("TEXT", "IMAGE", "IMAGE_BANNER", "IMAGE_VENDOR")
                ))
                .containsExactlyInAnyOrder(
                        getModerationEntity("https://s3.yandex.net/gif.png", "IMAGE", false,
                                new ModerationDataInfoModel(Map.of())),
                        getModerationEntity("https://s3.yandex.net/gif.png", "IMAGE_VENDOR", true,
                                null),
                        getModerationEntity("https://s3.yandex.net/gif.png", "IMAGE_BANNER", false,
                                new ModerationDataInfoModel(Map.of("FALSE", "TRUE"))),
                        getModerationEntity("https://s3.yandex.net/banner.png", "IMAGE_BANNER", false,
                                new ModerationDataInfoModel(Map.of("WRONG", "FAIL", "FALSE", "TRUE")))
                );
    }

    @DisplayName("Данные по модерации успешно сохранились в БД.")
    @DbUnitDataSet(
            after = "ModerationDataRepository/csv/insert_correctData_oneRow.after.csv"
    )
    @Test
    void insert_correctData_oneRow() {
        moderationDataRepository.insert(
                "https://s3.yandex.net/image.jpeg",
                "IMAGE",
                false,
                LocalDateTime.of(2021, 10, 3, 22, 49, 50)
                        .atZone(ZoneOffset.systemDefault())
                        .toInstant(),
                new ModerationDataInfoModel(Map.of("SUCCESS", "DONE"))
        );
    }

    @DisplayName("Данные по модерации без дополнительной ифнормации успешно сохранились в БД.")
    @DbUnitDataSet(
            after = "ModerationDataRepository/csv/insert_nullInfo_oneRow.after.csv"
    )
    @Test
    void insert_nullInfo_oneRow() {
        moderationDataRepository.insert(
                "https://s3.yandex.net/image.jpeg",
                "IMAGE",
                true,
                LocalDateTime.of(2021, 10, 3, 22, 49, 50)
                        .atZone(ZoneOffset.systemDefault())
                        .toInstant(),
                null
        );
    }

    @DisplayName("Исключительная ситуация при сохранении данных по модерации в БД, так как они уже есть.")
    @DbUnitDataSet(
            before = "ModerationDataRepository/csv/insert_existData_exception.before.csv"
    )
    @Test
    void insert_existData_exception() {
        Assertions.assertThatThrownBy(() ->
                        moderationDataRepository.insert(
                                "Привет",
                                "TEXT",
                                false,
                                LocalDateTime.of(2021, 10, 3, 22, 49, 50)
                                        .atZone(ZoneOffset.systemDefault())
                                        .toInstant(),
                                null
                        )
                )
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Nonnull
    private ModerationDataEntity getModerationEntity(String data, String type, boolean result,
                                                     ModerationDataInfoModel info) {
        ModerationDataEntity expected = new ModerationDataEntity();
        expected.setData(data);
        expected.setType(type);
        expected.setResult(result);
        expected.setInfo(info);
        expected.setCreated(
                LocalDateTime.of(2021, 10, 3, 22, 49, 50)
                        .atZone(ZoneOffset.systemDefault())
                        .toInstant()
        );
        return expected;
    }
}
