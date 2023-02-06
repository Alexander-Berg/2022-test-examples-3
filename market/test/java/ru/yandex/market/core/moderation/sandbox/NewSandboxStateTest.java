package ru.yandex.market.core.moderation.sandbox;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.validation.ConstraintViolation;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.core.testing.TestingType.FULL_PREMODERATION;

/**
 * @author zoom
 */
public class NewSandboxStateTest extends SandboxStateTest {

    @Test
    public void shouldHaveDefaultAttrValuesWhenNew() {
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        TestingState expected = new TestingState();
        expected.setTestingType(FULL_PREMODERATION);
        expected.setStatus(TestingStatus.INITED);
        expected.setDatasourceId(SHOP_ID);
        expected.setPushReadyButtonCount(0);
        expected.setIterationNum(1);
        assertEquals(expected, factory.getState(state));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void shouldReturn2030YearWhenStartDateIsNull() {
        SandboxState state = factory.create(SHOP_ID, FULL_PREMODERATION);
        Collection<ConstraintViolation> violations = state.resolveRequestModerationConstraintViolations();
        assertFalse(violations.isEmpty());
        assertThat(violations, Matchers.hasSize(1));
        assertThat(
                violations
                        .iterator()
                        .next()
                        .getConstraint()
                        .<StartTimeConstraint>cast()
                        .getStartDateTime(),
                equalTo(OffsetDateTime.of(2030, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant()));
    }
}