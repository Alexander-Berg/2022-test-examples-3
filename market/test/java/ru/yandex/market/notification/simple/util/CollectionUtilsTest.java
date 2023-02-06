package ru.yandex.market.notification.simple.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static ru.yandex.market.notification.simple.util.CollectionUtils.findFirst;
import static ru.yandex.market.notification.simple.util.CollectionUtils.toArray;
import static ru.yandex.market.notification.simple.util.CollectionUtils.unmodifiableCollection;
import static ru.yandex.market.notification.simple.util.CollectionUtils.unmodifiableList;
import static ru.yandex.market.notification.simple.util.CollectionUtils.unmodifiableMap;
import static ru.yandex.market.notification.simple.util.CollectionUtils.unmodifiableSet;

/**
 * Unit-тесты для {@link CollectionUtils}.
 *
 * @author Vladislav Bauer
 */
public class CollectionUtilsTest {

    @Test
    public void testConstructor() {
        ClassUtils.checkConstructor(CollectionUtils.class);
    }

    @Test
    public void testFindFirst() {
        assertThat(findFirst(Collections.emptySet(), Object.class).isPresent(), equalTo(false));

        final String obj = "";
        assertThat(findFirst(Collections.singleton(obj), String.class).orElse(null), equalTo(obj));
        assertThat(findFirst(Collections.singleton(obj), Number.class).isPresent(), equalTo(false));
    }

    @Test
    public void testUnmodifiableCollectionPositive() {
        assertThat(unmodifiableCollection((Collection<?>) null), empty());
        assertThat(unmodifiableCollection(Collections.emptySet()), empty());
        assertThat(unmodifiableCollection(Collections.singleton(new Object())), hasSize(1));
    }

    @Test
    public void testUnmodifiableSetPositive() {
        assertThat(unmodifiableSet((Set<?>) null), empty());
        assertThat(unmodifiableSet(Collections.emptySet()), empty());
        assertThat(unmodifiableSet(Collections.singleton(new Object())), hasSize(1));
    }

    @Test
    public void testUnmodifiableListPositive() {
        assertThat(unmodifiableList((List<?>) null), empty());
        assertThat(unmodifiableList(Collections.emptyList()), empty());
        assertThat(unmodifiableList(Collections.singletonList(new Object())), hasSize(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnmodifiableCollectionNegative() {
        final Collection<Object> collection = unmodifiableCollection(new ArrayList<>());
        fail(String.valueOf(collection.add(new Object())));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnmodifiableSetNegative() {
        final Collection<Object> collection = unmodifiableSet(new HashSet<>());
        fail(String.valueOf(collection.add(new Object())));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnmodifiableListNegative() {
        final Collection<Object> collection = unmodifiableList(new ArrayList<>());
        fail(String.valueOf(collection.add(new Object())));
    }

    @Test
    public void testUnmodifiableMapPositive() {
        assertThat(unmodifiableMap((Map<?, ?>) null).size(), equalTo(0));
        assertThat(unmodifiableMap(Collections.emptyMap()).size(), equalTo(0));
        assertThat(unmodifiableMap(Collections.singletonMap(new Object(), new Object())).size(), equalTo(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnmodifiableMapNegative() {
        final Map<Object, Object> map = unmodifiableMap(Collections.emptyMap());
        fail(String.valueOf(map.put(new Object(), new Object())));
    }

    @Test
    public void testToArray() {
        Assert.assertThat(toArray(null).length, equalTo(0));
        Assert.assertThat(toArray(Collections.emptyList()).length, equalTo(0));
        Assert.assertThat(toArray(Collections.singleton(5)).length, equalTo(1));
    }

}
