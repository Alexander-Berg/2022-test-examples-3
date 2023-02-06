package ru.yandex.market.mbisfintegration.salesforce;

import javax.xml.datatype.XMLGregorianCalendar;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import ru.yandex.market.mbisfintegration.config.SalesForceConfiguration;
import ru.yandex.market.mbisfintegration.generated.sf.model.Soap;

@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SalesForceTestConfiguration {

    @Bean
    @Primary
    public SoapHolder soapHolder() {
        var soap = Mockito.mock(Soap.class);
        var holder = Mockito.mock(SoapHolder.class);
        Mockito.when(holder.getSoap()).thenReturn(soap);
        return holder;
    }

    @Bean
    @Primary
    public ObjectMapper salesForceSerializer() {
        var sfDateModule = new SimpleModule();
        sfDateModule.addDeserializer(XMLGregorianCalendar.class, new SfDateDeserializer());
        return new ObjectMapper()
                .registerModule(new JaxbAnnotationModule(new SalesForceConfiguration.EmptyFieldsExcludeIntrospector()))
                .enable(MapperFeature.USE_ANNOTATIONS)
                .registerModule(sfDateModule);
    }

}
