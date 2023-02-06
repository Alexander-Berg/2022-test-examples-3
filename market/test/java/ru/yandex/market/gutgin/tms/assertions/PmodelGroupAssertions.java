package ru.yandex.market.gutgin.tms.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import ru.yandex.market.partner.content.common.entity.goodcontent.PmodelGroup;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class PmodelGroupAssertions extends AbstractObjectAssert<PmodelGroupAssertions, PmodelGroup> {
    public PmodelGroupAssertions(PmodelGroup pmodelGroup) {
        super(pmodelGroup, PmodelGroupAssertions.class);
    }

    public PmodelGroupAssertions groupOf(long targetPModelId, long... targetPskuIds) {
        super.isNotNull();

        String expected = toString(targetPModelId, Arrays.stream(targetPskuIds).sorted().boxed().collect(Collectors.toList()));
        String actual = toString(super.actual.getTargetPModelId(), super.actual.getTargetPSkuIds().stream().sorted().collect(Collectors.toList()));

        if (!Objects.equals(expected, actual)) {
            failWithMessage("Expected group of:\n%s\nActual:\n%s\nGroup:\n%s\n",
                expected, actual, super.actual);
        }
        return myself;
    }

    private String toString(long targetPModelId, Collection<Long> targetPskuIds) {
        return "target-pmodel: " + targetPModelId +
            ", target-pskus: " + targetPskuIds.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }
}
