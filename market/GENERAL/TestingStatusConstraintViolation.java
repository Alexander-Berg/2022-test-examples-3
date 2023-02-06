package ru.yandex.market.core.moderation.sandbox;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.validation.Constraint;
import ru.yandex.market.core.validation.ConstraintViolation;

/**
 * Изменения не не могут быть применены из-за того, что статус магазина в тестинге не соответствует этим изменениям.
 *
 * @author zoom
 */
@XmlType(name = "sandboxStateStatus")
public class TestingStatusConstraintViolation implements ConstraintViolation {

    @XmlElement(name = "status")
    private TestingStatus status;

    private TestingStatusConstraint constraint;

    public TestingStatusConstraintViolation() {
    }

    public TestingStatusConstraintViolation(TestingStatus status, TestingStatusConstraint constraint) {
        this.constraint = constraint;
        this.status = status;
    }

    @Override
    public String getMessage() {
        return "Shop's testing status is illegal for requested action. Status: " + status.getId();
    }

    @Override
    public Constraint getConstraint() {
        return constraint;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
