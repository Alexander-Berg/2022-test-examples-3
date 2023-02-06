package ru.yandex.vendor.util;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Sets.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.UUID.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static ru.yandex.vendor.util.Utils.*;

@RunWith(JUnit4.class)
public class UtilsTest {

    @Rule
    public final ExpectedException expectedExceptionRule = ExpectedException.none();
    
    @Test
    public void testRemoveByPredicate_simple() throws Exception {
        List<String> list = newArrayList("qwe", "QWE", "qaz");
        Utils.removeByPredicate(list, "qwe"::equals);
        assertEquals(asList("QWE", "qaz"), list);
    }

    @Test
    public void testRemoveByPredicate_ignoreCase() throws Exception {
        List<String> list = newArrayList("qwe", "QWE", "qaz");
        Utils.removeByPredicate(list, "qwe"::equalsIgnoreCase);
        assertEquals(singletonList("qaz"), list);
    }

    @Test
    public void testRemoveByPredicate_pattern() throws Exception {
        List<String> list = newArrayList("qwe", "QWE", "qaz");
        Utils.removeByPredicate(list, s -> s.matches("(?i)q.*"));
        assertEquals(emptyList(), list);
    }

    @Test
    public void test_uuidToBytes_length() throws Exception {
        for (int i = 0; i < 100; i++) {
            assertEquals(16, uuidToBytes(randomUUID()).length);
        }
    }

    @Test
    public void test_uuidToBytes_symmetry() throws Exception {
        for (int i = 0; i < 100; i++) {
            UUID uuid = randomUUID();
            assertEquals(uuid, bytesToUUID(uuidToBytes(uuid)));
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_uuidToBytes_NPE() throws Exception {
        uuidToBytes(null);
    }

    @Test
    public void test_bytesToUUID_length_assertion() throws Exception {
        for (byte[] bytes : Arrays.asList(new byte[0], new byte[15], new byte[17], new byte[42])) {
            try {
                bytesToUUID(bytes);
                Assert.fail("Length error expected for bytes array of length: " + bytes.length);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
    }

    @Test
    public void test_entryToMap() {
        Map<Integer, String> map = Stream.of(1, 2, 3, 4).map(x -> entry(x, String.valueOf(x))).collect(entryToMap());
        Map<Integer, String> expected = map(entry(1,"1"),entry(2,"2"),entry(3,"3"),entry(4,"4"));
        assertEquals(map, expected);
    }

    @Test
    public void toSetAssertive_collects_HashSet() throws Exception {
        Set<Integer> set = Stream.of(1, 2, 3, 4).collect(toSetAssertive("qwe"));
        assertEquals(newHashSet(1,2,3,4), set);
        assertThat(set, instanceOf(HashSet.class));
    }

    @Test
    public void toSetAssertive_throws_exception_on_duplicate_values() throws Exception {
        String message = "some message";
        expectedExceptionRule.expect(IllegalArgumentException.class);
        expectedExceptionRule.expectMessage(message);
        Stream.of(1, 2, 3, 4, 3).collect(toSetAssertive(message));
    }

    @Test
    public void toSetAssertive_allows_to_create_message_dynamically() throws Exception {
        expectedExceptionRule.expect(IllegalArgumentException.class);
        expectedExceptionRule.expectMessage("qweqwe: 42");
        Stream.of(1, 2, 3, 42, 42).collect(toSetAssertive(x -> "qweqwe: " + x));
    }

    @Test
    public void toSetAssertive_allows_to_create_any_set() throws Exception {
        Set<Integer> set = Stream.of(1, 2, 3, 4).collect(toSetAssertive(LinkedHashSet::new, x -> ""));
        assertThat(set, instanceOf(LinkedHashSet.class));
        assertEquals(newHashSet(1,2,3,4), set);
    }

    @Test
    public void testGetUriName() {
        assertThat(getUriName(null), is(nullValue()));
        assertThat(getUriName(""), is(""));
        assertThat(getUriName("qwe"), is("qwe"));
        assertThat(getUriName("qwe/qwe"), is("qwe"));
        assertThat(getUriName("qwe/rty/qaz"), is("qaz"));
        assertThat(getUriName("//qwe/rty/qaz"), is("qaz"));
        assertThat(getUriName("http://qwe/rty/qaz"), is("qaz"));
        assertThat(getUriName("https://qwe/rty/qaz"), is("qaz"));
        assertThat(getUriName("https://qwe/rty/qaz?some=42"), is("qaz"));
        assertThat(getUriName("/pop?"), is("pop"));
        assertThat(getUriName("pop?"), is("pop"));
        assertThat(getUriName("/pop"), is("pop"));
        assertThat(getUriName("/qwe?/pop"), is("qwe"));
        assertThat(getUriName("qwe?/pop"), is("qwe"));
        assertThat(getUriName("?/pop"), is(""));
        assertThat(getUriName("/pop/"), is(""));
        assertThat(getUriName("/pop/?"), is(""));
        assertThat(getUriName("/pop/?qwe"), is(""));
    }

    @Test
    public void testAllPredicatesSingle() {
        Predicate<String> p1 = mockPredicate();
        // all of singleton collection is the same as element
        assertSame(all(singleton(p1)), p1);
    }

    @Test
    public void testAllPredicatesOfEmptyIsAlwaysTrue() {
        Predicate<Object> p = all(emptySet());
        assertTrue(p.test(null));
        assertTrue(p.test(42));
        assertTrue(p.test(42L));
        assertTrue(p.test(""));
        assertTrue(p.test("qwe"));
        assertTrue(p.test(true));
        assertTrue(p.test(false));
        assertTrue(p.test(3.5));
        assertTrue(p.test(new UtilsTest()));
    }

    @Test
    public void testAllPredicatesCallsEach() {
        Predicate<String> p1 = mockPredicate();
        Predicate<String> p2 = mockPredicate();
        Predicate<String> p3 = mockPredicate();
        // when
        all(asList(p1,p2,p3)).test("qwe");
        // then
        verifyOnce(p1).test("qwe");
        verifyOnce(p2).test("qwe");
        verifyOnce(p3).test("qwe");
    }

    @Test
    public void testAllPredicatesNotCalledAfterFirstFalse() {
        Predicate<String> p1 = mockPredicate();
        Predicate<String> p2 = mockPredicate();
        Predicate<String> p3 = mockPredicate();
        Predicate<String> p4 = mockPredicate(false);
        // when
        all(asList(p1,p4,p2,p3)).test("qwe");
        // then
        verifyOnce(p1).test("qwe");
        verifyOnce(p4).test("qwe");
        verifyZero(p2).test(any());
        verifyZero(p3).test(any());
    }

    @Test
    public void testAllPredicatesResult() {
        Predicate<String> p1 = mockPredicate();
        Predicate<String> p2 = mockPredicate();
        Predicate<String> p3 = mockPredicate();
        Predicate<String> p4 = mockPredicate(false);
        // when
        boolean res1 = all(asList(p1,p2,p3)).test("qwe");
        boolean res2 = all(asList(p1,p2,p3,p4)).test("qwe");
        // then
        assertTrue(res1);
        assertFalse(res2);
    }

    private static <T> Predicate<T> mockPredicate() {
        return mockPredicate(true);
    }

    @SuppressWarnings("unchecked")
    private static <T> Predicate<T> mockPredicate(boolean returnValue) {
        Predicate<T> p = mock(Predicate.class);
        when(p.test(any())).thenReturn(returnValue);
        return p;
    }

    private static <T> T verifyOnce(T t) {
        return verify(t, times(1));
    }

    private static <T> T verifyZero(T t) {
        return verify(t, times(0));
    }
}
