package ru.yandex.direct.core.entity.lal

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.testing.configuration.CoreTest

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class CaLalSegmentRepositoryTest {

    @Autowired
    private lateinit var caLalSegmentRepository: CaLalSegmentRepository

    @Test
    fun shouldCreateNewLalSegment() {
        val goal = Goal()
            .withCryptaParentRule("{\"host\": \"yandex.ru\"}")
            .withCaText("yandex.ru") as Goal
        caLalSegmentRepository.createHostSegments(listOf(goal))
        val lalsInDb = caLalSegmentRepository.findAllByHosts(listOf("yandex.ru"))

        assertThat(lalsInDb)
            .isNotNull
            .hasSize(1)
            .hasEntrySatisfying("yandex.ru") {
                assertThat(it).isEqualTo(goal)
            }
    }
}
