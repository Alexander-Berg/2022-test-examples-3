package ru.yandex.market.clab.common.service.photo;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RawPhoto;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 */
public class RawPhotoRepositoryImplTestPgaas extends BasePgaasIntegrationTest {

    private static final long SEED = 21321321321321L;

    @Autowired
    private RawPhotoRepositoryImpl rawPhotoRepository;

    private EnhancedRandom random;

    @Before
    public void before() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(SEED).build();
    }

    @Test
    public void createRawPhotos() {
        List<RawPhoto> photos = Arrays.asList(
            rawPhoto(),
            rawPhoto(),
            rawPhoto()
        );

        List<RawPhoto> newPhotos = photos.stream()
            .map(this::newRawPhoto)
            .collect(Collectors.toList());

        rawPhotoRepository.createProcessedPhotos(photos);

        List<RawPhoto> savedPhotos = rawPhotoRepository.getNotUploadedPhotos();

        assertThat(savedPhotos).allMatch(rawPhoto -> rawPhoto.getId() != null);
        assertThat(savedPhotos.stream()
            .map(rawPhoto -> new RawPhoto(rawPhoto).setId(null)))
            .containsExactlyInAnyOrderElementsOf(newPhotos);

        // Subsequent creation shouldn't do anything
        rawPhotoRepository.createProcessedPhotos(photos);

        List<RawPhoto> savedAgainPhotos = rawPhotoRepository.getNotUploadedPhotos();
        assertThat(savedAgainPhotos).containsExactlyInAnyOrderElementsOf(savedPhotos);
    }

    @Test
    public void updateRawPhotos() {
        List<RawPhoto> photos = Arrays.asList(
            rawPhoto(),
            rawPhoto(),
            rawPhoto()
        );

        rawPhotoRepository.createProcessedPhotos(photos);

        List<RawPhoto> savedPhotos = rawPhotoRepository.getNotUploadedPhotos();

        savedPhotos.get(0).setUploadedTs(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        savedPhotos.get(0).setLastUploadStatus("OK");
        byte[] photo = new byte[20];
        random.nextBytes(photo);
        savedPhotos.get(0).setUploadedPath("somepath");

        savedPhotos.get(1).setLastUploadStatus("FAILURE");

        rawPhotoRepository.saveProcessedPhotos(savedPhotos);

        List<Long> goodIds = savedPhotos.stream()
            .map(RawPhoto::getGoodId)
            .distinct()
            .collect(Collectors.toList());
        List<RawPhoto> updatedPhotos = rawPhotoRepository.getProcessedPhotos(goodIds);

        assertThat(updatedPhotos).containsExactlyInAnyOrderElementsOf(savedPhotos);

        List<RawPhoto> notUploadedPhotos = rawPhotoRepository.getNotUploadedPhotos(goodIds);

        assertThat(notUploadedPhotos).containsExactlyInAnyOrderElementsOf(updatedPhotos.stream()
            .filter(p -> p.getUploadedTs() == null)
            .collect(Collectors.toList()));

    }

    private RawPhoto rawPhoto() {
        return random.nextObject(RawPhoto.class, "id");
    }

    private RawPhoto newRawPhoto(RawPhoto rawPhoto) {
        return new RawPhoto(rawPhoto)
            .setId(null)
            .setUploadedPath(null)
            .setLastUploadStatus(null)
            .setUploadedTs(null);
    }
}
