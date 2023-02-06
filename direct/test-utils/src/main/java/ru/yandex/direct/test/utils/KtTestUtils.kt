package ru.yandex.direct.test.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matcher

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.utils.JsonUtils

/**
 * Почему этот класс плох: <a href="http://xunitpatterns.com/Assertion%20Roulette.html">xunitpatterns<a/> </br>
 * Дока к <a href="https://assertj.github.io/doc/">assertj<a/>
 */

@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <T> T.checkEquals(expected: T?) = assertThat(this).isEqualTo(expected);
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
//https://assertj.github.io/doc/#assertj-core-recursive-comparison
fun <T> T.checkBeanDiffer(expected: T) = assertThat(this).`is`(matchedBy(beanDiffer(expected)));
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
//https://assertj.github.io/doc/#assertj-core-recursive-comparison
fun <T> T.checkBeanDiffer(expected: T, strategy: CompareStrategy) = assertThat(this).`is`(matchedBy(beanDiffer(expected).useCompareStrategy(strategy)))
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <T> List<T>.checkContains(vararg expectedItems: T) = assertThat(this).contains(*expectedItems);
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <T> List<T>.checkContainsInAnyOrder(vararg expectedItems: T) = assertThat(this).containsExactlyInAnyOrder(*expectedItems)
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <T> List<T>.checkEmpty() = assertThat(this).isEmpty();
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <T> List<T>.checkNotEmpty() = assertThat(this).isNotEmpty();
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <T> T?.checkNull() = assertThat(this).isNull();
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <T> T?.checkNotNull() = assertThat(this).isNotNull();
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <T> T.checkNotEquals(notExpected: T?) = assertThat(this).isNotEqualTo(notExpected);
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <T> T.check(matcher: Matcher<T>) = assertThat(this).`is`(matchedBy(matcher));
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <K, V> Map<K, V>.checkContainsKey(key: K) = assertThat(this).containsKey(key);
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <K, V> Map<K, V>.checkNotContainsKey(key: K) = assertThat(this).doesNotContainKey(key);
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <K, V> Map<K, V>.checkEmpty() = assertThat(this).isEmpty();
@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun <T> Collection<T>.checkSize(size: Int) = assertThat(this).hasSize(size);

@Deprecated("Нужно использовать Assertions.assertThat по месту и SoftAssertions")
fun JsonNode.checkEquals(expected: Any?, ignoredFieldNames: Collection<String> = emptyList()) {
    val expectedNode = if (expected is JsonNode) {
        expected
    } else {
        JsonUtils.MAPPER.readTree(JsonUtils.toJson(expected))
    }
    if (this is ObjectNode) {
        remove(ignoredFieldNames)
    }
    if (expectedNode is ObjectNode) {
        expectedNode.remove(ignoredFieldNames)
    }
    assertThat(this).isEqualTo(expectedNode)
}
