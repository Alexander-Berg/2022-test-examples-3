package ru.yandex.direct.bstransport.yt.repository.adgroup.resources

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.adv.direct.adgroup.AdGroup
import ru.yandex.adv.direct.adgroup.OptionalInternalLevel
import ru.yandex.adv.direct.expression.multipler.type.MultiplierTypeEnum
import ru.yandex.adv.direct.expression2.TargetingExpression
import ru.yandex.adv.direct.multipliers.Multiplier
import ru.yandex.adv.direct.showcondition.RelevanceMatchData
import ru.yandex.adv.direct.showcondition.RfOptions
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeListNodeImpl
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeProtoUtils
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree

class AdGroupYtRepositoryTest : AbstractAdGroupYtRepositoryTest(AdGroupYtRepository::class) {
    companion object {
        private val MULTIPLIER = Multiplier.newBuilder().run {
            value = 10
            type = MultiplierTypeEnum.DeviceType
            conditionBuilder.run {
                addAndBuilder().run {
                    addOrBuilder().run {
                        operation = 5
                        keyword = 4
                        value = "3"
                    }
                }
            }
            build()
        }

        private val SHOW_CONDITIONS = TargetingExpression.newBuilder().run {
            addAndBuilder().run {
                addOrBuilder().run {
                    value = "1"
                    keyword = 2
                    operation = 3
                }
            }
            build()
        }

        private val RELEVANCE_MATCH_DATA = RelevanceMatchData.newBuilder().run {
            addAllRelevanceMatchCategories(
                listOf(
                    RelevanceMatchData.RelevanceMatchCategory.ExactMark,
                    RelevanceMatchData.RelevanceMatchCategory.BroaderMark
                )
            )
            build()
        }

        private val RF_OPTIONS = RfOptions.newBuilder().run {
            RfOptions.newBuilder().run {
                maxShowsCount = 123
                maxShowsPeriod = 7
                stopShowsPeriod = 7
                build()
            }
        }

        private val defaultResource = AdGroup.newBuilder().run {
            adGroupId = 12L
            orderId = 456L
            iterId = 1643L
            updateTime = 123434242L
            engineId = 7L
            type = 15
            name = "Название"
            internalLevel = OptionalInternalLevel.newBuilder().setValue(123).build()
            addAllMinusPhrases(listOf("phrase 1", "phrase2"))
            addAllPageGroupTags(listOf("tag1", "tag2"))
            matchPriority = 10
            addAllTargetTags(listOf())
            clickUrlTail = "bid={banner_id}"
            addAllMultipliers(listOf(MULTIPLIER, MULTIPLIER))
            showConditions = SHOW_CONDITIONS
            contextId = 1234567890L
            serpPlacementType = 1
            relevanceMatchData = RELEVANCE_MATCH_DATA
            rfOptions = RF_OPTIONS
            fieldToUseAsName = "some value for fieldToUseAsName"
            fieldToUseAsBody = "some value for fieldToUseAsBody"
            feedId = 321123L
            build()
        }

        private val defaultColumnNameToValue = mapOf<String, Any?>(
            "AdGroupID" to 12L,
            "OrderID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1643L,
            "EngineID" to 7L,
            "Type" to 15L,
            "Name" to "Название",
            "InternalLevel" to 123L,
            "MinusPhrases" to YTreeListNodeImpl(null).apply {
                addAll(listOf("phrase 1", "phrase2").map { YTreeStringNodeImpl(it, null) })
            },
            "PageGroupTags" to YTreeListNodeImpl(null).apply {
                addAll(listOf("tag1", "tag2").map { YTreeStringNodeImpl(it, null) })
            },
            "ClickUrlTail" to "bid={banner_id}",
            "MatchPriority" to 10L,
            "TargetTags" to YTreeListNodeImpl(null),
            "Multipliers" to YTree.listBuilder().run {
                value(YTreeProtoUtils.marshal(MULTIPLIER))
                value(YTreeProtoUtils.marshal(MULTIPLIER))
                endList()
                build()
            },
            "ShowConditions" to SHOW_CONDITIONS,
            "ContextID" to 1234567890L,
            "SerpPlacementType" to 1L,
            "RelevanceMatchData" to RELEVANCE_MATCH_DATA,
            "RfOptions" to RF_OPTIONS,
            "FieldToUseAsName" to "some value for fieldToUseAsName",
            "FieldToUseAsBody" to "some value for fieldToUseAsBody",
            "FeedID" to 321123L
        )

        @JvmStatic
        @Suppress("unused")
        fun parameters(): List<Arguments> = listOf(
            Arguments.of(
                "Default",
                defaultResource,
                defaultColumnNameToValue,
            ),
            Arguments.of(
                "NullInternalLevel",
                defaultResource.toBuilder().run {
                    internalLevelBuilder.clearValue()
                    build()
                },
                defaultColumnNameToValue.toMutableMap().apply {
                    set("InternalLevel", null)
                }
            ),
            Arguments.of(
                "EmptyMinusPhrasesList",
                defaultResource.toBuilder().run {
                    clearMinusPhrases()
                    build()
                },
                defaultColumnNameToValue.toMutableMap().apply {
                    set("MinusPhrases", YTreeListNodeImpl(null))
                }
            ),
            Arguments.of(
                "EmptyMultipliers",
                defaultResource.toBuilder().clearMultipliers().build(),
                defaultColumnNameToValue + mapOf("Multipliers" to YTreeListNodeImpl(null))
            )
        )
    }

    @ParameterizedTest(name = "getSchemaWithMappingTest{0}")
    @MethodSource("parameters")
    fun getSchemaWithMappingTest(
        @Suppress("UNUSED_PARAMETER")
        name: String,
        resource: AdGroup,
        expectedColumnNameToValue: Map<String, Any?>
    ) {
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }
}
