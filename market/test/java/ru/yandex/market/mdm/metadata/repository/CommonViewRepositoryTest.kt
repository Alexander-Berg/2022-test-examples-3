package ru.yandex.market.mdm.metadata.repository

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.fixtures.commonParamViewSetting
import ru.yandex.market.mdm.fixtures.commonViewType
import ru.yandex.market.mdm.fixtures.mdmVersionRetiredNDaysAgo
import ru.yandex.market.mdm.fixtures.nDaysAgoMoment
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum.MDM_ATTR
import ru.yandex.market.mdm.lib.model.common.CommonViewType
import ru.yandex.market.mdm.lib.model.common.CommonViewTypeEnum
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion.Companion.fromVersions
import ru.yandex.market.mdm.metadata.filters.VersionedRepositorySearchFilter
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass

class CommonViewRepositoryTest : BaseAppTestClass() {

    @Autowired
    lateinit var commonViewTypeRepository: CommonViewTypeRepository
    @Autowired
    lateinit var commonParamViewSettingRepository: CommonParamViewSettingRepository

    @Test
    fun `insert and select new common view type`() {
        val instance = CommonViewType(commonEntityTypeEnum = MDM_ATTR,
            viewType = CommonViewTypeEnum.EXCEL_IMPORT, internalName = "IMPORT", ruTitle = "текст")
        val inserted = commonViewTypeRepository.insert(instance)

        val selected = commonViewTypeRepository.findLatestById(inserted.mdmId)

        assertSoftly {
            inserted shouldBe selected
        }
    }

    @Test
    fun `insert and select new common param view type`() {
        val instance = commonParamViewSetting()
        val inserted = commonParamViewSettingRepository.insert(instance)
        val selected = commonParamViewSettingRepository.findAll()

        assertSoftly {
            inserted shouldBe selected.last()
        }
    }

    @Test
    fun `find all by entity type for specific moment`() {
        // given
        val retiredViewTypeForAttribute = commonViewTypeRepository.insert(commonViewType(
            version = mdmVersionRetiredNDaysAgo(10),
            commonEntityTypeEnum = MDM_ATTR))
        val actualViewTypeForAttribute = commonViewTypeRepository.insert(commonViewType(
            version = fromVersions(nDaysAgoMoment(10), null),
            commonEntityTypeEnum = MDM_ATTR))
        val filter = VersionedRepositorySearchFilter(moment = nDaysAgoMoment(11))

        // when
        val selected = commonViewTypeRepository.findByEntityType(MDM_ATTR, filter)

        // then
        assertSoftly {
            selected shouldHaveSize 1
            retiredViewTypeForAttribute shouldBe selected[0]
        }
    }
}
