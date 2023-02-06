package ru.yandex.market.mboc.common.masterdata.services.document.picture;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.common.imageservice.UploadImageException;
import ru.yandex.market.mboc.common.masterdata.services.document.AvatarImageDepotServiceMock;
import ru.yandex.market.mdm.http.MdmDocument.AddSupplierDocumentsRequest.DocumentAddition.ScanFile;

import static ru.yandex.market.mboc.common.masterdata.services.document.picture.QualityDocumentPictureServiceImpl.PIC_PREFIX;

public class QualityDocumentPictureServiceImplTest {

    private QualityDocumentPictureService qualityDocumentPictureService;
    private AvatarImageDepotServiceMock imageServiceMock;

    @Before
    public void setUp() {
        imageServiceMock = Mockito.spy(new AvatarImageDepotServiceMock());
        qualityDocumentPictureService = Mockito.spy(new QualityDocumentPictureServiceImpl(imageServiceMock));
    }

    @Test
    public void whenURLIsMalformedFileShouldThrowException() {
        Assertions.assertThatThrownBy(() -> qualityDocumentPictureService
            .downloadPicture("some text", "name"))
            .isExactlyInstanceOf(MalformedURLException.class);
    }

    @Test
    public void whenCantUploadFileShouldThrowException() {
        Assertions.assertThatThrownBy(() -> qualityDocumentPictureService
            .downloadPicture("http://asdfasddfdasf.asdfasdfadsfasdfasddfasdfasdf/some.jpg", "name"))
            .isExactlyInstanceOf(IOException.class);
    }

    @Test
    public void whenSavingWithoutNameShouldReturnEmptyList() {
        Assertions.assertThat(qualityDocumentPictureService.saveDocumentPictures(null, new byte[]{}))
            .isEmpty();
    }

    @Test
    public void whenSavingEmptyOrNullFileShouldThrowError() {
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThatThrownBy(() -> qualityDocumentPictureService
                .saveDocumentPictures("1", null))
                .isExactlyInstanceOf(UploadImageException.class);
            softAssertions.assertThatThrownBy(() -> qualityDocumentPictureService
                .saveDocumentPictures("1", new byte[0]))
                .isExactlyInstanceOf(UploadImageException.class);
        });
    }

    @Test
    public void whenSavingIncorrectExtensionThrowError() {
        SoftAssertions.assertSoftly(softAssertions ->
            softAssertions.assertThatThrownBy(() -> qualityDocumentPictureService
                .saveDocumentPictures("1.mp3", new byte[]{1}))
                .isExactlyInstanceOf(UploadImageException.class));
    }

    @Test
    public void whenSavingValidFileShouldCallAvatarService() {
        List<String> saveDocumentPictures = qualityDocumentPictureService
            .saveDocumentPictures("1.jpg", new byte[]{1});
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(saveDocumentPictures).hasSize(1);
            softAssertions.assertThat(imageServiceMock
                .isImageExist(saveDocumentPictures.get(0).replace(PIC_PREFIX, ""))).isTrue();
        });
    }

    @Test
    public void whenDeletingOldPicturesShouldDeleteOnlyPicturesPassedInMethod() {
        String picture1 = qualityDocumentPictureService.saveDocumentPictures("1.jpg", new byte[]{1}).get(0);
        String picture2 = qualityDocumentPictureService.saveDocumentPictures("2.jpg", new byte[]{1}).get(0);

        qualityDocumentPictureService.deleteOldPictures(Collections.singletonList(picture1));
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(imageServiceMock.getImages()).hasSize(1);
            softAssertions.assertThat(imageServiceMock
                .isImageExist(picture1.replace(PIC_PREFIX, ""))).isFalse();
            softAssertions.assertThat(imageServiceMock
                .isImageExist(picture2.replace(PIC_PREFIX, ""))).isTrue();
        });
    }

    @Test
    public void whenGetPictureTypeWithCorrectExtensionCalledShouldReturnPictureType() {
        List<PictureType> types = Arrays.stream(new String[]{"lol.jpg", "lol.jpeg", "lol.png", "lol.pdf"})
            .map(QualityDocumentPictureServiceImpl::getPictureType)
            .collect(Collectors.toList());
        Assertions.assertThat(types).containsExactly(PictureType.PICTURE,
            PictureType.PICTURE, PictureType.PICTURE, PictureType.PDF);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void whenGetPictureTypeCalledWithNullShouldThrowException() {
        Assertions.assertThatThrownBy(() -> QualityDocumentPictureServiceImpl.getPictureType(null))
            .isInstanceOf(UploadImageException.class);
    }

    @Test
    public void whenGetPictureTypeCalledWithWrongExtensionShouldThrowException() {
        Assertions.assertThatThrownBy(() -> QualityDocumentPictureServiceImpl.getPictureType("lol.kek"))
            .isInstanceOf(UploadImageException.class);
    }

    @Test
    public void whenGetPictureTypeCalledWithEmptyExtensionShouldThrowException() {
        Assertions.assertThatThrownBy(() -> QualityDocumentPictureServiceImpl.getPictureType(""))
            .isInstanceOf(UploadImageException.class);
    }

    @Test
    public void whenScanFilePassedShouldCallDownloadPictureWithCorrectParameters() throws Exception {
        String url = "this does not exist";
        String name = "lol.jpeg";
        try {
            qualityDocumentPictureService.downloadPicture(ScanFile.newBuilder()
                .setUrl(url)
                .setFileName(name)
                .build());
        } catch (Exception ignore) {
            //need to catch malformed url
        }
        Mockito.verify(qualityDocumentPictureService, Mockito.times(1))
            .downloadPicture(Mockito.eq(url), Mockito.eq(name));
    }
}
