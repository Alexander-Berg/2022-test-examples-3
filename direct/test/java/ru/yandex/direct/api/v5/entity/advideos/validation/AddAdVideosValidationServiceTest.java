package ru.yandex.direct.api.v5.entity.advideos.validation;

import java.util.ArrayList;

import com.yandex.direct.api.v5.advideos.AdVideoAddItem;
import com.yandex.direct.api.v5.advideos.AddRequest;
import org.junit.Test;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.api.v5.entity.advideos.validation.AddAdVideosValidationService.MAX_ELEMENTS_PER_ADD_BY_FILE;
import static ru.yandex.direct.api.v5.entity.advideos.validation.AddAdVideosValidationService.MAX_ELEMENTS_PER_ADD_BY_URL;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class AddAdVideosValidationServiceTest {
    private final AddAdVideosValidationService validationService = new AddAdVideosValidationService();

    @Test
    public void validateRequest_AdVideo_Success() throws Exception {
        ValidationResult<AddRequest, DefectType> vr =
                validationService.validateRequest(createAdVideosFromUrlAddRequest(1));
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_EmptyReq_Success() throws Exception {
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(new AddRequest());
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_AdVideosMany_Success() throws Exception {
        AddRequest req = createAdVideosFromUrlAddRequest(MAX_ELEMENTS_PER_ADD_BY_URL);
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_AdVideosTooMany_Failure() throws Exception {
        AddRequest req = createAdVideosFromUrlAddRequest(MAX_ELEMENTS_PER_ADD_BY_URL + 1);
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(AddRequest.PropInfo.AD_VIDEOS.propertyName)),
                        DefectTypes.maxElementsPerRequest(MAX_ELEMENTS_PER_ADD_BY_URL)
                )
        ));
    }

    @Test
    public void validateRequest_NullAdVideo_Failure() throws Exception {
        AddRequest req = createAdVideosFromUrlAddRequest(0);
        req.getAdVideos().add(null);
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(AddRequest.PropInfo.AD_VIDEOS.propertyName), index(0)),
                        DefectTypes.invalidValue()
                )
        ));
    }

    @Test
    public void validateRequest_NullUrl_Failure() throws Exception {
        AddRequest req = createAdVideosFromUrlAddRequest(1);
        req.getAdVideos().get(0).setUrl(null);
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(AddRequest.PropInfo.AD_VIDEOS.propertyName), index(0),
                                field(AdVideoAddItem.PropInfo.URL.propertyName)),
                        DefectTypes.invalidValue()
                )
        ));
    }

    @Test
    public void validateRequest_AdVideosTooMany_OneBinary_Failure() throws Exception {
        AddRequest req = createAdVideosFromUrlAddRequest(MAX_ELEMENTS_PER_ADD_BY_FILE + 1);
        req.getAdVideos().get(1).setUrl(null);
        req.getAdVideos().get(1).setName("name");
        req.getAdVideos().get(1).setVideoData(new byte[]{});
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(AddRequest.PropInfo.AD_VIDEOS.propertyName)),
                        DefectTypes.maxElementsPerRequest(1)
                )
        ));
    }

    @Test
    public void validateRequest_NullName_Failure() throws Exception {
        AddRequest req = createAdVideosFromUrlAddRequest(1);
        req.getAdVideos().get(0).setUrl(null);
        req.getAdVideos().get(0).setVideoData(new byte[]{});
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(AddRequest.PropInfo.AD_VIDEOS.propertyName), index(0),
                                field(AdVideoAddItem.PropInfo.NAME.propertyName)),
                        DefectTypes.invalidValue()
                )
        ));
    }

    @Test
    public void validateRequest_binaryFile_Success() throws Exception {
        AddRequest req = createAdVideosFromUrlAddRequest(MAX_ELEMENTS_PER_ADD_BY_FILE);
        req.getAdVideos().forEach(v -> {
            v.setUrl(null);
            v.setName("name");
            v.setVideoData(new byte[]{});
        });
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasNoDefects());
    }

    private static AddRequest createAdVideosFromUrlAddRequest(int itemsCount) {
        var adVideos = new ArrayList<AdVideoAddItem>(itemsCount);
        for (var i = 0; i < itemsCount; i++) {
            adVideos.add(new AdVideoAddItem()
                    .withUrl("testUrl"));
        }
        return new AddRequest()
                .withAdVideos(adVideos);
    }
}
