package ru.yandex.market.pricelabs.tms.processing;

import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.Autostrategy;
import ru.yandex.market.pricelabs.model.AutostrategyHistory;
import ru.yandex.market.pricelabs.model.AutostrategyOfferSource;
import ru.yandex.market.pricelabs.model.AutostrategyOfferTarget;
import ru.yandex.market.pricelabs.model.AutostrategyShopState;
import ru.yandex.market.pricelabs.model.AutostrategyState;
import ru.yandex.market.pricelabs.model.AutostrategyStateHistory;
import ru.yandex.market.pricelabs.model.BlueBidsRecommendation;
import ru.yandex.market.pricelabs.model.BrandBusiness;
import ru.yandex.market.pricelabs.model.Business;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.MarketCategory;
import ru.yandex.market.pricelabs.model.MbiContactInfo;
import ru.yandex.market.pricelabs.model.ModelbidsRecommendation;
import ru.yandex.market.pricelabs.model.NewBlueBidsRecommendation;
import ru.yandex.market.pricelabs.model.NewBrandModelId;
import ru.yandex.market.pricelabs.model.NewBusiness;
import ru.yandex.market.pricelabs.model.NewModelbidsRecommendation;
import ru.yandex.market.pricelabs.model.NewOfferGen;
import ru.yandex.market.pricelabs.model.NewShopCategory;
import ru.yandex.market.pricelabs.model.NewShopsDat;
import ru.yandex.market.pricelabs.model.NewVendorBrandMap;
import ru.yandex.market.pricelabs.model.NewVendorDatasource;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.OfferVendor;
import ru.yandex.market.pricelabs.model.Shop;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.model.ShopsDat;
import ru.yandex.market.pricelabs.model.SourceMbiContactInfo;
import ru.yandex.market.pricelabs.model.VendorBrandMap;
import ru.yandex.market.pricelabs.model.VendorDatasource;
import ru.yandex.market.pricelabs.model.VendorModelBid;
import ru.yandex.market.pricelabs.model.program.AdvProgramActivationRequest;
import ru.yandex.market.pricelabs.model.program.AdvProgramBonus;
import ru.yandex.market.pricelabs.model.program.NewPartnerBusiness;
import ru.yandex.market.pricelabs.model.program.NewPartnerProgram;
import ru.yandex.market.pricelabs.model.program.NewProgramRecommendationSettings;
import ru.yandex.market.pricelabs.model.program.PartnerBusiness;
import ru.yandex.market.pricelabs.model.program.PartnerProgram;
import ru.yandex.market.pricelabs.model.program.ProgramRecommendationSettings;
import ru.yandex.market.pricelabs.model.recommendation.FeeRecommendation;
import ru.yandex.market.pricelabs.model.recommendation.FeeRecommendationSource;
import ru.yandex.market.pricelabs.model.recommendation.NewFeeRecommendationSource;
import ru.yandex.market.pricelabs.model.recommendation.NewOfferRecommendation;
import ru.yandex.market.pricelabs.model.recommendation.NewPriceRecommendation;
import ru.yandex.market.pricelabs.model.recommendation.OfferRecommendation;
import ru.yandex.market.pricelabs.model.recommendation.PriceRecommendation;
import ru.yandex.market.pricelabs.model.recommendation.PriceRecommendationsSource;
import ru.yandex.market.pricelabs.processing.CoreTables;
import ru.yandex.market.pricelabs.processing.autostrategies.AutostrategiesMetaProcessor;
import ru.yandex.market.pricelabs.processing.monetization.model.AdvCampaign;
import ru.yandex.market.pricelabs.processing.monetization.model.AdvCampaignHistory;
import ru.yandex.market.pricelabs.processing.monetization.model.OfferBids;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AutostrategiesShopOfferProcessor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AutostrategiesShopStateProcessor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AutostrategiesStateProcessor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.offers.AutostrategiesVendorModelBidProcessor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.offers.AutostrategiesVendorOfferSourceProcessor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.offers.AutostrategiesVendorOfferTargetProcessor;
import ru.yandex.market.pricelabs.tms.processing.categories.CategoriesProcessor;
import ru.yandex.market.pricelabs.tms.processing.categories.recommendations.CategoryRecommendationsProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.BusinessProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.FiltersProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.MbiContactInfoProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.PartnerBusinessProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.ShopsDatProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.ShopsProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.VendorBrandMapProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.VendorDatasourcesProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.program.PartnerProgramProcessor;
import ru.yandex.market.pricelabs.tms.processing.modelbids.ModelbidsRecommendationImportProcessor;
import ru.yandex.market.pricelabs.tms.processing.modelbids.ModelbidsRecommendationSyncProcessor;
import ru.yandex.market.pricelabs.tms.processing.offers.BrandBusinessProcessor;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersProcessor;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersVendorsProcessor;
import ru.yandex.market.pricelabs.tms.processing.recommendations.OffersRecommendationsProcessor;
import ru.yandex.market.pricelabs.tms.processing.recommendations.fee.FeeRecommendationsProcessor;
import ru.yandex.market.pricelabs.tms.processing.recommendations.fee.NewFeeRecommendationsProcessor;
import ru.yandex.market.pricelabs.tms.processing.recommendations.price.NewPriceRecommendationsProcessor;
import ru.yandex.market.pricelabs.tms.processing.recommendations.price.PriceRecommendationsProcessor;
import ru.yandex.market.pricelabs.tms.processing.stats.BlueBidsRecommenderProcessor;
import ru.yandex.market.pricelabs.yt.YtConfiguration;
import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.binding.ProcessorCfg;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.market.yt.client.YtClientProxySource;

import static ru.yandex.market.pricelabs.misc.TimingUtils.getInstant;
import static ru.yandex.market.pricelabs.tms.processing.TmsTables.OFFERS_GEN_INDEX_NAME;
import static ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor.ObjectModification.matched;

public class ExecutorSources {

    @Autowired
    private OffersProcessor offersProcessor;

    @Autowired
    private CategoriesProcessor categoriesProcessor;

    @Autowired
    private SourceTargetProcessorCfg<MarketCategory, ShopCategory> marketCategoryCfg;

    @Autowired
    private CategoryRecommendationsProcessor categoryRecommendationsProcessor;

    @Autowired
    private ShopsProcessor shopsProcessor;

    @Autowired
    private FiltersProcessor filtersProcessor;

    @Autowired
    @Qualifier("offersVendorsProcessorWhite")
    private OffersVendorsProcessor offersVendorsProcessorWhite;

    @Autowired
    @Qualifier("offersVendorsProcessorBlue")
    private OffersVendorsProcessor offersVendorsProcessorBlue;

    @Autowired
    private BrandBusinessProcessor brandBusinessProcessor;

    @Autowired
    AutostrategiesVendorModelBidProcessor autostrategiesVendorModelBidProcessor;

    @Autowired
    private ModelbidsRecommendationImportProcessor modelbidsRecommendationImportProcessor;

    @Autowired
    private ModelbidsRecommendationSyncProcessor modelbidsRecommendationSyncProcessor;

    @Autowired
    private BlueBidsRecommenderProcessor blueBidsRecommenderProcessor;

    @Autowired
    private OffersRecommendationsProcessor offersRecommendationsProcessor;

    @Autowired
    private VendorDatasourcesProcessor vendorDatasourcesProcessor;

    @Autowired
    private VendorBrandMapProcessor vendorBrandMapProcessor;

    @Autowired
    @Qualifier("autostrategiesOfferShopProcessorWhite")
    private AutostrategiesShopOfferProcessor autostrategiesOfferProcessorWhite;

    @Autowired
    @Qualifier("autostrategiesOfferShopProcessorBlue")
    private AutostrategiesShopOfferProcessor autostrategiesOfferShopProcessorBlue;

    @Autowired
    private AutostrategiesVendorOfferSourceProcessor autostrategiesVendorOfferSourceProcessor;

    @Autowired
    private AutostrategiesVendorOfferTargetProcessor autostrategiesVendorOfferTargetProcessor;

    @Autowired
    @Qualifier("autostrategiesStateWhite")
    private AutostrategiesStateProcessor autostrategiesStateWhite;

    @Autowired
    @Qualifier("autostrategiesMetaVendorBlue")
    private AutostrategiesMetaProcessor autostrategiesMetaVendorBlueProcessor;

    @Autowired
    @Qualifier("autostrategiesStateBlue")
    private AutostrategiesStateProcessor autostrategiesStateBlue;

    @Autowired
    @Qualifier("autostrategiesStateVendorBlue")
    private AutostrategiesStateProcessor autostrategiesStateVendorBlue;

    @Autowired
    @Qualifier("autostrategiesShopStateProcessorWhite")
    private AutostrategiesShopStateProcessor autostrategiesShopStateProcessorWhite;

    @Autowired
    @Qualifier("autostrategiesShopStateProcessorBlue")
    private AutostrategiesShopStateProcessor autostrategiesShopStateProcessorBlue;

    @Autowired
    @Qualifier("autostrategiesShopStateProcessorVendorBlue")
    private AutostrategiesShopStateProcessor autostrategiesShopStateProcessorVendorBlue;

    @Autowired
    private ShopsDatProcessor shopsDatProcessor;

    @Autowired
    private BusinessProcessor businessProcessor;

    @Autowired
    private PartnerProgramProcessor partnerProgramProcessor;

    @Autowired
    private PartnerBusinessProcessor partnerBusinessProcessor;

    @Autowired
    private MbiContactInfoProcessor mbiContactInfoProcessor;

    @Autowired
    private PriceRecommendationsProcessor priceRecommendationsProcessor;

    @Autowired
    private FeeRecommendationsProcessor feeRecommendationsProcessor;

    @Autowired
    private NewPriceRecommendationsProcessor newPriceRecommendationsProcessor;

    @Autowired
    private NewFeeRecommendationsProcessor newFeeRecommendationsProcessor;

    @Autowired
    private CoreTables core;

    @Autowired
    private YtConfiguration configuration;

    @Autowired
    @Qualifier("targetYtSource")
    private YtClientProxySource targetYt;

    @Autowired
    private TmsTables tms;

    @Value("${pricelabs.target.yt.table.batchSize}")
    private int batchSize;

    @Nullable
    private volatile String sourceOffersTable;
    @Nullable
    private volatile String sourceGenerationTable;
    @Nullable
    private volatile String sourceIndexerName;
    @Nullable
    private volatile String day;
    @Nullable
    private volatile String month;

    public void resetDefaults() {
        sourceOffersTable = "none";
        sourceGenerationTable = TmsTestUtils.DEFAULT_GENERATION;
        sourceIndexerName = TmsTestUtils.DEFAULT_INDEXER_NAME;
        day = "2019-07-01";
        month = "2019-07";
    }

    public void clearDefaults() {
        day = null;
        month = null;
    }

    @Nullable
    public String getSourceIndexerName() {
        return sourceIndexerName;
    }

    @Nullable
    public String getSourceOffersTable() {
        return sourceOffersTable;
    }

    @Nullable
    public String getSourceGenerationTable() {
        return sourceGenerationTable;
    }

    public void setSourceIndexerName(String sourceIndexerName) {
        this.sourceIndexerName = sourceIndexerName;
    }

    public void setSourceOffersTable(String sourceOffersTable) {
        this.sourceOffersTable = sourceOffersTable;
    }

    public void setSourceGenerationTable(String sourceGenerationTable) {
        this.sourceGenerationTable = sourceGenerationTable;
    }

    @Nullable
    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    @Nullable
    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    @Nullable
    public String getToday() {
        return Utils.formatToDate(getInstant());
    }

    public YtSourceTargetScenarioExecutor<NewShopCategory, ShopCategory> categories() {
        return YtSourceTargetScenarioExecutor.from(categoriesProcessor.getCfg(), matched(),
                this::getSourceGenerationTable, Map.of(OFFERS_GEN_INDEX_NAME,
                        Objects.requireNonNull(sourceIndexerName)));
    }

    public YtScenarioExecutor<ShopCategory> blueCategories() {
        return YtScenarioExecutor.from(categoriesProcessor.getBlueCfg());
    }

    public YtScenarioExecutor<Shop> shop() {
        return YtScenarioExecutor.from(shopsProcessor.getCfg());
    }

    public YtScenarioExecutor<Filter> filters() {
        return YtScenarioExecutor.from(filtersProcessor.getCfg());
    }

    public YtScenarioExecutor<Offer> offers() {
        return new YtScenarioExecutor<>(offersProcessor.getCfg(), matched()) {

            @Override
            public Offer asCreated(Offer obj) {
                obj.setStrategy_updated_at(getInstant());
                return super.asCreated(obj);
            }

            @Override
            public Offer asUpdated(Offer obj) {
                obj.setStrategy_updated_at(getInstant());
                return super.asUpdated(obj);
            }
        };
    }

    public YtSourceTargetScenarioExecutor<NewOfferGen, Offer> offersGen() {
        return new YtSourceTargetScenarioExecutor<>(offersProcessor.getCfg(),
                matched(), this::getSourceGenerationTable, Map.of(OFFERS_GEN_INDEX_NAME,
                Objects.requireNonNull(sourceIndexerName))) {

            @Override
            public Offer asCreated(Offer obj) {
                //obj.setStrategy_updated_at(getInstant());
                return super.asCreated(obj);
            }

            @Override
            public Offer asUpdated(Offer obj) {
                //obj.setStrategy_updated_at(getInstant());
                return super.asUpdated(obj);
            }
        };
    }

    public YtSourceTargetScenarioExecutor<NewOfferGen, Offer> blueOffersGen() {
        return new YtSourceTargetScenarioExecutor<>(
                new SourceTargetProcessorCfg<>(
                        offersProcessor.getCfg().getSourceClusters(),
                        offersProcessor.getCfg().getSourcePath(),
                        YTBinder.getStaticBinder(offersProcessor.getCfg().getSourcePath().getClazz()),
                        configuration.getTargetYtSource(),
                        offersBlue().getTable(),
                        offersBlue().getBinder(),
                        configuration.getBatchSize()
                ),
                matched(), this::getSourceGenerationTable, Map.of(OFFERS_GEN_INDEX_NAME,
                Objects.requireNonNull(sourceIndexerName))) {

            @Override
            public Offer asCreated(Offer obj) {
                //obj.setStrategy_updated_at(getInstant());
                return super.asCreated(obj);
            }

            @Override
            public Offer asUpdated(Offer obj) {
                //obj.setStrategy_updated_at(getInstant());
                return super.asUpdated(obj);
            }
        };
    }

    public YtScenarioExecutor<Offer> offersBlue() {
        return new YtScenarioExecutor<>(offersProcessor.getOffersBlueCfg(), matched()) {

            @Override
            public Offer asCreated(Offer obj) {
                obj.setStrategy_updated_at(getInstant());
                return super.asCreated(obj);
            }

            @Override
            public Offer asUpdated(Offer obj) {
                obj.setStrategy_updated_at(getInstant());
                return super.asUpdated(obj);
            }
        };
    }

    public YtScenarioExecutor<OfferVendor> offerVendors() {
        return YtScenarioExecutor.from(offersVendorsProcessorWhite.getCfg());
    }

    public YtScenarioExecutor<OfferVendor> offerBlueVendor() {
        return YtScenarioExecutor.from(offersVendorsProcessorBlue.getCfg());
    }

    public YtScenarioExecutor<BrandBusiness> brandBusiness() {
        return YtScenarioExecutor.from(brandBusinessProcessor.getCfg());
    }

    public YtScenarioExecutor<VendorModelBid> vendorModelBid() {
        return YtScenarioExecutor.from(autostrategiesVendorModelBidProcessor.getCfg());
    }

    public YtSourceTargetScenarioExecutor<NewBrandModelId, NewModelbidsRecommendation> modelBidsImport() {
        return YtSourceTargetScenarioExecutor.from(modelbidsRecommendationImportProcessor.getCfg(), matched());
    }

    public YtScenarioExecutor<ModelbidsRecommendation> modelBids() {
        return YtScenarioExecutor.from(modelbidsRecommendationSyncProcessor.getCfg());
    }

    public YtSourceTargetScenarioExecutor<NewBlueBidsRecommendation, BlueBidsRecommendation> blueBidsRecommender() {
        return YtSourceTargetScenarioExecutor.from(blueBidsRecommenderProcessor.getCfg(), matched());
    }

    public YtSourceTargetScenarioExecutor<NewOfferRecommendation, OfferRecommendation> offersRecommender() {
        return YtSourceTargetScenarioExecutor.from(offersRecommendationsProcessor.getCfg(), matched());
    }

    public YtSourceTargetScenarioExecutor<NewVendorDatasource, VendorDatasource> vendorDatasources() {
        return YtSourceTargetScenarioExecutor.from(vendorDatasourcesProcessor.getCfg(), matched());
    }

    public YtSourceTargetScenarioExecutor<NewVendorBrandMap, VendorBrandMap> vendorBrandMap() {
        return YtSourceTargetScenarioExecutor.from(vendorBrandMapProcessor.getCfg(), matched());
    }

    public YtScenarioExecutor<Autostrategy> vendorBlueAutostrategies() {
        return YtScenarioExecutor.from(autostrategiesMetaVendorBlueProcessor.getCfg());
    }

    public YtScenarioExecutor<AutostrategyOfferTarget> autostrategyOffersWhite() {
        return YtScenarioExecutor.from(autostrategiesOfferProcessorWhite.getCfg());
    }

    public YtScenarioExecutor<AutostrategyState> autostrategiesStateWhite() {
        return YtScenarioExecutor.from(autostrategiesStateWhite.getCfg());
    }

    public YtScenarioExecutor<AutostrategyStateHistory> autostrategiesStateHistoryWhite() {
        return YtScenarioExecutor.from(autostrategiesStateWhite.getHistoryCfg());
    }

    public YtScenarioExecutor<AutostrategyOfferTarget> autostrategyOffersBlue() {
        return YtScenarioExecutor.from(autostrategiesOfferShopProcessorBlue.getCfg());
    }

    public YtScenarioExecutor<AutostrategyState> autostrategiesStateBlue() {
        return YtScenarioExecutor.from(autostrategiesStateBlue.getCfg());
    }

    public YtScenarioExecutor<AutostrategyState> autostrategiesStateVendorBlue() {
        return YtScenarioExecutor.from(autostrategiesStateVendorBlue.getCfg());
    }

    public YtScenarioExecutor<AutostrategyStateHistory> autostrategiesStateHistoryBlue() {
        return YtScenarioExecutor.from(autostrategiesStateBlue.getHistoryCfg());
    }

    public YtScenarioExecutor<AutostrategyStateHistory> autostrategiesStateHistoryVendorBlue() {
        return YtScenarioExecutor.from(autostrategiesStateVendorBlue.getHistoryCfg());
    }

    public YtScenarioExecutor<AutostrategyOfferSource> autostrategyOffersVendorSource() {
        return YtScenarioExecutor.from(autostrategiesVendorOfferSourceProcessor.getCfg());
    }

    public YtScenarioExecutor<AutostrategyOfferTarget> autostrategyOffersVendorTarget() {
        return YtScenarioExecutor.from(autostrategiesVendorOfferTargetProcessor.getCfg());
    }

    public YtScenarioExecutor<AutostrategyShopState> autostrategiesShopStateWhite() {
        return YtScenarioExecutor.from(autostrategiesShopStateProcessorWhite.getCfg());
    }

    public YtScenarioExecutor<AutostrategyShopState> autostrategiesShopStateBlue() {
        return YtScenarioExecutor.from(autostrategiesShopStateProcessorBlue.getCfg());
    }

    public YtScenarioExecutor<AutostrategyShopState> autostrategiesShopStateVendorBlue() {
        return YtScenarioExecutor.from(autostrategiesShopStateProcessorVendorBlue.getCfg());
    }

    public YtSourceTargetScenarioExecutor<MarketCategory, ShopCategory> marketCategories() {
        return YtSourceTargetScenarioExecutor.from(marketCategoryCfg, matched());
    }

    public YtSourceTargetScenarioExecutor<NewBusiness, Business> business() {
        return YtSourceTargetScenarioExecutor.from(businessProcessor.getCfg(), matched());
    }

    public YtSourceTargetScenarioExecutor<NewPartnerProgram,
            PartnerProgram> partner() {
        return YtSourceTargetScenarioExecutor.from(partnerProgramProcessor.getCfg(), matched());
    }

    public YtSourceTargetScenarioExecutor<NewPartnerBusiness,
            PartnerBusiness> partnerBusiness() {
        return YtSourceTargetScenarioExecutor.from(partnerBusinessProcessor.getCfg(), matched());
    }

    public YtSourceTargetScenarioExecutor<NewProgramRecommendationSettings, ProgramRecommendationSettings>
    categoryRecommendations() {
        return YtSourceTargetScenarioExecutor.from(categoryRecommendationsProcessor.getCfg(), matched());
    }

    public YtSourceTargetScenarioExecutor<NewShopsDat, ShopsDat> shopsDat() {
        return YtSourceTargetScenarioExecutor.from(shopsDatProcessor.getCfg(), matched(),
                this::getSourceGenerationTable, Map.of(OFFERS_GEN_INDEX_NAME,
                        Objects.requireNonNull(sourceIndexerName)));
    }

    public YtScenarioExecutor<Autostrategy> whiteAutostrategiesExecutor() {
        return YtScenarioExecutor.from(cfg(core.getAutostrategiesTable()));
    }

    public YtScenarioExecutor<AutostrategyHistory> autostrategiesHistoryExecutor() {
        return YtScenarioExecutor.from(cfg(core.getAutostrategiesHistoryTable()));
    }


    public YtScenarioExecutor<Autostrategy> blueAutostrategiesExecutor() {
        return YtScenarioExecutor.from(cfg(core.getBlueAutostrategiesTable()));
    }

    public YtScenarioExecutor<PartnerBusiness> partnerBusinessExecutor() {
        return YtScenarioExecutor.from(cfg(core.getPartnerBusinessTable()));
    }


    public YtScenarioExecutor<PartnerProgram> partnerTypeBusinessExecutor() {
        return YtScenarioExecutor.from(cfg(core.getPartnerProgramTable()));
    }

    private <T> ProcessorCfg<T> cfg(BindingTable<T> bind) {
        return configuration.getProcessorCfg(bind);
    }

    public YtScenarioExecutor<AdvProgramBonus> bonusExecutor() {
        return YtScenarioExecutor.from(cfg(core.getAdvProgramBonusTable()));
    }

    public YtScenarioExecutor<AdvProgramActivationRequest> activationRequestExecutor() {
        return YtScenarioExecutor.from(cfg(core.getAdvProgramActivationRequest()));
    }

    public YtScenarioExecutor<AdvCampaign> advCampaignExecutor() {
        return YtScenarioExecutor.from(cfg(core.getAdvCampaignTable()));
    }

    public YtScenarioExecutor<AdvCampaignHistory> advCampaignHistoryExecutor() {
        return YtScenarioExecutor.from(cfg(core.getAdvCampaignHistoryTable()));
    }

    public YtScenarioExecutor<OfferBids> advOfferBidsYtScenarioExecutor() {
        return YtScenarioExecutor.from(cfg(core.getOfferBidsTable()));
    }

    public YtScenarioExecutor<AutostrategyOfferTarget> blueAutostrategyOffersYtScenarioExecutor() {
        return YtScenarioExecutor.from(cfg(core.getBlueAutostrategyOffersShopTable()));
    }

    public YtSourceTargetScenarioExecutor<SourceMbiContactInfo, MbiContactInfo> mbiContactInfoExecutor() {
        return YtSourceTargetScenarioExecutor.from(mbiContactInfoProcessor.getCfg(), matched());
    }

    public YtSourceTargetScenarioExecutor<NewPriceRecommendation, PriceRecommendation> priceRecommendations() {
        return YtSourceTargetScenarioExecutor.from(priceRecommendationsProcessor.getCfg(), matched());
    }

    public YtSourceTargetScenarioExecutor<FeeRecommendationSource, FeeRecommendation> feeRecommendations() {
        return YtSourceTargetScenarioExecutor.from(feeRecommendationsProcessor.getCfg(), matched());
    }

    public YtSourceTargetDiffScenarioExecutor<PriceRecommendationsSource,
            PriceRecommendation> priceRecommendationsExecutor1() {
        return YtSourceTargetDiffScenarioExecutor.from(newPriceRecommendationsProcessor.getCfg(), matched(),
                () -> "2022-07-12T12:00:00.123");
    }

    public YtSourceTargetDiffScenarioExecutor<PriceRecommendationsSource,
            PriceRecommendation> priceRecommendationsExecutor2() {
        return YtSourceTargetDiffScenarioExecutor.from(newPriceRecommendationsProcessor.getCfg(), matched(),
                () -> "2022-07-12T13:00:00.123");
    }

    public YtSourceTargetDiffScenarioExecutor<PriceRecommendationsSource,
            PriceRecommendation> priceRecommendationsExecutor3() {
        return YtSourceTargetDiffScenarioExecutor.from(newPriceRecommendationsProcessor.getCfg(), matched(),
                () -> "2022-07-12T14:00:00.123");
    }

    public YtSourceTargetDiffScenarioExecutor<NewFeeRecommendationSource,
            FeeRecommendation> feeRecommendationsExecutor1() {
        return YtSourceTargetDiffScenarioExecutor.from(newFeeRecommendationsProcessor.getCfg(), matched(),
                () -> "2022-07-12T12:00:00.123");
    }

    public YtSourceTargetDiffScenarioExecutor<NewFeeRecommendationSource,
            FeeRecommendation> feeRecommendationsExecutor2() {
        return YtSourceTargetDiffScenarioExecutor.from(newFeeRecommendationsProcessor.getCfg(), matched(),
                () -> "2022-07-12T13:00:00.123");
    }

    public YtSourceTargetDiffScenarioExecutor<NewFeeRecommendationSource,
            FeeRecommendation> feeRecommendationsExecutor3() {
        return YtSourceTargetDiffScenarioExecutor.from(newFeeRecommendationsProcessor.getCfg(), matched(),
                () -> "2022-07-12T14:00:00.123");
    }
}
