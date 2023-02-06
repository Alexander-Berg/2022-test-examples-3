package ru.yandex.market.clab.common.service.photo;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.UploadedPhoto;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectUploadedPhotoRepositoryImplTestPgaas extends BasePgaasIntegrationTest {
    @Autowired
    private DirectUploadedPhotoRepositoryImpl repository;

    @Test
    public void createAndGetPhoto() {
        List<UploadedPhoto> photos = Arrays.asList(
            RandomTestUtils.randomObject(UploadedPhoto.class, "id"),
            RandomTestUtils.randomObject(UploadedPhoto.class, "id"));

        photos.forEach(repository::save);
        List<UploadedPhoto> fetchedPhotoByUrl = repository.getByGoodIds(photos.stream()
            .map(UploadedPhoto::getGoodId)
            .collect(Collectors.toList()));
        List<UploadedPhoto> fetchedPhotoByGoodId = photos.stream()
            .flatMap(photo -> repository.getByGoodId(photo.getGoodId()).stream())
            .collect(Collectors.toList());

        assertThat(fetchedPhotoByGoodId).usingElementComparatorIgnoringFields("id")
            .containsSequence(photos);
        assertThat(fetchedPhotoByUrl).usingElementComparatorIgnoringFields("id")
            .containsSequence(photos);
    }
}
