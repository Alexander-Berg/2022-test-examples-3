package ru.yandex.direct.api.v5.entity.creatives.validation;

import java.util.ArrayList;

import com.yandex.direct.api.v5.creatives.AddRequest;
import com.yandex.direct.api.v5.creatives.CreativeAddItem;
import com.yandex.direct.api.v5.creatives.VideoExtensionCreativeAddItem;
import org.junit.Test;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.api.v5.entity.creatives.Constants.MAX_ELEMENTS_PER_ADD;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class AddCreativesValidationServiceTest {
    private final AddCreativesValidationService validationService = new AddCreativesValidationService();

    @Test
    public void validateRequest_VideoExtensionCreative_Success() throws Exception {
        ValidationResult<AddRequest, DefectType> vr =
                validationService.validateRequest(createVideoExtensionCreativesAddRequest(1));
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_EmptyReq_Success() throws Exception {
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(new AddRequest());
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_CreativesMany_Success() throws Exception {
        AddRequest req = createVideoExtensionCreativesAddRequest(MAX_ELEMENTS_PER_ADD);
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_CreativesTooMany_Failure() throws Exception {
        AddRequest req = createVideoExtensionCreativesAddRequest(MAX_ELEMENTS_PER_ADD + 1);
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(AddRequest.PropInfo.CREATIVES.propertyName)),
                        DefectTypes.maxElementsPerRequest(MAX_ELEMENTS_PER_ADD)
                )
        ));
    }

    @Test
    public void validateRequest_NullCreative_Failure() throws Exception {
        AddRequest req = createVideoExtensionCreativesAddRequest(0);
        req.getCreatives().add(null);
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(AddRequest.PropInfo.CREATIVES.propertyName), index(0)),
                        DefectTypes.invalidValue()
                )
        ));
    }

    @Test
    public void validateRequest_NullVideoExtensionCreative_Failure() throws Exception {
        AddRequest req = createVideoExtensionCreativesAddRequest(1);
        req.getCreatives().get(0).setVideoExtensionCreative(null);
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(AddRequest.PropInfo.CREATIVES.propertyName), index(0),
                                field(CreativeAddItem.PropInfo.VIDEO_EXTENSION_CREATIVE.propertyName)),
                        DefectTypes.invalidValue()
                )
        ));
    }

    @Test
    public void validateRequest_NullVideoIds_Failure() throws Exception {
        AddRequest req = createVideoExtensionCreativesAddRequest(1);
        req.getCreatives().get(0).getVideoExtensionCreative().setVideoId(null);
        ValidationResult<AddRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(AddRequest.PropInfo.CREATIVES.propertyName), index(0),
                                field(CreativeAddItem.PropInfo.VIDEO_EXTENSION_CREATIVE.propertyName),
                                field(VideoExtensionCreativeAddItem.PropInfo.VIDEO_ID.propertyName)),
                        DefectTypes.invalidValue()
                )
        ));
    }

    private static AddRequest createVideoExtensionCreativesAddRequest(int itemsCount) {
        var creatives = new ArrayList<CreativeAddItem>(itemsCount);
        for (var i = 0; i < itemsCount; i++) {
            creatives.add(new CreativeAddItem()
                    .withVideoExtensionCreative(new VideoExtensionCreativeAddItem()
                            .withVideoId("60dd854d802a66ed486e6f64005")));
        }
        return new AddRequest()
                .withCreatives(creatives);
    }
}
