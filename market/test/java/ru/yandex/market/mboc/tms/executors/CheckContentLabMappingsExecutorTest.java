package ru.yandex.market.mboc.tms.executors;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.mail.EmailService;
import ru.yandex.market.mboc.common.services.managers.ManagersServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

/**
 * @author kravchenko-aa
 * @date 2019-07-08
 */
@SuppressWarnings("checkstyle:magicnumber")
public class CheckContentLabMappingsExecutorTest extends BaseDbTestClass {
    private static final String EMAIL = "test_email";
    private static final DateTimeFormatter DATE_FORMAT = CheckContentLabMappingsExecutor.DATE_FORMAT;

    private CheckContentLabMappingsExecutor executor;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;

    private EmailService emailService = Mockito.mock(EmailService.class);
    @Autowired
    private SupplierRepository supplierRepository;
    private ManagersServiceMock managersService = new ManagersServiceMock();
    private ModelStorageCachingServiceMock modelStorageCachingService = new ModelStorageCachingServiceMock();
    private CategoryCachingServiceMock categoryCachingService = new CategoryCachingServiceMock().enableAuto();

    @Before
    public void init() {
        executor = new CheckContentLabMappingsExecutor(offerBatchProcessor,
            emailService, supplierRepository, managersService,
            modelStorageCachingService, categoryCachingService, EMAIL);
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
    }

    @Test
    public void testNotSendReportForEmptyOfferList() {
        supplierRepository.insert(new Supplier(1, "abibas.prod", "", ""));
        supplierRepository.insert(
            new Supplier(2, "abibas.test", "", "").setTestSupplier(true));
        offerRepository.insertOffers(
            OfferTestUtils.simpleOffer()
                .setId(1)
                .setShopSku("not-in-content-lab")
                .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
                .setThroughContentLab(false)
                .setBusinessId(1)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), Offer.MappingConfidence.CONTENT),
            OfferTestUtils.simpleOffer()
                .setId(2)
                .setShopSku("without-approved-mapping")
                .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
                .setThroughContentLab(true)
                .setBusinessId(1)
                .setSuggestSkuMapping(OfferTestUtils.mapping(2L)),
            OfferTestUtils.simpleOffer()
                .setId(3)
                .setShopSku("test-supplier")
                .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
                .setThroughContentLab(true)
                .setBusinessId(2)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(3L), Offer.MappingConfidence.CONTENT),
            OfferTestUtils.simpleOffer()
                .setId(4)
                .setShopSku("offer-in-filter-and-mapping-is-correct")
                .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
                .setThroughContentLab(true)
                .setBusinessId(1)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(4L), Offer.MappingConfidence.CONTENT)
        );
        modelStorageCachingService.addModel(new Model()
            .setId(4L)
            .setCategoryId(42)
            .setModelType(Model.ModelType.SKU)
            .setTitle("Test title")
            .setPublishedOnBlueMarket(true));

        assertNull(executor.doWork());
        Mockito.verify(emailService, Mockito.never())
            .mailCheckContentLabReport(any(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    public void testGenerateExcelFile() {
        supplierRepository.insert(new Supplier(1, "abibas.prod", "", ""));
        offerRepository.insertOffers(
            OfferTestUtils.simpleOffer()
                .setId(1)
                .setShopSku("sku-not-published-1")
                .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
                .setThroughContentLab(true)
                .setBusinessId(1)
                .setCategoryIdForTests(42L, Offer.BindingKind.SUGGESTED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), Offer.MappingConfidence.CONTENT),
            OfferTestUtils.simpleOffer()
                .setId(2)
                .setShopSku("sku-published")
                .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
                .setThroughContentLab(true)
                .setBusinessId(1)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(2L), Offer.MappingConfidence.CONTENT),
            OfferTestUtils.simpleOffer()
                .setId(3)
                .setShopSku("sku-not-published-2")
                .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
                .setThroughContentLab(true)
                .setBusinessId(1)
                .setCategoryIdForTests(4242L, Offer.BindingKind.SUGGESTED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(3L), Offer.MappingConfidence.CONTENT)
        );
        Instant createdDate1 = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant createdDate2 = Instant.now().minus(8, ChronoUnit.DAYS);
        modelStorageCachingService.addModel(new Model()
            .setId(1L)
            .setCategoryId(1)
            .setModelType(Model.ModelType.SKU)
            .setTitle("Test title 1")
            .setPublishedOnBlueMarket(false)
            .setCreatedTs(createdDate1));
        modelStorageCachingService.addModel(new Model()
            .setId(2L)
            .setCategoryId(1)
            .setModelType(Model.ModelType.SKU)
            .setTitle("Test title 2")
            .setPublishedOnBlueMarket(true));
        modelStorageCachingService.addModel(new Model()
            .setId(3L)
            .setCategoryId(1)
            .setModelType(Model.ModelType.SKU)
            .setTitle("Test title 3")
            .setPublishedOnBlueMarket(false)
            .setCreatedTs(createdDate2));
        managersService.addUser(42, new MboUser().setStaffLogin("test_user"));
        managersService.addUser(4242, new MboUser().setStaffLogin("YOU"));

        ExcelFile expected = new ExcelFile.Builder().addHeaders(CheckContentLabMappingsExecutor.HEADERS)
            .addLine(DATE_FORMAT.format(createdDate2), "8", "3", "Test title 3", "3", "1", "4242",
                "auto-category #4242", "YOU")
            .addLine(DATE_FORMAT.format(createdDate1), "1", "1", "Test title 1", "1", "1", "42",
                "auto-category #42", "test_user")
            .build();

        assertEquals(expected, executor.doWork());
        Mockito.verify(emailService, Mockito.times(1))
            .mailCheckContentLabReport(any(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    public void testNotAddOfferToReportIfSkuFromMappingNotFound() {
        supplierRepository.insert(new Supplier(1, "abibas.prod", "", ""));
        offerRepository.insertOffers(
            OfferTestUtils.simpleOffer()
                .setId(1)
                .setShopSku("sku-not-published-1")
                .setThroughContentLab(true)
                .setBusinessId(1)
                .setCategoryIdForTests(42L, Offer.BindingKind.SUGGESTED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(1L), Offer.MappingConfidence.CONTENT),
            OfferTestUtils.simpleOffer()
                .setId(2)
                .setShopSku("sku-not-found")
                .setThroughContentLab(true)
                .setBusinessId(1)
                .setCategoryIdForTests(42L, Offer.BindingKind.SUGGESTED)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(3L), Offer.MappingConfidence.CONTENT)
        );
        Instant createdDate1 = Instant.now().minus(1, ChronoUnit.DAYS);
        modelStorageCachingService.addModel(new Model()
            .setId(1L)
            .setCategoryId(1)
            .setModelType(Model.ModelType.SKU)
            .setTitle("Test title 1")
            .setPublishedOnBlueMarket(false)
            .setCreatedTs(createdDate1));
        managersService.addUser(42, new MboUser().setStaffLogin("test_user"));

        ExcelFile expected = new ExcelFile.Builder().addHeaders(CheckContentLabMappingsExecutor.HEADERS)
            .addLine(DATE_FORMAT.format(createdDate1), "1", "1", "Test title 1", "1", "1", "42",
                "auto-category #42", "test_user")
            .build();

        assertEquals(expected, executor.doWork());

        Mockito.verify(emailService, Mockito.times(1))
            .mailCheckContentLabReport(any(), anyInt(), anyInt(), anyInt(), any());
    }
}
