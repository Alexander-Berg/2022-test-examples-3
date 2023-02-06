package ru.yandex.direct.testing.matchers.result;

import java.util.ArrayList;
import java.util.List;

import org.junit.runners.Parameterized.Parameter;

import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.validation.result.PathHelper.index;

public class MassResultMatcherBaseTest {

    @Parameter
    public MatcherDescription matcherDescription;

    @Parameter(1)
    public ResultDescription resultDescription;

    @Parameter(2)
    public boolean expectedMatch;

    public void matches_worksFine() {
        MassResult<Long> result = resultDescription.getResult();
        MassResultMatcher<Long> matcher = matcherDescription.getMassResultMatcher();
        assertThat(matcher.matches(result), is(expectedMatch));
    }

    /**
     * Оболочка над матчером для формирования описания теста
     */
    protected static class MatcherDescription {
        private MassResultMatcher<Long> massResultMatcher;

        public MatcherDescription(MassResultMatcher<Long> massResultMatcher) {
            this.massResultMatcher = massResultMatcher;
        }

        public static MatcherDescription matcherDesc(MassResultMatcher<Long> massResultMatcher) {
            return new MatcherDescription(massResultMatcher);
        }

        public MassResultMatcher<Long> getMassResultMatcher() {
            return massResultMatcher;
        }

        @Override
        public String toString() {
            if (massResultMatcher.isCheckAllItems()) {
                return String.format("[operation success: %s; all items success: %s]",
                        massResultMatcher.isOperationSuccess(), massResultMatcher.isOperationSuccess());
            }

            if (massResultMatcher.getItemsSuccessFlags() == null) {
                return String.format("[operation success: %s]",
                        massResultMatcher.isOperationSuccess());
            }

            return String.format("[operation success: %s; items success: %s]",
                    massResultMatcher.isOperationSuccess(),
                    massResultMatcher.getItemsSuccessFlags());
        }
    }


    /**
     * Оболочка над результатом для формирования описания теста
     */
    protected static class ResultDescription {
        private boolean successful;
        private List<Boolean> itemsSuccessfulFlags;

        private ResultDescription(boolean successful, List<Boolean> itemsSuccessfulFlags) {
            this.successful = successful;
            this.itemsSuccessfulFlags = itemsSuccessfulFlags;
        }

        public static ResultDescription resultDesc(boolean successful, List<Boolean> itemsSuccessfulFlags) {
            return new ResultDescription(successful, itemsSuccessfulFlags);
        }

        public MassResult<Long> getResult() {
            List<String> inputItems = new ArrayList<>();
            ValidationResult<List<String>, Defect> validationResult =
                    new ValidationResult<>(inputItems);
            List<Long> results = new ArrayList<>();

            for (int i = 0; i < itemsSuccessfulFlags.size(); i++) {
                boolean successfulItem = itemsSuccessfulFlags.get(i);

                String inputItem = successfulItem ? getValidInputItem(i) : getInvalidInputItem(i);
                inputItems.add(inputItem);

                Long result = successfulItem ? (long) i : null;
                results.add(result);

                if (!successfulItem) {
                    validationResult.getOrCreateSubValidationResult(index(i), inputItem)
                            .addError(new Defect<>(DefectIds.INVALID_VALUE));
                }
            }

            if (!successful) {
                validationResult.addError(new Defect<>(DefectIds.INVALID_VALUE));
            }

            return successful ?
                    MassResult.successfulMassAction(results, validationResult) :
                    MassResult.brokenMassAction(results, validationResult);
        }

        @Override
        public String toString() {
            return String.format("[operation success: %s; items success: %s]", successful, itemsSuccessfulFlags);
        }

    }

    protected static String getValidInputItem(int index) {
        return "valid string " + index;
    }

    protected static String getInvalidInputItem(int index) {
        return "invalid string " + index;
    }
}
