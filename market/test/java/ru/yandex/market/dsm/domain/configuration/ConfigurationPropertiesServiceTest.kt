package ru.yandex.market.dsm.domain.configuration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.configuration.db.ConfigurationProperties
import ru.yandex.market.dsm.domain.configuration.db.ConfigurationPropertiesDboRepository
import ru.yandex.market.dsm.domain.configuration.model.ConfigurationName
import ru.yandex.market.dsm.domain.configuration.service.ConfigurationPropertiesService
import java.util.Objects
import java.util.UUID

class ConfigurationPropertiesServiceTest : AbstractTest()  {
    @Autowired
    private lateinit var configurationPropertiesService: ConfigurationPropertiesService
    @Autowired
    private lateinit var configurationPropertiesDboRepository: ConfigurationPropertiesDboRepository
    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @AfterEach
    fun after() {
        deleteValue()
    }

    @Test
    fun createProperty() {
        val value = "NEW VALUE"
        configurationPropertiesService.createProperty(ConfigurationName.TEST_PROPERTY, value)

        val res = configurationPropertiesDboRepository.findByKey(ConfigurationName.TEST_PROPERTY.name)
        Assertions.assertThat(res).isNotNull
        Assertions.assertThat(res!!.value).isEqualTo(value)
    }

    @Test
    fun updateProperty() {
        val newProperty = ConfigurationProperties(
            UUID.randomUUID().toString(),
            ConfigurationName.TEST_PROPERTY.name,
            Objects.toString("OLD_VALUE")
        )
        configurationPropertiesDboRepository.save(newProperty)

        val value = "NEW VALUE2"
        configurationPropertiesService.updateProperty(ConfigurationName.TEST_PROPERTY, value)

        val res = configurationPropertiesDboRepository.findByKey(ConfigurationName.TEST_PROPERTY.name)
        Assertions.assertThat(res).isNotNull
        Assertions.assertThat(res!!.value).isEqualTo(value)
    }

    @Test
    fun mergeValue() {
        val oldValue = "OLD_VALUE2"
        configurationPropertiesService.mergeValue(ConfigurationName.TEST_PROPERTY, "OLD_VALUE2")
        val resCreate = configurationPropertiesDboRepository.findByKey(ConfigurationName.TEST_PROPERTY.name)
        Assertions.assertThat(resCreate).isNotNull
        Assertions.assertThat(resCreate!!.value).isEqualTo(oldValue)

        val newValue = "NEW VALUE3"
        configurationPropertiesService.mergeValue(ConfigurationName.TEST_PROPERTY, newValue)
        val resUpdate = configurationPropertiesDboRepository.findByKey(ConfigurationName.TEST_PROPERTY.name)
        Assertions.assertThat(resUpdate).isNotNull
        Assertions.assertThat(resUpdate!!.value).isEqualTo(newValue)
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


