package ru.yandex.qe.mail.meetings.services.staff;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.services.staff.dto.Response;

import static org.junit.Assert.assertTrue;

/**
 * @author Sergey Galyamichev
 */
public class RequestTest {
    public static final String ENDPOINT_ADDRESS = "local://staff";

    @Before
    public void doSetup() {
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setServiceBean(new MockStaff(MockStaff.DISMISSED));
        factory.setAddress(ENDPOINT_ADDRESS);
        factory.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        factory.setProviders(getProviders());
        Server server = factory.create();
    }

    @Test
    public void testCall() {
        List<?> providers = getProviders();
        StaffApiV3 api = JAXRSClientFactory.create(ENDPOINT_ADDRESS, StaffApiV3.class, providers);
        WebClient.getConfig(api).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        Response<Person> response = api.person("unused", "5", null);
        assertTrue(response.getResult().size() > 0); // unable to configure server side mapping =(
    }

    private List<JacksonJaxbJsonProvider> getProviders() {
        return Collections.singletonList(new JacksonJaxbJsonProvider());
    }
}
