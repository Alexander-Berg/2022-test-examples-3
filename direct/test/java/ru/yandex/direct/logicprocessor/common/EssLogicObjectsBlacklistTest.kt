package ru.yandex.direct.logicprocessor.common

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.essblacklist.EssLogicObjectsBlacklistRepository
import ru.yandex.direct.ess.common.models.BaseLogicObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration
import java.util.Set

@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class EssLogicObjectsBlacklistTest {

    val TEST_ESS_PROCESSOR_NAME = "test_ess_processor"

    @Autowired
    private lateinit var essBlacklistRepository: EssLogicObjectsBlacklistRepository

    @Test
    fun test() {
        val filterSpec = "{\"bar\": \"baz\"}"
        val domainLogin = "loginEssTest"
        essBlacklistRepository.addItems(TEST_ESS_PROCESSOR_NAME, Set.of(filterSpec), domainLogin)
        val blackList = EssLogicObjectsBlacklist(essBlacklistRepository)
        val matchingLogicObject = TestLogicObject("baz");
        val notMatchingLogicObject = TestLogicObject("bazz");
        val notMatchingLogicObject2 = TestLogicObject2("baz");
        Assertions.assertThat(blackList.matches(TEST_ESS_PROCESSOR_NAME, matchingLogicObject))
            .isTrue
        Assertions.assertThat(blackList.matches(TEST_ESS_PROCESSOR_NAME, notMatchingLogicObject))
            .isFalse
        Assertions.assertThat(blackList.matches(TEST_ESS_PROCESSOR_NAME, notMatchingLogicObject2))
            .isFalse
    }

    private class TestLogicObject(var bar: String) : BaseLogicObject()

    private class TestLogicObject2(var bar2: String) : BaseLogicObject()

}
