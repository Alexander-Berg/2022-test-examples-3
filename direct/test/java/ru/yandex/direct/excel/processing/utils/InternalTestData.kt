package ru.yandex.direct.excel.processing.utils

import org.apache.commons.lang3.RandomStringUtils
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.data.TestNewInternalBanners
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils
import ru.yandex.direct.excel.processing.model.internalad.*
import ru.yandex.direct.test.utils.randomPositiveLong

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("delete after migrate all tests to Kotlin")
fun getDefaultInternalAdGroupRepresentation(): InternalAdGroupRepresentation =
        getDefaultInternalAdGroupRepresentation(campaignId = randomPositiveLong())

fun getDefaultInternalAdGroupRepresentation(
        campaignId: Long = randomPositiveLong()
): InternalAdGroupRepresentation = InternalAdGroupRepresentation()
        .setAdGroup(TestGroups.internalAdGroup(campaignId, null))
        .setTargetingRepresentation(AdGroupAdditionalTargetingRepresentation())
        .setRetargetingConditionRepresentation(RetargetingConditionRepresentation(null))

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("delete after migrate all tests to Kotlin")
fun getDefaultInternalBannerRepresentation(adGroup: InternalAdGroup): InternalBannerRepresentation =
        getDefaultInternalBannerRepresentation(adGroup = adGroup,
                templateId = TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1)

fun getDefaultInternalBannerRepresentation(
        adGroup: InternalAdGroup,
        templateId: Long = TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1
): InternalBannerRepresentation = InternalBannerRepresentation()
        .setBanner(TestNewInternalBanners.fullInternalBanner(adGroup.campaignId, adGroup.id)
                .withTemplateId(templateId))
        .setAdGroupId(adGroup.id)
        .setAdGroupName(adGroup.name)

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("delete after migrate all tests to Kotlin")
fun getDefaultSheetDescriptor(): SheetDescriptor = getDefaultSheetDescriptor(randomPositiveLong())

fun getDefaultSheetDescriptor(
        campaignId: Long = randomPositiveLong(),
        placeId: Long = TemplatePlaceRepositoryMockUtils.PLACE_1,
        templateId: Long = TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1
): SheetDescriptor = SheetDescriptor()
        .setCampaignId(campaignId)
        .setPlaceId(placeId)
        .setTemplateId(templateId)

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("delete after migrate all tests to Kotlin")
fun <T> createSheetFetchedData(objects: List<T>): ExcelSheetFetchedData<T> =
        createSheetFetchedData(getDefaultSheetDescriptor(), objects)

fun <T> createSheetFetchedData(
        sheetDescriptor: SheetDescriptor = getDefaultSheetDescriptor(),
        objects: List<T>
): ExcelSheetFetchedData<T> = ExcelSheetFetchedData
        .create(RandomStringUtils.randomAlphabetic(7), sheetDescriptor, objects, emptyList(), emptyList())
