package ru.yandex.market.mbo.mdm.common.utils;

import java.util.List;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mbo.mdm.common.automarkup.repository.MdmAutoMarkupHistoryRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.automarkup.repository.MdmAutoMarkupYqlQueryRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.datacamp.DatacampOffersFiltrator;
import ru.yandex.market.mbo.mdm.common.datacamp.MdmDatacampService;
import ru.yandex.market.mbo.mdm.common.datacamp.MdmDatacampServiceMock;
import ru.yandex.market.mbo.mdm.common.datacamp.ServiceOfferMigrationServiceImpl;
import ru.yandex.market.mbo.mdm.common.datacamp.SupplierSilverSskuTransformationServiceImpl;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmFileHistoryRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmS3FileServiceMock;
import ru.yandex.market.mbo.mdm.common.infrastructure.repository.LbDumpRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.infrastructure.repository.LbMessageQueueRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldenItemPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.BlocksToMasterDataMergerImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MasterDataIntoBlocksSplitterImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ParamValuesToCommonSskuGoldenMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.RslSilverItemSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.SilverCommonSskuSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.SupplierToBusinessSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.WeightDimensionsSilverItemSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenSplitterMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.utils.ValidationContextHelperService;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.BusinessLockStatusRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.CommonParamViewSettingProjectionRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ItemMetricRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MasterDataLogIdService;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmEntityTypeProjectionRepositoryImp;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMboUsersRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMergeSettingsRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmQueueStatisticsRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MercuryHashRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ServiceOfferMigrationRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SupplierDqScoreRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.WarehouseProjectionRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.bmdm.BmdmExternalReferenceProjectionRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldenSskuEntityRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MdmParamIoSettingsRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MdmParamRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuByStorageApiRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuMultistorageRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepositoryParamValueImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.StorageApiSilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.CopyReferenceItemToGoldenEntityQueueImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.LbFailedOfferQueueRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MarkupOfferForDeleteQueueImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MasterDataToSilverParamValuesQRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManagerImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuAndSskuQueueImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToMboQueueRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendReferenceItemQRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToDatacampQRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToErpQueueRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SilverSskuYtStorageQueue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SilverSskuYtStorageQueueImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToDeleteRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToDeleteRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.failed.SendToDatacampQueueFailedRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.failed.old.LbFailedOfferQueueFailedRepositoryOldImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.processed.SendToDatacampQueueProcessedRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.to_process.SendToDatacampQueueToProcessRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.CategoryRslRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.MskuRslRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.SskuRslRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.SupplierRslRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.GoldenSskuResolutionByStorageApiRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SilverSskuResolutionByStorageApiRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.warehouse.MdmWarehouseRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.GlobalParamValueService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.MboMskuUpdateService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.StorageKeyValueCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.WarehouseProjectionCacheImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmBusinessStageSwitcherImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmMergeSettingsCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmMergeSettingsCacheImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManagerImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierNonCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.ExistingCCCodeCacheMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.AddNewItemsServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.IrisEventProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.RslGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.SurplusAndCisGoldenItemServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionForceInheritanceService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionsGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.CachedMetadataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.MetadataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.MdmCommonMskuMboService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CommonSskuFromDataCampConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MasterDataGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmFromDatacampConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmLmsCargoTypeNoCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCacheImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmToDatacampConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuMdmParamExcelExportService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuMdmParamExcelService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuParamEnrichServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ParamIdsForUIProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ParamIdsForUIProviderImplBmdmCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ParamIdsForUIProviderImplMdmRepo;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverterImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ShopSkuKeyExcelService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.SskuMdmParamExcelExportService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.SskuMdmParamExcelImportService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.SupplierToBusinessGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.rsl.RslMarkupsParamsServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.MdmCommonSskuService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.MdmCommonSskuServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.MskuParamValuesForSskuLoaderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.MultivalueBusinessHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.OffersBySupplierTypeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.SskuGoldenMasterDataCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.TraceableSskuGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.TraceableSskuSilverSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingDataProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPipeProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingRawDataLoaderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.CommonSskuResolutionFetcher;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MasterDataValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MasterDataVersionMapServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MdmErrorInfoClarifier;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MdmErrorInfoMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictCalculationByCsHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictCalculationByMdHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictProtoConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.warehouse.MdmWarehouseBaseService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CommonSskuValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.GeneralBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.SskuBlocksServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.VghValidationRequirementsProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.AdditionalValidationBlocksProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.BoxCountValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.BusinessPartBlocksValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.CustomsCommodityCodeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.DeliveryTimeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.DimensionsBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.DocumentRegNumbersBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.GTINValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.GuaranteePeriodBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.LifeTimeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ManufacturerCountriesBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.MasterDataBlocksValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.MinShipmentBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.MskuItemBlockValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.QuantumOfSupplyBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ServicePartBlocksValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ShelfLifeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.TransportUnitBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.VetisGuidsBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.WeightNetValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.WeightTareValidator;
import ru.yandex.market.mbo.mdm.common.priceinfo.PriceInfoRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistantImpl;
import ru.yandex.market.mbo.mdm.common.service.MdmBusinessLockServiceImpl;
import ru.yandex.market.mbo.mdm.common.service.MdmParameterValueCachingServiceMock;
import ru.yandex.market.mbo.mdm.common.service.ServiceOfferMigrationRepositoryCleaningServiceImpl;
import ru.yandex.market.mbo.mdm.common.service.SskuValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.MdmEntityStorageServiceMock;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmBooleanAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmEnumAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmInt64AttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmNumericAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStringAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute.BmdmStructAttributeValuesToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.entity.BmdmEntityToParamValuesConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.entity.BmdmEntityToParamValuesConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.msku.BmdmEntityToCommonMskuConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmEntityTypeToParamsConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku.BmdmEntityToGoldenBusinessOfferConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku.BmdmEntityToGoldenBusinessSskuConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku.BmdmEntityToGoldenServiceSskuConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku.BmdmEntityToSilverBusinessSskuConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku.BmdmEntityToSilverCommonSskuConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku.BmdmEntityToSilverServiceSskuConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.verdict.BmdmVerdictConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.warehouse.BmdmEntityToWarehouseConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.mapping.MappingsUpdateServiceImpl;
import ru.yandex.market.mbo.mdm.common.service.mapping.MdmBestMappingsProviderImpl;
import ru.yandex.market.mbo.mdm.common.service.monitoring.MdmBusinessMigrationMonitoringServiceImpl;
import ru.yandex.market.mbo.mdm.common.service.queue.CopyReferenceItemToGoldenEntityQueueService;
import ru.yandex.market.mbo.mdm.common.service.queue.MdmQueueFailedOffersService;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.ErpExportDataProviderImpl;
import ru.yandex.market.mboc.common.config.KeyValueConfig;
import ru.yandex.market.mboc.common.erp.AlreadySentDataFilterService;
import ru.yandex.market.mboc.common.infrastructure.sql.SavePointTransactionHelper;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.parsing.CommonMskuValidator;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataValidator;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.repository.MskuSyncResultRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.repository.cutoff.OfferCutoffRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsServiceImpl;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffServiceImpl;
import ru.yandex.market.mboc.common.masterdata.services.document.AvatarImageDepotServiceMock;
import ru.yandex.market.mboc.common.masterdata.services.document.DocumentServiceImpl;
import ru.yandex.market.mboc.common.masterdata.services.document.picture.QualityDocumentPictureServiceImpl;
import ru.yandex.market.mboc.common.offers.repository.MboMappingsServiceMock;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsServiceMock;
import ru.yandex.market.mboc.db.config.PostgresSQLBeanPostProcessor;

/**
 * Scan-free configuration for autowiring repositories only (and some related stuff).
 *
 * @author yuramalinov
 * @created 11.10.18
 */
@TestConfiguration
@PropertySource("classpath:db-test.properties")
@Import({
    PostgresSQLBeanPostProcessor.class,
    TestMdmSqlDatasourceConfig.class,
    MasterDataRepositoryImpl.class,
    QualityDocumentRepositoryImpl.class,
    OfferCutoffRepositoryImpl.class,
    CargoTypeRepositoryImpl.class,
    KeyValueConfig.class,
    FromIrisItemRepositoryImpl.class,
    ReferenceItemRepositoryImpl.class,
    MercuryHashRepositoryImpl.class,
    MskuSyncResultRepositoryImpl.class,
    ItemMetricRepositoryImpl.class,
    CategoryParamValueRepositoryImpl.class,
    MdmParamRepositoryImpl.class,
    MdmParamIoSettingsRepositoryImpl.class,
    CustomsCommCodeRepositoryImpl.class,
    WeightDimensionsGoldenItemService.class,
    WeightDimensionForceInheritanceService.class,
    SskuToRefreshRepositoryImpl.class,
    MskuToRefreshRepositoryImpl.class,
    MappingsCacheRepositoryImpl.class,
    WeightDimensionsSilverItemSplitter.class,
    RslMarkupsParamsServiceImpl.class,
    SupplierConverterServiceMock.class,
    MdmWarehouseRepositoryImpl.class,
    WeightDimensionBlockValidationServiceImpl.class,
    WeightDimensionsValidator.class,
    StorageKeyValueCachingService.class,
    SurplusAndCisGoldenItemServiceImpl.class,
    BeruIdMock.class,
    SskuMdmParamExcelExportService.class,
    SskuMdmParamExcelImportService.class,
    MskuMdmParamExcelService.class,
    MskuMdmParamExcelExportService.class,
    MdmFileHistoryRepositoryImpl.class,
    MdmS3FileServiceMock.class,
    MdmSupplierRepositoryImpl.class,
    MboMskuUpdateService.class,
    MboModelsServiceMock.class,
    SupplierDqScoreRepositoryImpl.class,
    MdmSskuGroupManagerImpl.class,
    MdmSupplierNonCachingService.class,
    MdmMergeSettingsRepositoryImpl.class,
    MdmMergeSettingsCacheImpl.class,
    MdmGoodGroupRepositoryImpl.class,
    SendToDatacampQRepositoryImpl.class,
    SendToDatacampQueueToProcessRepositoryImpl.class,
    SendToDatacampQueueProcessedRepositoryImpl.class,
    SendToDatacampQueueFailedRepositoryImpl.class,
    MdmQueuesManagerImpl.class,
    CCCodeValidationService.class,
    MdmToDatacampConverter.class,
    MdmMboUsersRepositoryImpl.class,
    ServiceSskuConverterImpl.class,
    ShopSkuKeyExcelService.class,
    IrisEventProcessor.class,
    ComplexMonitoring.class,
    AddNewItemsServiceImpl.class,
    MdmFromDatacampConverter.class,
    AddNewItemsServiceImpl.class,
    MdmParameterValueCachingServiceMock.class,
    MdmCategorySettingsServiceImpl.class,
    MasterDataValidator.class,
    BmdmVerdictConverterImpl.class,
    SilverSskuResolutionByStorageApiRepositoryImpl.class,
    GoldenSskuResolutionByStorageApiRepositoryImpl.class,
    CommonSskuFromDataCampConverter.class,
    VerdictCalculationByMdHelper.class,
    VerdictCalculationByCsHelper.class,
    MasterDataValidationService.class,
    ExistingCCCodeCacheMock.class,
    GlobalParamValueService.class,
    MdmBusinessStageSwitcherImpl.class,
    MappingsUpdateServiceImpl.class,
    MasterDataLogIdService.class,
    MasterDataVersionMapServiceImpl.class,
    AlreadySentDataFilterService.class,
    LbDumpRepositoryImpl.class,
    MultivalueBusinessHelper.class,
    OfferCutoffServiceImpl.class,
    SskuGoldenParamUtil.class,
    FeatureSwitchingAssistantImpl.class,
    BusinessLockStatusRepositoryImpl.class,
    ServiceOfferMigrationRepositoryImpl.class,
    ServiceOfferMigrationServiceImpl.class,
    MdmBusinessLockServiceImpl.class,
    PriceInfoRepositoryImpl.class,
    SskuValidationServiceImpl.class,
    ValidationContextHelperService.class,
    MboMappingsServiceMock.class,
    CachedItemBlockValidationContextProviderImpl.class,
    MdmBusinessMigrationMonitoringServiceImpl.class,
    ServiceOfferMigrationRepositoryCleaningServiceImpl.class,
    MskuParamValuesForSskuLoaderImpl.class,
    SskuProcessingRawDataLoaderImpl.class,
    SskuProcessingDataProviderImpl.class,
    ShelfLifeBlockValidator.class,
    LifeTimeBlockValidator.class,
    GuaranteePeriodBlockValidator.class,
    BoxCountValidator.class,
    CustomsCommodityCodeBlockValidator.class,
    DeliveryTimeBlockValidator.class,
    GTINValidator.class,
    ManufacturerCountriesBlockValidator.class,
    MinShipmentBlockValidator.class,
    QuantumOfSupplyBlockValidator.class,
    TransportUnitBlockValidator.class,
    VetisGuidsBlockValidator.class,
    DocumentRegNumbersBlockValidator.class,
    MasterDataIntoBlocksSplitterImpl.class,
    BlocksToMasterDataMergerImpl.class,
    MasterDataBlocksValidationService.class,
    ServicePartBlocksValidationService.class,
    MskuGoldenSplitterMerger.class,
    MskuItemBlockValidationService.class,
    CommonMskuValidator.class,
    VghValidationRequirementsProviderImpl.class,
    SendReferenceItemQRepositoryImpl.class,
    BusinessPartBlocksValidationService.class,
    SskuRslRepositoryImpl.class,
    MskuRslRepositoryImpl.class,
    CategoryRslRepositoryImpl.class,
    SupplierRslRepositoryImpl.class,
    LbMessageQueueRepositoryImpl.class,
    LbFailedOfferQueueRepositoryImpl.class,
    MdmAutoMarkupYqlQueryRepositoryImpl.class,
    MdmAutoMarkupHistoryRepositoryImpl.class,
    MskuToMboQueueRepositoryImpl.class,
    SskuExistenceRepositoryImpl.class,
    OffersBySupplierTypeService.class,
    MdmParamCacheImpl.class,
    MdmEntityTypeProjectionRepositoryImp.class,
    CommonParamViewSettingProjectionRepositoryImpl.class,
    CachedMetadataProvider.class,
    ParamIdsForUIProviderImplBmdmCache.class,
    ParamIdsForUIProviderImplMdmRepo.class,
    ParamIdsForUIProviderImpl.class,
    MdmParamProviderImpl.class,
    MskuSilverItemPreProcessor.class,
    MskuParamEnrichServiceImpl.class,
    SilverSskuYtStorageQueueImpl.class,
    BmdmExternalReferenceProjectionRepositoryImpl.class,
    CommonSskuResolutionFetcher.class,
    BmdmEntityTypeToParamsConverterImpl.class,
    BmdmAttributeToMdmParamConverterImpl.class,
    VerdictProtoConverter.class,
    MdmErrorInfoMerger.class,
    MdmErrorInfoClarifier.class,
    MdmLmsCargoTypeNoCache.class,
    SendToErpQueueRepositoryImpl.class,
    MskuAndSskuQueueImpl.class,
    MdmQueueStatisticsRepositoryImpl.class,
    LbFailedOfferQueueFailedRepositoryOldImpl.class,
    MdmQueueFailedOffersService.class,
    MasterDataToSilverParamValuesQRepositoryImpl.class,
    WarehouseProjectionRepositoryImpl.class,
    BmdmEntityToWarehouseConverterImpl.class,
    MskuByStorageApiRepositoryImpl.class,
    BmdmEntityToCommonMskuConverterImpl.class,
    MasterDataGoldenItemService.class,
    WarehouseProjectionCacheImpl.class,
    BmdmEntityToSilverCommonSskuConverterImpl.class,
    BmdmEntityToSilverBusinessSskuConverterImpl.class,
    BmdmEntityToSilverServiceSskuConverterImpl.class,
    ErpExportDataProviderImpl.class,
    DatacampOffersFiltrator.class,
    TraceableSskuSilverSplitter.class,
    TraceableSskuGoldenItemService.class,
    MdmBestMappingsProviderImpl.class,
    SupplierSilverSskuTransformationServiceImpl.class,
    StorageApiSilverSskuRepository.class,
    GoldenSskuEntityRepositoryImpl.class,
    BmdmEntityToGoldenBusinessOfferConverterImpl.class,
    BmdmEntityToGoldenServiceSskuConverterImpl.class,
    BmdmEntityToGoldenBusinessSskuConverterImpl.class,
    MdmWarehouseBaseService.class,
    MarkupOfferForDeleteQueueImpl.class,
    MdmWarehouseBaseService.class,
    CategoryCachingServiceMock.class,
    SskuProcessingPipeProcessorImpl.class,
    SskuGoldenMasterDataCalculationHelper.class,
    SskuBlocksServiceImpl.class,
    CopyReferenceItemToGoldenEntityQueueImpl.class,
    SskuBlocksServiceImpl.class,
    AvatarImageDepotServiceMock.class,
    QualityDocumentPictureServiceImpl.class,
    DocumentServiceImpl.class,
    CopyReferenceItemToGoldenEntityQueueImpl.class,
    CopyReferenceItemToGoldenEntityQueueService.class,
    DimensionsBlockValidator.class,
    WeightTareValidator.class,
    WeightNetValidator.class,
    CommonSskuValidator.class,
    AdditionalValidationBlocksProvider.class,
})

public class MdmDbTestConfiguration {
    @Bean
    @Primary
    public TransactionHelper commonTransactionHelper(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return new SavePointTransactionHelper(transactionTemplate);
    }

    /**
     * Нужен для ситуаций, когда надо гарантировать новую транзакцию.
     */
    @Qualifier("newTransaction")
    @Bean
    public TransactionHelper newTransactionHelper(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new SavePointTransactionHelper(transactionTemplate);
    }

    @Bean
    public BeanPostProcessor postgresSQLBeanPostProcessor() {
        return new PostgresSQLBeanPostProcessor();
    }

    @Bean
    public MasterDataBusinessMergeService masterDataBusinessMergeService(
        StorageKeyValueService storageKeyValueService,
        MdmParamCache paramCache,
        MdmMergeSettingsCache mergeSettingsCache,
        FeatureSwitchingAssistant featureSwitchingAssistant
    ) {
        return new MasterDataBusinessMergeServiceImpl(
            storageKeyValueService,
            new SupplierToBusinessGoldenItemService<ServiceSsku>(
                paramValuesToCommonSskuGoldenMerger(),
                supplierToBusinessSplitter(paramCache),
                supplierToBusinessSplitter(paramCache),
                mergeSettingsCache,
                featureSwitchingAssistant
            ), new SupplierToBusinessGoldenItemService<SilverServiceSsku>(
            paramValuesToCommonSskuGoldenMerger(),
            supplierToBusinessSplitter(paramCache),
            supplierToBusinessSplitter(paramCache),
            mergeSettingsCache,
            featureSwitchingAssistant
        ));
    }

    @Bean
    @Primary
    public MasterDataGoldenItemService masterDataGoldenItemService(
        MdmParamCache paramCache,
        ShelfLifeBlockValidator shelfLifeBlockValidator,
        FeatureSwitchingAssistant featureSwitchingAssistant
    ) {
        return new MasterDataGoldenItemService(
            paramValuesToCommonSskuGoldenMerger(),
            silverCommonSskuSplitter(paramCache),
            supplierToBusinessSplitter(paramCache),
            new GeneralBlockValidationServiceImpl(shelfLifeBlockValidator),
            featureSwitchingAssistant
        );
    }

    @Bean
    public ParamValuesToCommonSskuGoldenMerger paramValuesToCommonSskuGoldenMerger() {
        return new ParamValuesToCommonSskuGoldenMerger();
    }

    @Bean
    public SilverCommonSskuSplitter silverCommonSskuSplitter(MdmParamCache paramCache) {
        return new SilverCommonSskuSplitter(
            supplierToBusinessSplitter(paramCache));
    }

    @Bean
    public SupplierToBusinessSplitter supplierToBusinessSplitter(MdmParamCache paramCache) {
        return new SupplierToBusinessSplitter(paramCache);
    }

    @Bean
    public GoldenItemPostProcessor<ReferenceItemWrapper> goldenItemPostProcessor() {
        return GoldenItemPostProcessor.doNothingPostProcessor();
    }

    @Bean
    @Primary
    public MdmSupplierCachingService mdmSupplierCachingService(
        MdmSupplierRepository mdmSupplierRepository,
        StorageKeyValueService storageKeyValueService
    ) {
        return new MdmSupplierNonCachingService(mdmSupplierRepository, storageKeyValueService);
    }

    @Bean
    public RslGoldenItemService rslGoldenItemService(FeatureSwitchingAssistant featureSwitchingAssistant) {
        return new RslGoldenItemService(new RslSilverItemSplitter(), featureSwitchingAssistant);
    }

    @Bean
    public BmdmEntityToParamValuesConverter bmdmEntityToParamValuesConverter(
        MetadataProvider metadataProvider,
        BmdmAttributeToMdmParamConverter bmdmAttributeToMdmParamConverter
    ) {
        BmdmEntityToParamValuesConverterImpl bmdmEntityToParamValuesConverter =
            new BmdmEntityToParamValuesConverterImpl(metadataProvider);
        bmdmEntityToParamValuesConverter.updateAttributeConverters(List.of(
            new BmdmStructAttributeValuesToParamValuesConverter(bmdmEntityToParamValuesConverter),
            new BmdmEnumAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmStringAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmBooleanAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmInt64AttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter),
            new BmdmNumericAttributeValuesToParamValuesConverter(bmdmAttributeToMdmParamConverter)
        ));
        return bmdmEntityToParamValuesConverter;
    }

    @Bean
    public MdmCommonSskuService mdmCommonSskuService(MdmSskuGroupManager mdmSskuGroupManager,
                                                     MasterDataBusinessMergeService masterDataBusinessMergeService) {
        return new MdmCommonSskuServiceImpl(
            null, null, null, null, null, null, null, null,
            mdmSskuGroupManager, masterDataBusinessMergeService,
            null, null, null, null, null, null, null, null, null, null
        );
    }

    @Bean
    public SskuMdmParamExcelImportService sskuMdmParamExcelImportService(MdmParamProvider mdmParamProvider,
                                                                         MdmCommonSskuService mdmCommonSskuService) {
        return new SskuMdmParamExcelImportService(mdmParamProvider, mdmCommonSskuService,
            Mockito.mock(MdmDatacampService.class));
    }

    @Bean
    public MdmCommonMskuMboService mdmCommonMskuMboService() {
        return Mockito.mock(MdmCommonMskuMboService.class);
    }

    @Bean
    public MdmDatacampService mdmDatacampService() {
        return new MdmDatacampServiceMock();
    }

    @Bean
    public SilverSskuRepositoryParamValueImpl silverSskuRepositoryParamValueImpl(
        NamedParameterJdbcTemplate jdbcTemplate,
        TransactionTemplate transactionTemplate,
        MdmSskuGroupManager mdmSskuGroupManager
    ) {
        return new SilverSskuRepositoryParamValueImpl(
            jdbcTemplate,
            transactionTemplate,
            mdmSskuGroupManager
        );
    }

    @Bean
    @Primary
    public SilverSskuRepository silverSskuRepository(SilverSskuRepositoryParamValueImpl pgRepository,
                                                     StorageApiSilverSskuRepository ytRepository,
                                                     SilverSskuYtStorageQueue silverSskuYtStorageQueue,
                                                     StorageKeyValueService storageKeyValueService,
                                                     TransactionTemplate transactionTemplate) {
        return new SilverSskuMultistorageRepository(
            pgRepository,
            ytRepository,
            silverSskuYtStorageQueue,
            storageKeyValueService,
            transactionTemplate
        );
    }

    @Bean
    public MdmEntityStorageServiceMock mdmEntityStorageService() {
        return new MdmEntityStorageServiceMock();
    }

    @Bean
    @Primary
    public SskuToDeleteRepository sskuToDeleteRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        TransactionTemplate transactionTemplate,
        StorageKeyValueService storageKeyValueService
    ) {
        return new SskuToDeleteRepositoryImpl(jdbcTemplate,
            transactionTemplate,
            0,
            storageKeyValueService);
    }

    @Primary
    @Bean
    public VerdictCalculationHelper verdictCalculationHelper(VerdictCalculationByCsHelper verdictCalculationByCsHelper) {
        return verdictCalculationByCsHelper;
    }
}
