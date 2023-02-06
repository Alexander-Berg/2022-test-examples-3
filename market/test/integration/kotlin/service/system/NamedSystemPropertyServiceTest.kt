package ru.yandex.market.logistics.calendaring.service.system

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest

class NamedSystemPropertyServiceTest(@Autowired private val namedSystemPropertyService: NamedSystemPropertyService): AbstractContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/system/gates-schedule/before-should-use-when-null.xml"])
    fun shouldUseIndividualGatesScheduleWhenNull() {
        softly.assertThat(namedSystemPropertyService.example(172)).isTrue
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/system/gates-schedule/before-should-not-use-when-empty.xml"])
    fun shouldNotUseIndividualGatesScheduleWhenEmptyList() {
        softly.assertThat(namedSystemPropertyService.example(172)).isFalse
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/system/gates-schedule/before-should-use-when-present-in-list.xml"])
    fun shouldUseIndividualGatesScheduleWhenPresentInList() {
        softly.assertThat(namedSystemPropertyService.example(172)).isTrue
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/system/gates-schedule/before-should-not-use-when-not-present-in-list.xml"])
    fun shouldNotUseIndividualGatesScheduleWhenNotPresentInList() {
        softly.assertThat(namedSystemPropertyService.example(173)).isFalse
    }
}
