package ru.yandex.direct.testing.matchers.result;

import java.util.List;
import java.util.Objects;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class MassResultMatcher<R> extends BaseMatcher<MassResult<R>> {

    private final boolean operationSuccess;

    private final boolean checkAllItems;
    private final List<Boolean> itemsSuccessFlags;

    private final List<Matcher<R>> itemsMatchers;

    private MassResultMatcher(boolean operationSuccess, boolean checkAllItemsSuccess,
                              List<Matcher<R>> itemsMatchers) {
        this.operationSuccess = operationSuccess;
        this.checkAllItems = checkAllItemsSuccess;
        this.itemsSuccessFlags = null;
        this.itemsMatchers = itemsMatchers;
    }

    private MassResultMatcher(boolean operationSuccess, List<Boolean> itemsSuccessFlags,
                              List<Matcher<R>> itemsMatchers) {
        this.operationSuccess = operationSuccess;
        this.checkAllItems = false;
        this.itemsSuccessFlags = itemsSuccessFlags;
        this.itemsMatchers = itemsMatchers;
    }

    /**
     * Проверяет, что отсутствуют ошибки уровня операции. Отдельные элементы не проверяются.
     */
    public static <R> MassResultMatcher<R> isSuccessful() {
        return new MassResultMatcher<>(true, false, null);
    }

    /**
     * Проверяет, что отсутствуют ошибки уровня операции, а так же проверяет
     * наличие ошибок на отдельных элементах в соответствии с переданными
     * флагами успешности.
     *
     * @param itemsSuccessFlags флаги, обозначающие ожидаемый результат валидации отдельных элементов
     */
    public static <R> MassResultMatcher<R> isSuccessful(Boolean... itemsSuccessFlags) {
        List<Boolean> itemsSuccessFlagsList = asList(itemsSuccessFlags);
        return isSuccessful(itemsSuccessFlagsList);
    }

    /**
     * Проверяет, что отсутствуют ошибки уровня операции, а так же проверяет
     * наличие ошибок на отдельных элементах в соответствии с переданными
     * флагами успешности.
     *
     * @param itemsSuccessFlags флаги, обозначающие ожидаемый результат валидации отдельных элементов
     */
    public static <R> MassResultMatcher<R> isSuccessful(List<Boolean> itemsSuccessFlags) {
        return new MassResultMatcher<>(true, itemsSuccessFlags, null);
    }

    /**
     * Проверяет, что отсутствуют ошибки уровня операции, а так же проверяет,
     * что все элементы успешны, а значения их результатов соответствуют переданному списку.
     * <p>
     * Ожидаемый размер результата соответствует длине входного списка.
     * Результат с индексом i должен быть равен элементу списка с индексом i.
     *
     * @param expectedItems список ожидаемых значений в элементах результата
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <R> MassResultMatcher<R> isSuccessfulWithItems(R... expectedItems) {
        List<R> expectedItemsList = asList(expectedItems);
        List<Matcher<R>> matchers = mapList(expectedItemsList, Matchers::equalTo);
        return isSuccessfulWithMatchers(matchers.toArray(new Matcher[matchers.size()]));
    }

    /**
     * Проверяет, что отсутствуют ошибки уровня операции, а так же проверяет
     * флаги упешности элементов и значения их результатов в соответствии со списком матчеров.
     * <p>
     * Ожидаемый размер результата соответствует длине входного списка.
     * Ожидаемые флаги успешности вычисляются на основе списка матчеров:
     * для успешных элементов должен присутствовать матчер, а для проваленных должен быть равен null.
     * Результат с индексом i проверяется матчером из списка с индексом i.
     *
     * @param itemsMatchers список матчеров для проверки значений в элементах результата,
     *                      для проваленных элементов нужно передавать null
     */
    @SafeVarargs
    public static <R> MassResultMatcher<R> isSuccessfulWithMatchers(Matcher<R>... itemsMatchers) {
        List<Matcher<R>> itemsMatchersList = asList(itemsMatchers);
        List<Boolean> itemsSuccessfulFlags = mapList(itemsMatchersList, Objects::nonNull);
        return new MassResultMatcher<>(true, itemsSuccessfulFlags, itemsMatchersList);
    }

    /**
     * Проверяет, что результат операции и результаты всех элементов успешны.
     */
    public static <R> MassResultMatcher<R> isFullySuccessful() {
        return new MassResultMatcher<>(true, true, null);
    }

    /**
     * Проверяет, что операция провалена.
     */
    public static <R> MassResultMatcher<R> isFailed() {
        return new MassResultMatcher<>(false, null, null);
    }

    public boolean isOperationSuccess() {
        return operationSuccess;
    }

    public boolean isCheckAllItems() {
        return checkAllItems;
    }

    public List<Boolean> getItemsSuccessFlags() {
        return itemsSuccessFlags;
    }

    public List<Matcher<R>> getItemsMatchers() {
        return itemsMatchers;
    }

    @Override
    public boolean matches(Object item) {
        return item instanceof MassResult &&
                massResultMatches((MassResult) item);
    }

    private boolean massResultMatches(MassResult<?> massResult) {
        boolean checkResult = true;

        if (!checkOperation(massResult)) {
            checkResult = false;
        }

        if (!checkItemsSuccess(massResult)) {
            checkResult = false;
        }

        if (!checkItemsResults(massResult)) {
            checkResult = false;
        }

        return checkResult;
    }

    private boolean checkOperation(MassResult<?> massResult) {
        return massResult.isSuccessful() == operationSuccess;
    }

    private boolean checkItemsSuccess(MassResult<?> massResult) {
        List<Boolean> actualItemsSuccessfulFlags = mapList(massResult.getResult(), Result::isSuccessful);
        if (checkAllItems) {
            if (massResult.getResult() == null) {
                return false;
            }
            List<Boolean> expectedItemsSuccessfulFlags = nCopies(massResult.getResult().size(), operationSuccess);
            return expectedItemsSuccessfulFlags.equals(actualItemsSuccessfulFlags);
        }
        if (itemsSuccessFlags != null) {
            return itemsSuccessFlags.equals(actualItemsSuccessfulFlags);
        }
        return true;
    }

    private boolean checkItemsResults(MassResult<?> massResult) {
        if (itemsMatchers != null) {
            if (massResult.getResult() == null) {
                return false;
            }

            List<?> items = mapList(massResult.getResult(), Result::getResult);
            if (items.size() != itemsMatchers.size()) {
                return false;
            }

            for (int i = 0; i < items.size(); i++) {
                Matcher matcher = itemsMatchers.get(i);
                if (matcher == null) {
                    continue;
                }
                if (!matcher.matches(items.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void describeMismatch(Object item, Description mismatchDescription) {
        if (item == null) {
            mismatchDescription.appendText("null");
            return;
        }
        if (!(item instanceof MassResult)) {
            mismatchDescription.appendText("not an instance of " + MassResult.class.getCanonicalName());
            return;
        }
        describeResultMismatch((MassResult) item, mismatchDescription);
    }

    private void describeResultMismatch(MassResult massResult, Description mismatchDescription) {
        mismatchDescription.appendText("\n");

        mismatchDescription.appendText("\nOperation success: ")
                .appendValue(massResult.isSuccessful());

        @SuppressWarnings("unchecked")
        List<Result> results = (List<Result>) massResult.getResult();

        if (checkAllItems || itemsSuccessFlags != null) {
            if (results != null) {
                List<Boolean> actualItemsSuccessfulFlags = mapList(results, Result::isSuccessful);
                mismatchDescription.appendText("\nElements success: ")
                        .appendValue(actualItemsSuccessfulFlags);
            } else {
                mismatchDescription.appendText("\nElements don't present");
            }
        }

        boolean matchersWereApplied = itemsMatchers != null &&
                results != null &&
                results.size() == itemsMatchers.size();

        if (matchersWereApplied) {
            mismatchDescription.appendText("\nElements results mismatches:\n");
            List items = mapList(results, Result::getResult);
            for (int i = 0; i < itemsMatchers.size(); i++) {
                Matcher matcher = itemsMatchers.get(i);
                mismatchDescription.appendText("[" + i + "]: ");
                if (matcher != null) {
                    if (!matcher.matches(items.get(i))) {
                        matcher.describeMismatch(items.get(i), mismatchDescription);
                    } else {
                        mismatchDescription.appendText("[match]");
                    }
                } else {
                    mismatchDescription.appendText("[no matcher]");
                }
                mismatchDescription.appendText("\n");
            }
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("\n");

        description.appendText("\nOperation success: ")
                .appendValue(operationSuccess);

        if (checkAllItems) {
            description.appendText("\nElements success: all are ")
                    .appendValue(operationSuccess);
        } else {
            if (itemsSuccessFlags != null) {
                description.appendText("\nElements success: ")
                        .appendValue(itemsSuccessFlags);
            }
        }

        if (itemsMatchers != null) {
            description.appendText("\nElements results matchers:\n");
            for (int i = 0; i < itemsMatchers.size(); i++) {
                Matcher matcher = itemsMatchers.get(i);
                description.appendText("[" + i + "]: ");
                if (matcher != null) {
                    matcher.describeTo(description);
                } else {
                    description.appendText("[no matcher]");
                }
                description.appendText("\n");
            }
        }

        description.appendText("\n");
    }
}
