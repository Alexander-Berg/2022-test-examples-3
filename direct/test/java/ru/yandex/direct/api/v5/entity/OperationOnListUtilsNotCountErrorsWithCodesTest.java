package ru.yandex.direct.api.v5.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.result.ApiResult;
import ru.yandex.direct.api.v5.result.ApiResultState;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.units.OperationSummary;
import ru.yandex.direct.validation.result.DefectInfo;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.OperationOnListUtils.notCountErrorsWithCodes;

@RunWith(JUnitParamsRunner.class)
public class OperationOnListUtilsNotCountErrorsWithCodesTest {
    private static final List<Object> INTERNAL_REQ = Arrays.asList(1, 2, 3);
    private static final int DEFECT_CODE = 1000;    // код ошибки, которая не должна отфильтроваться
    private static final int FILTERING_CODE = 2000; // код фильтруемой ошибки

    @Test
    public void notCountErrorsWithCodes_BrokenOperation() {
        ApiMassResult<Object> brokenApiMassResult = getBrokenApiMassResult();
        OperationSummary actualOperationSummary = notCountErrorsWithCodes(brokenApiMassResult, Set.of(FILTERING_CODE));
        OperationSummary expectedOperationSummary = OperationSummary.unsuccessful();
        Assertions.assertThat(actualOperationSummary).isEqualTo(expectedOperationSummary);
    }

    /**
     * Проверка логики фильтрации ошибок: если среди ошибок имеются ошибки с переданным кодом, то они не включаются
     * в общее число ошибок. Число успешно обработанных результатов при этом не изменяется.
     * @param defectNum    общее количество ошибок.
     * @param filteringNum количество фильтруемых ошибок.
     */
    @Test
    @Parameters({
            "0, 0",
            "1, 0",
            "1, 1",
            "2, 0",
            "2, 1",
            "2, 2",
            "3, 3",
    })
    public void notCountErrorsWithCodes_SuccessfulOperation(int defectNum, int filteringNum) {
        int successfulNum = INTERNAL_REQ.size() - defectNum;
        ApiMassResult<Object> apiMassResult = getApiMassResult(successfulNum, defectNum - filteringNum, filteringNum);
        OperationSummary actualOperationSummary = notCountErrorsWithCodes(apiMassResult, Set.of(FILTERING_CODE));
        // число успешно обработанных результатов не изменилось,
        // а в число неуспешных не включаются ошибки с неверным статусом
        OperationSummary expectedOperationSummary =
                OperationSummary.successful(successfulNum, defectNum - filteringNum);
        Assertions.assertThat(actualOperationSummary).isEqualTo(expectedOperationSummary);
    }

    /**
     * Если на элементе содержится несколько ошибок, то фильтрация происходит,
     * только если все ошибки подлежат фильтрации.
     */
    @Test
    public void notCountErrorsWithCodes_ItemWithSeveralErrors() {
        List<ApiResult<Object>> results = new ArrayList<>();
        results.add(getBrokenApiResultWithDefectCodes(DEFECT_CODE, DEFECT_CODE));       // broken
        results.add(getBrokenApiResultWithDefectCodes(DEFECT_CODE, FILTERING_CODE));    // broken
        results.add(getBrokenApiResultWithDefectCodes(FILTERING_CODE, FILTERING_CODE)); // filtered
        ApiMassResult<Object> apiMassResult = new ApiMassResult<>(results, emptyList(), emptyList(), ApiResultState.SUCCESSFUL);

        OperationSummary actualOperationSummary = notCountErrorsWithCodes(apiMassResult, Set.of(FILTERING_CODE));
        OperationSummary expectedOperationSummary = OperationSummary.successful(0, 2);
        Assertions.assertThat(actualOperationSummary).isEqualTo(expectedOperationSummary);
    }

    private ApiMassResult<Object> getBrokenApiMassResult() {
        ApiMassResult<Object> apiMassResult = mock(ApiMassResult.class);
        when(apiMassResult.isSuccessful()).thenReturn(false);
        when(apiMassResult.getOperationSummary()).thenReturn(OperationSummary.unsuccessful());
        return apiMassResult;
    }

    /**
     * @param successfulNum   количество успешных результатов.
     * @param notFilteringNum количество не фильтруемых ошибок
     * @param filteringNum    количество фильтруемых ошибок.
     */
    private ApiMassResult<Object> getApiMassResult(int successfulNum, int notFilteringNum, int filteringNum) {
        List<ApiResult<Object>> results = new ArrayList<>(INTERNAL_REQ.size());
        IntStream.range(0, successfulNum).forEach(i -> results.add(ApiResult.successful(INTERNAL_REQ.get(i))));
        IntStream.range(0, notFilteringNum)
                .forEach(i -> results.add(getBrokenApiResultWithDefectCodes(DEFECT_CODE)));
        IntStream.range(0, filteringNum)
                .forEach(i -> results.add(getBrokenApiResultWithDefectCodes(FILTERING_CODE)));
        return new ApiMassResult<>(results, emptyList(), emptyList(), ApiResultState.SUCCESSFUL);
    }

    private ApiResult<Object> getBrokenApiResultWithDefectCodes(int... codes) {
        List<DefectInfo<DefectType>> defectInfos = new ArrayList<>(codes.length);
        for (int code: codes) {
            DefectType defectType = mock(DefectType.class);
            when(defectType.getCode()).thenReturn(code);
            DefectInfo<DefectType> defectInfo = mock(DefectInfo.class);
            when(defectInfo.getDefect()).thenReturn(defectType);
            defectInfos.add(defectInfo);
        }
        return ApiResult.broken(defectInfos, emptyList());
    }
}
