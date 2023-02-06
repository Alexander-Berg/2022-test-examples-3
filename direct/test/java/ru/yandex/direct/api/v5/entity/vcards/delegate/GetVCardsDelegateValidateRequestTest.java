package ru.yandex.direct.api.v5.entity.vcards.delegate;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.general.IdsCriteria;
import com.yandex.direct.api.v5.general.LimitOffset;
import com.yandex.direct.api.v5.vcards.GetRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.common.validation.ApiDefectPresentation;
import ru.yandex.direct.api.v5.common.validation.DefaultApiPresentations;
import ru.yandex.direct.api.v5.common.validation.DefectPresentationService;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.ApiDefectDefinitions;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.vcard.service.VcardService;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_IDS_COUNT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_IDS_COUNT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_OFFSET;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.maxIdsInSelectionVcard;


@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetVCardsDelegateValidateRequestTest {

    // NB: тест рассчитан на значения ограничений по умолчанию

    private static GetVCardsDelegate service;
    private static DefectPresentationService defectPresentationService =
            new DefectPresentationService(DefaultApiPresentations.HOLDER);


    @Parameterized.Parameter()
    @SuppressWarnings("unused")
    public String description;

    @Parameterized.Parameter(1)
    public GetRequest request;

    @Parameterized.Parameter(2)
    public Consumer<ValidationResult<GetRequest, DefectType>> assertion;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                /*
                // NB: из-за особенностей реализации IdsCriteria::getIds всегда возвращает не нулевой new ArrayList<Long>()
                {"не передан список SelectionCriteria.Ids",
                        new GetRequest()
                                .withSelectionCriteria(new IdsCriteria()),
                        assertionForErrors(Defects.absentRequiredField())
                },
                */
                {"список SelectionCriteria.Ids пуст",
                        new GetRequest()
                                .withSelectionCriteria(new IdsCriteria().withIds(new ArrayList<>())),
                        assertionForErrors(CollectionDefects.minCollectionSize(DEFAULT_MIN_IDS_COUNT))
                },
                {"размер списка SelectionCriteria.Ids больше максимально допустимого",
                        new GetRequest()
                                .withSelectionCriteria(new IdsCriteria().withIds(getTooLongListOfIds())),
                        assertionForErrors(maxIdsInSelectionVcard())
                },
                {"значение SelectionCriteria.Limit меньше минимально допустимого (-1)",
                        new GetRequest()
                                .withPage(new LimitOffset().withLimit(DEFAULT_MIN_LIMIT - 1)),
                        assertionForErrors(ApiDefectDefinitions.incorrectPageNonPositiveLimit())
                },
                {"значение SelectionCriteria.Limit больше максимально допустимого",
                        new GetRequest()
                                .withPage(new LimitOffset().withLimit(DEFAULT_MAX_LIMIT + 1)),
                        assertionForErrors(ApiDefectDefinitions.incorrectPageLimitExceeded(DEFAULT_MAX_LIMIT))
                },
                {"SelectionCriteria.Limit не передан",
                        new GetRequest().withPage(new LimitOffset()),
                        assertionForNoErrors()
                },
                {"значение SelectionCriteria.Offset меньше минимально допустимого (-1)",
                        new GetRequest()
                                .withPage(new LimitOffset().withOffset(DEFAULT_MIN_OFFSET - 1)),
                        assertionForErrors(ApiDefectDefinitions.incorrectPageNegativeOffset())
                },
                {"значение SelectionCriteria.Offset не передано",
                        new GetRequest().withPage(new LimitOffset()),
                        assertionForNoErrors()
                }
        });
    }


    @BeforeClass
    public static void prepare() {
        ApiAuthenticationSource apiAuthenticationSource = mock(ApiAuthenticationSource.class);
        PropertyFilter propertyFilter = mock(PropertyFilter.class);
        VcardService vcardService = mock(VcardService.class);
        ResultConverter resultConverter =
                new ResultConverter(null, defectPresentationService);

        service = new GetVCardsDelegate(apiAuthenticationSource, vcardService, propertyFilter, resultConverter);
    }


    private static Consumer<ValidationResult<GetRequest, DefectType>> assertionForErrors(Defect defect) {
        ApiDefectPresentation presentation = defectPresentationService.getPresentationFor(defect.defectId());

        DefectType expectedDefectType = presentation.toDefectType(defect.params());

        return (vresult) -> assertThat(vresult.flattenErrors())
                .extracting(DefectInfo::getDefect)
                .containsExactlyInAnyOrder(expectedDefectType);
    }

    private static Consumer<ValidationResult<GetRequest, DefectType>> assertionForNoErrors() {
        return (vresult) -> assertThat(vresult.flattenErrors()).isEmpty();
    }

    private static List<Long> getTooLongListOfIds() {
        List<Long> idsList = new ArrayList<>();

        for (int i = 0; i < DEFAULT_MAX_IDS_COUNT + 1; i++) {
            idsList.add(new Long(i));
        }

        return idsList;
    }

    @Test
    public void test() {
        ValidationResult<GetRequest, DefectType> t = service.validateRequest(request);
        assertion.accept(t);
    }

}
