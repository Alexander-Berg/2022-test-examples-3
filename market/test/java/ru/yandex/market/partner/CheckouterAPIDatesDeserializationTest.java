package ru.yandex.market.partner;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.partner.test.context.FunctionalTest;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class CheckouterAPIDatesDeserializationTest extends FunctionalTest {

    @Autowired
    ObjectMapper checkouterAnnotationObjectMapper;

    @Test
    @DisplayName("Странный тест, подпирающий десериализацию дат в ответе чекаутера. " +
            "Происходило из-за подпёртой конфигурации dateFormat, которая нужная была ввиду кривого " +
            "autowire-by-name (см. изменение в том же коммите). Теперь эта подпорка не нужна.")
    public void test() throws IOException {
        // language = json
        String json = "{\n" +
                "  \"toDate\": \"18-05-2018\",\n" +
                "  \"fromDate\": \"17-05-2018\"\n" +
                "}\n";
        DeliveryDates dates = checkouterAnnotationObjectMapper.readValue(json, DeliveryDates.class);

        Assertions.assertEquals(LocalDate.of(2018, 5, 17),
                LocalDate.from(dates.getFromDate().toInstant().atZone(ZoneId.systemDefault())));
        Assertions.assertEquals(LocalDate.of(2018, 5, 18),
                LocalDate.from(dates.getToDate().toInstant().atZone(ZoneId.systemDefault())));
    }


}
