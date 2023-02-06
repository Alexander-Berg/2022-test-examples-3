package ru.yandex.market.adv.content.manager.database.repository.image;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.adv.content.manager.database.entity.image.ImageEntity;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Date: 04.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
class ImageRepositoryTest extends AbstractContentManagerTest {

    @Autowired
    private ImageRepository imageRepository;

    @DisplayName("Поиск по ссылкам вернул корректный результат.")
    @DbUnitDataSet(
            before = "ImageRepository/csv/findByUrls_fourElementInList_twoResult.before.csv"
    )
    @Test
    void findByUrls_fourElementInList_twoResult() {
        Assertions.assertThat(imageRepository.findByUrls(
                        443L,
                        List.of(
                                "https://s3.yandex.net/gif.png",
                                "https://s3.yandex.net/banner.png",
                                "https://s3.yandex.net/banner.jpeg",
                                "https://s3.yandex.net/image.png"
                        )
                ))
                .containsExactlyInAnyOrder(
                        getImageEntity("https://s3.yandex.net/gif.png", 4096, 4096, 2160),
                        getImageEntity("https://s3.yandex.net/banner.png", 512, 1280, 720)
                );
    }

    @DisplayName("Картинка успешно сохранилась в БД.")
    @DbUnitDataSet(
            after = "ImageRepository/csv/insert_correctData_oneRow.after.csv"
    )
    @Test
    void insert_correctData_oneRow() {
        imageRepository.insert(
                443L,
                "https://s3.yandex.net/gif.png",
                1024,
                1920,
                1080,
                LocalDateTime.of(2021, 10, 3, 22, 49, 50)
                        .atZone(ZoneOffset.systemDefault())
                        .toInstant()
        );
    }

    @Nonnull
    private ImageEntity getImageEntity(String url, int size, int width, int height) {
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setBusinessId(443L);
        imageEntity.setUrl(url);
        imageEntity.setSize(size);
        imageEntity.setWidth(width);
        imageEntity.setHeight(height);
        imageEntity.setCreated(
                LocalDateTime.of(2021, 10, 3, 22, 49, 50)
                        .atZone(ZoneOffset.systemDefault())
                        .toInstant()
        );
        return imageEntity;
    }
}
