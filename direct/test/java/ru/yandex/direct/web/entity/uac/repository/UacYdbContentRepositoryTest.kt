package ru.yandex.direct.web.entity.uac.repository

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacYdbContentRepositoryTest : AbstractUacRepositoryTest() {

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    private lateinit var content: UacYdbContent

    @Before
    fun before() {
        content = createDefaultImageContent()
        uacYdbContentRepository.saveContents(listOf(content))
    }

    @Test
    fun testGetContentsNonExisting() {
        val nonExistentId = randomPositiveLong().toIdString()
        assertThat(uacYdbContentRepository.getContents(listOf(nonExistentId))).isEmpty()
    }

    @Test
    fun testGetContents() {
        val actualContents = uacYdbContentRepository.getContents(listOf(content.id))
        assertThat(actualContents).containsExactly(content)
    }

    @Test
    fun testGetContentsBulk() {
        val content1 = createDefaultImageContent()
        val content2 = createDefaultImageContent()
        uacYdbContentRepository.saveContents(listOf(content1, content2))

        val actual = uacYdbContentRepository.getContents(listOf(content1.id, content2.id))

        assertThat(actual).containsExactlyInAnyOrder(content1, content2)
    }

    @Test
    @TestCaseName("testSaveContents({0})")
    @Parameters(source = UacIdsProvider::class)
    fun testSaveContent(caseName: String, id: String) {
        val content = createDefaultImageContent(id = id)

        uacYdbContentRepository.saveContents(listOf(content))

        val actualContents = uacYdbContentRepository.getContents(listOf(content.id))
        assertThat(actualContents).containsExactly(content)
    }
}
