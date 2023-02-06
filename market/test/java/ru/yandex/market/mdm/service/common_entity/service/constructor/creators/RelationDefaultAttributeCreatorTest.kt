package ru.yandex.market.mdm.service.common_entity.service.constructor.creators

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.show.show
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.neverNullMatcher
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.mdm.fixtures.mdmRelationType
import ru.yandex.market.mdm.lib.model.mdm.MdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmRelationType
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import ru.yandex.market.mdm.service.common_entity.model.UpdateContext
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.MetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.Update
import ru.yandex.market.mdm.service.common_entity.service.constructor.defaults.RelationDefaultAttributeCreator
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmAttributeSearchFilter

@RunWith(MockitoJUnitRunner::class)
class RelationDefaultAttributeCreatorTest {

    @Mock
    lateinit var attributeMetadataService: MetadataService<MdmAttribute, MdmAttributeSearchFilter>

    @InjectMocks
    lateinit var defaultAttributeCreator: RelationDefaultAttributeCreator

    @Test
    fun `should update default attributes`() {
        // given
        val entityType = mdmRelationType()
        val context = UpdateContext(commitMessage = "commit", userLogin = "user")

        // when
        defaultAttributeCreator.createDefaultAttributes(entityType, context)

        // then
        val requestCaptor = argumentCaptor<List<Update<MdmAttribute>>>()
        val contextCaptor = argumentCaptor<UpdateContext>()
        verify(attributeMetadataService, times(1)).update(
            requestCaptor.capture(),
            contextCaptor.capture()
        )
        val savedAttributes = requestCaptor.firstValue
        savedAttributes.size shouldBe 2
        savedAttributes.map { it.update.copy(version = MdmVersion()) } shouldBe containExactlyInAnyOrderExceptVersion(
            MdmRelationType.defaultRelationAttributes(entityType.mdmId)
        )

        // and
        contextCaptor.firstValue shouldBe context
    }

    private fun containExactlyInAnyOrderExceptVersion(expected: Collection<MdmAttribute>):
        Matcher<Collection<MdmAttribute>?> = neverNullMatcher { value ->
        val artificialVersion = MdmVersion()
        val valueGroupedCounts: Map<MdmAttribute, Int> = value
            .map { it.copy(version = artificialVersion) }
            .groupBy { it }.mapValues { it.value.size }
        val expectedGroupedCounts: Map<MdmAttribute, Int> = expected
            .map { it.copy(version = artificialVersion) }
            .groupBy { it }.mapValues { it.value.size }
        val passed = expectedGroupedCounts.size == valueGroupedCounts.size
            && expectedGroupedCounts.all { valueGroupedCounts[it.key] == it.value }

        MatcherResult(
            passed,
            "Collection should contain ${expected.show().value} in any order, but was ${value.show().value}",
            "Collection should not contain exactly ${expected.show().value} in any order"
        )
    }
}
