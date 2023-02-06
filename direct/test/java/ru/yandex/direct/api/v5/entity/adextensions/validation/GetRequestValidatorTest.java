package ru.yandex.direct.api.v5.entity.adextensions.validation;

import java.util.Arrays;
import java.util.Collections;

import com.yandex.direct.api.v5.adextensions.AdExtensionsSelectionCriteria;
import com.yandex.direct.api.v5.adextensions.GetRequest;
import com.yandex.direct.api.v5.adextensiontypes.AdExtensionStateSelectionEnum;
import com.yandex.direct.api.v5.adextensiontypes.AdExtensionTypeEnum;
import com.yandex.direct.api.v5.general.ExtensionStatusSelectionEnum;
import org.junit.Test;

import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.api.v5.entity.adextensions.validation.GetRequestValidator.GET_IDS_LIMIT;
import static ru.yandex.direct.api.v5.entity.adextensions.validation.GetRequestValidator.IDS_FIELD;
import static ru.yandex.direct.api.v5.entity.adextensions.validation.GetRequestValidator.MODIFIED_SINCE_FIELD;
import static ru.yandex.direct.api.v5.entity.adextensions.validation.GetRequestValidator.SELECTION_CRITERIA_FIELD;
import static ru.yandex.direct.api.v5.entity.adextensions.validation.GetRequestValidator.TYPES_FIELD;
import static ru.yandex.direct.api.v5.entity.adextensions.validation.GetRequestValidator.validateRequest;
import static ru.yandex.direct.api.v5.validation.DefectTypeCreators.invalidRequestParamsIncorrectDate;
import static ru.yandex.direct.api.v5.validation.DefectTypeCreators.invalidRequestParamsInvalidDateFormat;
import static ru.yandex.direct.api.v5.validation.DefectTypeCreators.wrongSelectionCriteriaUnsupportedFieldValues;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class GetRequestValidatorTest {
    private static final String MODIFIED_SINCE_WITH_WRONG_MONTH = "2011-99-03T16:00:01Z";

    @Test
    public void validateRequest_FullReq_Success() throws Exception {
        ValidationResult<GetRequest, DefectType> vr = validateRequest(createValidFullGetRequest());
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_EmptyReq_Success() throws Exception {
        ValidationResult<GetRequest, DefectType> vr = validateRequest(new GetRequest());
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_IdsMany_Success() throws Exception {
        GetRequest req = createValidFullGetRequest();
        req.getSelectionCriteria().setIds(Collections.nCopies(GET_IDS_LIMIT, 1L));
        ValidationResult<GetRequest, DefectType> vr = validateRequest(req);
        assertThat(vr, hasNoDefects());
    }

    @Test
    public void validateRequest_IdsTooMany() throws Exception {
        GetRequest req = createValidFullGetRequest();
        req.getSelectionCriteria().setIds(Collections.nCopies(GET_IDS_LIMIT + 1, 1L));
        ValidationResult<GetRequest, DefectType> vr = validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(SELECTION_CRITERIA_FIELD), field(IDS_FIELD)),
                        DefectTypes.maxIdsInSelection()
                )
        ));
    }

    @Test
    public void validateRequest_IdsWithNull() throws Exception {
        GetRequest req = createValidFullGetRequest();
        req.getSelectionCriteria().setIds(Arrays.asList(1L, null));
        ValidationResult<GetRequest, DefectType> vr = validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(SELECTION_CRITERIA_FIELD), field(IDS_FIELD)),
                        DefectTypes.absentElementInArray()
                )
        ));
    }

    @Test
    public void validateRequest_Types_SupportedCalloutOnly() throws Exception {
        GetRequest req = createValidFullGetRequest();
        req.getSelectionCriteria().setTypes(Collections.singletonList(AdExtensionTypeEnum.UNKNOWN));
        ValidationResult<GetRequest, DefectType> vr = validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(SELECTION_CRITERIA_FIELD), field(TYPES_FIELD), index(0)),
                        wrongSelectionCriteriaUnsupportedFieldValues(
                                TYPES_FIELD, Collections.singletonList("CALLOUT"))
                )
        ));
    }

    @Test
    public void validateRequest_ModifiedSince_WrongFormat() throws Exception {
        GetRequest req = createValidFullGetRequest();
        req.getSelectionCriteria().setModifiedSince("12");
        ValidationResult<GetRequest, DefectType> vr = validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(SELECTION_CRITERIA_FIELD), field(MODIFIED_SINCE_FIELD)),
                        invalidRequestParamsInvalidDateFormat(MODIFIED_SINCE_FIELD)
                )
        ));
    }

    @Test
    public void validateRequest_ModifiedSince_IncorrectDate() throws Exception {
        GetRequest req = createValidFullGetRequest();
        req.getSelectionCriteria().setModifiedSince(MODIFIED_SINCE_WITH_WRONG_MONTH);
        ValidationResult<GetRequest, DefectType> vr = validateRequest(req);
        assertThat(vr, hasDefectWith(
                validationError(
                        path(field(SELECTION_CRITERIA_FIELD), field(MODIFIED_SINCE_FIELD)),
                        invalidRequestParamsIncorrectDate(MODIFIED_SINCE_FIELD)
                )
        ));
    }

    private static GetRequest createValidFullGetRequest() {
        AdExtensionsSelectionCriteria sc = new AdExtensionsSelectionCriteria();
        sc.setIds(Arrays.asList(1L, 2L, 3L));
        sc.setTypes(Collections.singletonList(AdExtensionTypeEnum.CALLOUT));
        sc.setModifiedSince("2011-12-03T16:00:01Z");
        sc.setStates(Collections.singletonList(AdExtensionStateSelectionEnum.ON));
        sc.setStatuses(Collections.singletonList(ExtensionStatusSelectionEnum.ACCEPTED));
        GetRequest req = new GetRequest();
        req.setSelectionCriteria(sc);
        return req;
    }
}
