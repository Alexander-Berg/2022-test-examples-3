package ru.yandex.market.mbi.feed.processor.environment

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.feed.processor.FunctionalTest

/**
 * Тесты для [EnvironmentService].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = ["EnvironmentServiceTest.before.csv"])
internal class EnvironmentServiceTest : FunctionalTest() {

    @Autowired
    private lateinit var environmentService: EnvironmentService

    @Test
    fun `get all values as strings`() {
        val actual = environmentService.getAll("env-key-list")
        Assertions.assertThat(actual)
            .containsExactlyInAnyOrder("val1", "val2")
    }

    @Test
    fun `get all values as strings for invalid key`() {
        val actual = environmentService.getAll("invalid-key")
        Assertions.assertThat(actual)
            .isEmpty()
    }

    @Test
    fun `get single value as string for invalid key`() {
        val actual = environmentService.get("invalid-key")
        Assertions.assertThat(actual)
            .isNull()
    }

    @Test
    fun `get single value as string`() {
        val actual = environmentService.get("env-key-single")
        Assertions.assertThat(actual)
            .isEqualTo("single-val")
    }

    @Test
    @DbUnitDataSet(after = ["EnvironmentServiceTest.set.new.after.csv"])
    fun `set new value for new key`() {
        environmentService.set("new-env-key", "my-new-val")
    }

    @Test
    @DbUnitDataSet(
        before = ["EnvironmentServiceTest.set.new.before.csv"],
        after = ["EnvironmentServiceTest.set.new.after.csv"]
    )
    fun `set new value for existing key`() {
        environmentService.set("new-env-key", "my-new-val")
    }

    @Test
    @DbUnitDataSet(after = ["EnvironmentServiceTest.before.csv"])
    fun `delete non-existent key`() {
        val result = environmentService.delete("new-env-key")
        Assertions.assertThat(result)
            .isFalse
    }

    @Test
    @DbUnitDataSet(
        before = ["EnvironmentServiceTest.set.new.before.csv"],
        after = ["EnvironmentServiceTest.before.csv"]
    )
    fun `delete existent key`() {
        val result = environmentService.delete("new-env-key")
        Assertions.assertThat(result)
            .isTrue
    }

    @Test
    @DbUnitDataSet(
        after = ["EnvironmentServiceTest.add.new.after.csv"]
    )
    fun `add new values`() {
        environmentService.add("env-key-list", "val3")
    }

    @Test
    fun `get all values as ints`() {
        val actual = environmentService.getAllIntegers("int-env")
        Assertions.assertThat(actual)
            .containsExactlyInAnyOrder(123)
    }

    @Test
    fun `get all values as ints for invalid key`() {
        val actual = environmentService.getAllIntegers("invalid-key")
        Assertions.assertThat(actual)
            .isEmpty()
    }

    @Test
    fun `get single value as int for invalid key`() {
        val actual = environmentService.getInteger("invalid-key")
        Assertions.assertThat(actual)
            .isNull()
    }

    @Test
    fun `get single value as int`() {
        val actual = environmentService.getInteger("int-env")
        Assertions.assertThat(actual)
            .isEqualTo(123)
    }

    @Test
    fun `get all values as longs`() {
        val actual = environmentService.getAllLongs("int-env")
        Assertions.assertThat(actual)
            .containsExactlyInAnyOrder(123L)
    }

    @Test
    fun `get all values as longs for invalid key`() {
        val actual = environmentService.getAllLongs("invalid-key")
        Assertions.assertThat(actual)
            .isEmpty()
    }

    @Test
    fun `get single value as long for invalid key`() {
        val actual = environmentService.getLong("invalid-key")
        Assertions.assertThat(actual)
            .isNull()
    }

    @Test
    fun `get single value as long`() {
        val actual = environmentService.getLong("int-env")
        Assertions.assertThat(actual)
            .isEqualTo(123L)
    }

    @Test
    fun `get single value as boolean for invalid key`() {
        val actual = environmentService.getBoolean("invalid-key")
        Assertions.assertThat(actual)
            .isNull()
    }

    @Test
    fun `get single value as boolean`() {
        val actual = environmentService.getBoolean("bool-env")
        Assertions.assertThat(actual)
            .isEqualTo(true)
    }
}
