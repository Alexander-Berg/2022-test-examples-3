package ru.yandex.market.mdm.metadata.service.grpc

import io.grpc.testing.GrpcCleanupRule
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.fixtures.commonParamViewSetting
import ru.yandex.market.mdm.fixtures.commonViewType
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmVersionRetiredNDaysAgo
import ru.yandex.market.mdm.fixtures.nDaysAgoMoment
import ru.yandex.market.mdm.fixtures.randomId
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.http.common_view.AllCommonViewTypesForEntityTypeRequest
import ru.yandex.market.mdm.http.common_view.CommonEntityTypeEnum.COMMON_ENTITY_TYPE_ATTRIBUTE
import ru.yandex.market.mdm.http.common_view.CommonParamViewSettingSearchFilter
import ru.yandex.market.mdm.http.common_view.CommonParamViewSettingUpdate
import ru.yandex.market.mdm.http.common_view.CommonParamViewSettingUpdateList
import ru.yandex.market.mdm.http.common_view.CommonParamViewSettingsByFilterRequest
import ru.yandex.market.mdm.http.common_view.CommonParamViewTypeServiceGrpc
import ru.yandex.market.mdm.http.common_view.CommonViewTypeSearchFilter
import ru.yandex.market.mdm.http.common_view.CommonViewTypesByFilterRequest
import ru.yandex.market.mdm.http.common_view.UpdateCommonParamViewSettingsRequest
import ru.yandex.market.mdm.http.search.VersionCriterion
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.lib.converters.toProtoWith
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.lib.model.mdm.MdmPath
import ru.yandex.market.mdm.lib.model.mdm.MdmPathSegment
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import ru.yandex.market.mdm.metadata.repository.CommonParamViewSettingRepository
import ru.yandex.market.mdm.metadata.repository.CommonViewTypeRepository
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEventRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass
import ru.yandex.market.mdm.metadata.testutils.createTestManagedChannel
import java.time.Instant

class CommonViewTypesGrpcServiceTest : BaseAppTestClass() {

    @Autowired
    lateinit var commonViewTypesGrpcService: CommonViewTypesGrpcService

    @Autowired
    lateinit var mdmAttributeRepository: MdmAttributeRepository

    @Autowired
    lateinit var commonViewTypeRepository: CommonViewTypeRepository

    @Autowired
    lateinit var commonParamViewSettingRepository: CommonParamViewSettingRepository

    @Autowired
    lateinit var mdmEventRepository: MdmEventRepository

    @Rule
    @JvmField
    val grpcCleanupRule = GrpcCleanupRule()

    lateinit var commonViewTypesClient: CommonParamViewTypeServiceGrpc.CommonParamViewTypeServiceBlockingStub

    @Before
    fun initClient() {
        commonViewTypesClient =
            CommonParamViewTypeServiceGrpc.newBlockingStub(
                createTestManagedChannel(
                    grpcCleanupRule,
                    commonViewTypesGrpcService
                )
            )
    }

    @Test
    @Deprecated("переходим на пути MARKETMDM-204")
    fun `should return all common view type settings on specific moment by attribute id`() {
        // given
        val versionClosed5daysAgo = mdmVersionRetiredNDaysAgo(5)
        val retiredCommonViewType = commonViewTypeRepository.insert(
            commonViewType(version = versionClosed5daysAgo)
        )
        val retiredCommonParamViewSetting = commonParamViewSettingRepository.insert(
            commonParamViewSetting(
                commonViewTypeId = retiredCommonViewType.mdmId,
                version = versionClosed5daysAgo
            )
        )
        val filterForOldSettings = CommonParamViewSettingSearchFilter.newBuilder()
            .addAllMdmAttributeId(listOf(retiredCommonParamViewSetting.commonParamId))
            .setCriterion(
                VersionCriterion.newBuilder()
                    .setAliveAt(nDaysAgoMoment(6).toEpochMilli())
            )
            .build()
        val request = CommonParamViewSettingsByFilterRequest
            .newBuilder()
            .setFilter(filterForOldSettings)
            .build()

        // when
        val response =
            commonViewTypesClient.getCommonParamViewSettingsByFilter(request)

        // then
        assertSoftly {
            response.settings.settingCount shouldBe 1
            response.settings.settingList shouldContain
                retiredCommonParamViewSetting.toProtoWith(retiredCommonViewType)
        }
    }

    @Test
    fun `should return all common view type settings on specific moment by path`() {
        // given
        val versionClosed5daysAgo = mdmVersionRetiredNDaysAgo(5)
        val retiredCommonViewType = commonViewTypeRepository.insert(
            commonViewType(version = versionClosed5daysAgo)
        )
        val path = MdmPath(listOf(MdmPathSegment.entity(randomId()), MdmPathSegment.attribute(randomId())))
        val retiredCommonParamViewSetting = commonParamViewSettingRepository.insert(
            commonParamViewSetting(
                mdmPath = path,
                commonViewTypeId = retiredCommonViewType.mdmId,
                version = versionClosed5daysAgo
            )
        )
        val filterForOldSettings = CommonParamViewSettingSearchFilter.newBuilder()
            .addPaths(MdmBase.MdmPathFilter.newBuilder().setPath(path.toProto()))
            .setCriterion(
                VersionCriterion.newBuilder()
                    .setAliveAt(nDaysAgoMoment(6).toEpochMilli())
            )
            .build()
        val request = CommonParamViewSettingsByFilterRequest
            .newBuilder()
            .setFilter(filterForOldSettings)
            .build()

        // when
        val response =
            commonViewTypesClient.getCommonParamViewSettingsByFilter(request)

        // then
        assertSoftly {
            response.settings.settingCount shouldBe 1
            response.settings.settingList shouldContain
                retiredCommonParamViewSetting.toProtoWith(retiredCommonViewType)
        }
    }

    @Test
    fun `should return no common view type settings on specific moment by path when versions retired on this moment`() {
        val versionClosed5daysAgo = mdmVersionRetiredNDaysAgo(5)
        val actualCommonViewType = commonViewTypeRepository.insert(commonViewType())
        val path = MdmPath(listOf(MdmPathSegment.entity(randomId()), MdmPathSegment.attribute(randomId())))
        commonParamViewSettingRepository.insert(
            commonParamViewSetting(
                commonViewTypeId = actualCommonViewType.mdmId,
                version = versionClosed5daysAgo,
                mdmPath = path,
            )
        )
        val filterForActualSettings = CommonParamViewSettingSearchFilter.newBuilder()
            .addPaths(MdmBase.MdmPathFilter.newBuilder().setPath(path.toProto()))
        val request = CommonParamViewSettingsByFilterRequest
            .newBuilder()
            .setFilter(filterForActualSettings)
            .build()

        // when
        val response =
            commonViewTypesClient.getCommonParamViewSettingsByFilter(request)

        // then
        response.settings.settingCount shouldBe 0
    }

    @Test
    fun `should update common view type setting`() {
        // given
        val commonViewType = commonViewTypeRepository.insert(commonViewType())
        val mdmAttribute = mdmAttributeRepository.insert(mdmAttribute())
        val commonParamViewSetting = commonParamViewSettingRepository.insert(
            commonParamViewSetting(
                commonViewTypeId = commonViewType.mdmId,
                commonParamId = mdmAttribute.mdmId,
                isEnabled = true,
            )
        )
        val updatedParamViewSetting = commonParamViewSetting.copy(isEnabled = false)

        val request = UpdateCommonParamViewSettingsRequest.newBuilder()
            .setUpdates(
                CommonParamViewSettingUpdateList.newBuilder()
                    .addUpdate(CommonParamViewSettingUpdate.newBuilder().setSetting(updatedParamViewSetting.toProto()))
                    .build()
            )
            .setContext(MdmBase.MdmUpdateContext.newBuilder().setCommitMessage("updated by me").build())
            .build()

        // when
        val response = commonViewTypesClient.updateCommonParamViewSettings(request)

        // then
        assertSoftly {
            response.results.settingCount shouldBe 1
            response.results.settingList[0].isEnabled shouldBe false
            response.errorsCount shouldBe 0
        }

        // and
        val saved = commonParamViewSettingRepository.findLatestById(updatedParamViewSetting.mdmId)
        saved!!.isEnabled shouldBe false

        // and
        val auditEvent = mdmEventRepository.findById(saved.version.eventId)
        auditEvent.commitMessage shouldBe "updated by me"
    }

    @Test
    fun `find all by entity type for specific moment`() {
        // given
        val retiredViewTypeForAttribute = commonViewTypeRepository.insert(
            commonViewType(
                version = mdmVersionRetiredNDaysAgo(50),
                commonEntityTypeEnum = CommonEntityTypeEnum.MDM_ATTR
            )
        )
        val request = AllCommonViewTypesForEntityTypeRequest.newBuilder()
            .setCriterion(VersionCriterion.newBuilder().setAliveAt(nDaysAgoMoment(60).toEpochMilli()))
            .setEntityType(COMMON_ENTITY_TYPE_ATTRIBUTE)
            .build()

        // when
        val response = commonViewTypesClient.getAllCommonViewTypesForEntityType(request)

        // then
        assertSoftly {
            response.viewTypes.viewTypeCount shouldBe 1
            response.viewTypes.viewTypeList shouldContain retiredViewTypeForAttribute.toProto()
        }
    }

    @Test
    fun `should save audit event on create`() {
        // given
        val commonViewType = commonViewTypeRepository.insert(commonViewType())
        val mdmAttribute = mdmAttributeRepository.insert(mdmAttribute())
        val commonParamViewSetting = commonParamViewSetting(
            commonViewTypeId = commonViewType.mdmId,
            commonParamId = mdmAttribute.mdmId,
        )

        val toCreate = CommonParamViewSettingUpdateList.newBuilder()
            .addUpdate(CommonParamViewSettingUpdate.newBuilder().setSetting(commonParamViewSetting.toProto()))
            .build()

        val context = MdmBase.MdmUpdateContext.newBuilder().build()
        val request = UpdateCommonParamViewSettingsRequest.newBuilder()
            .setUpdates(toCreate)
            .setContext(context).build()

        // when
        val response = commonViewTypesClient.updateCommonParamViewSettings(request)

        // then
        assertSoftly {
            response.errorsCount shouldBe 0
            response.results.settingCount shouldBe 1

            val auditEventId = response.results.settingList[0].mdmUpdateMeta.auditEventId
            auditEventId shouldNotBe 0L

            val auditEvent = mdmEventRepository.findByIds(listOf(auditEventId))
            auditEvent.size shouldBe 1

            val commonParamViewSettings = commonParamViewSettingRepository.findLatestById(commonParamViewSetting.mdmId)
            commonParamViewSettings shouldNotBe null
            commonParamViewSettings!!.version.eventId shouldBe auditEventId
        }
    }

    @Test
    fun `should save audit event on update`() {
        // given
        val commonViewType = commonViewTypeRepository.insert(commonViewType())
        val mdmAttribute = mdmAttributeRepository.insert(mdmAttribute())
        val commonParamViewSetting = commonParamViewSettingRepository.insert(
            commonParamViewSetting(
                commonViewTypeId = commonViewType.mdmId,
                commonParamId = mdmAttribute.mdmId,
            )
        )

        val toCreate = CommonParamViewSettingUpdateList.newBuilder()
            .addUpdate(CommonParamViewSettingUpdate.newBuilder().setSetting(commonParamViewSetting.toProto()))
            .build()
        val context = MdmBase.MdmUpdateContext.newBuilder().build()
        val request = UpdateCommonParamViewSettingsRequest.newBuilder()
            .setUpdates(toCreate)
            .setContext(context).build()

        // when
        val response = commonViewTypesClient.updateCommonParamViewSettings(request)

        // then
        assertSoftly {
            response.errorsCount shouldBe 0
            response.results.settingCount shouldBe 1

            val auditEventId = response.results.settingList[0].mdmUpdateMeta.auditEventId
            auditEventId shouldNotBe 0L

            val auditEvent = mdmEventRepository.findByIds(listOf(auditEventId))
            auditEvent.size shouldBe 1

            val commonParamViewSettings = commonParamViewSettingRepository.findLatestById(commonParamViewSetting.mdmId)
            commonParamViewSettings shouldNotBe null
            commonParamViewSettings!!.version.eventId shouldBe auditEventId
        }
    }

    @Test
    fun `should save audit event on delete`() {
        // given
        val commonViewType = commonViewTypeRepository.insert(commonViewType())
        val mdmAttribute = mdmAttributeRepository.insert(mdmAttribute())
        val commonParamViewSetting = commonParamViewSettingRepository.insert(
            commonParamViewSetting(
                commonViewTypeId = commonViewType.mdmId,
                commonParamId = mdmAttribute.mdmId,
            )
        )

        val toDelete = CommonParamViewSettingUpdateList.newBuilder()
            .addUpdate(
                CommonParamViewSettingUpdate.newBuilder()
                    .setSetting(commonParamViewSetting.copy(version = MdmVersion(to = Instant.now())).toProto())
            )
            .build()

        val context = MdmBase.MdmUpdateContext.newBuilder().build()
        val request = UpdateCommonParamViewSettingsRequest.newBuilder()
            .setUpdates(toDelete)
            .setContext(context).build()

        // when
        val response = commonViewTypesClient.updateCommonParamViewSettings(request)

        // then
        assertSoftly {
            response.errorsCount shouldBe 0
            response.results.settingCount shouldBe 1

            val deleted = response.results.settingList[0]
            val auditEventId = deleted.mdmUpdateMeta.auditEventId
            auditEventId shouldNotBe 0L

            val auditEvent = mdmEventRepository.findByIds(listOf(auditEventId))
            auditEvent.size shouldBe 1

            val commonParamViewSettings = commonParamViewSettingRepository.findAll().sortedBy { it.version.from }
            commonParamViewSettings.last().version.eventId shouldBe auditEventId
        }
    }

    @Test
    fun `should return all common view types`() {
        // given
        val request = MdmBase.MdmEmptyRequest.newBuilder().build()
        val initialViewTypeCount = commonViewTypesClient.getAllCommonViewTypes(request).viewTypes.viewTypeCount
        val commonViewType1 = commonViewTypeRepository.insert(commonViewType())
        val commonViewType2 = commonViewTypeRepository.insert(commonViewType())
        val commonViewType3 = commonViewTypeRepository.insert(commonViewType())

        // when
        val response = commonViewTypesClient.getAllCommonViewTypes(request)

        // then
        assertSoftly {
            response.viewTypes.viewTypeCount shouldBe 3 + initialViewTypeCount
            response.viewTypes.viewTypeList shouldContain commonViewType1.toProto()
            response.viewTypes.viewTypeList shouldContain commonViewType2.toProto()
            response.viewTypes.viewTypeList shouldContain commonViewType3.toProto()
        }
    }

    @Test
    fun `should return all common view types on specific moment by filter`() {
        // given
        val versionClosed5daysAgo = mdmVersionRetiredNDaysAgo(5)
        val retiredCommonViewType = commonViewTypeRepository.insert(
            commonViewType(version = versionClosed5daysAgo)
        )
        val filterForOldSettings = CommonViewTypeSearchFilter.newBuilder()
            .addAllMdmId(listOf(retiredCommonViewType.mdmId))
            .setCriterion(
                VersionCriterion.newBuilder()
                    .setAliveAt(nDaysAgoMoment(6).toEpochMilli())
            )
            .build()
        val request = CommonViewTypesByFilterRequest
            .newBuilder()
            .setFilter(filterForOldSettings)
            .build()

        // when
        val response =
            commonViewTypesClient.getCommonViewTypesByFilter(request)

        // then
        assertSoftly {
            response.viewTypes.viewTypeCount shouldBe 1
            response.viewTypes.viewTypeList shouldContain retiredCommonViewType.toProto()
        }
    }

    @Test
    fun `should return no common view types on specific moment by id when versions retired on this moment`() {
        val versionClosed5daysAgo = mdmVersionRetiredNDaysAgo(5)
        val retiredCommonViewType = commonViewTypeRepository.insert(
            commonViewType(version = versionClosed5daysAgo)
        )

        val filterForActualSettings = CommonViewTypeSearchFilter.newBuilder()
            .addAllMdmId(listOf(retiredCommonViewType.mdmId))
        val request = CommonViewTypesByFilterRequest
            .newBuilder()
            .setFilter(filterForActualSettings)
            .build()

        // when
        val response = commonViewTypesClient.getCommonViewTypesByFilter(request)

        // then
        response.viewTypes.viewTypeCount shouldBe 0
    }
}
