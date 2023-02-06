package ru.yandex.market.core.delivery.tariff.hierarchy;

import java.util.Map;
import java.util.Objects;

import org.junit.ComparisonFailure;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@SuppressWarnings("unchecked")
public final class HierarchyTestUtil {

    private HierarchyTestUtil() {
        throw new UnsupportedOperationException();
    }

    public static <T, V> void assertEquals(OwnedHierarchicalId<T, V> one, OwnedHierarchicalId<T, V> two) {
        if (one != two && (
                one == null || two == null ||
                        !(Objects.equals(one.getId(), two.getId())
                                && Objects.equals(one.getParentId(), two.getParentId())
                                && Objects.equals(one.getIncludedId(), two.getIncludedId())
                                && Objects.equals(one.getChildrenIds(), two.getChildrenIds())
                        )
        )) {
            throw new ComparisonFailure(null, String.valueOf(one), String.valueOf(two));
        }
    }

    public static <T, V> void assertEquals(Map<T, OwnedHierarchicalId<T, V>> one, Map<T, OwnedHierarchicalId<T, V>> two) {
        if (one != two) {
            if (one == null || two == null || one.size() != two.size()) {
                throw new ComparisonFailure(null, String.valueOf(one), String.valueOf(two));
            }
        }
        try {
            for (OwnedHierarchicalId<T, V> oneId : one.values()) {
                assertEquals(oneId, two.get(oneId.getId()));
            }
        } catch (ComparisonFailure f) {
            throw new ComparisonFailure(null, String.valueOf(one), String.valueOf(two));
        }
    }

}
