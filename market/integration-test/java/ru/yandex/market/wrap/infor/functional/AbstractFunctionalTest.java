package ru.yandex.market.wrap.infor.functional;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl;
import ru.yandex.market.wrap.infor.configuration.AbstractContextualTest;
import ru.yandex.market.wrap.infor.configuration.InforTestClientProperties;

import java.io.IOException;
import java.nio.charset.Charset;

import static java.lang.ClassLoader.getSystemResourceAsStream;

public abstract class AbstractFunctionalTest extends AbstractContextualTest {

    @Autowired
    @Qualifier("deliveryMapper")
    protected XmlMapper deliveryMapper;

    @Autowired
    @Qualifier("fulfillmentMapper")
    protected XmlMapper fulfillmentMapper;

    @Autowired
    @Qualifier("inforClientRestTemplate")
    protected RestTemplate restTemplate;

    @Autowired
    protected InforTestClientProperties clientProperties;

    public static FulfillmentInteraction inforInteraction(FulfillmentUrl fulfillmentUrl) {
        return new FulfillmentInteraction()
            .setResponseStatus(HttpStatus.OK)
            .setResponseContentType(MediaType.APPLICATION_JSON)
            .setInvocationCount(1)
            .setFulfillmentUrl(fulfillmentUrl);
    }

    protected String readResource(String path) {
        try {
            return IOUtils.toString(
                getSystemResourceAsStream(path), Charset.defaultCharset()
            );
        } catch (IOException e) {
            throw new RuntimeException("Can not read resource: " + path, e);
        }
    }

}
