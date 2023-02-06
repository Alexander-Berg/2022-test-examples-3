package ru.yandex.market.logistics.tarifficator.util;

import java.util.Map;
import java.util.Objects;

import org.opentest4j.AssertionFailedError;

import ru.yandex.market.logistics.tarifficator.utils.hierarchy.OwnedHierarchicalId;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@SuppressWarnings("unchecked")
public final class HierarchyTestUtil {

    private HierarchyTestUtil() {
        throw new UnsupportedOperationException();
    }

    public static <T, V> void assertEquals(
        OwnedHierarchicalId<T, V> one,
        OwnedHierarchicalId<T, V> two
    ) {
        if (one != two && (
            one == null || two == null ||
                !(Objects.equals(one.getId(), two.getId())
                    && Objects.equals(one.getParentId(), two.getParentId())
                    && Objects.equals(one.getIncludedId(), two.getIncludedId())
                    && Objects.equals(one.getChildrenIds(), two.getChildrenIds())
                )
        )) {
            throw new AssertionFailedError(null, String.valueOf(one), String.valueOf(two));
        }
    }

    public static <T, V> void assertEquals(
        Map<T, OwnedHierarchicalId<T, V>> one,
        Map<T, OwnedHierarchicalId<T, V>> two
    ) {
        if (one != two) {
            if (one == null || two == null || one.size() != two.size()) {
                throw new AssertionFailedError(null, String.valueOf(one), String.valueOf(two));
            }
        }
        try {
            for (OwnedHierarchicalId<T, V> oneId : one.values()) {
                assertEquals(oneId, two.get(oneId.getId()));
            }
        } catch (AssertionFailedError f) {
            throw new AssertionFailedError(null, String.valueOf(one), String.valueOf(two));
        }
    }

}
