package ru.yandex.qe.mail.meetings.mocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.qe.mail.meetings.mocks.calendar.MockServerCalendar;
import ru.yandex.qe.mail.meetings.services.calendar.CalendarWeb;
import ru.yandex.qe.mail.meetings.services.calendar.CommuneFaultInterceptor;
import ru.yandex.qe.mail.meetings.services.calendar.DateParameterConverterProvider;

/**
 * @author Sergey Galyamichev
 */
@Profile("test")
@Configuration
public class MockCalendarConfiguration {
    private static final String ENDPOINT_ADDRESS = "local://calendar";

    static {
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setServiceBean(new MockServerCalendar());
        factory.setAddress(ENDPOINT_ADDRESS);
        factory.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        List<Object> providers = new ArrayList<>(getProviders());
        factory.setProviders(providers);
        factory.create();
    }

    @Bean
    public CalendarWeb calendarWeb() {
        List<Object> providers = new ArrayList<>(getProviders());
        CalendarWeb calendarWeb = JAXRSClientFactory.create(ENDPOINT_ADDRESS, CalendarWeb.class, providers);
        WebClient.getConfig(calendarWeb).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        WebClient.getConfig(calendarWeb).getInInterceptors().add(new CommuneFaultInterceptor());
        return calendarWeb;
    }

    private static List<Object> getProviders() {
        return Arrays.asList(new JacksonJaxbJsonProvider(), new DateParameterConverterProvider(), new CommuneFaultInterceptor());
    }
}
