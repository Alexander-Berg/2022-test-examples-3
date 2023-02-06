package ru.yandex.market.clab.ui.service.model.merge;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AssertFactory;
import org.assertj.core.api.Condition;

import ru.yandex.market.clab.common.merge.Change;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 12.04.2019
 */
public class ChangeAssertion<T> extends AbstractObjectAssert<ChangeAssertion<T>, Change<T>> {

    private final Change<T> change;

    public ChangeAssertion(Change<T> change) {
        super(change, ChangeAssertion.class);
        this.change = change;
    }

    public static <T> ChangeAssertion<T> assertSingle(Condition<Change<T>> condition, List<Change<T>> list) {
        return assertThat(list.stream().filter(condition::matches).collect(Collectors.toList()), ChangeAssertion::new)
            .hasSize(1)
            .first();
    }

    public AbstractObjectAssert<?, T> withBefore() {
        assertThat(change.getBefore()).withFailMessage("Expecting before not to be null").isNotNull();
        return assertThat(change.getBefore());
    }

    public <ASSERT> ASSERT withBefore(AssertFactory<T, ? extends ASSERT> factory) {
        assertThat(change.getBefore()).withFailMessage("Expecting before not to be null").isNotNull();
        return factory.createAssert(change.getBefore());
    }

    public AbstractObjectAssert<?, T> withAfter() {
        assertThat(change.getAfter()).withFailMessage("Expecting after not to be null").isNotNull();
        return assertThat(change.getAfter());
    }

    public <ASSERT> ASSERT withAfter(AssertFactory<T, ? extends ASSERT> factory) {
        assertThat(change.getAfter()).withFailMessage("Expecting after not to be null").isNotNull();
        return factory.createAssert(change.getAfter());
    }

    public static <T> Condition<Change<T>> adding() {
        return new Condition<>(Change::isAdd, "adding value");
    }

    public static <T> Condition<Change<T>> removing() {
        return new Condition<>(Change::isRemove, "removing value");
    }

    public static <T> Condition<Change<T>> updating() {
        return new Condition<>(Change::isUpdate, "change value");
    }
}
