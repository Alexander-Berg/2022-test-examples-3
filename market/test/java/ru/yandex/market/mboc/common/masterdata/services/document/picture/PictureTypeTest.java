package ru.yandex.market.mboc.common.masterdata.services.document.picture;

import java.util.Optional;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class PictureTypeTest {

    @Test
    public void whenExtensionIsJpgShouldFindPicture() {
        Optional<PictureType> byExtension = PictureType.getByExtension("jpg");
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(byExtension).isPresent();
            s.assertThat(byExtension.get()).isEqualTo(PictureType.PICTURE);
        });
    }

    @Test
    public void whenExtensionIsJpegShouldFindPicture() {
        Optional<PictureType> byExtension = PictureType.getByExtension("jpeg");
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(byExtension).isPresent();
            s.assertThat(byExtension.get()).isEqualTo(PictureType.PICTURE);
        });
    }

    @Test
    public void whenExtensionIsPngShouldFindPicture() {
        Optional<PictureType> byExtension = PictureType.getByExtension("png");
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(byExtension).isPresent();
            s.assertThat(byExtension.get()).isEqualTo(PictureType.PICTURE);
        });
    }

    @Test
    public void whenExtensionIsPdfShouldFindPDF() {
        Optional<PictureType> byExtension = PictureType.getByExtension("pdf");
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(byExtension).isPresent();
            s.assertThat(byExtension.get()).isEqualTo(PictureType.PDF);
        });
    }

    @Test
    public void whenSearchingByExtensionShouldIgnoreCase() {
        Optional<PictureType> byExtension = PictureType.getByExtension("jPEg");
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(byExtension).isPresent();
            s.assertThat(byExtension.get()).isEqualTo(PictureType.PICTURE);
        });
    }

    @Test
    public void whenNotFoundPictureTypeShouldReturnOptionalEmpty() {
        Optional<PictureType> byExtension = PictureType.getByExtension("hello there");
        SoftAssertions.assertSoftly(s -> s.assertThat(byExtension).isEmpty());
    }

    @Test
    public void whenExtensionIsNullShouldReturnOptionalEmpty() {
        Optional<PictureType> byExtension = PictureType.getByExtension(null);
        SoftAssertions.assertSoftly(s -> s.assertThat(byExtension).isEmpty());
    }
}
