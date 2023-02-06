package ru.yandex.travel.hotels.common.partners.bronevik;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import io.opentracing.mock.MockTracer;

import ru.yandex.travel.commons.retry.Retry;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class DefaultBronevikClientTest {


    public static String apiClientKey; // fill only if local run
    public static String login; // fill only if local run
    public static String password; // fill only if local run
    public static SOAPType soapType;


    public BronevikClient bronevikClient;

    private Retry retryHelper;

    @Before
    public void prepare() {
        System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dumpTreshold", "999999");
        var properties = new BronevikClientProperties();
        properties.setPassword(password);
        properties.setClientKey(apiClientKey);
        properties.setSoapType(soapType);
        properties.setLogin(login);
        properties.setWsdlLocation("bronevik.wsdl");
        var logger = LoggerFactory.getLogger("DefaultBronevikClientTest");
        retryHelper = new Retry(new MockTracer());
        bronevikClient = new DefaultBronevikClient(properties, logger, retryHelper);

    }

    @Test
    public void testPing() {
        var futureResponse = bronevikClient.ping();
        var response = futureResponse.join();
        assertThat(response.traceId).isNotEmpty();
    }

    @Test
    public void testGetHotelsInfo() {
        var futureResponse = bronevikClient.getHotelsInfo(List.of(244), "testUUId");
        var response = futureResponse.join();
        assertThat(response.getHotel().size()).isEqualTo(1);
        assertThat(response.getHotel().get(0).id).isEqualTo(244);
    }

    @Test
    public void testGetMeals() {
        var futureResponse = bronevikClient.getMeals("testUUID");
        var response = futureResponse.join();
        assertThat(response.getMeals().getMeal()).isNotEmpty();
    }

    @Test
    public void testSearchHotelOffers() {
        var futureResponse = bronevikClient.searchHotelOffers(2,List.of(5), List.of(244), "2022-06-03", "2022-06-05", "RUB", "testUUID");
        var response = futureResponse.join();
        assertThat(response.getHotels().getHotel()).isNotEmpty();
    }


}
