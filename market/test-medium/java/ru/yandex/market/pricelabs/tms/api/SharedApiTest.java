package ru.yandex.market.pricelabs.tms.api;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.pricelabs.MockMvcProxy;
import ru.yandex.market.pricelabs.MockMvcProxyHttpException;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersArg;
import ru.yandex.market.pricelabs.tms.services.database.TasksService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.assertThrowsWithMessage;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;

class SharedApiTest extends AbstractTmsSpringConfiguration {

    private static final String GENERATION = "20190701_0000";
    private static final String TABLE = "none";

    private static final int SHOP_ID = 1;
    private static final int FEED_ID_2 = 2;
    private static final int FEED_ID_3 = 3;

    @Autowired
    private SharedApi sharedApiBean;
    private SharedApiInterfaces sharedApi;

    @Autowired
    private TasksService tasksService;

    @BeforeEach
    void init() {
        sharedApi = MockMvcProxy.buildProxy(SharedApiInterfaces.class, sharedApiBean);

        testControls.initOnce(this.getClass(), () ->
                testControls.saveShop(shop(SHOP_ID, s -> s.setFeeds(Set.of(1L, (long) FEED_ID_2, (long) FEED_ID_3)))));

        testControls.cleanupTasksService();
        testControls.initShopLoopJob();
    }

    @Test
    void testValidApi() {
        assertNotNull(getValidator().getConstraintsForClass(SharedApi.class));
    }

    @Test
    void versionGet() {
        assertNotNull(sharedApi.versionGet().getBody());
    }

    @Test
    void scheduleOffersPostInvalid() {
        assertThrowsWithMessage(MockMvcProxyHttpException.class, () ->
                sharedApi.scheduleOffersPost(null, List.of(), false, null, false),
                "Required Integer parameter 'shopId' is not present");
    }

    @Test
    void scheduleOffersPostUnknownShop1() {
        checkResponseIsOk(sharedApi.scheduleOffersPost(2, List.of(), false, null, false));
    }

    @Test
    void scheduleOffersPostUnknownShop2() {
        checkResponseIsOk(sharedApi.scheduleOffersPost(2, List.of(), true, null, false));
    }

    @Test
    void scheduleOffersPostUnknownShop3() {
        checkResponseIsOk(sharedApi.scheduleOffersPost(2, List.of(2), false, null, false));
    }

    private void checkActiveTask() {
        var task = testControls.startScheduledTask(JobType.SHOP_LOOP_FULL_PRIORITY);
        assertEquals(
                new OffersArg()
                        .setShopId(SHOP_ID)
                        .setType(ShopType.DSBS)
                        .setCluster(testControls.getCurrentCluster())
                        .setIndexer(testControls.getCurrentIndexer())
                        .setGeneration(GENERATION)
                        .setCategoriesTable(GENERATION),
                task.getArgs()
        );
        testControls.completeTaskSuccess(task);
    }

    private void checkResponseIsOk(ResponseEntity<?> response) {
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
