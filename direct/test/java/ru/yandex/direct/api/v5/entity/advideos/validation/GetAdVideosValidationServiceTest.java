package ru.yandex.direct.api.v5.entity.advideos.validation;

import java.util.Arrays;
import java.util.Collections;

import com.yandex.direct.api.v5.advideos.AdVideosSelectionCriteria;
import com.yandex.direct.api.v5.advideos.GetRequest;
import org.junit.Test;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.api.v5.entity.advideos.validation.GetAdVideosValidationService.MAX_ELEMENTS_PER_GET;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class GetAdVideosValidationServiceTest {
    private final GetAdVideosValidationService validationService = new GetAdVideosValidationService();

    @Test
    public void validateRequest_FullReq_Success() throws Exception {
        ValidationResult<GetRequest, DefectType> vr = validationService.validateRequest(createAdVideosGetRequest());
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_EmptyReq_Success() throws Exception {
        ValidationResult<GetRequest, DefectType> vr = validationService.validateRequest(new GetRequest());
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_IdsMany_Success() throws Exception {
        GetRequest req = createAdVideosGetRequest();
        req.getSelectionCriteria().setIds(Collections.nCopies(MAX_ELEMENTS_PER_GET, "60dd854d802a66ed486e6f64005"));
        ValidationResult<GetRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_IdsTooMany_Failure() throws Exception {
        GetRequest req = createAdVideosGetRequest();
        req.getSelectionCriteria().setIds(Collections.nCopies(MAX_ELEMENTS_PER_GET + 1, "60dd854d802a66ed486e6f64005"));
        ValidationResult<GetRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(GetRequest.PropInfo.SELECTION_CRITERIA.propertyName),
                                field(AdVideosSelectionCriteria.PropInfo.IDS.propertyName)),
                        DefectTypes.maxElementsInSelection(MAX_ELEMENTS_PER_GET)
                )
        ));
    }

    @Test
    public void validateRequest_IdsWithNull_Failure() throws Exception {
        GetRequest req = createAdVideosGetRequest();
        req.getSelectionCriteria().setIds(Arrays.asList("60dd854d802a66ed486e6f64005", null));
        ValidationResult<GetRequest, DefectType> vr = validationService.validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(GetRequest.PropInfo.SELECTION_CRITERIA.propertyName),
                                field(AdVideosSelectionCriteria.PropInfo.IDS.propertyName)),
                        DefectTypes.absentElementInArray()
                )
        ));
    }

    private static GetRequest createAdVideosGetRequest() {
        AdVideosSelectionCriteria sc = new AdVideosSelectionCriteria();
        sc.setIds(Arrays.asList("60dd854d802a66ed486e6f64005", "60dd854d802a66ed486e6f64006"));
        GetRequest req = new GetRequest();
        req.setSelectionCriteria(sc);
        return req;
    }
}
