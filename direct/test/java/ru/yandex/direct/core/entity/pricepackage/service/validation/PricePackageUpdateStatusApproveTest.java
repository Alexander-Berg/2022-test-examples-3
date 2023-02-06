package ru.yandex.direct.core.entity.pricepackage.service.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.pricepackage.model.StatusApprove.NEW;
import static ru.yandex.direct.core.entity.pricepackage.model.StatusApprove.NO;
import static ru.yandex.direct.core.entity.pricepackage.model.StatusApprove.WAITING;
import static ru.yandex.direct.core.entity.pricepackage.model.StatusApprove.YES;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class PricePackageUpdateStatusApproveTest extends PricePackageUpdateValidationTestBase {

    private static final String PRICE_MANAGER = "Price Manager";
    private static final String PRICE_APPROVER = "Price Approver";
    private static final String SUPPORT = "Support";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter(0)
    public String operatorString;

    @Parameterized.Parameter(1)
    public StatusApprove statusApproveFrom;

    @Parameterized.Parameter(2)
    public StatusApprove statusApproveTo;

    @Parameterized.Parameter(3)
    public boolean expectedAllowed;

    @Parameterized.Parameters(name = "{0}: {1} -> {2}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{PRICE_MANAGER, NEW, NEW, true},
                new Object[]{PRICE_MANAGER, NEW, WAITING, true},
                new Object[]{PRICE_MANAGER, NEW, NO, false},
                new Object[]{PRICE_MANAGER, NEW, YES, false},
                new Object[]{PRICE_MANAGER, WAITING, WAITING, true},
                new Object[]{PRICE_MANAGER, WAITING, NEW, false},
                new Object[]{PRICE_MANAGER, WAITING, NO, false},
                new Object[]{PRICE_MANAGER, WAITING, YES, false},
                new Object[]{PRICE_MANAGER, NO, NO, true},
                new Object[]{PRICE_MANAGER, NO, NEW, false},
                new Object[]{PRICE_MANAGER, NO, WAITING, true},
                new Object[]{PRICE_MANAGER, NO, YES, false},
                new Object[]{PRICE_MANAGER, YES, YES, true},
                new Object[]{PRICE_MANAGER, YES, NEW, false},
                new Object[]{PRICE_MANAGER, YES, WAITING, false},
                new Object[]{PRICE_MANAGER, YES, NO, false},

                new Object[]{PRICE_APPROVER, NEW, NEW, true},
                new Object[]{PRICE_APPROVER, NEW, WAITING, false},
                new Object[]{PRICE_APPROVER, NEW, NO, false},
                new Object[]{PRICE_APPROVER, NEW, YES, false},
                new Object[]{PRICE_APPROVER, WAITING, WAITING, true},
                new Object[]{PRICE_APPROVER, WAITING, NEW, false},
                new Object[]{PRICE_APPROVER, WAITING, NO, true},
                new Object[]{PRICE_APPROVER, WAITING, YES, true},
                new Object[]{PRICE_APPROVER, NO, NO, true},
                new Object[]{PRICE_APPROVER, NO, NEW, false},
                new Object[]{PRICE_APPROVER, NO, WAITING, false},
                new Object[]{PRICE_APPROVER, NO, YES, false},
                new Object[]{PRICE_APPROVER, YES, YES, true},
                new Object[]{PRICE_APPROVER, YES, NEW, false},
                new Object[]{PRICE_APPROVER, YES, WAITING, false},
                new Object[]{PRICE_APPROVER, YES, NO, true},

                new Object[]{SUPPORT, NEW, NEW, true},
                new Object[]{SUPPORT, NEW, WAITING, false},
                new Object[]{SUPPORT, NEW, NO, false},
                new Object[]{SUPPORT, NEW, YES, false},
                new Object[]{SUPPORT, WAITING, WAITING, true},
                new Object[]{SUPPORT, WAITING, NEW, false},
                new Object[]{SUPPORT, WAITING, NO, true},
                new Object[]{SUPPORT, WAITING, YES, true},
                new Object[]{SUPPORT, NO, NO, true},
                new Object[]{SUPPORT, NO, NEW, false},
                new Object[]{SUPPORT, NO, WAITING, false},
                new Object[]{SUPPORT, NO, YES, false},
                new Object[]{SUPPORT, YES, YES, true},
                new Object[]{SUPPORT, YES, NEW, false},
                new Object[]{SUPPORT, YES, WAITING, false},
                new Object[]{SUPPORT, YES, NO, true}
        );
    }

    @Test
    public void test() {
        User operator = null;
        if (operatorString.equals(PRICE_APPROVER)) {
            operator = priceApprover;
        } else if (operatorString.equals(PRICE_MANAGER)) {
            operator = priceManager;
        } else if (operatorString.equals(SUPPORT)) {
            operator = support;
        }
        var pricePackage = activePricePackageWithStatusApprove(statusApproveFrom);
        var changes = ModelChanges.build(pricePackage, PricePackage.STATUS_APPROVE, statusApproveTo);
        var result = validate(operator, changes, pricePackage);

        if (expectedAllowed) {
            assertNoDefects(result);
        } else {
            assertDefects(result);
        }
    }

    private void assertNoDefects(Optional<MassResult<Long>> result) {
        assertThat(result).isEmpty();
    }

    private void assertDefects(Optional<MassResult<Long>> result) {
        assertThat(result).isNotEmpty();

        var validationResult = result.get().get(0).getValidationResult();
        assertThat(validationResult).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(PricePackage.STATUS_APPROVE)), forbiddenToChange()))));
    }

}
