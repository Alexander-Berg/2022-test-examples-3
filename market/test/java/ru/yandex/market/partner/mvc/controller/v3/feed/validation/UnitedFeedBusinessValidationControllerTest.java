package ru.yandex.market.partner.mvc.controller.v3.feed.validation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.feed.validation.model.FeedValidationLogbrokerEvent;
import ru.yandex.market.core.logbroker.samovar.SamovarEvent;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.environment.TestEnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static Market.DataCamp.API.UpdateTask.FeedClass.FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE;
import static Market.DataCamp.API.UpdateTask.FeedClass.FEED_CLASS_COMPLETE;
import static org.mockito.Mockito.times;

@DbUnitDataSet(before = "UnitedFeedValidationController/csv/result/business.before.csv")
public class UnitedFeedBusinessValidationControllerTest extends FunctionalTest {

    @Autowired
    @Qualifier("samovarLogbrokerService")
    private LogbrokerService samovarLogBrokerService;
    @Autowired
    @Qualifier("qParserLogBrokerService")
    private LogbrokerService qParserLogBrokerService;

    @Autowired
    private TestEnvironmentService environmentService;

    @BeforeEach
    void init() {
        environmentService.setEnvironmentType(EnvironmentType.DEVELOPMENT);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("environment");
    }

    @DbUnitDataSet(
            after = "UnitedFeedValidationController/csv/validate/business.after.csv"
    )
    @Test
    @DisplayName("Запуск процесса валидации фида. Бизнес по ссылке.")
    void validate_supplier_successful()
            throws IOException, URISyntaxException {
        assertJson("business", "1111");
        Mockito.verifyZeroInteractions(qParserLogBrokerService);
        var eventCaptor = ArgumentCaptor.forClass(SamovarEvent.class);
        Mockito.verify(samovarLogBrokerService, times(1))
                .publishEvent(eventCaptor.capture());
        SamovarEvent event = eventCaptor.getValue();
        Assertions.assertThat(event.getPayload().hasUrl()).isTrue();
    }

    @DbUnitDataSet(
            after = "UnitedFeedValidationController/csv/validate/business.upload.after.csv"
    )
    @Test
    @DisplayName("Запуск процесса валидации фида. Бизнес по загруженному фиду.")
    void validate_supplierUpload_successful() throws IOException, URISyntaxException {
        assertJson("business.upload", "1111");
        var eventCaptor = ArgumentCaptor.forClass(FeedValidationLogbrokerEvent.class);

        Mockito.verify(qParserLogBrokerService, times(1))
                .publishEvent(eventCaptor.capture());
        Mockito.verifyZeroInteractions(samovarLogBrokerService);

        FeedValidationLogbrokerEvent event = eventCaptor.getValue();
        Assertions.assertThat(event.getPayload()
                        .getFeedParsingTask()
                        .getType())
                .isEqualTo(FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE);
        Assertions.assertThat(event.getPayload().getFeedParsingTask().hasShopId()).isFalse();
        Assertions.assertThat(event.getPayload().getFeedParsingTask().getBusinessId()).isEqualTo(1111);
        Assertions.assertThat(event.getPayload().getFeedParsingTask().hasTimestamp()).isTrue();
        Assertions.assertThat(event.getPayload().getFeedParsingTask().getShopsDatParameters().hasColor()).isFalse();
    }

    @Test
    void getResultByBusinessTest() throws URISyntaxException {
        ResponseEntity<String> response = FunctionalTestHelper.get(buildValidationResultUrl("1111", "2014"));
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedValidationController/json/result/2014.json");
    }

    private void assertJson(String test, String businessId) throws URISyntaxException, IOException {
        ResponseEntity<String> response = FunctionalTestHelper.post(buildUpdateFeedUrl(businessId),
                IOUtils.toString(this.getClass()
                        .getResourceAsStream(
                                "UnitedFeedValidationController/json/validate/" + test + ".body.json"
                        ), StandardCharsets.UTF_8
                )
        );
        JsonTestUtil.assertEquals(response, this.getClass(),
                "UnitedFeedValidationController/json/validate/" + test + ".json");
    }

    private String buildUpdateFeedUrl(String businessId) throws URISyntaxException {
        return new URIBuilder(baseUrl)
                .setPathSegments("businesses", businessId, "feed", "validation")
                .build()
                .toString();
    }

    private String buildValidationResultUrl(String businessId, String validationId) throws URISyntaxException {
        return new URIBuilder(baseUrl)
                .setPathSegments("businesses", businessId, "feed", "validation", validationId)
                .build()
                .toString();
    }
}
