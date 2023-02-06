package ru.yandex.direct.core.testing.configuration;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.NotImplementedException;
import org.asynchttpclient.AsyncHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import ru.yandex.direct.advq.AdvqClient;
import ru.yandex.direct.appmetrika.AppMetrikaClient;
import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.audience.client.YaAudienceClient;
import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.bangenproxy.client.BanGenProxyClient;
import ru.yandex.direct.bangenproxy.client.zenmeta.ZenMetaInfoClient;
import ru.yandex.direct.bannersystem.BannerSystemClient;
import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.bmapi.client.BmapiClient;
import ru.yandex.direct.bs.id.generator.BsDomainIdGeneratorClient;
import ru.yandex.direct.bsauction.BsRequest;
import ru.yandex.direct.bsauction.BsRequestPhrase;
import ru.yandex.direct.bsauction.BsResponse;
import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.bsauction.FullBsTrafaretResponsePhrase;
import ru.yandex.direct.bvm.client.BvmClient;
import ru.yandex.direct.canvas.client.CanvasClient;
import ru.yandex.direct.common.configuration.GraphiteConfiguration;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.common.log.service.LogUaasDataService;
import ru.yandex.direct.common.mds.MdsHolder;
import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.common.testing.CommonTestingConfiguration;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.configuration.CoreConfiguration;
import ru.yandex.direct.core.copyentity.CopyLocker;
import ru.yandex.direct.core.entity.abt.container.AllowedFeatures;
import ru.yandex.direct.core.entity.abt.container.UaasInfoRequest;
import ru.yandex.direct.core.entity.abt.container.UaasInfoResponse;
import ru.yandex.direct.core.entity.abt.repository.UaasInfoRepository;
import ru.yandex.direct.core.entity.abt.service.EnvironmentNameGetter;
import ru.yandex.direct.core.entity.abt.service.UaasConditionEvaluator;
import ru.yandex.direct.core.entity.abt.service.UaasInfoService;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupTestCommons;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerPixelsRepository;
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService;
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.entity.bids.service.BidBsStatisticFacade;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.client.repository.ClientBrandsRepository;
import ru.yandex.direct.core.entity.client.repository.ClientDomainsStrippedRepository;
import ru.yandex.direct.core.entity.client.repository.ClientLimitsRepository;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.clientphone.ClientPhoneLocker;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.deal.repository.DealRepository;
import ru.yandex.direct.core.entity.deal.service.DealNotificationService;
import ru.yandex.direct.core.entity.deal.service.DealService;
import ru.yandex.direct.core.entity.deal.service.validation.DealValidationService;
import ru.yandex.direct.core.entity.dialogs.repository.ClientDialogsRepository;
import ru.yandex.direct.core.entity.feature.repository.ClientFeaturesRepository;
import ru.yandex.direct.core.entity.feature.service.DirectAuthContextService;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.feed.repository.FeedRepository;
import ru.yandex.direct.core.entity.freelancer.repository.ClientAvatarRepository;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerProjectRepository;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerRepository;
import ru.yandex.direct.core.entity.freelancer.service.AvatarsClientPool;
import ru.yandex.direct.core.entity.internalads.repository.DirectTemplatePlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.PlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.TemplatePlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.TemplatePlaceYtRepository;
import ru.yandex.direct.core.entity.internalads.repository.TemplateResourceRepository;
import ru.yandex.direct.core.entity.internalads.repository.TemplateResourceYtRepository;
import ru.yandex.direct.core.entity.internalads.service.InternalAdsProductService;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.mailnotification.repository.MailNotificationEventRepository;
import ru.yandex.direct.core.entity.masterreport.MasterReportService;
import ru.yandex.direct.core.entity.mdsfile.model.MdsStorageHost;
import ru.yandex.direct.core.entity.mdsfile.repository.MdsFileRepository;
import ru.yandex.direct.core.entity.mdsfile.service.MdsFileService;
import ru.yandex.direct.core.entity.metrika.repository.CalltrackingExternalRepository;
import ru.yandex.direct.core.entity.metrika.repository.CalltrackingNumberClicksRepository;
import ru.yandex.direct.core.entity.metrika.repository.CalltrackingPhonesWithoutReplacementsRepository;
import ru.yandex.direct.core.entity.metrika.repository.LalSegmentRepository;
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCounterByDomainRepository;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppConversionStatisticRepository;
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentYtHelper;
import ru.yandex.direct.core.entity.moderation.repository.ModResyncQueueRepository;
import ru.yandex.direct.core.entity.moderationdiag.repository.ModerationDiagRepository;
import ru.yandex.direct.core.entity.offerretargeting.repository.OfferRetargetingRepository;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.entity.outdoor.repository.PlacementsOutdoorDataRepository;
import ru.yandex.direct.core.entity.placements.repository.PlacementBlockRepository;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.entity.product.repository.ProductsCache;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.retargeting.repository.TargetingCategoriesCache;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkRepository;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.entity.stopword.repository.StopWordRepository;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.entity.trustedredirects.repository.TrustedRedirectsRepository;
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService;
import ru.yandex.direct.core.entity.uac.grut.GrutContext;
import ru.yandex.direct.core.entity.uac.grut.ThreadLocalGrutContext;
import ru.yandex.direct.core.entity.uac.repository.mysql.EcomOfferCatalogsRepository;
import ru.yandex.direct.core.entity.uac.repository.mysql.ShopInShopBusinessesRepository;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.entity.vcard.repository.internal.AddressesRepository;
import ru.yandex.direct.core.redis.LettuceStorage;
import ru.yandex.direct.core.service.integration.passport.PassportService;
import ru.yandex.direct.core.service.urlchecker.RedirectChecker;
import ru.yandex.direct.core.service.urlchecker.UrlChecker;
import ru.yandex.direct.core.testing.MockMvcCreator;
import ru.yandex.direct.core.testing.data.TestNewCpcVideoBanners;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.db.TestPpcPropertiesSupport;
import ru.yandex.direct.core.testing.db.TestShardHelper;
import ru.yandex.direct.core.testing.mock.AdvqClientStub;
import ru.yandex.direct.core.testing.mock.AvatarsClientMockUtils;
import ru.yandex.direct.core.testing.mock.MdsHolderStub;
import ru.yandex.direct.core.testing.mock.PlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.core.testing.redis.TestLettuceStorage;
import ru.yandex.direct.core.testing.repository.TestAdGroupBsTagsRepository;
import ru.yandex.direct.core.testing.repository.TestAdGroupRepository;
import ru.yandex.direct.core.testing.repository.TestAddressesRepository;
import ru.yandex.direct.core.testing.repository.TestAgencyNdsRepository;
import ru.yandex.direct.core.testing.repository.TestAgencyRepository;
import ru.yandex.direct.core.testing.repository.TestAutoPriceCampQueueRepository;
import ru.yandex.direct.core.testing.repository.TestAutobudgetAlertRepository;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestBannerImageFormatRepository;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;
import ru.yandex.direct.core.testing.repository.TestBannerPixelsRepository;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestBannerStorageDictRepository;
import ru.yandex.direct.core.testing.repository.TestBannerUserFlagsUpdatesRepository;
import ru.yandex.direct.core.testing.repository.TestBidsRepository;
import ru.yandex.direct.core.testing.repository.TestCalloutRepository;
import ru.yandex.direct.core.testing.repository.TestCalltrackingPhonesRepository;
import ru.yandex.direct.core.testing.repository.TestCalltrackingSettingsRepository;
import ru.yandex.direct.core.testing.repository.TestCampAdditionalDataRepository;
import ru.yandex.direct.core.testing.repository.TestCampCalltrackingPhonesRepository;
import ru.yandex.direct.core.testing.repository.TestCampCalltrackingSettingsRepository;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.repository.TestClientDialogsRepository;
import ru.yandex.direct.core.testing.repository.TestClientDomainStrippedRepository;
import ru.yandex.direct.core.testing.repository.TestClientLimitsRepository;
import ru.yandex.direct.core.testing.repository.TestClientNdsRepository;
import ru.yandex.direct.core.testing.repository.TestClientOptionsRepository;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.repository.TestCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.repository.TestDealRepository;
import ru.yandex.direct.core.testing.repository.TestFeedCategoryRepository;
import ru.yandex.direct.core.testing.repository.TestFeedHistoryRepository;
import ru.yandex.direct.core.testing.repository.TestFixationPhraseRepository;
import ru.yandex.direct.core.testing.repository.TestGeoRegionRepository;
import ru.yandex.direct.core.testing.repository.TestImageRepository;
import ru.yandex.direct.core.testing.repository.TestKeywordRepository;
import ru.yandex.direct.core.testing.repository.TestLalSegmentRepository;
import ru.yandex.direct.core.testing.repository.TestMailNotificationEventRepository;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.repository.TestModResyncQueueRepository;
import ru.yandex.direct.core.testing.repository.TestModerationReasonsRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.repository.TestOrganizationRepository;
import ru.yandex.direct.core.testing.repository.TestPlacementRepository;
import ru.yandex.direct.core.testing.repository.TestProductRepository;
import ru.yandex.direct.core.testing.repository.TestShardOrderIdRepository;
import ru.yandex.direct.core.testing.repository.TestSitelinkSetRepository;
import ru.yandex.direct.core.testing.repository.TestSmsQueueRepository;
import ru.yandex.direct.core.testing.repository.TestTagRepository;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.repository.TestUserRepository;
import ru.yandex.direct.core.testing.repository.TestWalletCampaignRepository;
import ru.yandex.direct.core.testing.steps.AdGroupAdditionalTargetingSteps;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.AgencyClientProveSteps;
import ru.yandex.direct.core.testing.steps.AvatarSteps;
import ru.yandex.direct.core.testing.steps.BannerCreativeSteps;
import ru.yandex.direct.core.testing.steps.BannerModerationVersionSteps;
import ru.yandex.direct.core.testing.steps.BannerPriceSteps;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.BaseUserSteps;
import ru.yandex.direct.core.testing.steps.BidModifierSteps;
import ru.yandex.direct.core.testing.steps.BsFakeSteps;
import ru.yandex.direct.core.testing.steps.CalloutSteps;
import ru.yandex.direct.core.testing.steps.CalltrackingPhoneSteps;
import ru.yandex.direct.core.testing.steps.CalltrackingSettingsSteps;
import ru.yandex.direct.core.testing.steps.CampCalltrackingPhonesSteps;
import ru.yandex.direct.core.testing.steps.CampCalltrackingSettingsSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.CampaignsMobileContentSteps;
import ru.yandex.direct.core.testing.steps.CampaignsPerformanceSteps;
import ru.yandex.direct.core.testing.steps.ClientBrandSteps;
import ru.yandex.direct.core.testing.steps.ClientOptionsSteps;
import ru.yandex.direct.core.testing.steps.ClientPhoneSteps;
import ru.yandex.direct.core.testing.steps.ClientPixelProviderSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.ContentPromotionSteps;
import ru.yandex.direct.core.testing.steps.CpcVideoBannerSteps;
import ru.yandex.direct.core.testing.steps.CpmAudioBannerSteps;
import ru.yandex.direct.core.testing.steps.CpmBannerSteps;
import ru.yandex.direct.core.testing.steps.CpmGeoPinBannerSteps;
import ru.yandex.direct.core.testing.steps.CpmIndoorBannerSteps;
import ru.yandex.direct.core.testing.steps.CpmOutdoorBannerSteps;
import ru.yandex.direct.core.testing.steps.CreativeSteps;
import ru.yandex.direct.core.testing.steps.CryptaGoalsSteps;
import ru.yandex.direct.core.testing.steps.CurrencySteps;
import ru.yandex.direct.core.testing.steps.DealSteps;
import ru.yandex.direct.core.testing.steps.DialogSteps;
import ru.yandex.direct.core.testing.steps.DomainSteps;
import ru.yandex.direct.core.testing.steps.DynamicBannerSteps;
import ru.yandex.direct.core.testing.steps.DynamicTextAdTargetSteps;
import ru.yandex.direct.core.testing.steps.DynamicsSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.FeedSteps;
import ru.yandex.direct.core.testing.steps.FreelancerSteps;
import ru.yandex.direct.core.testing.steps.IdmGroupSteps;
import ru.yandex.direct.core.testing.steps.ImageBannerSteps;
import ru.yandex.direct.core.testing.steps.InternalAdPlaceSteps;
import ru.yandex.direct.core.testing.steps.InternalAdProductSteps;
import ru.yandex.direct.core.testing.steps.InternalBannerSteps;
import ru.yandex.direct.core.testing.steps.KeywordSteps;
import ru.yandex.direct.core.testing.steps.McBannerSteps;
import ru.yandex.direct.core.testing.steps.MdsFileSteps;
import ru.yandex.direct.core.testing.steps.MetrikaServiceSteps;
import ru.yandex.direct.core.testing.steps.MinusKeywordsPackSteps;
import ru.yandex.direct.core.testing.steps.MobileAppBannerSteps;
import ru.yandex.direct.core.testing.steps.ModerateBannerPageSteps;
import ru.yandex.direct.core.testing.steps.ModerationDiagSteps;
import ru.yandex.direct.core.testing.steps.ModerationReasonSteps;
import ru.yandex.direct.core.testing.steps.OldContentPromotionBannerSteps;
import ru.yandex.direct.core.testing.steps.OrganizationsSteps;
import ru.yandex.direct.core.testing.steps.PerformanceBannerSteps;
import ru.yandex.direct.core.testing.steps.PerformanceFiltersSteps;
import ru.yandex.direct.core.testing.steps.PerformanceMainBannerSteps;
import ru.yandex.direct.core.testing.steps.PlacementSteps;
import ru.yandex.direct.core.testing.steps.PricePackageSteps;
import ru.yandex.direct.core.testing.steps.ProductSteps;
import ru.yandex.direct.core.testing.steps.RelevanceMatchSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.RetargetingGoalsSteps;
import ru.yandex.direct.core.testing.steps.RetargetingSteps;
import ru.yandex.direct.core.testing.steps.RolesSteps;
import ru.yandex.direct.core.testing.steps.SitelinkSetSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.StoredVarsSteps;
import ru.yandex.direct.core.testing.steps.TagCampaignSteps;
import ru.yandex.direct.core.testing.steps.TextBannerSteps;
import ru.yandex.direct.core.testing.steps.TrustedRedirectSteps;
import ru.yandex.direct.core.testing.steps.TurboAppSteps;
import ru.yandex.direct.core.testing.steps.TurboLandingSteps;
import ru.yandex.direct.core.testing.steps.TypedCampaignSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.core.testing.steps.VcardSteps;
import ru.yandex.direct.core.testing.steps.WarnplaceSteps;
import ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignRepository;
import ru.yandex.direct.core.testing.stub.BalanceClientStub;
import ru.yandex.direct.core.testing.stub.BlackboxUserStub;
import ru.yandex.direct.core.testing.stub.BmapiClientStub;
import ru.yandex.direct.core.testing.stub.CanvasClientStub;
import ru.yandex.direct.core.testing.stub.GeminiClientStub;
import ru.yandex.direct.core.testing.stub.GeosearchClientStub;
import ru.yandex.direct.core.testing.stub.IntApiClientStub;
import ru.yandex.direct.core.testing.stub.MarketClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.core.testing.stub.PassportClientStub;
import ru.yandex.direct.core.testing.stub.TurboAppsClientStub;
import ru.yandex.direct.core.testing.stub.TvmIntegrationTestStub;
import ru.yandex.direct.crypta.client.CryptaClient;
import ru.yandex.direct.crypta.client.impl.CryptaClientStub;
import ru.yandex.direct.dbqueue.repository.DbQueueTypeMap;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.dssclient.DssClient;
import ru.yandex.direct.dssclient.DssClientCredentials;
import ru.yandex.direct.dssclient.DssUserCredentials;
import ru.yandex.direct.dssclient.http.HttpConnectorSettings;
import ru.yandex.direct.dssclient.http.certificates.Certificate;
import ru.yandex.direct.dssclient.pdfstamp.Stamp;
import ru.yandex.direct.expert.client.ExpertClient;
import ru.yandex.direct.facebook.graph.client.FacebookGraphClient;
import ru.yandex.direct.gemini.GeminiClient;
import ru.yandex.direct.geobasehelper.GeoBaseHelper;
import ru.yandex.direct.geobasehelper.GeoBaseHelperStub;
import ru.yandex.direct.geosearch.GeosearchClient;
import ru.yandex.direct.geosuggest.client.GeoSuggestClient;
import ru.yandex.direct.graphite.GraphiteClient;
import ru.yandex.direct.imagesearch.ImageSearchClient;
import ru.yandex.direct.intapi.client.IntApiClient;
import ru.yandex.direct.integrations.configuration.IntegrationsConfiguration;
import ru.yandex.direct.landlord.client.LandlordClient;
import ru.yandex.direct.landlord.client.LandlordConfiguration;
import ru.yandex.direct.libs.collections.CollectionsClient;
import ru.yandex.direct.libs.curator.CuratorFrameworkProvider;
import ru.yandex.direct.libs.video.VideoClient;
import ru.yandex.direct.liveresource.LiveResource;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.liveresource.LiveResourceWatcherFactory;
import ru.yandex.direct.liveresource.provider.LiveResourceFactoryBean;
import ru.yandex.direct.mail.LoggingMailSender;
import ru.yandex.direct.mail.MailSender;
import ru.yandex.direct.market.client.MarketClient;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.metrika.client.MetrikaHelper;
import ru.yandex.direct.organizations.swagger.OrganizationsClient;
import ru.yandex.direct.pokazometer.PokazometerClient;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.rbac.RbacClientsRelations;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.redislock.DistributedLock;
import ru.yandex.direct.redislock.StubDistributedLock;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.rotor.client.RotorClient;
import ru.yandex.direct.sender.YandexSenderClient;
import ru.yandex.direct.staff.client.StaffClient;
import ru.yandex.direct.telephony.client.TelephonyClient;
import ru.yandex.direct.turboapps.client.TurboAppsClient;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.direct.uaas.UaasClient;
import ru.yandex.direct.utils.crypt.Encrypter;
import ru.yandex.direct.xiva.client.XivaClient;
import ru.yandex.direct.xiva.client.XivaConfig;
import ru.yandex.direct.xiva.client.model.Signature;
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfig;
import ru.yandex.direct.ytcomponents.service.BlrtDynContextProvider;
import ru.yandex.direct.ytcomponents.statistics.service.CampaignStatService;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.grut.client.GrutClient;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.common.configuration.TvmIntegrationConfiguration.TVM_INTEGRATION;
import static ru.yandex.direct.core.configuration.CoreConfiguration.DIRECT_FILES_MDS_HOLDER_BEAN_NAME;
import static ru.yandex.direct.core.configuration.CoreConfiguration.GEO_BASE_HELPER;
import static ru.yandex.direct.core.configuration.CoreConfiguration.GRUT_CLIENT_FOR_WATCHLOG;
import static ru.yandex.direct.core.configuration.CoreIntegrationConfiguration.UAC_AVATARS_CLIENT_POOL;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_DOMAIN_ID_GENERATOR_CLIENT;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT_WEB;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.CURATOR_FRAMEWORK_PROVIDER;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.ROTOR_CLIENT;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.ROTOR_TRACKING_URL_ANDROID_CLIENT;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.ROTOR_TRACKING_URL_IOS_CLIENT;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.YA_AUDIENCE_CLIENT;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.YA_AUDIENCE_TOKEN_PROVIDER;

/**
 * Переопределяет некоторые бин-дефинишены из модулей core и common
 * для подмены некоторых внешних интерфейсов стабами, а так же
 * добавляет бины, необходимые исключительно для тестирования,
 * такие как рулы и степы.
 * <p>
 * Все переопределенные бины должны иметь явно заданное имя,
 * совпадающее с именем оригинальных бинов. Это необходимо
 * для того, чтобы исключить возможность создания нескольких
 * бинов одного типа с разными именами вместо переопределения.
 */
@Configuration
@Import({CoreConfiguration.class, CommonTestingConfiguration.class})
public class CoreTestingConfiguration {

    private static final String STOPWORDS_PATH_FILE = "classpath:///externalData/stopwords.txt";

    // переопределенные бины-заглушки

    @Bean
    public DirectYtDynamicConfig directYtDynamicConfig(DirectConfig directConfig) {
        return new DirectYtDynamicTestingConfig(directConfig.getBranch("dynamic-yt"));
    }

    @SpyBean
    public YtProvider ytProvider;

    @SpyBean
    public BlrtDynContextProvider blrtDynContextProvider;

    /**
     * Стаб хелпера к Метрике
     */
    @Bean(name = IntegrationsConfiguration.METRIKA_HELPER)
    public MetrikaHelper metrikaHelper(MetrikaClient metrikaClient) {
        return new MetrikaHelperStub(metrikaClient);
    }

    /**
     * Стаб клиента к Метрике
     */
    @Bean(name = IntegrationsConfiguration.METRIKA_CLIENT)
    public MetrikaClient metrikaClient(MetrikaClientStub metrikaClientStub) {
        return metrikaClientStub;
    }

    /**
     * Стаб клиента к Метрике
     */
    @Bean
    public MetrikaClientStub metrikaClientStub() {
        return spy(new MetrikaClientStub());
    }

    /**
     * Замоканый клиент к сертификатнице
     */
    @MockBean(name = IntegrationsConfiguration.EXPERT_CLIENT)
    public ExpertClient expertClient;

    @MockBean(name = IntegrationsConfiguration.IMAGE_SEARCH_CLIENT)
    public ImageSearchClient imageSearchClient;

    @SpyBean
    public CryptaClient cryptaClient;

    @Bean(name = IntegrationsConfiguration.CRYPTA_CLIENT)
    public CryptaClient cryptaClient() {
        return new CryptaClientStub();
    }

    @SpyBean
    public OrganizationsClient organizationsClient;

    @Bean(name = IntegrationsConfiguration.ORGANIZATION_CLIENT)
    public OrganizationsClient organizationClient() {
        return new OrganizationsClientStub();
    }

    @MockBean(name = IntegrationsConfiguration.TELEPHONY_CLIENT)
    public TelephonyClient telephonyClient;

    @Bean
    public ClientPhoneLocker clientPhoneLocker() {
        return new ClientPhoneLocker(mock(LettuceConnectionProvider.class)) {
            @Override
            public DistributedLock create(String key) {
                return new StubDistributedLock();
            }
        };
    }

    @SpyBean
    public MarketClient marketClient;

    @Bean(name = IntegrationsConfiguration.MARKET_CLIENT)
    public MarketClient marketClient(ShardHelper shardHelper, FeedRepository feedRepository) {
        return new MarketClientStub(shardHelper, feedRepository);
    }

    @MockBean(name = IntegrationsConfiguration.BANNER_SYSTEM_CLIENT)
    public BannerSystemClient bannerSystemClient;

    @MockBean
    public GeoSuggestClient geoSuggestClient;

    /**
     * Стаб клиента к ручке для получения инфморации о турбо-аппах
     */
    @Bean(name = CoreConfiguration.TURBO_APPS_CLIENT)
    public TurboAppsClient turboappsClient() {
        return spy(new TurboAppsClientStub());
    }

    @MockBean
    public StaffClient staffClient;

    @MockBean
    public BlackboxUserService blackboxUserService;

    @MockBean
    public BlackboxClient blackboxClient;

    @MockBean
    public PassportService passportService;

    @MockBean
    public MasterReportService masterReportService;

    @MockBean
    public ZenMetaInfoClient zenMetaInfoClient;

    /**
     * Стаб клиента к Canvas
     */
    @Bean(name = IntegrationsConfiguration.CANVAS_CLIENT)
    public CanvasClient canvasClient() {
        return new CanvasClientStub();
    }

    /**
     * Стаб клиента к BmAPI
     */
    @Bean(IntegrationsConfiguration.BMAPI_CLIENT)
    public BmapiClient bmapiClient() {
        return new BmapiClientStub();
    }

    @Bean
    public ShardHelper shardHelper(ShardSupport shardSupport) {
        return new TestShardHelper(shardSupport);
    }

    /**
     * Стаб клиента к RBAC
     */
    @Bean(name = RbacService.RBAC_SERVICE)
    public RbacService rbacService(
            ShardHelper shardHelper,
            DslContextProvider dslContextProvider,
            PpcRbac ppcRbac,
            RbacClientsRelations rbacClientsRelations
    ) {
        return new RbacService(
                shardHelper,
                dslContextProvider,
                ppcRbac,
                rbacClientsRelations
        );
    }

    @SpyBean
    public BalanceClient balanceClient;

    @SpyBean
    public MobileContentYtHelper mobileContentYtHelper;

    /**
     * Стаб клиента к Балансу
     * (когда появится клиент к балансу, нужно сменить тип возвращаемого значения,
     * чтобы этот бин заменял бин реального клиента)
     */
    @Bean
    public BalanceClient balanceClient(DatabaseWrapperProvider databaseWrapperProvider) {
        return new BalanceClientStub(databaseWrapperProvider);
    }

    @Bean
    public BlackboxUserStub blackboxUserStub(DatabaseWrapperProvider databaseWrapperProvider) {
        return new BlackboxUserStub(databaseWrapperProvider);
    }

    @Bean
    public IdmGroupSteps idmGroupSteps() {
        return new IdmGroupSteps();
    }

    @Bean
    public IntApiClient intApiClient() {
        return spy(new IntApiClientStub());
    }

    /**
     * Stub клиента к Паспорту
     */
    @Bean
    public PassportClientStub passportClientStub(DatabaseWrapperProvider databaseWrapperProvider) {
        return PassportClientStub.newInstance(databaseWrapperProvider);
    }

    @MockBean(name = IntegrationsConfiguration.POKAZOMETER_CLIENT)
    public PokazometerClient pokazometerClient;

    @SpyBean(name = BS_TRAFARET_AUCTION_CLIENT)
    public BsTrafaretClient bsTrafaretClient;

    @SpyBean(name = BS_TRAFARET_AUCTION_CLIENT_WEB)
    public BsTrafaretClient bsTrafaretClientWeb;

    @Bean(BS_TRAFARET_AUCTION_CLIENT)
    public BsTrafaretClient bsTrafaretClient() {
        return new BsTrafaretClient(mock(ParallelFetcherFactory.class), "", "", mock(Supplier.class)) {
            @Override
            public <R extends BsRequestPhrase, T extends BsRequest<R>> IdentityHashMap<T, BsResponse<R,
                    FullBsTrafaretResponsePhrase>> getAuctionResultsWithPositionCtrCorrection(List<T> auctionRequests) {
                return new IdentityHashMap<>();
            }
        };
    }

    @Bean(BS_TRAFARET_AUCTION_CLIENT_WEB)
    public BsTrafaretClient bsTrafaretClientWeb(@Qualifier(BS_TRAFARET_AUCTION_CLIENT)
                                                        BsTrafaretClient bsTrafaretClient) {
        return bsTrafaretClient;
    }

    @MockBean
    public BidBsStatisticFacade bidBsStatisticFacade;

    @SpyBean(name = IntegrationsConfiguration.ADVQ_CLIENT)
    public AdvqClient advqClient;

    @Bean(IntegrationsConfiguration.ADVQ_CLIENT)
    public AdvqClient advqClient() {
        return new AdvqClientStub();
    }

    @MockBean(name = GraphiteConfiguration.GRAPHITE_CLIENT)
    public GraphiteClient graphiteClient;

    @MockBean(name = IntegrationsConfiguration.UAAS_CLIENT)
    public UaasClient uaasClient;

    @MockBean
    public FacebookGraphClient facebookGraphClient;

    @SpyBean
    public LandlordClient landlordClient;

    @Bean
    public LandlordClient landlordClient(TvmIntegration tvmIntegration) {
        var someUrl = "https://ll.yandex.net";
        var conf = new LandlordConfiguration(someUrl, TvmService.LANDLORD_API_TEST.getId(), "");
        return new LandlordClient(conf, mock(ParallelFetcherFactory.class), tvmIntegration) {
            @Override
            public @NotNull String buildUrlFromSlug(@NotNull String slug) {
                return slug;
            }
        };
    }

    @MockBean
    public BvmClient bvmClient;

    @MockBean(name = BS_DOMAIN_ID_GENERATOR_CLIENT)
    public BsDomainIdGeneratorClient bsDomainIdGeneratorClient;

    // бины исключительно для тестирования (степы, рулы и т.п.) объявляем только здесь,
    // чтобы они были видны только в тестах и не пролезали в конфигурацию приложения

    /**
     * Новый и недоработанный репозиторий для кампаний
     */
    @Bean
    public CampaignRepository campaignRepository0() {
        return new CampaignRepository();
    }

    /**
     * Репозиторий для сохранения тестовых клиентов
     */
    @Bean
    public TestClientRepository testClientRepository() {
        return new TestClientRepository();
    }

    @Bean
    public ClientSteps clientSteps(
            BaseUserSteps baseUserSteps,
            ClientRepository clientRepository,
            TestClientRepository testClientRepository,
            PassportClientStub passportClientStub,
            ShardSupport shardSupport,
            TestClientLimitsRepository testClientLimitsRepository,
            ClientLimitsRepository clientLimitsRepository,
            TestClientNdsRepository testClientNdsRepository,
            TestAgencyNdsRepository testAgencyNdsRepository,
            ClientOptionsRepository clientOptionsRepository,
            RbacClientsRelations rbacClientsRelations,
            BalanceClient balanceClient) {
        return new ClientSteps(baseUserSteps, clientRepository, testClientRepository,
                passportClientStub, shardSupport, testClientLimitsRepository, clientLimitsRepository,
                testClientNdsRepository, testAgencyNdsRepository, clientOptionsRepository, rbacClientsRelations,
                balanceClient);
    }

    @Bean
    public TestClientOptionsRepository testClientOptionsRepository() {
        return new TestClientOptionsRepository();
    }

    @Bean
    public ClientOptionsSteps clientOptionsSteps(TestClientOptionsRepository testClientOptionsRepository,
                                                 ClientOptionsRepository clientOptionsRepository) {
        return new ClientOptionsSteps(testClientOptionsRepository, clientOptionsRepository);
    }

    @Bean
    public FreelancerSteps freelancerSteps(ClientSteps clientSteps, FreelancerRepository freelancerRepository,
                                           FreelancerProjectRepository freelancerProjectRepository) {
        return new FreelancerSteps(clientSteps, freelancerRepository, freelancerProjectRepository);
    }

    @Bean
    public TypedCampaignSteps typedCampaignSteps(CampaignModifyRepository campaignModifyRepository,
                                                 Steps steps,
                                                 DslContextProvider dslContextProvider,
                                                 CampaignAddOperationSupportFacade campaignAddOperationSupportFacade,
                                                 MetrikaClient metrikaClient) {
        return new TypedCampaignSteps(campaignModifyRepository, steps, dslContextProvider,
                campaignAddOperationSupportFacade, metrikaClient);
    }

    @Bean
    public AdGroupSteps adGroupSteps() {
        return new AdGroupSteps();
    }

    @Bean
    public TestKeywordRepository testKeywordRepository(DslContextProvider dslContextProvider) {
        return new TestKeywordRepository(dslContextProvider);
    }

    @Bean
    public TestMinusKeywordsPackRepository testMinusKeywordsPackRepository(DslContextProvider dslContextProvider) {
        return new TestMinusKeywordsPackRepository(dslContextProvider);
    }

    @Bean
    public MinusKeywordsPackSteps minusKeywordsPackSteps(ClientSteps clientSteps,
                                                         MinusKeywordsPackRepository minusKeywordsPackRepository,
                                                         TestMinusKeywordsPackRepository testMinusKeywordsPackRepository) {
        return new MinusKeywordsPackSteps(clientSteps, minusKeywordsPackRepository, testMinusKeywordsPackRepository);
    }

    @Bean
    public MdsFileSteps mdsFileSteps() {
        return new MdsFileSteps();
    }

    @Bean
    public KeywordSteps keywordSteps() {
        return new KeywordSteps();
    }

    @Bean
    public CreativeSteps creativeSteps() {
        return new CreativeSteps();
    }

    @Bean
    public CalloutSteps calloutSteps() {
        return new CalloutSteps();
    }

    @Bean
    public InternalAdProductSteps internalAdProductSteps(ClientSteps clientSteps,
                                                         ClientService clientService, UserService userService,
                                                         InternalAdsProductService interalAdsProductService) {
        return new InternalAdProductSteps(clientSteps, clientService, userService, interalAdsProductService);
    }

    @Bean
    public InternalAdPlaceSteps internalAdPlaceSteps(TemplatePlaceRepository templatePlaceRepository,
                                                     DirectTemplatePlaceRepository directTemplatePlaceRepository,
                                                     PlaceRepository placeRepository) {
        return new InternalAdPlaceSteps(templatePlaceRepository, directTemplatePlaceRepository, placeRepository);
    }

    @SpyBean
    public PlaceRepository placeRepository;

    @Bean
    public PlaceRepository placeRepository() {
        return PlaceRepositoryMockUtils.createMySqlRepositoryMock();
    }

    @Bean
    public PricePackageSteps pricePackageSteps(PricePackageRepository pricePackageRepository,
                                               DslContextProvider dslContextProvider) {
        return new PricePackageSteps(pricePackageRepository, dslContextProvider);
    }

    @Bean
    public TestCreativeRepository testCreativeRepository() {
        return new TestCreativeRepository();
    }

    @Bean
    public TestBannerStorageDictRepository testBannerStorageDictRepository() {
        return new TestBannerStorageDictRepository();
    }

    /**
     * Репозиторий для добавления тестовых записей в таблицу форматов изображений
     */
    @Bean
    public TestBannerImageFormatRepository testBannerImageFormatRepository() {
        return new TestBannerImageFormatRepository();
    }

    @Bean
    public TestBannerCreativeRepository testBannerCreativeRepository() {
        return new TestBannerCreativeRepository();
    }

    @Bean
    public TestBannerRepository testBannerRepository() {
        return new TestBannerRepository();
    }

    @Bean
    public TestAdGroupRepository testAdGroupRepository() {
        return new TestAdGroupRepository();
    }

    @Bean
    public TestAutoPriceCampQueueRepository testAutoPriceCampQueueRepository() {
        return new TestAutoPriceCampQueueRepository();
    }

    @Bean
    public TestBannerImageRepository testBannerImageRepository(DslContextProvider dslContextProvider,
                                                               ShardHelper shardHelper) {
        return new TestBannerImageRepository(dslContextProvider, shardHelper);
    }

    @Bean
    public TestBannerPixelsRepository testBannerPixelsRepository(OldBannerPixelsRepository bannerPixelsRepository) {
        return new TestBannerPixelsRepository(bannerPixelsRepository);
    }

    @Bean
    public TestBidsRepository testBidsRepository(DslContextProvider dslContextProvider) {
        return new TestBidsRepository(dslContextProvider);
    }

    @Bean
    public TestShardOrderIdRepository testShardOrderIdRepository(DslContextProvider dslContextProvider) {
        return new TestShardOrderIdRepository(dslContextProvider);
    }

    @Bean
    public TestImageRepository testImageRepository(DslContextProvider dslContextProvider,
                                                   ShardHelper shardHelper) {
        return new TestImageRepository(dslContextProvider, shardHelper);
    }

    @Bean
    public TestMailNotificationEventRepository testMailNotificationEventRepository(
            DslContextProvider dslContextProvider,
            MailNotificationEventRepository mailNotificationEventRepository) {
        return new TestMailNotificationEventRepository(dslContextProvider, mailNotificationEventRepository);
    }

    @Bean
    public TestCalloutRepository testCalloutRepository(ShardHelper shardHelper, DslContextProvider dslContextProvider) {
        return new TestCalloutRepository(shardHelper, dslContextProvider);
    }

    @Bean
    public TestModerationReasonsRepository moderationReasonsSteps(DslContextProvider dslContextProvider) {
        return new TestModerationReasonsRepository(dslContextProvider);
    }

    @Bean
    public TestAdGroupBsTagsRepository testAdGroupBsTagsRepository() {
        return new TestAdGroupBsTagsRepository();
    }

    @Bean
    public DomainSteps domainSteps() {
        return new DomainSteps();
    }

    @Bean
    public TestClientDomainStrippedRepository testClientDomainStrippedRepository(
            ClientDomainsStrippedRepository clientDomainsStrippedRepository,
            DslContextProvider dslContextProvider) {
        return new TestClientDomainStrippedRepository(clientDomainsStrippedRepository, dslContextProvider);
    }

    @Bean
    public TestCryptaSegmentRepository testCryptaSegmentRepository(
            CryptaSegmentRepository cryptaSegmentRepository,
            DslContextProvider dslContextProvider) {
        return new TestCryptaSegmentRepository(cryptaSegmentRepository, dslContextProvider);
    }

    @Bean
    public TestLalSegmentRepository testLalSegmentsRepository(DslContextProvider dslContextProvider,
                                                              LalSegmentRepository lalSegmentRepository) {
        return new TestLalSegmentRepository(dslContextProvider, lalSegmentRepository);
    }

    @Bean
    public LettuceStorage lettuceStorage() {
        return new TestLettuceStorage(mock(LettuceConnectionProvider.class));
    }

    @Bean
    public CopyLocker copyLocker() {
        return new CopyLocker(mock(LettuceConnectionProvider.class)) {
            @Override
            public DistributedLock create(String key) {
                return new StubDistributedLock();
            }
        };
    }

    @Bean
    public PpcPropertiesSupport ppcPropertiesSupport(DslContextProvider dslContextProvider) {
        return new TestPpcPropertiesSupport(dslContextProvider);
    }

    @MockBean
    public MetrikaCounterByDomainRepository metrikaCounterByDomainRepository;

    @MockBean
    public ShopInShopBusinessesRepository shopInShopBusinessesRepository;

    @MockBean
    public CalltrackingNumberClicksRepository calltrackingNumberClicksRepository;

    @MockBean
    public CalltrackingPhonesWithoutReplacementsRepository calltrackingPhonesWithoutReplacementsRepository;

    @MockBean
    public CalltrackingExternalRepository calltrackingExternalRepository;

    @Bean
    public TestCampaignRepository testCampaignRepository(DslContextProvider dslContextProvider) {
        return new TestCampaignRepository(dslContextProvider);
    }

    @Bean
    public TestWalletCampaignRepository testWalletCampaignRepository(DslContextProvider dslContextProvider) {
        return new TestWalletCampaignRepository(dslContextProvider);
    }

    @Bean
    public TestDealRepository testDealRepository(DslContextProvider dslContextProvider) {
        return new TestDealRepository(dslContextProvider);
    }

    @Bean
    public TestFixationPhraseRepository testFixationPhraseRepository(DslContextProvider dslContextProvider) {
        return new TestFixationPhraseRepository(dslContextProvider);
    }

    @Bean
    public BannerSteps bannerSteps() {
        return new BannerSteps();
    }

    @Bean
    public TextBannerSteps textBannerSteps() {
        return new TextBannerSteps();
    }

    @Bean
    public OldContentPromotionBannerSteps oldContentPromotionBannerSteps() {
        return new OldContentPromotionBannerSteps();
    }

    @Bean
    public CpmOutdoorBannerSteps cpmOutdoorBannerSteps() {
        return new CpmOutdoorBannerSteps();
    }

    @Bean
    public CpmIndoorBannerSteps cpmIndoorBannerSteps() {
        return new CpmIndoorBannerSteps();
    }

    @Bean
    public CpmAudioBannerSteps cpmAudioBannerSteps() {
        return new CpmAudioBannerSteps();
    }

    @Bean
    public CpmBannerSteps cpmBannerSteps() {
        return new CpmBannerSteps();
    }

    @Bean
    public CpmGeoPinBannerSteps cpmBannerGeoPinSteps() {
        return new CpmGeoPinBannerSteps();
    }

    @Bean
    public CpcVideoBannerSteps cpcVideoBannerSteps() {
        return new CpcVideoBannerSteps();
    }

    @Bean
    public DynamicBannerSteps dynamicBannerSteps() {
        return new DynamicBannerSteps();
    }

    @Bean
    public PerformanceBannerSteps performanceBannerSteps() {
        return new PerformanceBannerSteps();
    }

    @Bean
    public PerformanceMainBannerSteps performanceMainBannerSteps() {
        return new PerformanceMainBannerSteps();
    }

    @Bean
    public ImageBannerSteps imageBannerSteps() {
        return new ImageBannerSteps();
    }

    @Bean
    public McBannerSteps mcBannerSteps() {
        return new McBannerSteps();
    }

    @Bean
    public MobileAppBannerSteps mobileAppBannerSteps() {
        return new MobileAppBannerSteps();
    }

    @Bean
    public InternalBannerSteps internalBannerSteps() {
        return new InternalBannerSteps();
    }

    @Bean
    public BannerCreativeSteps bannerCreativeSteps() {
        return new BannerCreativeSteps();
    }

    @Bean
    public VcardSteps vcardSteps(VcardRepository vcardRepository, CampaignSteps campaignSteps,
                                 DslContextProvider dslContextProvider) {
        return new VcardSteps(campaignSteps, dslContextProvider, vcardRepository);
    }

    @Bean
    public TrustedRedirectSteps trustedRedirectSteps(TrustedRedirectsRepository trustedRedirectsRepository) {
        return new TrustedRedirectSteps(trustedRedirectsRepository);
    }

    @Bean
    public TestSitelinkSetRepository testSitelinkSetRepository(DslContextProvider dslContextProvider) {
        return new TestSitelinkSetRepository(dslContextProvider);
    }

    @Bean
    public SitelinkSetSteps sitelinkSetSteps(ClientSteps clientSteps,
                                             SitelinkRepository sitelinkRepository,
                                             SitelinkSetRepository sitelinkSetRepository,
                                             TestSitelinkSetRepository testSitelinkSetRepository) {
        return new SitelinkSetSteps(clientSteps, sitelinkRepository, sitelinkSetRepository, testSitelinkSetRepository);
    }

    @Bean
    public RetConditionSteps retConditionSteps() {
        return new RetConditionSteps();
    }

    @Bean
    public RetargetingSteps retargetingSteps() {
        return new RetargetingSteps();
    }

    @Bean
    public BidModifierSteps bidModifierSteps() {
        return new BidModifierSteps();
    }

    @Bean
    public StoredVarsSteps storedVarsSteps() {
        return new StoredVarsSteps();
    }

    @Bean
    public CampaignsPerformanceSteps campaignsPerformanceSteps() {
        return new CampaignsPerformanceSteps();
    }

    @Bean
    public CampaignsMobileContentSteps campaignsMobileContentSteps(DslContextProvider dslContextProvider) {
        return new CampaignsMobileContentSteps(dslContextProvider);
    }

    @Bean
    public TurboAppSteps turboAppSteps() {
        return new TurboAppSteps();
    }

    @Bean
    public BsFakeSteps bsFakeSteps() {
        return new BsFakeSteps();
    }

    @Bean
    public RetargetingGoalsSteps retargetintGoalsSteps() {
        return new RetargetingGoalsSteps();
    }

    @Bean
    public RelevanceMatchSteps relevanceMatchSteps() {
        return new RelevanceMatchSteps();
    }

    @Bean
    public PerformanceFiltersSteps bidsPerformanceSteps() {
        return new PerformanceFiltersSteps();
    }

    @Bean
    public DynamicTextAdTargetSteps dynamicTextAdTargetSteps() {
        return new DynamicTextAdTargetSteps();
    }

    @Bean
    public DynamicsSteps dynamicConditionsFakeSteps() {
        return new DynamicsSteps();
    }

    @Bean
    public AgencyClientProveSteps agencyClientProveSteps() {
        return new AgencyClientProveSteps();
    }

    @Bean
    public BannerPriceSteps bannerPriceSteps() {
        return new BannerPriceSteps();
    }

    @Bean
    public CryptaGoalsSteps cryptaGoalsSteps() {
        return new CryptaGoalsSteps();
    }

    @Bean
    public TestUserRepository testUserRepository() {
        return new TestUserRepository();
    }

    @Bean
    public TestSmsQueueRepository testSmsQueueRepository() {
        return new TestSmsQueueRepository();
    }

    @Bean
    public TestAddressesRepository testAddressesRepository(AddressesRepository addressesRepository,
                                                           DslContextProvider dslContextProvider) {
        return new TestAddressesRepository(addressesRepository, dslContextProvider);
    }

    @Bean
    public TestAutobudgetAlertRepository testAutobudgetAlertRepository() {
        return new TestAutobudgetAlertRepository();
    }

    @Bean
    public TestGeoRegionRepository testGeoRegionRepository(DslContextProvider dslContextProvider) {
        return new TestGeoRegionRepository(dslContextProvider);
    }

    @Bean
    public TestAgencyRepository testAgencyRepository() {
        return new TestAgencyRepository();
    }

    @Bean
    public UserSteps userSteps(
            BaseUserSteps baseUserSteps,
            UserRepository userRepository,
            ClientSteps clientSteps,
            PassportClientStub passportClientStub, BalanceClient balanceClient,
            BlackboxUserService blackboxUserService, TestAgencyRepository testAgencyRepository) {
        return new UserSteps(baseUserSteps, userRepository, clientSteps,
                passportClientStub, blackboxUserService, balanceClient, testAgencyRepository);
    }

    @Bean
    public RolesSteps rolesSteps(UserSteps userSteps,
                                 ClientSteps clientSteps,
                                 IdmGroupSteps idmGroupSteps,
                                 UserService userService,
                                 ClientService clientService,
                                 AgencyClientRelationService agencyService,
                                 RbacService rbacService,
                                 TestClientRepository testClientRepository) {
        return new RolesSteps(userSteps, clientSteps, idmGroupSteps, userService, clientService, agencyService,
                rbacService, testClientRepository);
    }

    @Bean
    public ClientBrandSteps clientBrandSteps(ClientSteps clientSteps, ClientBrandsRepository clientBrandsRepository) {
        return new ClientBrandSteps(clientSteps, clientBrandsRepository);
    }

    @Bean
    AvatarSteps avatarSteps(FreelancerSteps freelancerSteps, ClientAvatarRepository clientAvatarRepository,
                            ShardHelper shardHelper) {
        return new AvatarSteps(freelancerSteps, clientAvatarRepository, shardHelper);
    }

    @Bean
    public OrganizationsSteps organizationsSteps(OrganizationRepository organizationsRepository,
                                                 TestOrganizationRepository testOrganizationRepository,
                                                 ShardHelper shardHelper) {
        return new OrganizationsSteps(organizationsRepository, testOrganizationRepository, shardHelper);
    }

    @Bean
    public ContentPromotionSteps contentPromotionSteps(ContentPromotionRepository contentPromotionRepository) {
        return new ContentPromotionSteps(contentPromotionRepository);
    }

    @Bean
    public HttpConnectorSettings dssClientHttpConnectorSettings() {
        // никто не ожидает, что там что-то будет, бин переопределён, чтобы юнит-тесты не пытались ходить в
        // какой-то DSS вообще
        return new HttpConnectorSettings("https://localhost:443", 40);
    }

    @Bean
    public DssClient dssClient(HttpConnectorSettings httpConnectorSettings) {
        return new DssClient(httpConnectorSettings,
                mock(DssUserCredentials.class, Answers.RETURNS_MOCKS),
                mock(DssClientCredentials.class, Answers.RETURNS_MOCKS)) {
            @Override
            public List<Certificate> getCertificateList() {
                int currentYear = LocalDate.now().getYear();
                return singletonList(new Certificate(
                        1L,
                        "1234",
                        BigInteger.valueOf(0xDEADBEEFL),
                        true,
                        new Date(new GregorianCalendar(currentYear, Calendar.JANUARY, 1).toInstant().toEpochMilli()),
                        new Date(new GregorianCalendar(currentYear, Calendar.DECEMBER, 31).toInstant().toEpochMilli()),
                        emptyMap()));
            }

            @Override
            public byte[] signPdf(byte[] documentContent, String documentName, Stamp stamp, Long certificateId,
                                  String pinCode) {
                return new byte[]{1, 2, 3, 4};
            }
        };
    }

    @SpyBean
    public MdsHolder directFilesMds;

    @Bean(name = DIRECT_FILES_MDS_HOLDER_BEAN_NAME)
    public MdsHolder directFilesMds() {
        return new MdsHolderStub();
    }

    @Bean
    public MdsFileService mdsFileService(ShardHelper shardHelper,
                                         MdsFileRepository mdsFileRepository,
                                         MdsHolder directFilesMds) {
        return new MdsFileService(shardHelper, mdsFileRepository, directFilesMds,
                MdsStorageHost.STORAGE_INT_MDST_YANDEX_NET);
    }

    @Bean
    public MailSender mailSender() {
        return new LoggingMailSender();
    }

    @Bean
    public Steps steps() {
        return new Steps();
    }

    @Bean
    public WarnplaceSteps warnplaceSteps() {
        return new WarnplaceSteps();
    }

    @Bean
    public DealSteps dealSteps(DatabaseWrapperProvider databaseWrapperProvider, DealRepository dealRepository,
                               TestDealRepository testDealRepository, ClientSteps clientSteps,
                               ShardHelper shardHelper) {
        return new DealSteps(databaseWrapperProvider, dealRepository, testDealRepository, clientSteps, shardHelper);
    }

    @Bean
    public TestClientDialogsRepository testClientDialogsRepository(DslContextProvider dslContextProvider) {
        return new TestClientDialogsRepository(dslContextProvider);
    }

    @Bean
    public DialogSteps dialogSteps(CampaignSteps campaignSteps, ClientDialogsRepository clientDialogsRepository,
                                   TestClientDialogsRepository testClientDialogsRepository) {
        return new DialogSteps(campaignSteps, clientDialogsRepository, testClientDialogsRepository);
    }

    @Bean
    public CurrencySteps currencySteps(DatabaseWrapperProvider databaseWrapperProvider) {
        return new CurrencySteps();
    }

    @Bean
    public FeatureSteps featureSteps(FeatureManagingService featureManagingService,
                                     ClientFeaturesRepository clientFeaturesRepository,
                                     DirectAuthContextService directAuthContextService) {
        return new FeatureSteps(featureManagingService, clientFeaturesRepository, directAuthContextService);
    }

    @Bean
    public TurboLandingSteps turboLandingSteps() {
        return new TurboLandingSteps();
    }

    @Bean
    public TestModerationRepository testModerationRepository() {
        return new TestModerationRepository();
    }

    @Bean
    public TestModResyncQueueRepository testModResyncQueueRepository(
            DslContextProvider dslContextProvider,
            ModResyncQueueRepository modResyncQueueRepository) {
        return new TestModResyncQueueRepository(dslContextProvider, modResyncQueueRepository);
    }

    @Bean
    public TestBannerUserFlagsUpdatesRepository testBannerUserFlagsUpdatesRepository() {
        return new TestBannerUserFlagsUpdatesRepository();
    }

    @Bean
    public TestPlacementRepository testPlacementRepository(DslContextProvider dslContextProvider,
                                                           PlacementBlockRepository placementBlockRepository,
                                                           PlacementsOutdoorDataRepository placementsOutdoorDataRepository) {
        return new TestPlacementRepository(dslContextProvider, placementBlockRepository,
                placementsOutdoorDataRepository);
    }

    @Bean
    public TestTargetingCategoriesRepository testTargetingCategoriesRepository(
            DslContextProvider dslContextProvider, TargetingCategoriesCache targetingCategoriesCache) {
        return new TestTargetingCategoriesRepository(dslContextProvider, targetingCategoriesCache);
    }

    @Bean
    public TestTagRepository testTagRepository(DslContextProvider dslContextProvider, ShardHelper shardHelper,
                                               TagRepository tagRepository) {
        return new TestTagRepository(dslContextProvider, shardHelper, tagRepository);
    }

    @Bean
    public MockMvcCreator mockMvcCreator(ApplicationContext ctxt) {
        return new MockMvcCreator(ctxt);
    }

    @Lazy
    @Bean(name = CoreConfiguration.GEOSEARCH_CLIENT)
    public GeosearchClient geosearchClient() {
        return new GeosearchClientStub();
    }

    @Bean
    public TestFeedCategoryRepository testFeedCategoryRepository(DslContextProvider dslContextProvider) {
        return new TestFeedCategoryRepository(dslContextProvider);
    }

    @Bean
    public TestFeedHistoryRepository testFeedHistoryRepository(DslContextProvider dslContextProvider) {
        return new TestFeedHistoryRepository(dslContextProvider);
    }

    @Bean
    public TestProductRepository testProductRepository(DslContextProvider dslContextProvider,
                                                       ProductsCache productsCache) {
        return new TestProductRepository(dslContextProvider, productsCache);
    }

    @Bean
    public FeedSteps feedSteps(ShardHelper shardHelper, ClientSteps clientSteps,
                               FeedRepository feedRepository, TestFeedCategoryRepository categoryRepository,
                               TestFeedHistoryRepository historyRepository) {
        return new FeedSteps(shardHelper, clientSteps, feedRepository, categoryRepository, historyRepository);
    }

    @Bean
    public ClientPhoneSteps clientPhoneSteps() {
        return new ClientPhoneSteps();
    }

    @Bean
    public DbQueueSteps dbQueueSteps(ShardHelper shardHelper,
                                     DslContextProvider dslContextProvider,
                                     DbQueueTypeMap typeMap) {
        return new DbQueueSteps(shardHelper, dslContextProvider, typeMap);
    }

    @Bean(name = TVM_INTEGRATION)
    public TvmIntegration tvmIntegration() {
        TvmService service = TvmService.DUMMY;
        return new TvmIntegrationTestStub(service);
    }

    @Bean(name = StopWordRepository.BEAN_NAME)
    public StopWordRepository stopWordRepository() {
        return new StopWordRepository(mock(DslContextProvider.class)) {
            @Override
            public boolean replaceStopWords(@Nullable Set<String> newStopWords) {
                throw new NotImplementedException("Метод не реализован");
            }

            @Override
            public Set<String> getStopWords() {
                String txtBody = LiveResourceFactory.get(STOPWORDS_PATH_FILE).getContent();
                return ImmutableSet.copyOf(stream(txtBody.split("[\\n\\s]+")).collect(toSet()));
            }
        };
    }

    @Bean
    public TagCampaignSteps tagCampaignSteps() {
        return new TagCampaignSteps();
    }

    @Bean
    public MetrikaServiceSteps metrikaServiceSteps(MetrikaGoalsService metrikaGoalsService) {
        return new MetrikaServiceSteps(metrikaGoalsService);
    }

    @Bean
    public ModerationDiagSteps moderationDiagSteps(ModerationDiagRepository moderationDiagRepository,
                                                   DslContextProvider dslContextProvider) {
        return new ModerationDiagSteps(moderationDiagRepository, dslContextProvider);
    }

    @Bean
    public ModerationReasonSteps moderationReasonSteps(TestModerationReasonsRepository testModerationReasonsRepository,
                                                       BannerSteps bannerSteps) {
        return new ModerationReasonSteps(testModerationReasonsRepository, bannerSteps);
    }

    @Bean
    public PlacementSteps placementSteps(TestPlacementRepository placementRepository,
                                         DslContextProvider dslContextProvider) {
        return new PlacementSteps(placementRepository, dslContextProvider);
    }

    @Bean
    public BannerModerationVersionSteps bannerModerationVersionSteps() {
        return new BannerModerationVersionSteps();
    }

    @Bean
    public ModerateBannerPageSteps moderateBannerPageSteps() {
        return new ModerateBannerPageSteps();
    }

    @Bean
    public ProductSteps productSteps() {
        return new ProductSteps();
    }

    @Bean
    public DealService dealService(DealRepository dealRepository, ShardHelper shardHelper,
                                   ru.yandex.direct.core.entity.campaign.repository.CampaignRepository campaignRepository,
                                   DealValidationService dealValidationService,
                                   DealNotificationService dealNotificationService,
                                   DslContextProvider dslContextProvider,
                                   CampaignStatService campaignStatService,
                                   CampaignService campaignService,
                                   UserService userService,
                                   PlacementsRepository placementsRepository,
                                   PpcRbac ppcRbac) {
        return new DealService(dealRepository, shardHelper, campaignRepository, dealValidationService,
                dealNotificationService, dslContextProvider, campaignStatService, campaignService, userService,
                placementsRepository, ppcRbac, true);
    }

    @Bean
    public ComplexAdGroupTestCommons complexAdGroupTestCommons(KeywordRepository keywordRepository,
                                                               RelevanceMatchRepository relevanceMatchRepository,
                                                               OfferRetargetingRepository offerRetargetingRepository,
                                                               RetargetingService retargetingService,
                                                               BidModifierRepository bidModifierRepository) {
        return new ComplexAdGroupTestCommons(keywordRepository, relevanceMatchRepository, offerRetargetingRepository,
                retargetingService, bidModifierRepository);
    }

    @MockBean(name = CURATOR_FRAMEWORK_PROVIDER, answer = Answers.RETURNS_DEEP_STUBS)
    public CuratorFrameworkProvider curatorFrameworkProvider;

    @Bean(GEO_BASE_HELPER)
    public GeoBaseHelper geoBaseHelper(GeoTreeFactory geoTreeFactory) {
        return new GeoBaseHelperStub(geoTreeFactory);
    }

    @MockBean
    public YandexSenderClient yandexSenderClient;

    @SpyBean
    public TemplatePlaceYtRepository templatePlaceYtRepository;

    @Bean
    public TemplatePlaceYtRepository templatePlaceYtRepository() {
        return TemplatePlaceRepositoryMockUtils.createYtRepositoryMock();
    }

    @SpyBean
    public TemplatePlaceRepository templatePlaceRepository;

    @Bean
    public TemplatePlaceRepository templatePlaceRepository() {
        return TemplatePlaceRepositoryMockUtils.createMySqlRepositoryMock();
    }

    @SpyBean
    public TemplateResourceYtRepository templateResourceYtRepository;

    @Bean
    public TemplateResourceYtRepository templateResourceYtRepository() {
        return TemplateResourceRepositoryMockUtils.createYtRepositoryMock();
    }

    @SpyBean
    public TemplateResourceRepository templateResourceRepository;

    @Bean
    public TemplateResourceRepository templateResourceRepository() {
        return TemplateResourceRepositoryMockUtils.createMySqlRepositoryMock();
    }

    @MockBean
    public VideoClient videoClient;

    @MockBean
    public AppMetrikaClient appMetrikaClient;

    @Bean(name = UAC_AVATARS_CLIENT_POOL)
    public AvatarsClientPool uacAvatarsClientPool() {
        return AvatarsClientMockUtils.getMockAvatarsClientPool();
    }

    @MockBean(name = YA_AUDIENCE_TOKEN_PROVIDER)
    public LiveResource yaAudienceTokenProvider;

    @MockBean(name = YA_AUDIENCE_CLIENT)
    public YaAudienceClient yaAudienceClient;

    @MockBean
    public CollectionsClient collectionsClient;

    @Bean
    public ClientPixelProviderSteps clientPixelProviderSteps() {
        return new ClientPixelProviderSteps();
    }

    @Bean
    public AdGroupAdditionalTargetingSteps adGroupAdditionalTargetingSteps(AdGroupAdditionalTargetingRepository repository) {
        return new AdGroupAdditionalTargetingSteps(repository);
    }

    @Bean
    public TestContentPromotionBanners testNewContentPromotionBanners(
            BannersUrlHelper bannersUrlHelper) {
        return new TestContentPromotionBanners(bannersUrlHelper);
    }

    @Bean
    public TestNewCpcVideoBanners testNewCpcVideoBanners(
            BannersUrlHelper bannersUrlHelper) {
        return new TestNewCpcVideoBanners(bannersUrlHelper);
    }

    @Bean
    public TestCalltrackingPhonesRepository testCalltrackingPhonesRepository() {
        return new TestCalltrackingPhonesRepository();
    }

    @Bean
    public CalltrackingPhoneSteps calltrackingPhoneSteps() {
        return new CalltrackingPhoneSteps();
    }

    @Bean
    public TestCalltrackingSettingsRepository testCalltrackingSettingsRepository() {
        return new TestCalltrackingSettingsRepository();
    }

    @Bean
    public CalltrackingSettingsSteps calltrackingSettingsSteps() {
        return new CalltrackingSettingsSteps();
    }

    @Bean
    public TestCampCalltrackingSettingsRepository testCampCalltrackingSettingsRepository() {
        return new TestCampCalltrackingSettingsRepository();
    }

    @Bean
    public CampCalltrackingSettingsSteps campCalltrackingSettingsSteps() {
        return new CampCalltrackingSettingsSteps();
    }

    @Bean
    public CampCalltrackingPhonesSteps campCalltrackingPhonesSteps() {
        return new CampCalltrackingPhonesSteps();
    }

    @Bean
    public TestCampCalltrackingPhonesRepository testCampCalltrackingPhonesRepository() {
        return new TestCampCalltrackingPhonesRepository();
    }

    @Bean
    public NetAcl networkConfigFactory(DirectConfig directConfig,
                                       LiveResourceWatcherFactory liveResourceWatcherFactory,
                                       LiveResourceFactoryBean liveResourceFactoryBean) {
        String networkConfig = directConfig.getString("network_config");
        LiveResource liveResource = liveResourceFactoryBean.get(networkConfig);
        return spy(NetAcl.createAndWatch(liveResource, liveResourceWatcherFactory));
    }

    @Bean
    public TestCampAdditionalDataRepository testCampAdditionalDataRepository() {
        return new TestCampAdditionalDataRepository();
    }

    @Bean
    public XivaClient xivaClient() {
        return new XivaClient(new XivaConfig("http://localhost", "xiva"),
                mock(ParallelFetcherFactory.class),
                mock(TvmIntegration.class),
                TvmService.XIVA_API_TEST) {
            @Nullable
            @Override
            public Signature getSignature(String user) {
                return new Signature()
                        .withSign("test-sign")
                        .withTs("test-ts");
            }
        };
    }

    @SpyBean
    public GeminiClient geminiClient;

    @Bean
    public GeminiClient geminiClient() {
        return new GeminiClientStub();
    }

    @Bean
    public BanGenProxyClient banGenProxyClient(@Value("${bangen_proxy.url}") String baseUrl,
                                               AsyncHttpClient asyncHttpClient,
                                               TvmIntegration tvmIntegration) {
        FetcherSettings fetcherSettings = new FetcherSettings()
                .withFailFast(true)
                .withRequestRetries(2)
                .withSoftTimeout(java.time.Duration.ofSeconds(10))
                .withRequestTimeout(java.time.Duration.ofSeconds(20));
        ParallelFetcherFactory fetcherFactory = new ParallelFetcherFactory(asyncHttpClient, fetcherSettings);
        return spy(new BanGenProxyClient(baseUrl, fetcherFactory, tvmIntegration));
    }

    @Bean
    public BannerUrlCheckService bannerUrlCheckService(
            PpcPropertiesSupport ppcPropertiesSupport,
            UrlChecker urlChecker,
            RedirectChecker redirectChecker,
            TrustedRedirectsService trustedRedirectsService) {
        return spy(new BannerUrlCheckService(ppcPropertiesSupport, urlChecker, redirectChecker,
                trustedRedirectsService));
    }

    @MockBean
    public MobileAppConversionStatisticRepository mobileAppConversionStatisticRepository;

    @SpyBean
    public GrutClient grutClient;

    @Bean
    @Primary
    public GrutClient grutClient() {
        return mock(GrutClient.class, RETURNS_MOCKS);
    }

    @SpyBean(name = GRUT_CLIENT_FOR_WATCHLOG)
    public GrutClient grutClientForWatchlog;

    @Bean(GRUT_CLIENT_FOR_WATCHLOG)
    public GrutClient grutClientForWatchlog() {
        return mock(GrutClient.class, RETURNS_MOCKS);
    }

    /**
     * Для веба используется request scope context бин, но не во всех тестах есть http-запросы, поэтому для всех
     * случаев в тестах будет использоваться thread-local контекст
     */
    @Bean
    public GrutContext grutContext(GrutClient grutClient) {
        return new ThreadLocalGrutContext(grutClient);
    }

    @Bean(name = ROTOR_CLIENT)
    public RotorClient rotorClient(AsyncHttpClient asyncHttpClient, TvmIntegration tvmIntegration) {
        return mock(RotorClient.class);
    }

    @Bean(name = ROTOR_TRACKING_URL_ANDROID_CLIENT)
    public RotorClient rotorTrackingUrlAndroidClient(AsyncHttpClient asyncHttpClient, TvmIntegration tvmIntegration) {
        return mock(RotorClient.class);
    }

    @Bean(name = ROTOR_TRACKING_URL_IOS_CLIENT)
    public RotorClient rotorTrackingUrlIosClient(AsyncHttpClient asyncHttpClient, TvmIntegration tvmIntegration) {
        return mock(RotorClient.class);
    }

    @SpyBean
    public UaasInfoService uaasInfoService;

    @Bean
    public UaasInfoService uaasInfoService() {
        return new UaasInfoService(mock(UaasClient.class),
                "",
                "",
                mock(LogUaasDataService.class),
                mock(UaasInfoRepository.class),
                mock(UaasConditionEvaluator.class), mock(EnvironmentNameGetter.class)
        ) {
            @Override
            public List<UaasInfoResponse> getInfo(Collection<UaasInfoRequest> uaasInfoRequests,
                                                  AllowedFeatures allowedFeatures) {
                return emptyList();
            }
        };
    }

    @MockBean
    public EcomOfferCatalogsRepository ecomOfferCatalogsRepository;

    @Lazy
    @Bean(CoreConfiguration.CONVERSION_CENTER_ENCRYPTER_BEAN_NAME)
    public Encrypter conversionCenterEncrypter() {
        return new Encrypter("test_secret");
    }
}
