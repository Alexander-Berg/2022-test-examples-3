package ru.yandex.market.mboc.common.erp;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbo.tracker.utils.IssueStatus;
import ru.yandex.market.mbo.tracker.utils.IssueUtils;
import ru.yandex.market.mbo.tracker.utils.TrackerConstants;
import ru.yandex.market.mboc.common.BaseIntegrationTestClass;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.Component;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.SearchRequest;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.mapping;

/**
 * @author yuramalinov
 * @created 28.08.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
@Ignore("Тесты нужно переделать, чтобы не так сильно нагружать трекер кучей ненужных тикетов")
public class ErpSyncServiceTest extends BaseIntegrationTestClass {
    private static final String QUEUE = "MCPTEST";

    @Qualifier("erpNamedParameterJdbcTemplate")
    @Autowired
    private NamedParameterJdbcTemplate erpTemplate;

    @Autowired
    private ErpSyncService erpSyncService;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private Session trackerClient;

    private Component component;

    private String started;
    private boolean shouldCleanup;

    @Before
    public void setUp() {
        started = String.valueOf(System.currentTimeMillis());
        erpSyncService.setTestComment(started);

        supplierRepository.insert(new Supplier(1, "test")
            .setType(MbocSupplierType.REAL_SUPPLIER)
            .setRealSupplierId("911"));

        supplierRepository.insert(new Supplier(2, "third party")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setRealSupplierId(null));

        ReflectionTestUtils.setField(erpSyncService, "queue", QUEUE);
        if (component == null) {
            component = erpSyncService.findOrCreateComponent();
        }

        shouldCleanup = true;
    }

    @After
    public void tearDown() {
        if (!shouldCleanup) {
            return;
        }
        List<Issue> issues = await().until(() -> findOpenIssues(component, started), v -> !v.isEmpty());
        issues.forEach(IssueUtils::closeIssue);
        await().until(() -> findOpenIssues(component, started).isEmpty());
    }

    @Test
    public void testItCreatesTicket() {
        insertCommonData();

        erpSyncService.checkAndNotify();

        await().until(() -> {
            List<Issue> issues = findOpenIssues(component, started);
            if (issues.isEmpty()) {
                return false;
            }

            assertEquals(1, issues.size());
            Issue issue = issues.get(0);
            assertEquals(1, issue.getComments().count());

            Comment comment = issue.getComments().iterator().next();
            verifyCommonComment(comment);
            return true;
        });
    }

    @Test
    @Ignore("Timeouts accessing tracker API")
    public void testItReusesTicketInCaseItExists() {
        insertCommonData();

        // Call it once
        erpSyncService.checkAndNotify();

        await().until(() -> findOpenIssues(component, started),
            it -> !it.isEmpty() && it.get(0).getComments().count() == 1);

        offerRepository.insertOffer(offer("test25").updateApprovedSkuMapping(mapping(21), CONTENT));

        // Call it twice
        erpSyncService.checkAndNotify();

        List<Issue> issues = await().until(() -> findOpenIssues(component, started),
            it -> !it.isEmpty() && it.get(0).getComments().count() == 2);

        assertEquals(1, issues.size());
        Issue issue = issues.get(0);
        assertEquals(2, issue.getComments().count());

        Iterator<Comment> comments = issue.getComments();
        comments.next();

        Comment secondComment = comments.next();
        verifyCommonComment(secondComment, "Нет в ERP - 5");
    }

    private void verifyCommonComment(Comment comment) {
        verifyCommonComment(comment, "Нет в ERP - 4");
    }

    private void verifyCommonComment(Comment comment, String notInErpCheck) {
        Assertions.assertThat(comment.getText().get()).contains(
            "Отличаются MSKU - 1", // 3
            notInErpCheck, // 4, 5, 6
            "Нет в КИ - 1" // absent
        );
    }

    private void insertCommonData() {
        offerRepository.insertOffers(Arrays.asList(
            offer("test1", 1).updateApprovedSkuMapping(mapping(1), CONTENT),
            offer("test2", 1).updateApprovedSkuMapping(mapping(4), CONTENT),
            offer("test3", 1).updateApprovedSkuMapping(mapping(3), CONTENT),
            offer("test4", 1).updateApprovedSkuMapping(mapping(3), CONTENT),
            offer("test5", 1).updateApprovedSkuMapping(mapping(3), CONTENT),
            offer("test6", 1).updateApprovedSkuMapping(mapping(3), CONTENT),
            // mapping is different but ok as it's deleted
            offer("test7", 1).updateApprovedSkuMapping(mapping(0), CONTENT),
            // Not found and OK
            offer("test8", 1).updateApprovedSkuMapping(mapping(0), CONTENT),

            // Third party supplier

            // Found
            offer("test9", 2).updateApprovedSkuMapping(mapping(1), CONTENT),
            // Not found
            offer("test10", 2).updateApprovedSkuMapping(mapping(4), CONTENT),

            // mapping is different but ok as it's deleted
            offer("test11", 2).updateApprovedSkuMapping(mapping(0), CONTENT),
            // Not found and OK
            offer("test12", 2).updateApprovedSkuMapping(mapping(0), CONTENT)
        ));

        erpTemplate.batchUpdate(
            "insert into MBOMapCheckOUT (RS_ID, RSSKU, RSSKU_NAME, MSKU, EXPORT_TS, ISDELETEDINMBO, SUPPLIER_ID) " +
                "values (:real_supplier_id, :ssku, :title, :msku, now(), :deleted, :supplier_id)",
            new MapSqlParameterSource[]{
                erpData("911", "test1", "Title   ", 1, 0, 465852),
                erpData("911", "test2", "different-title", 4, 0, 465852),
                erpData("911", "test3", "Title", 2, 0, 465852),
                erpData("911", "absent-sku", "title", 3, 0, 465852),
                erpData("911", "test7", "Title", 3, 1, 465852),
                erpData("ERP_STRANGE_RS_ID", "test9", "Title", 1, 0, 2),
                erpData("ERP_STRANGE_RS_ID", "test11", "Title", 3, 1, 2)
            }
        );
    }

    private MapSqlParameterSource erpData(String realSupplierId,
                                          String ssku,
                                          String title,
                                          long msku,
                                          int deleted,
                                          int supplierId) {
        return new MapSqlParameterSource()
            .addValue("real_supplier_id", realSupplierId)
            .addValue("ssku", ssku)
            .addValue("title", title)
            .addValue("msku", msku)
            .addValue("deleted", deleted)
            .addValue("supplier_id", supplierId);
    }

    @Test
    public void testItDoesntCreateIssueIfNoProblemsFound() {
        shouldCleanup = false;
        erpSyncService.checkAndNotify();

        // Unfortunately, no way to gently await that something doesn't appear
        ThreadUtils.sleep(500);
        List<Issue> issues = findOpenIssues(component, started);
        Assertions.assertThat(issues).isEmpty();
    }

    private Offer offer(String shopSku) {
        return offer(shopSku, 1);
    }

    private Offer offer(String shopSku, int supplierId) {
        return new Offer().setBusinessId(supplierId)
            .setShopSku(shopSku).setTitle("Title");
    }

    private List<Issue> findOpenIssues(Component component, String testComment) {
        return trackerClient.issues().find(SearchRequest.builder()
            .filter(TrackerConstants.QUEUE, QUEUE)
            .filter(TrackerConstants.AUTHOR, TrackerConstants.ME)
            .filter(TrackerConstants.TEST_COMMENT, testComment)
            .filter(TrackerConstants.COMPONENTS, component.getId())
            .filter(TrackerConstants.STATUS, IssueStatus.OPEN.getIssueKey())
            .build()).toList();
    }

    private ConditionFactory await() {
        return Awaitility.await().pollDelay(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS);
    }
}
