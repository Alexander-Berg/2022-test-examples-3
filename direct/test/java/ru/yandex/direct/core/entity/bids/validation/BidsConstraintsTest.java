package ru.yandex.direct.core.entity.bids.validation;

import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.bids.validation.BidsConstraints.autoBudgetPriorityIsAccepted;

public class BidsConstraintsTest {
    @Test
    public void autoBudgetPriorityIsAccepted_nullValue() {
        assertThat(autoBudgetPriorityIsAccepted().apply(null))
                .isEqualTo(null);
    }

    @Test
    public void autoBudgetPriorityIsAccepted_outOfScope() {
        assertThat(autoBudgetPriorityIsAccepted().apply(10))
                .isEqualTo(new Defect<>(BidsDefects.Ids.PRIORITY_HAS_WRONG_VALUE));
    }

    @Test
    public void autoBudgetPriorityIsAccepted_validValue() {
        assertThat(autoBudgetPriorityIsAccepted().apply(5))
                .isEqualTo(null);
    }
}
