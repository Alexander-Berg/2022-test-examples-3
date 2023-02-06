package ru.yandex.market.contentmapping.services

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.dto.fileprocessing.ProcessingConfig
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingType
import ru.yandex.market.contentmapping.dto.mapping.ShopParam
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.dto.model.ShopModel
import ru.yandex.market.contentmapping.dto.model.ShopModelSaveContext
import ru.yandex.market.contentmapping.dto.model.ShopModelSaveContext.Companion.saveContextWithSource
import ru.yandex.market.contentmapping.dto.model.SimpleOffer
import ru.yandex.market.contentmapping.repository.ParamMappingRepository
import ru.yandex.market.contentmapping.repository.ShopRepository
import ru.yandex.market.contentmapping.services.fileprocessing.FileProcessingService
import ru.yandex.market.contentmapping.services.fileprocessing.FileProcessingService.ImportResult
import ru.yandex.market.contentmapping.services.fileprocessing.XlsxModelReader
import ru.yandex.market.contentmapping.services.mapping.ParamMappingService
import ru.yandex.market.contentmapping.services.shop.ShopService
import ru.yandex.market.contentmapping.testdata.TestDataUtils
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.contentmapping.testutils.RequestMockUtils
import ru.yandex.market.contentmapping.utils.smartlogger.LoggerSmartLogger
import ru.yandex.market.ir.http.Formalizer.FormalizedOffer
import ru.yandex.market.ir.http.FormalizerService
import ru.yandex.market.ir.http.UltraController.EnrichedOffer
import ru.yandex.market.ir.http.UltraControllerService
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper

/**
 * @author yuramalinov
 * @created 24.02.2020
 */
class FileProcessingServiceTest : BaseAppTestClass() {
    companion object {
        private val log = LogManager.getLogger()
        private const val CATEGORY_ID = 1
        private const val VENDOR_ID = 101
        private const val MODEL_ID = 201
        private const val MARKET_SKU_ID = 301
        private const val TEST_DATA_DIR = "data-files"
        private const val SINGLE_MODEL_XLSX = "single-model.xlsx"
        private const val HIDDEN_TRASH_XLS = "hidden_trash.xls"
        private const val HIDDEN_TRASH_XLSX = "hidden_trash.xlsx"
        private const val OZON_SAMPLE = "ozon-sample.xlsm"
        private const val YML_SAMPLE = "test.yml.xml"
    }

    lateinit var fileProcessingService: FileProcessingService

    @Autowired
    lateinit var offerImportService: OfferImportService

    @Autowired
    lateinit var transactionHelper: TransactionHelper

    @Autowired
    lateinit var shopService: ShopService

    @Autowired
    lateinit var shopModelService: ShopModelService

    @Autowired
    lateinit var shopRepository: ShopRepository

    @Autowired
    lateinit var ultraControllerService: UltraControllerService

    @Autowired
    lateinit var formalizerService: FormalizerService

    @Autowired
    lateinit var paramMappingRepository: ParamMappingRepository

    @Autowired
    lateinit var paramMappingService: ParamMappingService

    private var shopId: Long = 0

    @Before
    fun setup() {
        shopId = shopRepository.insert(Shop(TestDataUtils.TEST_SHOP_ID, "Name")).id
        Mockito.reset(ultraControllerService, formalizerService)
        fileProcessingService = FileProcessingService(
                offerImportService,
                transactionHelper,
                shopService
        )
    }

    @Test
    fun testItDoesSomething() {
        RequestMockUtils.mockUltraControllerEnrich(ultraControllerService) { _ ->
            EnrichedOffer.newBuilder()
                    .setCategoryId(CATEGORY_ID)
                    .setVendorId(VENDOR_ID)
                    .setModelId(MODEL_ID)
                    .setMarketSkuId(MARKET_SKU_ID.toLong())
                    .setMarketSkuName("SKU 301")
                    .build()
        }
        RequestMockUtils.mockFormalizerFormalize(formalizerService) {
            FormalizedOffer.newBuilder().build()
        }
        var result = fileProcessingService.importFile(shopId, SINGLE_MODEL_XLSX,
                loadResource("$TEST_DATA_DIR/$SINGLE_MODEL_XLSX"),
                null,
                true,
                LoggerSmartLogger(log, Level.INFO))

        assertThat(result.modelsCount).isNull()

        val processingConfigInfo = result.processingConfigInfo
        assertThat(processingConfigInfo).isNotNull
        assertThat(processingConfigInfo!!.processingConfig).isEqualToComparingFieldByField(
                ProcessingConfig().apply {
                    shopSku = "ean"
                    name = "название"
                    vendorCode = "артикул"
                    barcode = "ean"
                }
        )
        assertThat(processingConfigInfo.fileColumns).hasSize(23)

        val columnInfoMap = processingConfigInfo.fileColumns.associateBy { it.header }

        // To correctly check uniqueness we actually need more then one column.
        assertThat(columnInfoMap["EAN"]!!.unique).isTrue
        assertThat(columnInfoMap["EAN"]!!.sampleValues).containsExactly("3337875597197")


        result = fileProcessingService.importFile(shopId, SINGLE_MODEL_XLSX,
                loadResource("$TEST_DATA_DIR/$SINGLE_MODEL_XLSX"),
                processingConfigInfo.processingConfig,
                false,
                LoggerSmartLogger(log, Level.INFO))
        assertThat(result.modelsCount).isEqualTo(0)
        assertThat(shopModelService.getAll()).hasSize(0)

        val excelShopModels = XlsxModelReader.readExcel(
                shopId, loadResource("$TEST_DATA_DIR/$SINGLE_MODEL_XLSX"), processingConfigInfo.processingConfig)
        val context = saveContextWithSource(ShopModelSaveContext.Source.CT)
        excelShopModels.offers
                .map { it.toShopModel()}
                .forEach { model -> shopModelService.saveAndUpdateStatistics(model.copy(externalCategoryId = 0), context) }
        result = fileProcessingService.importFile(shopId, SINGLE_MODEL_XLSX,
                loadResource("$TEST_DATA_DIR/$SINGLE_MODEL_XLSX"),
                processingConfigInfo.processingConfig,
                false,
                LoggerSmartLogger(log, Level.INFO))
        assertThat(result.modelsCount).isEqualTo(0)
        assertThat(shopModelService.getAll()).hasSize(1)
    }

    @Test
    fun testHiddenTrashXls() {
        val result = getFileProcessingResult("$TEST_DATA_DIR/$HIDDEN_TRASH_XLS")
        assertThat(result.processingConfigInfo!!.fileColumns.size).isEqualTo(9)
    }

    @Test
    fun testHiddenTrashXlsx() {
        val result = getFileProcessingResult("$TEST_DATA_DIR/$HIDDEN_TRASH_XLSX")
        assertThat(result.processingConfigInfo!!.fileColumns.size).isEqualTo(9)
    }

    @Test
    fun `test ozon file (format + duplicate headers)`() {
        val result = getFileProcessingResult("$TEST_DATA_DIR/$OZON_SAMPLE")
        val config = result.processingConfigInfo!!
        assertThat(config.fileColumns)
                .extracting({ it.header })
                .contains(
                        tuple("Название товара на OZON.RU"), // Should be trimmed
                        tuple("Сертификация* Срок действия"), // Should replace \n
                )

        val processingConfig = config.processingConfig
        processingConfig.shopSku = "Код товара от поставщика"
        processingConfig.name = "Наименование товара для OZON.RU"

        val processResult = XlsxModelReader.readExcel(shopId,
                loadResource("$TEST_DATA_DIR/$OZON_SAMPLE"),
                config.processingConfig)

        val models = processResult.offers
        assertThat(models).hasSize(1)
        assertThat(models[0].shopSku).isEqualTo("4626016180015")
        assertThat(models[0].name).isEqualTo("Таблетки ELLY для посудомоечных машин, 70 шт")
        assertThat(models[0].shopValues["Дубль"]).isEqualTo("1/1/2119, 1095") // two columns united with comma
        assertThat(processResult.errors).isEmpty()
    }

    @Test
    fun `test yml import and auto-mappings`() {
        RequestMockUtils.mockFormalizerFormalize(formalizerService) {
            FormalizedOffer.newBuilder().build()
        }

        shopModelService.saveAndUpdateStatistics(
                TestDataUtils.testShopModel(id = 0, shopSku = "29406").copy(externalCategoryId = 42),
                ShopModelSaveContext(ShopModelSaveContext.Source.CT))
        val result = getFileProcessingResult("$TEST_DATA_DIR/$YML_SAMPLE")

        result.modelsCount shouldBe 1

        val mappings = paramMappingRepository.findByShopId(shopId)
        mappings shouldHaveSize 1
        mappings[0].asClue {
            it.shopParams shouldBe listOf(ShopParam("Изображения из YML", split=","))
            it.isDeleted shouldBe false
            it.mappingType shouldBe ParamMappingType.PICTURE
        }

        paramMappingService.deleteParamMapping(mappings[0])

        // NOTE: Extra import shouldn't revive mapping
        getFileProcessingResult("$TEST_DATA_DIR/$YML_SAMPLE")

        val updatedMappings = paramMappingRepository.findByShopId(shopId)
        updatedMappings shouldHaveSize 1
        updatedMappings[0].asClue {
            it.id shouldBe mappings[0].id
            it.shopParams shouldBe listOf(ShopParam("Изображения из YML", split=","))
            it.isDeleted shouldBe true
            it.mappingType shouldBe ParamMappingType.PICTURE
        }
    }

    private fun getFileProcessingResult(fileName: String): ImportResult {
        return fileProcessingService.importFile(shopId, "$TEST_DATA_DIR/$fileName",
                loadResource(fileName),
                null,
                true,
                LoggerSmartLogger(log, Level.INFO))
    }
}


fun SimpleOffer.toShopModel() = ShopModel(
        shopSku = shopSku,
        shopId = shopId,
        name = name ?: error("No name given, can't create fresh ShopModel"),
        description = description,
        shopCategoryName = shopCategoryName,
        shopVendor = shopVendor,
        vendorCode = vendorCode,
        barCode = barCode,
        shopValues = shopValues,
)
