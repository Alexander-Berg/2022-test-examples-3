package ru.yandex.market.dsm.domain.configuration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.configuration.db.ConfigurationProperties
import ru.yandex.market.dsm.domain.configuration.db.ConfigurationPropertiesDboRepository
import ru.yandex.market.dsm.domain.configuration.model.ConfigurationName
import ru.yandex.market.dsm.domain.configuration.service.ConfigurationPropertiesQueryService
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

class ConfigurationPropertiesQueryServiceTest : AbstractTest() {
    @Autowired
    private lateinit var configurationPropertiesDboRepository: ConfigurationPropertiesDboRepository

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var configurationPropertiesQueryService: ConfigurationPropertiesQueryService

    @AfterEach
    fun after() {
        deleteValue()
    }

    @Test
    fun getValue() {
        //null value
        val nullResult = configurationPropertiesQueryService.getValue(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(nullResult).isNull()

        //real value
        val testValue = "TEST"
        putValue(testValue)
        val realValue = configurationPropertiesQueryService.getValue(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(realValue).isEqualTo(testValue)

    }

    @Test
    fun getValueAsLocalDateTime() {
        //null value
        val nullResult = configurationPropertiesQueryService.getValueAsLocalDateTime(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(nullResult).isNull()

        //real value
        val testValue = LocalDateTime.now()
        putValue(testValue.toString())
        val realValue = configurationPropertiesQueryService.getValueAsLocalDateTime(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(realValue).isEqualTo(testValue)

        //bad value
        val testBadValue = "201A"
        putValue(testBadValue)
        assertThrows<DateTimeParseException> {
            configurationPropertiesQueryService.getValueAsLocalDateTime(ConfigurationName.TEST_PROPERTY)
        }
    }

    @Test
    fun isBooleanEnabled() {
        //null value
        val nullResult = configurationPropertiesQueryService.isBooleanEnabled(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(nullResult).isEqualTo(false)

        //real value
        val testValue = "true"
        putValue(testValue)
        val realValue = configurationPropertiesQueryService.isBooleanEnabled(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(realValue).isEqualTo(true)

        //bad value
        val testBadValue = "tru"
        putValue(testBadValue)
        val badValue = configurationPropertiesQueryService.isBooleanEnabled(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(badValue).isEqualTo(false)

    }

    @Test
    fun getValueAsInteger() {
        //null value
        val nullResult = configurationPropertiesQueryService.getValueAsInteger(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(nullResult).isNull()

        //real value
        val testValue = "278945345"
        putValue(testValue)
        val realValue = configurationPropertiesQueryService.getValueAsInteger(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(realValue).isEqualTo(278945345)

        //bad value
        val testBadValue = "number"
        putValue(testBadValue)
        assertThrows<NumberFormatException> {
            configurationPropertiesQueryService.getValueAsInteger(ConfigurationName.TEST_PROPERTY)
        }

    }

    @Test
    fun getValueAsIntegers() {
        //null value
        val nullResult = configurationPropertiesQueryService.getValueAsIntegers(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(nullResult).isEmpty()

        //real value
        val testValue = "3453,3443,   57567"
        putValue(testValue)
        val realValue = configurationPropertiesQueryService.getValueAsIntegers(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(realValue).containsExactlyInAnyOrder(3453, 3443, 57567)

        //bad value
        val testBadValue = "number"
        putValue(testBadValue)
        assertThrows<NumberFormatException> {
            configurationPropertiesQueryService.getValueAsIntegers(ConfigurationName.TEST_PROPERTY)
        }

    }

    @Test
    fun getValueAsLong() {
        //null value
        val nullResult = configurationPropertiesQueryService.getValueAsLong(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(nullResult).isNull()

        //real value
        val testValue = "278945345"
        putValue(testValue)
        val realValue = configurationPropertiesQueryService.getValueAsLong(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(realValue).isEqualTo(278945345L)

        //bad value
        val testBadValue = "number"
        putValue(testBadValue)
        assertThrows<NumberFormatException> {
            configurationPropertiesQueryService.getValueAsLong(ConfigurationName.TEST_PROPERTY)
        }

    }

    @Test
    fun getValueAsLongs() {
        //null value
        val nullResult = configurationPropertiesQueryService.getValueAsLongs(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(nullResult).isEmpty()

        //real value
        val testValue = "3453,  3443,57567"
        putValue(testValue)
        val realValue = configurationPropertiesQueryService.getValueAsLongs(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(realValue).containsExactlyInAnyOrder(3453L, 3443L, 57567L)

        //bad value
        val testBadValue = "number"
        putValue(testBadValue)
        assertThrows<NumberFormatException> {
            configurationPropertiesQueryService.getValueAsLongs(ConfigurationName.TEST_PROPERTY)
        }

    }

    @Test
    fun getValueAsStrings() {
        //null value
        val nullResult = configurationPropertiesQueryService.getValueAsStrings(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(nullResult).isEmpty()

        //real value
        val testValue = "blue,   green,black"
        putValue(testValue)
        val realValue = configurationPropertiesQueryService.getValueAsStrings(ConfigurationName.TEST_PROPERTY)
        Assertions.assertThat(realValue).containsExactlyInAnyOrder("blue", "green", "black")

    }

    private fun putValue(value: String) {
        transactionTemplate.execute {
            val property = configurationPropertiesDboRepository.findByKey(ConfigurationName.TEST_PROPERTY.name)
            if (property == null) {
                val newProperty = ConfigurationProperties(
                    UUID.randomUUID().toString(),
                    ConfigurationName.TEST_PROPERTY.name,
                    value
                )
                configurationPropertiesDboRepository.save(newProperty)
            } else {
                property.value = value
                configurationPropertiesDboRepository.save(property)
            }
        }
    }

    private fun deleteValue() {
        transactionTemplate.execute {
            val property = configurationPropertiesDboRepository.findByKey(ConfigurationName.TEST_PROPERTY.name)
            if (property != null) {
                configurationPropertiesDboRepository.delete(property)
            }
        }
    }
}

