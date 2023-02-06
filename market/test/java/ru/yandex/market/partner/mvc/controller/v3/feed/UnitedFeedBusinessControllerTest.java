package ru.yandex.market.partner.mvc.controller.v3.feed;

import java.net.URISyntaxException;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.feed.datacamp.FeedParsingType;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StandardLocation;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarUtils;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.core.feed.datacamp.FeedParsingType.UPDATE_FEED;
import static ru.yandex.market.core.feed.model.FeedType.ASSORTMENT;

public class UnitedFeedBusinessControllerTest extends FunctionalTest {

    @Autowired
    private FeedFileStorage feedFileStorage;

    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;

    @Autowired
    private MbiBpmnClient mbiBpmnClient;


    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidationBusiness.before.csv",
            after = "UnitedFeedController/csv/createFeed/businessCorrectData.after.csv"
    )
    @Test
    @DisplayName("Создание фида бизнеса. По ссылке")
    void createFeed_businessCorrectData_successful() throws URISyntaxException {
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2010", "20774", "COMPLETE"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/business.url.response.json");
        checkFeedProcessorEvent(1L, ASSORTMENT, FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE);
    }

    @DbUnitDataSet(
            before = "UnitedFeedController/csv/createFeed/completeValidationBusiness.before.csv",
            after = "UnitedFeedController/csv/createFeed/businessCorrectDataUpload.after.csv"
    )
    @Test
    @DisplayName("Создание фида бизнеса. По файлу")
    void createFeed_businessUploadCreated_successful() throws URISyntaxException {
        Mockito.when(feedFileStorage.getUrl(eq(StandardLocation.supplierFeed(20774L, 1L))))
                .thenReturn("http://test.feed2.ru");
        var response = FunctionalTestHelper.post(buildCreateFeedUrl("2011", "20774", "UPDATE"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/business.upload.response.json");

        assertFeedParsingType(20774, 1,
                FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.UPDATE, true);
    }


    @DbUnitDataSet(
            before = {"UnitedFeedController/csv/createFeed/completeValidationBusiness.before.csv",
                    "UnitedFeedController/csv/createFeed/feedByBusinessUpdate.before.csv"}
    )
    @Test
    @DisplayName("Обновление существующего фида партнера по id.")
    void updateFeed_businessUploadCorrectData_successful() throws URISyntaxException {
        Mockito.when(feedFileStorage.getUrl(eq(StandardLocation.supplierFeed(20774L, 11L))))
                .thenReturn("http://test.feed2.ru");
        var response = FunctionalTestHelper.put(
                buildUpdateFeedUrl("2012", "11", "20774", "UPDATE"),
                null
        );
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedController/json/createFeed/business.update.response.json");

        assertFeedParsingType(20774L, 11L,
                FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.UPDATE,true);
    }

    @Test
    @Disabled("TBD:MBI-78371")
    @DisplayName("Удаление ссылочного фида под бизнесом.")
    @DbUnitDataSet(
            before = {"UnitedFeedController/csv/createFeed/completeValidationBusiness.before.csv",
                    "UnitedFeedController/csv/createFeed/feedByBusinessDelete.before.csv"},
            after = "UnitedFeedController/csv/createFeed/feedByBusinessDelete.after.csv"
    )
    void removeSupplierFeed_supported_convertToDefaultAndHideOffers() throws URISyntaxException {
        FunctionalTestHelper.delete(buildRemoveFeedUrl("20774", "11"));

        Map<String, Object> expectedParams = Map.of(
                "needHide", "true",
                "needMigrate", "false",
                "partnerId", "774",
                "feedId", "11",
                "operationId", "1"
        );

        // checkCamundaRequest(expectedParams);
        checkFeedProcessorEvent(11, ASSORTMENT,
                FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE);
    }

    @Test
    @DisplayName("Удаление бизнесового фида. Фид становится дефолтным")
    @DbUnitDataSet(
            before = "UnitedFeedController/csv/removeFeed/business.before.csv",
            after = "UnitedFeedController/csv/removeFeed/supplier.after.csv"
    )
    void removeBusinessFeedTest() throws URISyntaxException {
        FunctionalTestHelper.delete(buildRemoveFeedUrl("774", "11"));

        Mockito.verifyNoInteractions(mbiBpmnClient);
        checkFeedProcessorEvent(11, ASSORTMENT, FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType.UPDATE_TYPE_CREATE);
    }

    private void assertFeedParsingType(long partnerId, long feedId,
                                       FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType expectedType,
                                       boolean isUpload) {
        var eventCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher)
                .publishEvent(eventCaptor.capture());

        FeedProcessorUpdateRequestEvent event = eventCaptor.getValue();
        assertThat(event.getPayload())
                .extracting(e -> e.getFeed().getShopId(), e -> e.getFeed().getFeedId(),
                        FeedUpdateTaskOuterClass.FeedUpdateTask::getFeedParsingType,
                        e -> e.getShopsDatParameters().getIsUpload())
                .containsExactly(partnerId, feedId, expectedType, isUpload);
    }

    private String buildCreateFeedUrl(String validationId, String businessId,
                                      String type) throws URISyntaxException {
        return new URIBuilder(baseUrl)
                .setPathSegments("businesses", businessId, "feed")
                .setParameter("validation_id", validationId)
                .setParameter("type", type)
                .build()
                .toString();
    }

    private String buildUpdateFeedUrl(String validationId, String feedId, String businessId,
                                      String type) throws URISyntaxException {
        return new URIBuilder(baseUrl)
                .setPathSegments("businesses", businessId, "feed", feedId)
                .setParameter("validation_id", validationId)
                .setParameter("type", type)
                .build()
                .toString();
    }

    private String buildRemoveFeedUrl(String businessId, String feedId) throws URISyntaxException {
        return new URIBuilder(baseUrl)
                .setPathSegments("businesses", businessId, "feed", feedId)
                .build()
                .toString();
    }


    private void checkFeedProcessorEvent(long feedId, FeedType feedType,
                                         FeedUpdateTaskOuterClass.FeedUpdateTask.FeedUpdateType updateType) {
        ArgumentCaptor<FeedProcessorUpdateRequestEvent> captor =
                ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher,
                Mockito.times(1)).publishEvent(captor.capture());
        FeedUpdateTaskOuterClass.FeedUpdateTask event = captor.getValue().getPayload();
        Assertions.assertEquals(updateType,
                event.getFeedUpdateType());
        Assertions.assertEquals(feedId, event.getFeed().getFeedId());
        Assertions.assertEquals(SamovarUtils.toFeedType(feedType), event.getFeed().getFeedType());
    }
}
