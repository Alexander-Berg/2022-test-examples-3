package ru.yandex.direct.api.v5.entity.creatives.validation;

import java.util.Arrays;
import java.util.Collections;

import com.yandex.direct.api.v5.creatives.CreativeTypeEnum;
import com.yandex.direct.api.v5.creatives.CreativesSelectionCriteria;
import com.yandex.direct.api.v5.creatives.GetRequest;
import org.junit.Test;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.api.v5.entity.creatives.Constants.MAX_ELEMENTS_PER_GET;
import static ru.yandex.direct.api.v5.entity.creatives.validation.GetCreativesValidationService.IDS_FIELD;
import static ru.yandex.direct.api.v5.entity.creatives.validation.GetCreativesValidationService.SELECTION_CRITERIA_FIELD;
import static ru.yandex.direct.api.v5.entity.creatives.validation.GetCreativesValidationService.TYPES_FIELD;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class GetCreativesValidationServiceTest {

    private final GetCreativesValidationService validationService = new GetCreativesValidationService();

    @Test
    public void validateRequest_FullReq_Success() throws Exception {
        ValidationResult<GetRequest, DefectType> vr = validationService.validateRequest(createValidFullGetRequest());
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_EmptyReq_Success() throws Exception {
        ValidationResult<GetRequest, DefectType> vr = validationService.validateRequest(new GetRequest());
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_IdsMany_Success() throws Exception {
        GetRequest req = createValidFullGetRequest();
        req.getSelectionCriteria().setIds(Collections.nCopies(MAX_ELEMENTS_PER_GET, 1L));
        ValidationResult<GetRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_IdsTooMany() throws Exception {
        GetRequest req = createValidFullGetRequest();
        req.getSelectionCriteria().setIds(Collections.nCopies(MAX_ELEMENTS_PER_GET + 1, 1L));
        ValidationResult<GetRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(SELECTION_CRITERIA_FIELD), field(IDS_FIELD)),
                        DefectTypes.maxElementsInSelection(MAX_ELEMENTS_PER_GET)
                )
        ));
    }

    @Test
    public void validateRequest_IdsWithNull() throws Exception {
        GetRequest req = createValidFullGetRequest();
        req.getSelectionCriteria().setIds(Arrays.asList(1L, null));
        ValidationResult<GetRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(SELECTION_CRITERIA_FIELD), field(IDS_FIELD)),
                        DefectTypes.absentElementInArray()
                )
        ));
    }

    @Test
    public void validateRequest_TypesWithNull() throws Exception {
        GetRequest req = createValidFullGetRequest();
        req.getSelectionCriteria().setTypes(Arrays.asList(CreativeTypeEnum.CPM_VIDEO_CREATIVE, null));
        ValidationResult<GetRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(SELECTION_CRITERIA_FIELD), field(TYPES_FIELD)),
                        DefectTypes.absentElementInArray()
                )
        ));
    }

    private static GetRequest createValidFullGetRequest() {
        CreativesSelectionCriteria sc = new CreativesSelectionCriteria();
        sc.setIds(Arrays.asList(1L, 2L, 3L));
        sc.setTypes(Collections.singletonList(CreativeTypeEnum.VIDEO_EXTENSION_CREATIVE));
        GetRequest req = new GetRequest();
        req.setSelectionCriteria(sc);
        return req;
    }
}
