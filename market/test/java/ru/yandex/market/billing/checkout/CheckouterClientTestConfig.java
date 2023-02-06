package ru.yandex.market.billing.checkout;

import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import ru.yandex.market.checkout.checkouter.jackson.ObjectMapperTimeZoneSetter;

@Configuration
@ImportResource("classpath:WEB-INF/checkouter-client.xml")
public class CheckouterClientTestConfig {
    @Autowired
    private ObjectMapperTimeZoneSetter checkouterAnnotationObjectMapperTimeZoneSetter;

    @PostConstruct
    public void postConstruct() {
        // Клиент чекаутера десериализует дату-время в Московской временной зоне, наши тесты во Владивостоке падают
        checkouterAnnotationObjectMapperTimeZoneSetter.setTimeZone(TimeZone.getDefault());
        checkouterAnnotationObjectMapperTimeZoneSetter.afterPropertiesSet();
    }
}
