package ru.yandex.market.mdm.metadata.service

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.fixtures.commonParamViewSetting
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.nDaysAfterMoment
import ru.yandex.market.mdm.fixtures.nDaysAgoMoment
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType
import ru.yandex.market.mdm.lib.model.mdm.MdmPath
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import ru.yandex.market.mdm.metadata.repository.CommonParamViewSettingRepository
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEntityTypeRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass
import ru.yandex.market.mdm.metadata.testutils.TestDataUtils

class CommonParamViewSettingsServiceTest : BaseAppTestClass() {

    @Autowired
    lateinit var mdmEntityTypeRepository: MdmEntityTypeRepository

    @Autowired
    lateinit var mdmAttributeRepository: MdmAttributeRepository

    @Autowired
    lateinit var commonParamViewSettingRepository: CommonParamViewSettingRepository

    @Autowired
    lateinit var commonParamViewSettingsService: CommonParamViewSettingsService

    @Test
    fun `should create propagated settings on all attributes of outerStruct`() {
        // given
        val entityTypeWithOneStruct = TestDataUtils.entityTypeWithOneStruct(
            innerSimpleAttributes = listOf(mdmAttribute(mdmId = 42)),
            outerSimpleAttributes = listOf(mdmAttribute()),
        )
        val outerMdmEntityType = entityTypeWithOneStruct.outerMdmEntityType
        val innerMdmEntityType = entityTypeWithOneStruct.innerMdmEntityType
        mdmEntityTypeRepository.insertOrUpdateBatch(listOf(innerMdmEntityType, outerMdmEntityType))
        mdmAttributeRepository.insertOrUpdateBatch(
            innerMdmEntityType.attributes + outerMdmEntityType.attributes
        )

        // given new setting on struct attribute
        val newSetting = commonParamViewSetting(
            commonViewTypeId = 15L,
            commonParamId = entityTypeWithOneStruct.structAttribute.mdmId,
            mdmPath = MdmPath.fromLongs(
                listOf(outerMdmEntityType.mdmId, entityTypeWithOneStruct.structAttribute.mdmId),
                MdmMetaType.MDM_ATTR
            ),
            version = MdmVersion.fromVersions(nDaysAfterMoment(1), null)
        )

        // when
        val propagatesSettings = commonParamViewSettingsService.createPropagatedSettings(newSetting)

        // then
        propagatesSettings shouldContainExactlyInAnyOrder listOf(
            newSetting.copy(
                mdmId = 0,
                commonParamId = 42,
                mdmPath = MdmPath.fromLongs(
                    listOf(
                        outerMdmEntityType.mdmId,
                        entityTypeWithOneStruct.structAttribute.mdmId,
                        innerMdmEntityType.mdmId,
                        42
                    ),
                    MdmMetaType.MDM_ATTR
                )
            )
        )
    }

    @Test
    fun `should update propagated settings on all attributes of outerStruct`() {
        // given
        val entityTypeWithOneStruct = TestDataUtils.entityTypeWithOneStruct(
            innerSimpleAttributes = listOf(mdmAttribute(mdmId = 42)),
            outerSimpleAttributes = listOf(mdmAttribute()),
        )
        val outerMdmEntityType = entityTypeWithOneStruct.outerMdmEntityType
        val innerMdmEntityType = entityTypeWithOneStruct.innerMdmEntityType
        mdmEntityTypeRepository.insertOrUpdateBatch(listOf(innerMdmEntityType, outerMdmEntityType))
        mdmAttributeRepository.insertOrUpdateBatch(
            innerMdmEntityType.attributes + outerMdmEntityType.attributes
        )

        // given old setting on struct attribute
        val oldSettingOnStruct = commonParamViewSetting(
            mdmId = 111L,
            commonViewTypeId = 15L,
            commonParamId = entityTypeWithOneStruct.structAttribute.mdmId,
            mdmPath = MdmPath.fromLongs(
                listOf(outerMdmEntityType.mdmId, entityTypeWithOneStruct.structAttribute.mdmId),
                MdmMetaType.MDM_ATTR
            ),
            version = MdmVersion.fromVersions(nDaysAgoMoment(1), null)
        )

        val oldSettingOnInner = commonParamViewSetting(
            mdmId = 333,
            commonViewTypeId = 15L,
            commonParamId = 42,
            mdmPath = MdmPath.fromLongs(
                listOf(outerMdmEntityType.mdmId, entityTypeWithOneStruct.structAttribute.mdmId, innerMdmEntityType.mdmId, 42),
                MdmMetaType.MDM_ATTR
            ),
            version = MdmVersion.fromVersions(nDaysAgoMoment(1), null)
        )
        commonParamViewSettingRepository.insertOrUpdateBatch(listOf(oldSettingOnStruct, oldSettingOnInner))

        // update on setting
        val newSettingOnStruct = oldSettingOnStruct.copy(
            commonViewTypeId = 115L,
            version = MdmVersion.fromVersions(nDaysAfterMoment(1), null)
        )

        // when
        val propagatedSettings = commonParamViewSettingsService.createPropagatedSettings(newSettingOnStruct)

        // then
        propagatedSettings shouldContainExactlyInAnyOrder listOf(
            newSettingOnStruct.copy(
                mdmId = 333L,
                commonParamId = 42,
                commonViewTypeId = 115L,
                mdmPath = MdmPath.fromLongs(
                    listOf(
                        outerMdmEntityType.mdmId,
                        entityTypeWithOneStruct.structAttribute.mdmId,
                        innerMdmEntityType.mdmId,
                        42
                    ),
                    MdmMetaType.MDM_ATTR
                )
            )
        )
    }
}
