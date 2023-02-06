package ru.yandex.market.core.moderation.sandbox;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;

import com.google.common.base.MoreObjects;

import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.validation.Constraint;
import ru.yandex.market.core.validation.ConstraintViolation;

/**
 * @author zoom
 */

@XmlSeeAlso({IllegalTestingStatusConstraint.class, RequiredTestingStatusConstraint.class})
public abstract class TestingStatusConstraint implements Constraint {

    @XmlElement(name = "id")
    @XmlElementWrapper(name = "statuses")
    private Collection<TestingStatus> statuses;

    public TestingStatusConstraint(@Nonnull Collection<TestingStatus> statuses) {
        this.statuses = statuses;
    }

    @Nonnull
    public abstract Collection<ConstraintViolation> getViolations(@Nonnull TestingStatus value);

    @Nonnull
    public Collection<TestingStatus> getStatuses() {
        return statuses;
    }

    @Nonnull
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("statuses", statuses)
                .toString();
    }

}
