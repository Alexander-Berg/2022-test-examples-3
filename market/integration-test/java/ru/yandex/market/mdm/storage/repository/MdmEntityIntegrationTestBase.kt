package ru.yandex.market.mdm.storage.repository

import io.kotest.properties.nextPrintableString
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.common.util.db.MultiIdGenerator
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.market.ir.yt.util.tables.YtClientWrapper
import ru.yandex.market.mbo.yt.utils.UnstableInit
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.lib.model.mdm.I18nStrings
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValue
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValues
import ru.yandex.market.mdm.lib.model.mdm.MdmEntity
import ru.yandex.market.mdm.lib.model.mdm.MdmEventContext
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import ru.yandex.market.mdm.lib.model.mdm.ProtoEntity
import ru.yandex.market.mdm.storage.MdmStorageApiIntegrationTest
import ru.yandex.market.mdm.storage.config.YtStorageFixedSettings
import ru.yandex.market.mdm.storage.service.physical.YtTableManager
import ru.yandex.misc.concurrent.TimeoutRuntimeException
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.random.Random

/**
 * Тест нетранзакционный и хранит стейт в живой табличке Yt. Поэтому во избежание конфликтов на CRUD операциях лучше
 * под каждый тест юзать свои уникальные ИДшники.
 *
 * Вычистка таблиц происходит один раз при подъёме контекста.
 */
abstract class MdmEntityIntegrationTestBase: MdmStorageApiIntegrationTest() {
    private val log = LoggerFactory.getLogger(MdmEntityRepositoryImplTest::class.java)

    @Autowired
    @Qualifier("markovYt")
    protected lateinit var markovYt: UnstableInit<Yt>
    @Autowired
    @Qualifier("hahnYt")
    protected lateinit var hahnYt: UnstableInit<Yt>
    @Autowired
    @Qualifier("arnoldYt")
    protected lateinit var arnoldYt: UnstableInit<Yt>
    @Autowired
    @Qualifier("markovYtClient")
    protected lateinit var markovYtClient: UnstableInit<YtClientWrapper>
    @Autowired
    @Qualifier("ytTableManager")
    protected lateinit var ytTableManager: YtTableManager
    @Autowired
    @Qualifier("mdmYtStorageSettings")
    protected lateinit var settings: YtStorageFixedSettings
    @Autowired
    @Qualifier("ytIdGenerator")
    protected lateinit var idGenerator: MultiIdGenerator

    protected lateinit var random: Random
    protected lateinit var repository: MdmEntityRepository
    protected var mdmEntityTypeId: Long = 0L
    protected var mskuIdAttributeId: Long = 0L
    protected var businessIdAttributeId: Long = 1L
    protected var sskuIdAttributeId: Long = 2L
    protected val eventContext = MdmEventContext("test commit")

    fun setUp() {
        waitForTableInit()
        mdmEntityTypeId = settings.tableModels[0].mdmEntityTypeId
        mskuIdAttributeId = settings.tableModels[0].primaryIndexModel.searchColumns[0].mdmAttributePath[0]
        businessIdAttributeId = settings.tableModels[0].secondaryIndexModels[0].searchColumns[0].mdmAttributePath[0]
        sskuIdAttributeId = settings.tableModels[0].secondaryIndexModels[0].searchColumns[1].mdmAttributePath[0]
        repository = ytTableManager.getMdmEntityRepositoryForMdmEntityType(mdmEntityTypeId)
    }

    fun entity(
        mdmId: Long,
        from: Long
    ) = sskuEntity(mdmId = mdmId, from = from)

    fun deadEntity(
        mdmId: Long,
        from: Long,
        to: Long
    ) = sskuEntity(mdmId = mdmId, from = from, to = to)

    fun mskuEntity(
        mdmId: Long,
        from: Long,
        mskuId: Long
    ) = sskuEntity(mdmId, mskuId, from)

    fun deadMskuEntity(
        mdmId: Long,
        from: Long,
        mskuId: Long,
        to: Long
    ) = sskuEntity(mdmId, mskuId, from, to = to)

    fun sskuEntity(
        mdmId: Long,
        mskuId: Long? = idGenerator.id,
        from: Long,
        businessId: Long? = idGenerator.id,
        sskuId: String? = random.nextPrintableString(10),
        to: Long? = null
    ): MdmEntity {
        val entity = randomMdmEntity(mdmId, from, to, 3)
        val values = entity.values as MutableMap<Long, MdmAttributeValues>
        values[mskuIdAttributeId] = MdmAttributeValues(mskuIdAttributeId, listOf(MdmAttributeValue(int64 = mskuId)))
        values[businessIdAttributeId] = MdmAttributeValues(businessIdAttributeId, listOf(MdmAttributeValue(int64 = businessId)))
        values[sskuIdAttributeId] = MdmAttributeValues(sskuIdAttributeId, listOf(MdmAttributeValue(string = I18nStrings.fromRu(
            sskuId?:random.nextPrintableString(10)))))

        if (mskuId == null) {
            values.remove(mskuIdAttributeId)
        }
        if (businessId == null) {
            values.remove(businessIdAttributeId)
        }
        if (sskuId == null) {
            values.remove(sskuIdAttributeId)
        }
        return entity
    }

    private fun randomMdmEntity(mdmId: Long, from: Long, to: Long? = null, depth: Int = 3): MdmEntity {
        val attributes = randomFlatAttributes()
        if (depth > 0) {
            val structAttributeId = abs(random.nextLong())
            attributes[structAttributeId] = MdmAttributeValues(
                structAttributeId, listOf(
                    MdmAttributeValue(struct = randomMdmEntity(0L, from, to, depth - 1))
                )
            )
        }
        return MdmEntity(
            mdmId,
            mdmEntityTypeId,
            attributes,
            MdmVersion(from, to)
        )
    }

    private fun randomFlatAttributes(): MutableMap<Long, MdmAttributeValues> {
        val boolAttributeId = abs(random.nextLong())
        val numericAttributeId = abs(random.nextLong())
        val stringAttributeId = abs(random.nextLong())
        val optionAttributeId = abs(random.nextLong())
        val timestampAttributeId = abs(random.nextLong())
        val referenceAttributeId = abs(random.nextLong())
        val in64AttributeId = abs(random.nextLong())
        return mutableMapOf(
            boolAttributeId to MdmAttributeValues(boolAttributeId, listOf(
                MdmAttributeValue(bool = random.nextBoolean())
            )),
            numericAttributeId to MdmAttributeValues(numericAttributeId, listOf(
                MdmAttributeValue(numeric = BigDecimal.valueOf(random.nextDouble()))
            )),
            stringAttributeId to MdmAttributeValues(stringAttributeId, listOf(
                MdmAttributeValue(string = I18nStrings.fromRu(random.nextPrintableString(10)))
            )),
            optionAttributeId to MdmAttributeValues(optionAttributeId, listOf(
                MdmAttributeValue(option = abs(random.nextLong()))
            )),
            timestampAttributeId to MdmAttributeValues(timestampAttributeId, listOf(
                MdmAttributeValue(timestamp = Instant.ofEpochMilli(random.nextLong(0L, 32503680000L)))
            )),
            referenceAttributeId to MdmAttributeValues(referenceAttributeId, listOf(
                MdmAttributeValue(referenceMdmId = abs(random.nextLong()))
            )),
            in64AttributeId to MdmAttributeValues(in64AttributeId, listOf(
                MdmAttributeValue(int64 = random.nextLong())
            )),
            // индексный атрибут
            mskuIdAttributeId to MdmAttributeValues(mskuIdAttributeId, listOf(
                MdmAttributeValue(int64 = abs(random.nextLong()))
            )),
            businessIdAttributeId to MdmAttributeValues(businessIdAttributeId, listOf(
                MdmAttributeValue(int64 = abs(random.nextLong()))
            )),
            sskuIdAttributeId to MdmAttributeValues(sskuIdAttributeId, listOf(
                MdmAttributeValue(string = I18nStrings.fromRu(random.nextPrintableString(10)))
            )),
        )
    }

    protected fun wipeVersions(proto: ProtoEntity): ProtoEntity {
        return proto.toBuilder().setMdmUpdateMeta(MdmBase.MdmUpdateMeta.getDefaultInstance()).build()
    }

    private fun waitForTableInit() {
        val waitStartTs = System.currentTimeMillis()
        while (!tablesInitialized(settings.tableModels[0].path) ||
            !tablesInitialized(settings.tableModels[0].primaryIndexModel.ytTableModel.path)) {
            if (System.currentTimeMillis() - waitStartTs > TimeUnit.SECONDS.toMillis(30)) {
                throw TimeoutRuntimeException("Wait for yt init timeouted")
            }
            try {
                TimeUnit.SECONDS.sleep(3L)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            log.warn("Tables are not yet available for yt rpc client, waiting...")
        }
    }

    private fun tablesInitialized(path: String): Boolean {
        return try {
            val ypath = YPath.simple(path)
            markovYt.get().cypress().exists(ypath) &&
                hahnYt.get().cypress().exists(ypath) &&
                arnoldYt.get().cypress().exists(ypath) &&
                markovYtClient.get().client.existsNode(path).join()
        } catch (e: Exception) {
            false
        }
    }
}
