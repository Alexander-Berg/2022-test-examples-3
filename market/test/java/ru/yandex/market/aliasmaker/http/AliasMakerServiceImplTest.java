package ru.yandex.market.aliasmaker.http;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.aliasmaker.cache.AliasMakerUtils;
import ru.yandex.market.aliasmaker.cache.KnowledgeService;
import ru.yandex.market.aliasmaker.cache.SuspendedCategorySupplierService;
import ru.yandex.market.aliasmaker.cache.deep_matcher.DeepMatcherProcessedItemService;
import ru.yandex.market.aliasmaker.cache.models.ModelService;
import ru.yandex.market.aliasmaker.cache.models.ModelsShadowReloadingCache;
import ru.yandex.market.aliasmaker.cache.offers.DeepMatcherOffersGenerationQueue;
import ru.yandex.market.aliasmaker.cache.offers.OfferService;
import ru.yandex.market.aliasmaker.cache.offers.SupplierOffersCache;
import ru.yandex.market.aliasmaker.cache.offers.WhiteOffersGenerationQueue;
import ru.yandex.market.aliasmaker.cache.tovar_tree.TovarTreeCachingService;
import ru.yandex.market.aliasmaker.cache.users.UserServiceImpl;
import ru.yandex.market.aliasmaker.cache.yang.AssignmentService;
import ru.yandex.market.aliasmaker.cache.yang.AssignmentServiceMock;
import ru.yandex.market.aliasmaker.config.HttpClientConfig;
import ru.yandex.market.aliasmaker.dao.TolokaProfilesDaoMock;
import ru.yandex.market.aliasmaker.models.AssignmentInfo;
import ru.yandex.market.aliasmaker.models.User;
import ru.yandex.market.aliasmaker.offers.Offer;
import ru.yandex.market.aliasmaker.offers.matching.MatchingService;
import ru.yandex.market.aliasmaker.offers.matching.OffersMatchingService;
import ru.yandex.market.aliasmaker.utils.FrontendStatService;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.mbo.http.ModelRuleService;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.YangLogStorageService;
import ru.yandex.market.mbo.model.forms.ModelFormService;
import ru.yandex.market.mbo.users.MboUsers;
import ru.yandex.market.mbo.users.MboUsersService;
import ru.yandex.market.mboc.http.CategorySupplierVendors;
import ru.yandex.market.mboc.http.CategorySupplierVendorsService;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.MboCategoryService;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mdm.http.MasterDataService;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;

public class AliasMakerServiceImplTest {
    private static final String RANDOM_OFFER_ID = "12345";
    private static final String RANDOM_OFFER_ID_2 = "123456";
    private static final int SUSPENDED_VENDOR = 10;
    private static final int ALIVE_VENDOR = 12;
    private static final String WORKER_ID = "worker_id";

    private AliasMakerServiceImpl service;

    @Mock
    private MboCategoryService mboCategoryService;
    @Mock
    private CategorySizeMeasureService sizeMeasureService;
    @Mock
    private SuspendedCategorySupplierService suspendedCategorySupplierService;
    @Mock
    private CategorySupplierVendorsService categorySupplierVendorsService;
    @Mock
    private WhiteOffersGenerationQueue whiteOffersGenerationQueue;
    @Mock
    private DeepMatcherOffersGenerationQueue deepMatcherOffersGenerationQueue;
    @Mock
    private DeepMatcherProcessedItemService deepMatcherProcessedItemService;
    @Mock
    private CategoryParametersService categoryParametersService;
    @Mock
    private ModelFormService modelFormService;
    @Mock
    private OfferService offerService;
    @Mock
    private OffersMatchingService offerMatchingService;
    @Mock
    private MboMappingsService mboMappingsService;
    @Mock
    private ModelRuleService modelRuleService;
    @Mock
    private YangLogStorageService yangLogStorageService;
    @Mock
    private FrontendStatService frontendStatService;
    @Mock
    private MasterDataService masterDataService;
    @Mock
    MatchingService matchingService;
    @Mock
    ModelsShadowReloadingCache modelsShadowReloadingCache;

    private SupplierOffersCache supplierOffersCache;

    private KnowledgeService knowledgeService;
    private ModelService modelService;
    private UserServiceImpl userService;
    private AssignmentServiceMock assignmentService;
    private TovarTreeCachingService tovarTreeCachingService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        Offer offer = Mockito.mock(Offer.class);
        AliasMaker.Offer offerA = AliasMaker.Offer.newBuilder()
                .setOfferId(RANDOM_OFFER_ID)
                .build();
        Mockito.when(offer.toOfferSafely(any()))
                .thenReturn(offerA);

        offerService = new OfferService();
        offerService.setSupplierOffersCache(supplierOffersCache);

        knowledgeService = Mockito.spy(KnowledgeService.class);
        Mockito.doNothing().when(offerMatchingService).rematchOffer(Mockito.any(Offer.class), anyInt());

        userService = initUserService();

        assignmentService = new AssignmentServiceMock();

        Mockito.when(mboMappingsService.searchMappingsByMarketSkuId(any()))
                .thenAnswer(inv -> {
                    MboMappings.SearchMappingsByMarketSkuIdRequest request =
                            inv.getArgument(0, MboMappings.SearchMappingsByMarketSkuIdRequest.class);

                    return MboMappings.SearchMappingsResponse.newBuilder()
                            .addOffers(SupplierOffer.Offer.newBuilder()
                                    .setApprovedMapping(SupplierOffer.Mapping.newBuilder()
                                            .setSkuId(request.getMarketSkuId(0))
                                            .build())
                                    .build())
                            .build();
                });
        categoryParametersService = Mockito.mock(CategoryParametersService.class);
        modelService = Mockito.mock(ModelService.class);
        service = new AliasMakerServiceImpl(knowledgeService, modelService,
                offerService, offerMatchingService, userService,
                assignmentService, mboCategoryService, mboMappingsService, null,
                sizeMeasureService, categorySupplierVendorsService,
                deepMatcherOffersGenerationQueue, whiteOffersGenerationQueue, suspendedCategorySupplierService,
                deepMatcherProcessedItemService, categoryParametersService,
                modelFormService, modelRuleService, yangLogStorageService, frontendStatService,
                tovarTreeCachingService, matchingService, modelsShadowReloadingCache, true, true);
    }

    @After
    public void tearDown() throws Exception {
        setUpFakeProdInAssigmentService(false);
    }

    private UserServiceImpl initUserService() {
        HttpClientConfig httpConfigs = new HttpClientConfig();
        MboUsersService mboUsersService = Mockito.mock(MboUsersService.class);
        Mockito.when(mboUsersService.getMboUsers(any())).thenReturn(MboUsers.GetMboUsersResponse.newBuilder()
                .addUser(MboUsers.MboUser.newBuilder()
                        .setUid(11)
                        .setMboFullname("Alice Fox")
                        .setStaffLogin("alicealice")
                        .build())
                .addUser(MboUsers.MboUser.newBuilder()
                        .setUid(15)
                        .setMboFullname("robot-mbo-autotester")
                        .setStaffLogin(UserServiceImpl.AUTOTESTS_USER)
                        .build())
                .addUser(MboUsers.MboUser.newBuilder()
                        .setUid(12)
                        .setMboFullname("Bob Bear")
                        .setStaffLogin("bobobob")
                        .build())
                .addUser(MboUsers.MboUser.newBuilder()
                        .setUid(25)
                        .setMboFullname("Bob Bear")
                        .setStaffLogin("bobobob")  // sometimes we have duplicate users in mbo, gotta test that too
                        .build())
                .build());
        TolokaProfilesDaoMock tolokaProfilesDao = new TolokaProfilesDaoMock();
        tolokaProfilesDao.put("alicealice", "alice-id");
        tolokaProfilesDao.put("bobobob", "bob-id");
        return new UserServiceImpl(tolokaProfilesDao, mboUsersService);
    }

    @Test
    public void testAutotests() throws Exception {
        AssignmentInfo assignmentInfo = assignmentService.getAssignmentInfo(AssignmentService.AUTOTESTS_ASSIGNMENT_ID);
        User user = userService.getUserByWorkerId(assignmentInfo.getUserId());
        Assertions.assertThat(user.getUserId())
                .isEqualTo(15L);
    }

    @Test
    public void testValidationWhenAssignmentIsExpired_prod() {
        setUpFakeProdInAssigmentService(true);
        try {
            String assignmentId = "1234";
            assignmentService.addAssignmentInfo(assignmentId, new AssignmentInfo(WORKER_ID, "EXPIRED", null));
            AliasMaker.UpdateModelsGroupResponse response = service.updateModelsGroup(
                    AliasMaker.UpdateModelsGroupRequest.newBuilder().setAssignmentId(assignmentId).build());
            AliasMaker.OperationResult result = response.getResult();
            Assert.assertEquals("Работа в этом задании ограничена, так как задание уже завершено", result.getMessage());
            Assert.assertEquals(AliasMaker.OperationStatus.ERROR, result.getStatus());
        } finally {
            setUpFakeProdInAssigmentService(false);
        }

    }

    @Test
    public void testValidationWhenAssignmentIsFinishedLongTimeAgo_prod() {
        setUpFakeProdInAssigmentService(true);
        try {
            Instant now = Instant.now();
            Instant submitted = now.minusSeconds(AssignmentService.BLOCK_SUBMITTED_DELAY_SEC + 10);
            String assignmentId = "1234";

            assignmentService.addAssignmentInfo(assignmentId, new AssignmentInfo(WORKER_ID, "SUBMITTED", submitted));
            AliasMaker.UpdateVendorResponse response = service.updateVendor(
                    AliasMaker.UpdateVendorRequest.newBuilder().setAssignmentId(assignmentId).build());
            AliasMaker.OperationResult result = response.getResult();
            Assert.assertEquals("Работа в этом задании ограничена, так как задание уже завершено", result.getMessage());
            Assert.assertEquals(AliasMaker.OperationStatus.ERROR, result.getStatus());
        } finally {
            setUpFakeProdInAssigmentService(false);

        }
    }

    private void setUpFakeProdInAssigmentService(boolean isProd) {
        assignmentService.setProd(isProd);
    }

    @Test
    public void testNotProdAssignmentAlwaysValid() {
        Instant now = Instant.now();
        Instant submitted = now.minusSeconds(AssignmentService.BLOCK_SUBMITTED_DELAY_SEC + 10);
        AssignmentInfo assignmentInfo = new AssignmentInfo(WORKER_ID, "SUBMITTED", submitted);
        boolean assignmentIsValid = assignmentService.checkAssignmentIsValid(assignmentInfo);

        Assert.assertTrue(assignmentIsValid);
    }

    @Test
    public void testNoErrorWhenAssignmentIsFinishedRecently() {
        Instant now = Instant.now();
        Instant submitted = now.minusSeconds(AssignmentService.BLOCK_SUBMITTED_DELAY_SEC - 10);
        String assignmentId = "asdf";
        assignmentService.addAssignmentInfo(assignmentId, new AssignmentInfo(WORKER_ID, "SUBMITTED", submitted));

        AliasMaker.UpdateVendorResponse response = service.updateVendor(AliasMaker.UpdateVendorRequest.newBuilder()
                .setAssignmentId(assignmentId)
                .build());
        AliasMaker.OperationResult result = response.getResult();
        Assert.assertNotEquals(AliasMaker.OperationStatus.ERROR, result.getStatus());
    }

    @Test
    public void findModelsUsesOnlyOperatorQualityFlag() throws Exception {
        Mockito.when(modelService.findModels(any()))
                .thenReturn(AliasMaker.GetModelsResponse.newBuilder()
                        .setResult(AliasMaker.OperationResult.newBuilder()
                                .setStatus(AliasMaker.OperationStatus.SUCCESS)
                                .build())
                        .build());

        AliasMaker.GetModelsResponse response = service.findModels(
                ModelStorage.FindModelsRequest.newBuilder()
                        .setCategoryId(1)
                        .addAllModelIds(Collections.singletonList(1L))
                        .build()
        );

        Assertions.assertThat(response.getResult().getStatus()).isEqualTo(AliasMaker.OperationStatus.SUCCESS);

        ArgumentCaptor<ModelStorage.FindModelsRequest> argument =
                ArgumentCaptor.forClass(ModelStorage.FindModelsRequest.class);
        Mockito.verify(modelService).findModels(argument.capture());

        ModelStorage.FindModelsRequest findModelsRequest = argument.getValue();
        Assertions.assertThat(findModelsRequest.getOnlyOperatorQuality()).isTrue();
    }

    @Test
    public void searchMappingsByMarketSkuId() {
        MboMappings.SearchMappingsByMarketSkuIdRequest request =
                MboMappings.SearchMappingsByMarketSkuIdRequest.newBuilder()
                        .addMarketSkuId(123)
                        .build();
        MboMappings.SearchMappingsResponse response = service.searchMappingsByMarketSkuId(request);
        Assert.assertEquals(123, response.getOffers(0).getApprovedMapping().getSkuId());
    }

    @Test
    public void testSetStaffLoginToTaskResult() {
        Mockito.when(mboCategoryService.saveTaskMappings(Mockito.any())).thenReturn(
                MboCategory.SaveTaskMappingsResponse.newBuilder()
                        .setResult(SupplierOffer.OperationResult.newBuilder()
                                .setStatus(SupplierOffer.OperationStatus.ERROR)
                                .build())
                        .build()
        );

        AliasMaker.UpdateSupplierOfferMappingsRequest request = AliasMaker
                .UpdateSupplierOfferMappingsRequest.newBuilder()
                .addMapping(SupplierOffer.ContentTaskResult.newBuilder()
                        .setStatus(SupplierOffer.SupplierOfferMappingStatus.MAPPED)
                        .setOfferId("1")
                        .setWorkerId("alice-id")
                )
                .addMapping(SupplierOffer.ContentTaskResult.newBuilder()
                        .setStatus(SupplierOffer.SupplierOfferMappingStatus.MAPPED)
                        .setOfferId("2")
                        .setWorkerId("bob-id")
                )
                .addMapping(SupplierOffer.ContentTaskResult.newBuilder()
                        .setStatus(SupplierOffer.SupplierOfferMappingStatus.MAPPED)
                        .setOfferId("3")
                )
                .build();

        service.updateSupplierOfferMappings(request);

        ArgumentCaptor<MboCategory.SaveTaskMappingsRequest> argument =
                ArgumentCaptor.forClass(MboCategory.SaveTaskMappingsRequest.class);
        Mockito.verify(mboCategoryService).saveTaskMappings(argument.capture());

        MboCategory.SaveTaskMappingsRequest mbocRequest = argument.getValue();
        SupplierOffer.ContentTaskResult mapping1 = mbocRequest.getMapping(0);
        SupplierOffer.ContentTaskResult mapping2 = mbocRequest.getMapping(1);
        SupplierOffer.ContentTaskResult mapping3 = mbocRequest.getMapping(2);
        Assert.assertEquals("alicealice", mapping1.getStaffLogin());
        Assert.assertEquals("bobobob", mapping2.getStaffLogin());
        Assert.assertTrue(!mapping3.hasStaffLogin());

        // same goes for moderation pipeline:

        Mockito.when(mboCategoryService.saveMappingsModeration(Mockito.any())).thenReturn(
                MboCategory.SaveMappingModerationResponse.newBuilder()
                        .setResult(SupplierOffer.OperationResult.newBuilder()
                                .setStatus(SupplierOffer.OperationStatus.ERROR)
                                .build())
                        .build()
        );
        AliasMaker.UpdateSupplierMappingModerationRequest moderationRequest = AliasMaker
                .UpdateSupplierMappingModerationRequest.newBuilder()
                .addResult(SupplierOffer.MappingModerationTaskResult.newBuilder()
                        .setStatus(SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.ACCEPTED)
                        .setOfferId("1")
                        .setWorkerId("alice-id")
                )
                .addResult(SupplierOffer.MappingModerationTaskResult.newBuilder()
                        .setStatus(SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.NEED_INFO)
                        .setOfferId("2")
                        .setWorkerId("bob-id")
                )
                .addResult(SupplierOffer.MappingModerationTaskResult.newBuilder()
                        .setStatus(SupplierOffer.MappingModerationTaskResult.SupplierMappingModerationResult.REJECTED)
                        .setOfferId("3")
                )
                .build();


        service.updateSupplierMappingModerationResults(moderationRequest);

        ArgumentCaptor<MboCategory.SaveMappingsModerationRequest> moderationArgument =
                ArgumentCaptor.forClass(MboCategory.SaveMappingsModerationRequest.class);
        Mockito.verify(mboCategoryService).saveMappingsModeration(moderationArgument.capture());

        MboCategory.SaveMappingsModerationRequest mbocModerationRequest = moderationArgument.getValue();
        SupplierOffer.MappingModerationTaskResult result1 = mbocModerationRequest.getResults(0);
        SupplierOffer.MappingModerationTaskResult result2 = mbocModerationRequest.getResults(1);
        SupplierOffer.MappingModerationTaskResult result3 = mbocModerationRequest.getResults(2);
        Assert.assertEquals("alicealice", result1.getStaffLogin());
        Assert.assertEquals("bobobob", result2.getStaffLogin());
        Assert.assertTrue(!result3.hasStaffLogin());
    }


    @Test
    public void testGetSizeMeasuresSuccess() {
        Mockito.when(sizeMeasureService.getSizeMeasuresInfo(Mockito.any()))
                .thenReturn(MboSizeMeasures.GetSizeMeasuresInfoResponse.getDefaultInstance());
        Mockito.when(sizeMeasureService.getSizeMeasuresVendorsInfo(Mockito.any()))
                .thenReturn(MboSizeMeasures.GetSizeMeasuresInfoVendorResponse.getDefaultInstance());


        AliasMaker.GetSizeMeasuresInfoResponse response1 = service.getSizeMeasuresInfo(
                MboSizeMeasures.GetSizeMeasuresInfoRequest.getDefaultInstance());
        Assert.assertEquals(AliasMakerUtils.SUCCESS, response1.getStatus());
        Assert.assertNotNull(response1.getResponse());

        AliasMaker.GetSizeMeasuresInfoVendorResponse response2 = service.getSizeMeasuresVendorsInfo(
                MboSizeMeasures.GetSizeMeasureInfoVendorRequest.getDefaultInstance());
        Assert.assertEquals(AliasMakerUtils.SUCCESS, response2.getStatus());
        Assert.assertNotNull(response2.getResponse());
    }

    @Test
    public void testGetSizeMeasuresError() {
        Mockito.when(sizeMeasureService.getSizeMeasuresInfo(Mockito.any()))
                .thenThrow(new RuntimeException());
        Mockito.when(sizeMeasureService.getSizeMeasuresVendorsInfo(Mockito.any()))
                .thenThrow(new RuntimeException());

        AliasMaker.GetSizeMeasuresInfoResponse response1 = service.getSizeMeasuresInfo(
                MboSizeMeasures.GetSizeMeasuresInfoRequest.getDefaultInstance());
        Assert.assertEquals(AliasMaker.OperationStatus.INTERNAL_ERROR, response1.getStatus().getStatus());
        Assert.assertFalse(response1.hasResponse());

        AliasMaker.GetSizeMeasuresInfoVendorResponse response2 = service.getSizeMeasuresVendorsInfo(
                MboSizeMeasures.GetSizeMeasureInfoVendorRequest.getDefaultInstance());
        Assert.assertEquals(AliasMaker.OperationStatus.INTERNAL_ERROR, response2.getStatus().getStatus());
        Assert.assertFalse(response2.hasResponse());
    }


    @Test
    public void testGetContentCommentTypes() {
        Mockito.when(mboCategoryService.getContentCommentTypes(any()))
                .thenReturn(MboCategory.ContentCommentTypes.Response.getDefaultInstance());

        MboCategory.ContentCommentTypes.Request request = MboCategory.ContentCommentTypes.Request.getDefaultInstance();
        AliasMaker.GetContentCommentTypesResponse response = service.getContentCommentTypes(request);
        Assert.assertEquals(AliasMakerUtils.SUCCESS, response.getStatus());
        Assert.assertNotNull(response.getResponse());
    }

    @Test
    public void testSuspendCategorySupplierVendorSuccess() {
        Mockito.when(categorySupplierVendorsService.getCategorySupplierVendors(Mockito.any()))
                .thenReturn(CategorySupplierVendors.GetCategorySupplierVendorsResponse.newBuilder()
                        .setStatus(CategorySupplierVendors.ResponseStatus.SUCCESS)
                        .setMessage("Found 0 entries")
                        .build());
        Mockito.when(categorySupplierVendorsService.addCategorySupplierVendor(Mockito.any()))
                .thenReturn(CategorySupplierVendors.AddCategorySupplierVendorResponse.newBuilder()
                        .setStatus(CategorySupplierVendors.ResponseStatus.SUCCESS)
                        .setMessage("Successfully added entry")
                        .build());

        AliasMaker.SuspendCategorySupplierVendorRequest request =
                AliasMaker.SuspendCategorySupplierVendorRequest.getDefaultInstance();

        AliasMaker.SuspendCategorySupplierVendorResponse response = service.suspendCategorySupplierVendor(request);

        Mockito.verify(categorySupplierVendorsService, Mockito.times(1)).getCategorySupplierVendors(Mockito.any());
        Mockito.verify(categorySupplierVendorsService, Mockito.times(1)).addCategorySupplierVendor(Mockito.any());
        Mockito.verify(categorySupplierVendorsService, Mockito.never()).removeCategorySupplierVendors(Mockito.any());

        Assert.assertEquals(AliasMaker.OperationStatus.SUCCESS, response.getStatus().getStatus());
        Assert.assertEquals("Successfully added entry",
                response.getStatus().getMessage());
    }

    @Test
    public void testSuspendCategorySupplierVendorFailedAlreadySuspended() {
        Mockito.when(categorySupplierVendorsService.getCategorySupplierVendors(Mockito.any()))
                .thenReturn(CategorySupplierVendors.GetCategorySupplierVendorsResponse.newBuilder()
                        .setStatus(CategorySupplierVendors.ResponseStatus.SUCCESS)
                        .setMessage("Found 1 entries")
                        .addCategorySupplierVendors(CategorySupplierVendors.CategorySupplierVendor.newBuilder()
                                .setState(CategorySupplierVendors.CategorySupplierVendor.State.SUSPENDED)
                                .build())
                        .build());

        AliasMaker.SuspendCategorySupplierVendorRequest request =
                AliasMaker.SuspendCategorySupplierVendorRequest.getDefaultInstance();

        AliasMaker.SuspendCategorySupplierVendorResponse response = service.suspendCategorySupplierVendor(request);

        Mockito.verify(categorySupplierVendorsService, Mockito.times(1)).getCategorySupplierVendors(Mockito.any());
        Mockito.verify(categorySupplierVendorsService, Mockito.never()).addCategorySupplierVendor(Mockito.any());
        Mockito.verify(categorySupplierVendorsService, Mockito.never()).removeCategorySupplierVendors(Mockito.any());

        Assert.assertEquals(AliasMaker.OperationStatus.DUPLICATE, response.getStatus().getStatus());
        Assert.assertEquals("Category-supplier-vendor already suspended",
                response.getStatus().getMessage());
    }

    @Test
    public void testSuspendCategorySupplierVendorSuccessButAlreadyAlive() {
        Mockito.when(categorySupplierVendorsService.getCategorySupplierVendors(Mockito.any()))
                .thenReturn(CategorySupplierVendors.GetCategorySupplierVendorsResponse.newBuilder()
                        .setStatus(CategorySupplierVendors.ResponseStatus.SUCCESS)
                        .setMessage("Found 1 entries")
                        .addCategorySupplierVendors(CategorySupplierVendors.CategorySupplierVendor.newBuilder()
                                .setState(CategorySupplierVendors.CategorySupplierVendor.State.ALIVE)
                                .build())
                        .build());
        Mockito.when(categorySupplierVendorsService.addCategorySupplierVendor(Mockito.any()))
                .thenReturn(CategorySupplierVendors.AddCategorySupplierVendorResponse.newBuilder()
                        .setStatus(CategorySupplierVendors.ResponseStatus.SUCCESS)
                        .setMessage("Successfully added entry")
                        .build());
        Mockito.when(categorySupplierVendorsService.removeCategorySupplierVendors(Mockito.any()))
                .thenReturn(CategorySupplierVendors.RemoveCategorySupplierVendorsResponse.newBuilder()
                        .setStatus(CategorySupplierVendors.ResponseStatus.SUCCESS)
                        .setMessage("Successfully removed")
                        .build());

        AliasMaker.SuspendCategorySupplierVendorRequest request =
                AliasMaker.SuspendCategorySupplierVendorRequest.getDefaultInstance();

        AliasMaker.SuspendCategorySupplierVendorResponse response = service.suspendCategorySupplierVendor(request);

        Mockito.verify(categorySupplierVendorsService, Mockito.times(1)).getCategorySupplierVendors(Mockito.any());
        Mockito.verify(categorySupplierVendorsService, Mockito.times(1)).addCategorySupplierVendor(Mockito.any());
        Mockito.verify(categorySupplierVendorsService, Mockito.times(1)).removeCategorySupplierVendors(Mockito.any());

        Assert.assertEquals(AliasMaker.OperationStatus.SUCCESS, response.getStatus().getStatus());
        Assert.assertEquals("Successfully added entry",
                response.getStatus().getMessage());
    }

    @Test
    public void testSuspendCategorySupplierVendorFailedInternalError() {
        Mockito.when(categorySupplierVendorsService.getCategorySupplierVendors(Mockito.any()))
                .thenThrow(new RuntimeException("exception"));

        AliasMaker.SuspendCategorySupplierVendorRequest request =
                AliasMaker.SuspendCategorySupplierVendorRequest.getDefaultInstance();

        AliasMaker.SuspendCategorySupplierVendorResponse response = service.suspendCategorySupplierVendor(request);

        Mockito.verify(categorySupplierVendorsService, Mockito.times(1)).getCategorySupplierVendors(Mockito.any());
        Mockito.verify(categorySupplierVendorsService, Mockito.never()).addCategorySupplierVendor(Mockito.any());
        Mockito.verify(categorySupplierVendorsService, Mockito.never()).removeCategorySupplierVendors(Mockito.any());

        Assert.assertEquals(AliasMaker.OperationStatus.INTERNAL_ERROR, response.getStatus().getStatus());
        Assert.assertEquals("exception",
                response.getStatus().getMessage());
    }

}
