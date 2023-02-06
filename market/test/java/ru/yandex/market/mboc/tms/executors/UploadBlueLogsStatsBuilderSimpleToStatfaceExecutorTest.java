package ru.yandex.market.mboc.tms.executors;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.statface.StatfaceUpsertService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.tms.executors.statface.UploadBlueLogsStatsSimpleToStatfaceExecutor;

import static ru.yandex.market.mboc.common.utils.OfferTestUtils.TEST_SUPPLIER_ID;

@SuppressWarnings("checkstyle:magicnumber")
public class UploadBlueLogsStatsBuilderSimpleToStatfaceExecutorTest extends BaseDbTestClass {

    private UploadBlueLogsStatsSimpleToStatfaceExecutor executor;
    private String yqlStatFacePath = "TestPath/daily";
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private JdbcTemplate yqlJdbcTemplate;
    @Autowired
    @Qualifier("sqlJdbcTemplate")
    private JdbcTemplate sqlJdbcTemplate;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    @Before
    public void setUp() {
        categoryCachingServiceMock = new CategoryCachingServiceMock();
        yqlJdbcTemplate = Mockito.mock(JdbcTemplate.class);
        executor = new UploadBlueLogsStatsSimpleToStatfaceExecutor(new StatfaceUpsertService(yqlJdbcTemplate),
            categoryCachingServiceMock, sqlJdbcTemplate, yqlStatFacePath);

        categoryCachingServiceMock.addCategory(new Category()
            .setCategoryId(1)
            .setName("Level 1 _1"));
        categoryCachingServiceMock.addCategory(new Category()
            .setCategoryId(2)
            .setName("Level 1 _2"));
        categoryCachingServiceMock.addCategory(new Category()
            .setCategoryId(3)
            .setName("Level 2 _1_1")
            .setParentCategoryId(1));
        categoryCachingServiceMock.addCategory(new Category()
            .setCategoryId(4)
            .setName("Level 2 _1_2")
            .setParentCategoryId(1));
        categoryCachingServiceMock.addCategory(new Category()
            .setCategoryId(5)
            .setName("Level 3 _1_1_1")
            .setParentCategoryId(3));
        categoryCachingServiceMock.addCategory(new Category()
            .setCategoryId(6)
            .setName("Level 2 _2_1")
            .setParentCategoryId(2));

        supplierRepository.insert(new Supplier().setId(TEST_SUPPLIER_ID).setName("test"));
        offerRepository.insertOffer(OfferTestUtils.nextOffer()
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_MODERATION)
            .setCategoryIdForTests(3L, Offer.BindingKind.SUGGESTED));
        offerRepository.insertOffer(OfferTestUtils.nextOffer()
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS)
            .setCategoryIdForTests(3L, Offer.BindingKind.SUGGESTED));
        offerRepository.insertOffer(OfferTestUtils.nextOffer()
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS)
            .setCategoryIdForTests(5L, Offer.BindingKind.SUGGESTED));
        offerRepository.insertOffer(OfferTestUtils.nextOffer()
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION)
            .setCategoryIdForTests(5L, Offer.BindingKind.SUGGESTED));
        offerRepository.insertOffer(OfferTestUtils.nextOffer()
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECLASSIFICATION)
            .setCategoryIdForTests(6L, Offer.BindingKind.SUGGESTED));
        offerRepository.insertOffer(OfferTestUtils.nextOffer()
            .setProcessingStatusInternal(Offer.ProcessingStatus.WAIT_CONTENT)
            .setCategoryIdForTests(6L, Offer.BindingKind.SUGGESTED));
        // should not be found
        offerRepository.insertOffer(OfferTestUtils.nextOffer()
            .setProcessingStatusInternal(Offer.ProcessingStatus.WAIT_CONTENT)
            .setCategoryIdForTests(10L, Offer.BindingKind.SUGGESTED));
    }

    @Test
    public void shouldCreateDailyStats() {
        LocalDate now = LocalDate.now();
        executor.execute();
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(yqlJdbcTemplate, Mockito.times(1))
            .execute(queryCaptor.capture());
        String value = queryCaptor.getValue();
        Assertions.assertThat(value).contains("USE hahn;\n" +
            "PRAGMA yt.InferSchema = \"100\";\n" +
            "PRAGMA yt.IgnoreYamrDsv;\n" +
            "UPSERT INTO stat.`TestPath/daily`\n" +
            "(fielddate,category,total,in_moderation,in_process,in_classification,in_reclassification,wait_content," +
            "re_sort,need_info,need_size_measure,no_size_measure_values)\n" +
            "VALUES\n");
        Assertions.assertThat(value)
            .contains("(\"" + now + "\",\"\\tВсе товары\\t\",6,1,2,1,1,1,0,0,0,0)");
        Assertions.assertThat(value)
            .contains("(\"" + now + "\",\"\\tВсе товары\\tLevel 1 _2\\t\",2,0,0,0,1,1,0,0,0,0),\n");
        Assertions.assertThat(value)
            .contains("(\"" + now + "\",\"\\tВсе товары\\tLevel 1 _2\\tLevel 2 _2_1\\t\",2,0,0,0,1,1,0,0,0,0),\n");
        Assertions.assertThat(value)
            .contains("(\"" + now + "\",\"\\tВсе товары\\tLevel 1 _1\\t\",4,1,2,1,0,0,0,0,0,0),\n");
        Assertions.assertThat(value)
            .contains("(\"" + now + "\",\"\\tВсе товары\\tLevel 1 _1\\tLevel 2 _1_1\\t\",4,1,2,1,0,0,0,0,0,0),\n");
        Assertions.assertThat(value)
            .contains("(\"" + now + "\",\"\\tВсе товары\\tLevel 1 _1\\tLevel 2 _1_1\\tLevel 3 _1_1_1\\t\"," +
                "2,0,1,1,0,0,0,0,0,0),\n");
    }
}
