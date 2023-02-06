package ru.yandex.market.ff4shops.api.xml;

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.idx.IndexerApiClient;
import ru.yandex.market.common.idx.model.Dimensions;
import ru.yandex.market.common.idx.model.FeedOfferId;
import ru.yandex.market.common.idx.model.SupplierFeedOfferIds;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.partner.dao.PartnerRepository;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.market.ff4shops.util.FfAsserts;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DimensionsController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "DimensionsControllerTest.csv")
class DimensionsControllerTest extends FunctionalTest {

    private static final long SERVICE_ID = 10L;
    private static final long SERVICE_ID_2 = 15L;

    @Autowired
    @Qualifier("indexerApiClient")
    private IndexerApiClient indexerApiClient;

    @Autowired
    @Qualifier("planeshiftApiClient")
    private IndexerApiClient planeshiftApiClient;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PartnerRepository partnerRepository;

    @Test
    void requestByItems() {

        String request = //language=xml
                "<root>\n" +
                "    <token>ff_token_1</token>\n" +
                "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                "    <request type=\"getReferenceItems\">\n" +
                "        <limit>10</limit>\n" +
                "        <offset>10</offset>\n" +
                "        <unitIds>\n" +
                "            <unitId>\n" +
                "                <id>1</id>\n" +
                "                <vendorId>100</vendorId>\n" +
                "                <article>AAA</article>\n" +
                "            </unitId>\n" +
                "            <unitId>\n" +
                "                <id>1</id>\n" +
                "                <vendorId>100</vendorId>\n" +
                "                <article>BBB</article>\n" +
                "            </unitId>\n" +
                "        </unitIds>\n" +
                "    </request>\n" +
                "</root>";

        expectRequestToIndexerByItems();

        String expectedResponse =  //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<root>\n" +
                "   <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                "   <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                "   <requestState>\n" +
                "      <isError>false</isError>\n" +
                "   </requestState>\n" +
                "   <response type=\"getReferenceItems\">\n" +
                "      <itemReferences>\n" +
                "         <itemReference>\n" +
                "            <unitId>\n" +
                "               <id>AAA</id>\n" +
                "               <vendorId>100</vendorId>\n" +
                "               <article>AAA</article>\n" +
                "            </unitId>\n" +
                "            <korobyte>\n" +
                "               <width>21</width>\n" +
                "               <height>22</height>\n" +
                "               <length>11</length>\n" +
                "               <weightGross>1.001</weightGross>\n" +
                "               <weightNet />\n" +
                "               <weightTare />\n" +
                "            </korobyte>\n" +
                "            <lifeTime />\n" +
                "            <item />\n" +
                "         </itemReference>\n" +
                "         <itemReference>\n" +
                "            <unitId>\n" +
                "               <id>BBB</id>\n" +
                "               <vendorId>100</vendorId>\n" +
                "               <article>BBB</article>\n" +
                "            </unitId>\n" +
                "            <korobyte>\n" +
                "               <width>1</width>\n" +
                "               <height>22</height>\n" +
                "               <length>11</length>\n" +
                "               <weightGross>1.001</weightGross>\n" +
                "               <weightNet />\n" +
                "               <weightTare />\n" +
                "            </korobyte>\n" +
                "            <lifeTime />\n" +
                "            <item />\n" +
                "         </itemReference>\n" +
                "      </itemReferences>\n" +
                "   </response>\n" +
                "</root>";

        ResponseEntity<String> response = getReferenceItems(request, SERVICE_ID);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody());
    }

    @Test
    void requestByItemsWithEmptyResponse() {

        String request = //language=xml
                "<root>\n" +
                "    <token>ff_token_1</token>\n" +
                "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                "    <request type=\"getReferenceItems\">\n" +
                "        <limit>1</limit>\n" +
                "        <offset>1000000</offset>\n" +
                "    </request>\n" +
                "</root>";

        String expectedResponse =  //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<root>\n" +
                "   <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                "   <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                "   <requestState>\n" +
                "      <isError>false</isError>\n" +
                "   </requestState>\n" +
                "   <response type=\"getReferenceItems\">\n" +
                "      <itemReferences>\n" +
                "         <itemReference />\n" +
                "      </itemReferences>\n" +
                "   </response>\n" +
                "</root>";

        ResponseEntity<String> response = getReferenceItems(request, SERVICE_ID);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody());
    }

    private void expectRequestToIndexerByItems() {
        when(indexerApiClient.getDimensions(eq(new SupplierFeedOfferIds(100L, 10L, List.of(
                new FeedOfferId(500L, "AAA"),
                new FeedOfferId(500L, "BBB"))))))
                .thenReturn(List.of(
                        new Dimensions(500, "AAA",
                                BigDecimal.valueOf(1001, 3),
                                BigDecimal.valueOf(21001, 3),
                                BigDecimal.valueOf(10001, 3),
                                BigDecimal.valueOf(20001, 3)),
                        new Dimensions(500, "BBB",
                                BigDecimal.valueOf(1001, 3),
                                BigDecimal.valueOf(21001, 3),
                                BigDecimal.valueOf(10001, 3),
                                BigDecimal.valueOf(1, 3))));
    }

    @Test
    void requestByLimit() {
        String request = //language=xml
                "<root>\n" +
                "    <token>ff_token_1</token>\n" +
                "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                "    <request type=\"getReferenceItems\">\n" +
                "        <limit>2</limit>\n" +
                "        <offset>1</offset>\n" +
                "    </request>\n" +
                "</root>";

        expectRequestToIndexerByLimit();
        String expectedResponse = //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<root>\n" +
                "   <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                "   <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                "   <requestState>\n" +
                "      <isError>false</isError>\n" +
                "   </requestState>\n" +
                "   <response type=\"getReferenceItems\">\n" +
                "      <itemReferences>\n" +
                "         <itemReference>\n" +
                "            <unitId>\n" +
                "               <id>DDD</id>\n" +
                "               <vendorId>95</vendorId>\n" +
                "               <article>DDD</article>\n" +
                "            </unitId>\n" +
                "            <korobyte>\n" +
                "               <width>31</width>\n" +
                "               <height>22</height>\n" +
                "               <length>11</length>\n" +
                "               <weightGross>1.001</weightGross>\n" +
                "               <weightNet />\n" +
                "               <weightTare />\n" +
                "            </korobyte>\n" +
                "            <lifeTime />\n" +
                "            <item />\n" +
                "         </itemReference>\n" +
                "         <itemReference>\n" +
                "            <unitId>\n" +
                "               <id>AAA</id>\n" +
                "               <vendorId>100</vendorId>\n" +
                "               <article>AAA</article>\n" +
                "            </unitId>\n" +
                "            <korobyte>\n" +
                "               <width>21</width>\n" +
                "               <height>22</height>\n" +
                "               <length>11</length>\n" +
                "               <weightGross>10.001</weightGross>\n" +
                "               <weightNet />\n" +
                "               <weightTare />\n" +
                "            </korobyte>\n" +
                "            <lifeTime />\n" +
                "            <item />\n" +
                "         </itemReference>\n" +
                "      </itemReferences>\n" +
                "   </response>\n" +
                "</root>";
        ResponseEntity<String> response = getReferenceItems(request, SERVICE_ID);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody());
    }

    private void expectRequestToIndexerByLimit() {
        when(indexerApiClient.getDimensions(eq(new SupplierFeedOfferIds(95L, 10L, List.of(
                new FeedOfferId(450L, "DDD"))))))
                .thenReturn(List.of(
                        new Dimensions(450, "DDD",
                                BigDecimal.valueOf(1001, 3),
                                BigDecimal.valueOf(21001, 3),
                                BigDecimal.valueOf(10001, 3),
                                BigDecimal.valueOf(30001, 3))
                ));
        when(indexerApiClient.getDimensions(eq(new SupplierFeedOfferIds(100L, 10L, List.of(
                new FeedOfferId(500L, "AAA"))))))
                .thenReturn(List.of(
                        new Dimensions(500, "AAA",
                                BigDecimal.valueOf(10001, 3),
                                BigDecimal.valueOf(21001, 3),
                                BigDecimal.valueOf(10001, 3),
                                BigDecimal.valueOf(20001, 3))
                ));
    }

    @Test
    void planeshiftIndexerRequest() {
        String request = //language=xml
                "<root>\n" +
                "    <token>ff_token_2</token>\n" +
                "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                "    <request type=\"getReferenceItems\">\n" +
                "        <limit>1</limit>\n" +
                "    </request>\n" +
                "</root>";

        expectRequestToIndexerWithPlaneshift();
        String expectedResponse = //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<root>\n" +
                "   <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                "   <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                "   <requestState>\n" +
                "      <isError>false</isError>\n" +
                "   </requestState>\n" +
                "   <response type=\"getReferenceItems\">\n" +
                "      <itemReferences>\n" +
                "         <itemReference>\n" +
                "            <unitId>\n" +
                "               <id>AAA</id>\n" +
                "               <vendorId>105</vendorId>\n" +
                "               <article>AAA</article>\n" +
                "            </unitId>\n" +
                "            <korobyte>\n" +
                "               <width>30</width>\n" +
                "               <height>21</height>\n" +
                "               <length>10</length>\n" +
                "               <weightGross>10.001</weightGross>\n" +
                "               <weightNet />\n" +
                "               <weightTare />\n" +
                "            </korobyte>\n" +
                "            <lifeTime />\n" +
                "            <item />\n" +
                "         </itemReference>\n" +
                "      </itemReferences>\n" +
                "   </response>\n" +
                "</root>";
        ResponseEntity<String> response = getReferenceItems(request, SERVICE_ID_2);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody());
    }

    @Test
    void requestByLimitWithMissingItems() {
        String request = //language=xml
                "<root>\n" +
                "    <token>ff_token_1</token>\n" +
                "    <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                "    <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                "    <request type=\"getReferenceItems\">\n" +
                "        <limit>2</limit>\n" +
                "        <offset>2</offset>\n" +
                "    </request>\n" +
                "</root>";

        expectRequestToIndexerWithMissingKorobyte();
        String expectedResponse = //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<root>\n" +
                "   <uniq>36fc8f6373206300cd2d3350611cc50c</uniq>\n" +
                "   <hash>36fc8f6373206300cd2d3350611cc50c</hash>\n" +
                "   <requestState>\n" +
                "      <isError>false</isError>\n" +
                "   </requestState>\n" +
                "   <response type=\"getReferenceItems\">\n" +
                "      <itemReferences>\n" +
                "         <itemReference>\n" +
                "            <unitId>\n" +
                "               <id>AAA</id>\n" +
                "               <vendorId>100</vendorId>\n" +
                "               <article>AAA</article>\n" +
                "            </unitId>\n" +
                "            <korobyte>\n" +
                "               <width>30</width>\n" +
                "               <height>21</height>\n" +
                "               <length>10</length>\n" +
                "               <weightGross>10.001</weightGross>\n" +
                "               <weightNet />\n" +
                "               <weightTare />\n" +
                "            </korobyte>\n" +
                "            <lifeTime />\n" +
                "            <item />\n" +
                "         </itemReference>\n" +
                "         <itemReference>\n" +
                "            <unitId>\n" +
                "               <id>BBB</id>\n" +
                "               <vendorId>100</vendorId>\n" +
                "               <article>BBB</article>\n" +
                "            </unitId>\n" +
                "            <korobyte>\n" +
                "               <width>0</width>\n" +
                "               <height>0</height>\n" +
                "               <length>0</length>\n" +
                "               <weightGross>0</weightGross>\n" +
                "               <weightNet />\n" +
                "               <weightTare />\n" +
                "            </korobyte>\n" +
                "            <lifeTime />\n" +
                "            <item />\n" +
                "         </itemReference>\n" +
                "      </itemReferences>\n" +
                "   </response>\n" +
                "</root>";
        ResponseEntity<String> response = getReferenceItems(request, SERVICE_ID);
        FfAsserts.assertHeader(response, XmlExceptionHandlerAdvice.X_RETURN_CODE, HttpStatus.OK.value());
        FfAsserts.assertXmlEquals(expectedResponse, response.getBody());
    }

    private void expectRequestToIndexerWithPlaneshift() {
        when(planeshiftApiClient.getDimensions(eq(new SupplierFeedOfferIds(105L, 15L,  List.of(
                new FeedOfferId(400L, "AAA"))))))
                .thenReturn(List.of(
                        new Dimensions(400, "AAA",
                                BigDecimal.valueOf(10001, 3),
                                BigDecimal.valueOf(21),
                                BigDecimal.valueOf(10),
                                BigDecimal.valueOf(30))));
    }

    private void expectRequestToIndexerWithMissingKorobyte() {
        when(indexerApiClient.getDimensions(eq(new SupplierFeedOfferIds(100L, 10L, List.of(
                new FeedOfferId(500L, "AAA"),
                new FeedOfferId(500L, "BBB"))))))
                .thenReturn(List.of(
                        new Dimensions(500L, "AAA",
                                BigDecimal.valueOf(10001, 3),
                                BigDecimal.valueOf(21),
                                BigDecimal.valueOf(10),
                                BigDecimal.valueOf(30))));
    }

    private ResponseEntity<String> getReferenceItems(String request, long serviceId) {
        String referenceItemsUrl = FF4ShopsUrlBuilder.getReferenceUrl(randomServerPort, serviceId);
        return FunctionalTestHelper.postForXml(referenceItemsUrl, request);
    }
}
