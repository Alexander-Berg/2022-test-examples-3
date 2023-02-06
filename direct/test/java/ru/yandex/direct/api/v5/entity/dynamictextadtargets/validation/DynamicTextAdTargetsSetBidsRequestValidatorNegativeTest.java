package ru.yandex.direct.api.v5.entity.dynamictextadtargets.validation;

import java.util.Collection;
import java.util.function.Consumer;

import com.yandex.direct.api.v5.dynamictextadtargets.SetBidsItem;
import com.yandex.direct.api.v5.dynamictextadtargets.SetBidsRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.common.validation.ApiDefectPresentation;
import ru.yandex.direct.api.v5.common.validation.DefectPresentationService;
import ru.yandex.direct.api.v5.entity.dynamictextadtargets.delegate.SetBidsDynamicTextAdTargetsDelegate;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static freemarker.template.utility.Collections12.singletonList;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.direct.api.v5.validation.DefectTypes.absentElementInArray;
import static ru.yandex.direct.api.v5.validation.DefectTypes.mixedTypes;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class DynamicTextAdTargetsSetBidsRequestValidatorNegativeTest {

    private static DynamicTextAdTargetsSetBidsRequestValidator validator;
    private static DefectPresentationService defectPresentationService =
            new DefectPresentationService(SetBidsDynamicTextAdTargetsDelegate.CUSTOM_SET_BIDS_PRESENTATION);

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public SetBidsRequest request;

    @Parameterized.Parameter(2)
    public Consumer<ValidationResult<SetBidsRequest, DefectType>> assertion;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"Запрос c одним null",
                        new SetBidsRequest()
                                .withBids(singletonList(null)),
                        assertionForErrors(path(field("Bids")),
                                absentElementInArray())},
                {"Запрос c одним SetBidItem и одним null",
                        new SetBidsRequest()
                                .withBids(asList(new SetBidsItem(), null)),
                        assertionForErrors(path(field("Bids")),
                                absentElementInArray())},
                {"Запрос без полей Id, CampaignId, AdGroupId",
                        new SetBidsRequest()
                                .withBids(new SetBidsItem()),
                        assertionForErrors(path(field("Bids")),
                                mixedTypes())}
        });
    }

    @BeforeClass
    public static void setUp() {
        validator = new DynamicTextAdTargetsSetBidsRequestValidator();
    }

    private static Consumer<ValidationResult<SetBidsRequest, DefectType>> assertionForErrors(Path path,
                                                                                             Defect defect) {
        ApiDefectPresentation presentation = defectPresentationService.getPresentationFor(defect.defectId());
        DefectType expectedDefectType = presentation.toDefectType(defect.params());
        return assertionForErrors(path, expectedDefectType);
    }

    private static Consumer<ValidationResult<SetBidsRequest, DefectType>> assertionForErrors(Path path,
                                                                                             DefectType defectType) {
        return (vresult) -> assertThat(vresult.flattenErrors(), contains(validationError(path, defectType)));
    }

    @Test
    public void test() {
        ValidationResult<SetBidsRequest, DefectType> vr = validator.validate(request);
        assertion.accept(vr);
    }

}
