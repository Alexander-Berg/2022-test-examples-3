package ru.yandex.direct.validation.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.direct.validation.result.PathHelper;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.validation.result.PathHelper.index;

public class ListValidationBuilderTest {

    // Objects under validation
    private static final Object ITEM1 = new Object();
    private static final Object ITEM2 = new Object();
    private static final Object ITEM3 = new Object();
    private static final Object ITEM4 = new Object();
    private static final List<Object> LIST = new ArrayList<>();

    static {
        LIST.add(ITEM1);
        LIST.add(ITEM2);
        LIST.add(ITEM3);
        LIST.add(ITEM4);
    }

    // Defects
    private static final String ITEM1_DEFECT = "item1 defect";
    private static final String ITEM2_DEFECT = "item2 defect";
    private static final String ITEM_OVERRIDE_DEFECT = "item overriding defect";
    private static final String LIST_DEFECT = "list defect";
    private static final String LIST_OVERRIDE_DEFECT = "list overriding defect";

    // Constraint implementations for entire list
    private static final Constraint<List<Object>, String> LIST_PASSING_CONSTRAINT = t -> null;
    private static final Constraint<List<Object>, String> LIST_FAILING_CONSTRAINT = t -> LIST_DEFECT;

    // When implementations for entire list
    private static final When<List<Object>, String> LIST_POSITIVE_WHEN = When.isTrue(true);
    private static final When<List<Object>, String> LIST_NEGATIVE_WHEN = When.isTrue(false);

    // check

    @Test
    public void check_CheckPasses_DoesNotAddErrors() {
        checkWithNoExpectedDefects(
                vb -> vb.check(LIST_PASSING_CONSTRAINT));
    }

    @Test
    public void check_CheckFails_AddsError() {
        checkWithExpectedError(
                vb -> vb.check(LIST_FAILING_CONSTRAINT), LIST_DEFECT);
    }

    // check with overrideDefect

    @Test
    public void check_CheckPassesWithOverrideDefect_DoesNotAddErrors() {
        checkWithNoExpectedDefects(
                vb -> vb.check(LIST_PASSING_CONSTRAINT, LIST_OVERRIDE_DEFECT));
    }

    @Test
    public void check_CheckFailsWithOverrideDefect_AddsOverrideError() {
        checkWithExpectedError(
                vb -> vb.check(LIST_FAILING_CONSTRAINT, LIST_OVERRIDE_DEFECT),
                LIST_OVERRIDE_DEFECT);
    }

    // check with When

    @Test
    public void check_WhenIsNegative_CheckIsNotExecuted() {
        checkWithNoExpectedDefects(
                vb -> vb.check(LIST_FAILING_CONSTRAINT, LIST_NEGATIVE_WHEN));
    }

    @Test
    public void check_WhenIsPositive_CheckIsExecuted() {
        checkWithExpectedError(
                vb -> vb.check(LIST_FAILING_CONSTRAINT, LIST_POSITIVE_WHEN),
                LIST_DEFECT);
    }

    // check with When and overrideDefect

    @Test
    public void check_WhenIsNegativeWithOverrideDefect_CheckIsNotExecuted() {
        checkWithNoExpectedDefects(
                vb -> vb.check(LIST_FAILING_CONSTRAINT, LIST_OVERRIDE_DEFECT, LIST_NEGATIVE_WHEN));
    }

    @Test
    public void check_WhenIsPositiveWithOverrideDefect_CheckExecutedAndOverrideDefectAdded() {
        checkWithExpectedError(
                vb -> vb.check(LIST_FAILING_CONSTRAINT, LIST_OVERRIDE_DEFECT, LIST_POSITIVE_WHEN),
                LIST_OVERRIDE_DEFECT);
    }

    // check (two calls)

    @Test
    @SuppressWarnings("unchecked")
    public void check_TwoCallsWithFails_TwoErrorsAdded() {
        String secondError = "second error";
        ListValidationBuilder<Object, String> vb = ListValidationBuilder.of(LIST);
        vb.check(LIST_FAILING_CONSTRAINT)
                .check(t -> secondError);

        assertThat(vb.getResult().getWarnings(), hasSize(0));
        assertThat(vb.getResult().getErrors(), contains(is(LIST_DEFECT), is(secondError)));
    }

    // weakCheck

    @Test
    public void weakCheck_CheckPasses_DoesNotAddErrors() {
        checkWithNoExpectedDefects(
                vb -> vb.weakCheck(LIST_PASSING_CONSTRAINT));
    }

    @Test
    public void weakCheck_CheckFails_AddsError() {
        checkWithExpectedWarning(
                vb -> vb.weakCheck(LIST_FAILING_CONSTRAINT), LIST_DEFECT);
    }

    // weakCheck with overrideDefect

    @Test
    public void weakCheck_CheckPassesWithOverrideDefect_DoesNotAddErrors() {
        checkWithNoExpectedDefects(
                vb -> vb.weakCheck(LIST_PASSING_CONSTRAINT, LIST_OVERRIDE_DEFECT));
    }

    @Test
    public void weakCheck_CheckFailsWithOverrideDefect_AddsOverrideError() {
        checkWithExpectedWarning(
                vb -> vb.weakCheck(LIST_FAILING_CONSTRAINT, LIST_OVERRIDE_DEFECT),
                LIST_OVERRIDE_DEFECT);
    }

    // weakCheck with When

    @Test
    public void weakCheck_WhenIsNegative_CheckIsNotExecuted() {
        checkWithNoExpectedDefects(
                vb -> vb.weakCheck(LIST_FAILING_CONSTRAINT, LIST_NEGATIVE_WHEN));
    }

    @Test
    public void weakCheck_WhenIsPositive_CheckIsExecuted() {
        checkWithExpectedWarning(
                vb -> vb.weakCheck(LIST_FAILING_CONSTRAINT, LIST_POSITIVE_WHEN),
                LIST_DEFECT);
    }

    // weakCheck with When and overrideDefect

    @Test
    public void weakCheck_WhenIsNegativeWithOverrideDefect_CheckIsNotExecuted() {
        checkWithNoExpectedDefects(
                vb -> vb.weakCheck(LIST_FAILING_CONSTRAINT, LIST_OVERRIDE_DEFECT, LIST_NEGATIVE_WHEN));
    }

    @Test
    public void weakCheck_WhenIsPositiveWithOverrideDefect_CheckExecutedAndOverrideDefectAdded() {
        checkWithExpectedWarning(
                vb -> vb.weakCheck(LIST_FAILING_CONSTRAINT, LIST_OVERRIDE_DEFECT, LIST_POSITIVE_WHEN),
                LIST_OVERRIDE_DEFECT);
    }

    // weakCheck (two calls)

    @Test
    @SuppressWarnings("unchecked")
    public void weakCheck_TwoCallsWithFails_TwoErrorsAdded() {
        String secondDefect = "second defect";
        ListValidationBuilder<Object, String> vb = ListValidationBuilder.of(LIST);
        vb.weakCheck(LIST_FAILING_CONSTRAINT)
                .weakCheck(t -> secondDefect);

        assertThat(vb.getResult().getErrors(), hasSize(0));
        assertThat(vb.getResult().getWarnings(), contains(is(LIST_DEFECT), is(secondDefect)));
    }

    // checkBy

    @Test
    @SuppressWarnings("unchecked")
    public void checkBy_HasNotErrorsAndWarnings_WorksFine() {
        checkWithExpectedErrorsAndWarnings(vb -> vb.checkBy(listValidator(null, null)), null, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkBy_HasErrorsAndWarnings_MergesErrorsAndWarnings() {
        String error1 = "error1";
        String warning1 = "warning1";
        String error2 = "error2";
        String warning2 = "warning2";

        ValidationResult<List<Object>, String> vr = new ValidationResult<>(LIST);
        vr.addError(error1);
        vr.addWarning(warning1);

        ListValidationBuilder<Object, String> vb = new ListValidationBuilder<>(vr);
        vb.checkBy(listValidator(error2, warning2));

        assertThat(vb.getResult().getErrors(), containsInAnyOrder(is(error1), is(error2)));
        assertThat(vb.getResult().getWarnings(), containsInAnyOrder(is(warning1), is(warning2)));
    }

    // checkBy with When

    @Test
    @SuppressWarnings("unchecked")
    public void checkBy_WhenIsNegative_ValidationIsNotExecuted() {
        String error = "error";
        String warning = "warning";
        checkWithExpectedErrorsAndWarnings(
                vb -> vb.checkBy(listValidator(error, warning), LIST_NEGATIVE_WHEN),
                null, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkBy_WhenIsPositive_ValidationIsExecuted() {
        String error = "error";
        String warning = "warning";
        checkWithExpectedErrorsAndWarnings(
                vb -> vb.checkBy(listValidator(error, warning), LIST_POSITIVE_WHEN),
                error, warning);
    }

    // checkEach

    @Test
    public void checkEach_ConstraintPassesOnBothItems_DoesNotAddItemsErrors() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(null, null)),
                null, null);
    }

    @Test
    public void checkEach_ConstraintFailsOnBothItems_AddItemsErrors() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT)),
                ITEM1_DEFECT, ITEM2_DEFECT);
    }

    @Test
    public void checkEach_ConstraintFailsOnItem1_AddItem1Error() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(ITEM1_DEFECT, null)),
                ITEM1_DEFECT, null);
    }

    @Test
    public void checkEach_ConstraintFailsOnItem2_AddItem2Error() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(null, ITEM2_DEFECT)),
                null, ITEM2_DEFECT);
    }

    // checkEach with overrideDefect

    @Test
    public void checkEach_ConstraintPassesOnBothItemsWithOverrideDefect_DoesNotAddItemsErrors() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(null, null), ITEM_OVERRIDE_DEFECT),
                null, null);
    }

    @Test
    public void checkEach_ConstraintFailsOnBothItemsWithOverrideDefect_AddItemsErrors() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT),
                ITEM_OVERRIDE_DEFECT, ITEM_OVERRIDE_DEFECT);
    }

    @Test
    public void checkEach_ConstraintFailsOnItem1WithOverrideDefect_AddItem1Error() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(ITEM1_DEFECT, null), ITEM_OVERRIDE_DEFECT),
                ITEM_OVERRIDE_DEFECT, null);
    }

    @Test
    public void checkEach_ConstraintFailsOnItem2WithOverrideDefect_AddItem2Error() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(null, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT),
                null, ITEM_OVERRIDE_DEFECT);
    }

    // checkEach with When

    @Test
    public void checkEach_WhenIsNegativeOnBothItems_CheckIsNotExecutedOnBothItems() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), when(false, false)),
                null, null);
    }

    @Test
    public void checkEach_WhenIsPositiveOnBothItems_CheckIsExecutedOnBothItems() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), when(true, true)),
                ITEM1_DEFECT, ITEM2_DEFECT);
    }

    @Test
    public void checkEach_WhenIsPositiveOnItem1_CheckIsExecutedOnItem1() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), when(true, false)),
                ITEM1_DEFECT, null);
    }

    @Test
    public void checkEach_WhenIsPositiveOnItem2_CheckIsExecutedOnItem2() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), when(false, true)),
                null, ITEM2_DEFECT);
    }

    // checkEach with When and overrideDefect

    @Test
    public void checkEach_WhenIsNegativeOnBothItemsWithOverrideDefect_CheckIsNotExecutedOnBothItems() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT,
                        when(false, false)),
                null, null);
    }

    @Test
    public void checkEach_WhenIsPositiveOnBothItemsWithOverrideDefect_CheckIsExecutedOnBothItems() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT, when(true, true)),
                ITEM_OVERRIDE_DEFECT, ITEM_OVERRIDE_DEFECT);
    }

    @Test
    public void checkEach_WhenIsPositiveOnItem1WithOverrideDefect_CheckIsExecutedOnItem1() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT, when(true, false)),
                ITEM_OVERRIDE_DEFECT, null);
    }

    @Test
    public void checkEach_WhenIsPositiveOnItem2WithOverrideDefect_CheckIsExecutedOnItem2() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT, when(false, true)),
                null, ITEM_OVERRIDE_DEFECT);
    }

    // checkEach for ListConstraint

    @Test
    public void checkEach_ListConstraintPassesOnBothItems_DoesNotAddItemsErrors() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(listConstraint(null, null)),
                null, null);
    }

    @Test
    public void checkEach_ListConstraintFailsOnBothItems_AddItemsErrors() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(listConstraint(ITEM1_DEFECT, ITEM2_DEFECT)),
                ITEM1_DEFECT, ITEM2_DEFECT);
    }

    @Test
    public void checkEach_ListConstraintFailsOnItem1_AddItem1Error() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(listConstraint(ITEM1_DEFECT, null)),
                ITEM1_DEFECT, null);
    }

    @Test
    public void checkEach_ListConstraintFailsOnItem2_AddItem2Error() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(listConstraint(null, ITEM2_DEFECT)),
                null, ITEM2_DEFECT);
    }

    // checkEach for ListConstraint with overrideDefect

    @Test
    public void checkEach_ListConstraintPassesOnBothItemsWithOverrideDefect_DoesNotAddItemsErrors() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(listConstraint(null, null), ITEM_OVERRIDE_DEFECT),
                null, null);
    }

    @Test
    public void checkEach_ListConstraintFailsOnBothItemsWithOverrideDefect_AddItemsErrors() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(listConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT),
                ITEM_OVERRIDE_DEFECT, ITEM_OVERRIDE_DEFECT);
    }

    @Test
    public void checkEach_ListConstraintFailsOnItem1WithOverrideDefect_AddItem1Error() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(listConstraint(ITEM1_DEFECT, null), ITEM_OVERRIDE_DEFECT),
                ITEM_OVERRIDE_DEFECT, null);
    }

    @Test
    public void checkEach_ListConstraintFailsOnItem2WithOverrideDefect_AddItem2Error() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(listConstraint(null, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT),
                null, ITEM_OVERRIDE_DEFECT);
    }

    // checkEach for ListConstraint with When

    @Test
    public void checkEach_ListConstraint_WhenIsPositiveOnBothItems_CheckIsExecutedOnBothItems() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(listConstraint(ITEM1_DEFECT, ITEM2_DEFECT), when(true, true)),
                ITEM1_DEFECT, ITEM2_DEFECT);
    }

    @Test
    public void checkEach_ListConstraint_WhenIsNegativeOnBothItems_CheckIsNotExecutedOnBothItems() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(listConstraint(ITEM1_DEFECT, ITEM2_DEFECT), when(false, false)),
                null, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkEach_ListConstraint_WhenIsNegativeOnBothItems_ConstraintCalledOnEmptyList() {
        ListConstraint<Object, String> listConstraintMock = mock(ListConstraint.class);
        Mockito.when(listConstraintMock.apply(any())).thenReturn(emptyMap());

        ListValidationBuilder<Object, String> vb = ListValidationBuilder.of(LIST);
        vb.checkEach(listConstraintMock, when(false, false));

        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        verify(listConstraintMock).apply(argument.capture());

        List<?> value = argument.getValue();
        assertThat((value), emptyIterable());
    }

    // checkEach for ListConstraint with overrideDefect and When

    @Test
    public void checkEach_ListConstraint_WhenIsPositiveOnBothItemsWithOverrideDefect_CheckIsExecutedOnBothItems() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(listConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT, when(true, true)),
                ITEM_OVERRIDE_DEFECT, ITEM_OVERRIDE_DEFECT);
    }

    @Test
    public void checkEach_ListConstraint_WhenIsNegativeOnBothItemsWithOverrideDefect_CheckIsNotExecutedOnBothItems() {
        checkWithExpectedItemsErrors(
                vb -> vb.checkEach(listConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT,
                        when(false, false)),
                null, null);
    }

    // weakCheckEach

    @Test
    public void weakCheckEach_ConstraintPassesOnBothItems_DoesNotAddItemsErrors() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(itemConstraint(null, null)),
                null, null);
    }

    @Test
    public void weakCheckEach_ConstraintFailsOnBothItems_AddItemsErrors() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT)),
                ITEM1_DEFECT, ITEM2_DEFECT);
    }

    @Test
    public void weakCheckEach_ConstraintFailsOnItem1_AddItem1Error() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(itemConstraint(ITEM1_DEFECT, null)),
                ITEM1_DEFECT, null);
    }

    @Test
    public void weakCheckEach_ConstraintFailsOnItem2_AddItem2Error() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(itemConstraint(null, ITEM2_DEFECT)),
                null, ITEM2_DEFECT);
    }

    // weakCheckEach with overrideDefect

    @Test
    public void weakCheckEach_ConstraintPassesOnBothItemsWithOverrideDefect_DoesNotAddItemsErrors() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(itemConstraint(null, null), ITEM_OVERRIDE_DEFECT),
                null, null);
    }

    @Test
    public void weakCheckEach_ConstraintFailsOnBothItemsWithOverrideDefect_AddItemsErrors() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT),
                ITEM_OVERRIDE_DEFECT, ITEM_OVERRIDE_DEFECT);
    }

    @Test
    public void weakCheckEach_ConstraintFailsOnItem1WithOverrideDefect_AddItem1Error() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(itemConstraint(ITEM1_DEFECT, null), ITEM_OVERRIDE_DEFECT),
                ITEM_OVERRIDE_DEFECT, null);
    }

    @Test
    public void weakCheckEach_ConstraintFailsOnItem2WithOverrideDefect_AddItem2Error() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(itemConstraint(null, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT),
                null, ITEM_OVERRIDE_DEFECT);
    }

    // weakCheckEach with When

    @Test
    public void weakCheckEach_WhenIsNegativeOnBothItems_CheckIsNotExecutedOnBothItems() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), when(false, false)),
                null, null);
    }

    @Test
    public void weakCheckEach_WhenIsPositiveOnBothItems_CheckIsExecutedOnBothItems() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), when(true, true)),
                ITEM1_DEFECT, ITEM2_DEFECT);
    }

    @Test
    public void weakCheckEach_WhenIsPositiveOnItem1_CheckIsExecutedOnItem1() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), when(true, false)),
                ITEM1_DEFECT, null);
    }

    @Test
    public void weakCheckEach_WhenIsPositiveOnItem2_CheckIsExecutedOnItem2() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), when(false, true)),
                null, ITEM2_DEFECT);
    }

    // weakCheckEach with When and overrideDefect

    @Test
    public void weakCheckEach_WhenIsNegativeOnBothItemsWithOverrideDefect_CheckIsNotExecutedOnBothItems() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(
                        itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT, when(false, false)),
                null, null);
    }

    @Test
    public void weakCheckEach_WhenIsPositiveOnBothItemsWithOverrideDefect_CheckIsExecutedOnBothItems() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(
                        itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT, when(true, true)),
                ITEM_OVERRIDE_DEFECT, ITEM_OVERRIDE_DEFECT);
    }

    @Test
    public void weakCheckEach_WhenIsPositiveOnItem1WithOverrideDefect_CheckIsExecutedOnItem1() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(
                        itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT, when(true, false)),
                ITEM_OVERRIDE_DEFECT, null);
    }

    @Test
    public void weakCheckEach_WhenIsPositiveOnItem2WithOverrideDefect_CheckIsExecutedOnItem2() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(
                        itemConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT, when(false, true)),
                null, ITEM_OVERRIDE_DEFECT);
    }

    // weakCheckEach for ListConstraint

    @Test
    public void weakCheckEach_ListConstraintPassesOnBothItems_DoesNotAddItemsErrors() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(listConstraint(null, null)),
                null, null);
    }

    @Test
    public void weakCheckEach_ListConstraintFailsOnBothItems_AddItemsErrors() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(listConstraint(ITEM1_DEFECT, ITEM2_DEFECT)),
                ITEM1_DEFECT, ITEM2_DEFECT);
    }

    @Test
    public void weakCheckEach_ListConstraintFailsOnItem1_AddItem1Error() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(listConstraint(ITEM1_DEFECT, null)),
                ITEM1_DEFECT, null);
    }

    @Test
    public void weakCheckEach_ListConstraintFailsOnItem2_AddItem2Error() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(listConstraint(null, ITEM2_DEFECT)),
                null, ITEM2_DEFECT);
    }

    // weakCheckEach for ListConstraint with overrideDefect

    @Test
    public void weakCheckEach_ListConstraintPassesOnBothItemsWithOverrideDefect_DoesNotAddItemsErrors() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(listConstraint(null, null), ITEM_OVERRIDE_DEFECT),
                null, null);
    }

    @Test
    public void weakCheckEach_ListConstraintFailsOnBothItemsWithOverrideDefect_AddItemsErrors() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(listConstraint(ITEM1_DEFECT, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT),
                ITEM_OVERRIDE_DEFECT, ITEM_OVERRIDE_DEFECT);
    }

    @Test
    public void weakCheckEach_ListConstraintFailsOnItem1WithOverrideDefect_AddItem1Error() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(listConstraint(ITEM1_DEFECT, null), ITEM_OVERRIDE_DEFECT),
                ITEM_OVERRIDE_DEFECT, null);
    }

    @Test
    public void weakCheckEach_ListConstraintFailsOnItem2WithOverrideDefect_AddItem2Error() {
        checkWithExpectedItemsWarnings(
                vb -> vb.weakCheckEach(listConstraint(null, ITEM2_DEFECT), ITEM_OVERRIDE_DEFECT),
                null, ITEM_OVERRIDE_DEFECT);
    }

    // checkEachBy

    @Test
    public void checkEachBy_BothItemsHasErrorsAndWarnings_AddsErrorsAndWarningsForBothItems() {
        String item1Error = "item1 error";
        String item2Error = "item2 error";
        String item1Warning = "item1 warning";
        String item2Warning = "item2 warning";
        checkWithExpectedItemsErrorsAndWarnings(
                vb -> vb.checkEachBy(itemValidator(item1Error, item1Warning, item2Error, item2Warning)),
                item1Error, item1Warning, item2Error, item2Warning);
    }

    @Test
    public void checkEachBy_Item1HasErrorsAndWarnings_AddsErrorsAndWarningsForItem1() {
        String item1Error = "item1 error";
        String item1Warning = "item1 warning";
        checkWithExpectedItemsErrorsAndWarnings(
                vb -> vb.checkEachBy(itemValidator(item1Error, item1Warning, null, null)),
                item1Error, item1Warning, null, null);
    }

    // checkEachBy with when

    @Test
    public void checkEachBy_WhenIsNegativeForBothItems_DoesNotAddErrorsAndWarnings() {
        String item1Error = "item1 error";
        String item2Error = "item2 error";
        String item1Warning = "item1 warning";
        String item2Warning = "item2 warning";
        checkWithExpectedItemsErrorsAndWarnings(
                vb -> vb.checkEachBy(itemValidator(item1Error, item1Warning, item2Error, item2Warning),
                        when(false, false)),
                null, null, null, null);
    }

    @Test
    public void checkEachBy_WhenIsPositiveForItem1_AddsErrorsAndWarningsForItem1() {
        String item1Error = "item1 error";
        String item2Error = "item2 error";
        String item1Warning = "item1 warning";
        String item2Warning = "item2 warning";
        checkWithExpectedItemsErrorsAndWarnings(
                vb -> vb.checkEachBy(itemValidator(item1Error, item1Warning, item2Error, item2Warning),
                        when(true, false)),
                item1Error, item1Warning, null, null);
    }

    @Test
    public void checkEachBy_WhenIsPositiveForBothItems_AddsErrorsAndWarningsForBothItems() {
        String item1Error = "item1 error";
        String item2Error = "item2 error";
        String item1Warning = "item1 warning";
        String item2Warning = "item2 warning";
        checkWithExpectedItemsErrorsAndWarnings(
                vb -> vb.checkEachBy(itemValidator(item1Error, item1Warning, item2Error, item2Warning),
                        when(true, true)),
                item1Error, item1Warning, item2Error, item2Warning);
    }

    // checkSublistBy

    @Test
    public void checkSublistBy_WhenIsNegativeForAllItems_DoesNotAddAnyDefects() {

        // sublistValidator повесит дефекты на всё, что к нему попадёт
        Map<Integer, Pair<String, String>> sublistDefects = new HashMap<>();
        for (int i = 0; i < 4; i++) {
            sublistDefects.put(i, Pair.of("error", "warning"));
        }

        // ничто попасть не должно
        When<Object, String> whenResults = when(false, false, false, false);

        checkWithExpectedItemsErrorsAndWarnings(
                vb -> vb.checkSublistBy(sublistValidator(sublistDefects), whenResults),
                new HashMap<>());   // не ожидаем ошибок
    }

    @Test
    public void checkSublistBy_WhenIsPositiveForTwoOfFourItems_AddsErrorsAndWarningsForThem() {
        String item2Error = "item2 error";
        String item2Warning = null;
        String item4Error = null;
        String item4Warning = "item4 warning";

        // sublist формируется так
        When<Object, String> whenResults = when(false, true, false, true);

        // такие дефекты повесит sublistValidator на попавший ему sublist
        Map<Integer, Pair<String, String>> sublistDefects = new HashMap<>();
        sublistDefects.put(0, Pair.of(item2Error, item2Warning));
        sublistDefects.put(1, Pair.of(item4Error, item4Warning));

        // эти дефекты мы ожидаем на нодах с индексами из полного списка в результате
        Map<Integer, Pair<String, String>> listDefects = new HashMap<>();
        listDefects.put(1, sublistDefects.get(0));
        listDefects.put(3, sublistDefects.get(1));

        checkWithExpectedItemsErrorsAndWarnings(
                vb -> vb.checkSublistBy(sublistValidator(sublistDefects), whenResults),
                listDefects);
    }

    // multiple calls to checkEach collects all errors and warnings

    @Test
    @SuppressWarnings("unchecked")
    public void checkEach_MultipleCalls_CollectsAllErrorsAndWarnings() {
        String item1Error1 = "item1 error1";
        String item1Error2 = "item1 error2";
        String item2Warning1 = "item2 warning1";
        String item2Warning2 = "item2 warning2";

        ListValidationBuilder<Object, String> vb = ListValidationBuilder.of(LIST);

        vb.checkEach(itemConstraint(item1Error1, null))
                .checkEach(listConstraint(item1Error2, null))
                .weakCheckEach(itemConstraint(null, item2Warning1))
                .checkEachBy(itemValidator(null, null, null, item2Warning2));

        ValidationResult<?, String> item1Vr = vb.getResult().getSubResults().get(index(0));
        ValidationResult<?, String> item2Vr = vb.getResult().getSubResults().get(index(1));
        assertThat(item1Vr.getErrors(), containsInAnyOrder(is(item1Error1), is(item1Error2)));
        assertThat(item2Vr.getWarnings(), containsInAnyOrder(is(item2Warning1), is(item2Warning2)));
        assertThat(item1Vr.getWarnings(), emptyIterable());
        assertThat(item2Vr.getErrors(), emptyIterable());
    }


    private void checkWithNoExpectedDefects(Consumer<ListValidationBuilder<Object, String>> checkCaller) {
        checkWithExpectedErrorsAndWarnings(checkCaller, null, null);
    }

    private void checkWithExpectedError(Consumer<ListValidationBuilder<Object, String>> checkCaller,
                                        String expectedError) {
        checkWithExpectedErrorsAndWarnings(checkCaller, expectedError, null);
    }

    private void checkWithExpectedWarning(Consumer<ListValidationBuilder<Object, String>> checkCaller,
                                          String expectedWarning) {
        checkWithExpectedErrorsAndWarnings(checkCaller, null, expectedWarning);
    }

    private void checkWithExpectedErrorsAndWarnings(Consumer<ListValidationBuilder<Object, String>> checkCaller,
                                                    String expectedError, String expectedWarning) {
        ListValidationBuilder<Object, String> vb = ListValidationBuilder.of(LIST);

        checkCaller.accept(vb);

        if (expectedError != null) {
            assertThat(vb.getResult().getErrors(), contains(is(expectedError)));
        } else {
            assertThat(vb.getResult().getErrors(), emptyIterable());
        }

        if (expectedWarning != null) {
            assertThat(vb.getResult().getWarnings(), contains(is(expectedWarning)));
        } else {
            assertThat(vb.getResult().getWarnings(), emptyIterable());
        }
    }

    private void checkWithExpectedItemsErrors(
            Consumer<ListValidationBuilder<Object, String>> checkCaller,
            String item1ExpectedError, String item2ExpectedError) {
        checkWithExpectedItemsErrorsAndWarnings(checkCaller, item1ExpectedError, null, item2ExpectedError, null);
    }

    private void checkWithExpectedItemsWarnings(
            Consumer<ListValidationBuilder<Object, String>> checkCaller,
            String item1ExpectedWarning, String item2ExpectedWarning) {
        checkWithExpectedItemsErrorsAndWarnings(checkCaller, null, item1ExpectedWarning, null, item2ExpectedWarning);
    }

    private void checkWithExpectedItemsErrorsAndWarnings(
            Consumer<ListValidationBuilder<Object, String>> checkCaller,
            String item1ExpectedError, String item1ExpectedWarning,
            String item2ExpectedError, String item2ExpectedWarning) {
        Map<Integer, Pair<String, String>> expectedErrorsAndWarnings = new HashMap<>();
        expectedErrorsAndWarnings.put(0, Pair.of(item1ExpectedError, item1ExpectedWarning));
        expectedErrorsAndWarnings.put(1, Pair.of(item2ExpectedError, item2ExpectedWarning));

        checkWithExpectedItemsErrorsAndWarnings(checkCaller, expectedErrorsAndWarnings);
    }

    private void checkWithExpectedItemsErrorsAndWarnings(Consumer<ListValidationBuilder<Object, String>> checkCaller,
                                                         Map<Integer, Pair<String, String>> expectedDefects) {
        ListValidationBuilder<Object, String> vb = ListValidationBuilder.of(LIST);
        checkCaller.accept(vb);

        Map<PathNode, ValidationResult<?, String>> subResults = vb.getResult().getSubResults();

        // проверяем, что ожидаемые дефекты присутствуют
        for (Map.Entry<Integer, Pair<String, String>> expectedItemDefects : expectedDefects.entrySet()) {
            int index = expectedItemDefects.getKey();
            ValidationResult<?, String> itemResult = subResults.get(index(index));
            if (expectedItemDefects.getValue() == null) {
                if (itemResult != null) {
                    assertThat(itemResult.getErrors(), emptyIterable());
                    assertThat(itemResult.getWarnings(), emptyIterable());
                }
                continue;
            }
            String expectedItemError = expectedItemDefects.getValue().getLeft();
            String expectedItemWarning = expectedItemDefects.getValue().getRight();

            assertThat(itemResult == null ? emptyList() : itemResult.getErrors(),
                    expectedItemError == null ? emptyIterable() : contains(is(expectedItemError)));
            assertThat(itemResult == null ? emptyList() : itemResult.getWarnings(),
                    expectedItemWarning == null ? emptyIterable() : contains(is(expectedItemWarning)));
        }

        // проверяем, что в остальных нодах дефектов нет
        Set<PathNode> nodesWithDefects = expectedDefects.keySet().stream().map(PathHelper::index).collect(toSet());
        Set<PathNode> noDefectsExpected = new HashSet<>(subResults.keySet());
        noDefectsExpected.removeAll(nodesWithDefects);
        for (PathNode nodeWithNoDefectsExpected : noDefectsExpected) {
            ValidationResult<?, String> itemResult = subResults.get(nodeWithNoDefectsExpected);
            if (itemResult != null) {
                List<String> errors = itemResult.getErrors() == null ? emptyList() : itemResult.getErrors();
                List<String> warnings = itemResult.getWarnings() == null ? emptyList() : itemResult.getWarnings();
                assertThat(errors, emptyIterable());
                assertThat(warnings, emptyIterable());
            }
        }
    }

    private static Constraint<Object, String> itemConstraint(String item1Defect, String item2Defect) {
        return item -> {
            if (item == ITEM1) {
                return item1Defect;
            }
            if (item == ITEM2) {
                return item2Defect;
            }
            return null;
        };
    }

    private static ListConstraint<Object, String> listConstraint(String item1Defect, String item2Defect) {
        Constraint<Object, String> itemConstraint = itemConstraint(item1Defect, item2Defect);
        return list -> {
            Map<Integer, String> defectMap = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                String defect = itemConstraint.apply(list.get(i));
                if (defect != null) {
                    defectMap.put(i, defect);
                }
            }
            return defectMap;
        };
    }

    private static Validator<List<Object>, String> listValidator(String error, String warning) {
        return t -> {
            ValidationResult<List<Object>, String> internalVr = new ValidationResult<>(t);
            if (error != null) {
                internalVr.addError(error);
            }
            if (warning != null) {
                internalVr.addWarning(warning);
            }
            return internalVr;
        };
    }

    private static Validator<List<Object>, String> sublistValidator(
            Map<Integer, Pair<String, String>> sublistErrorsAndWarnings)    // передаются индексы сублиста
    {
        return sublist -> {
            ValidationResult<List<Object>, String> sublistVr = new ValidationResult<>(sublist);
            for (int i = 0; i < sublist.size(); i++) {
                Object item = sublist.get(i);
                Pair<String, String> errorAndWarning = sublistErrorsAndWarnings.get(i);
                ValidationResult<Object, String> itemVr = sublistVr.getOrCreateSubValidationResult(index(i), item);
                if (errorAndWarning == null) {
                    continue;
                }
                String error = errorAndWarning.getLeft();
                String warning = errorAndWarning.getRight();
                if (error != null) {
                    itemVr.addError(error);
                }
                if (warning != null) {
                    itemVr.addWarning(warning);
                }
            }
            return sublistVr;
        };
    }

    private static Validator<Object, String> itemValidator(
            String item1Error, String item1Warning,
            String item2Error, String item2Warning) {
        return t -> {
            ValidationResult<Object, String> internalVr = new ValidationResult<>(t);
            if (t == ITEM1) {
                if (item1Error != null) {
                    internalVr.addError(item1Error);
                }
                if (item1Warning != null) {
                    internalVr.addWarning(item1Warning);
                }
            }
            if (t == ITEM2) {
                if (item2Error != null) {
                    internalVr.addError(item2Error);
                }
                if (item2Warning != null) {
                    internalVr.addWarning(item2Warning);
                }
            }
            return internalVr;
        };
    }

    private static When<Object, String> when(boolean resultForItem1, boolean resultForItem2) {
        return new When<>(vr -> vr.getValue() == ITEM1 ? resultForItem1 : resultForItem2);
    }

    private static When<Object, String> when(boolean resultForItem1, boolean resultForItem2,
                                             boolean resultForItem3, boolean resultForItem4) {
        return new When<>(vr -> {
            Object value = vr.getValue();
            if (value == ITEM1) {
                return resultForItem1;

            } else if (value == ITEM2) {
                return resultForItem2;

            } else if (value == ITEM3) {
                return resultForItem3;

            } else {
                return resultForItem4;
            }
        });
    }
}
