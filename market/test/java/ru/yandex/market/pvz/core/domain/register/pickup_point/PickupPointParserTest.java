package ru.yandex.market.pvz.core.domain.register.pickup_point;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

import liquibase.util.csv.opencsv.CSVReader;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointParams;

import static org.assertj.core.api.Assertions.assertThat;

class PickupPointParserTest {

    public static final long DELIVERY_SERVICE_ID = 123L;
    private final PickupPointParser parser = new PickupPointParser();

    @SneakyThrows
    @Test
    void parse() {
        CSVReader reader = new CSVReader(new StringReader("13304783,2020-10-16 17:29:25,2020-10-16 17:29:25,1005645,," +
                "ПВЗ Тест,+7 919 123-45-67,3,5,Нет,Нет,\"ПН-ПТ 09:00-21:00\n" +
                "СБ-ВС 10:00-20:00\",90,90,90,120,20,55.644689,37.400099,Москва и Московская область,Москва,Солнцево," +
                "Авиаторов,18,,,1,,,,119620,\"\"\"Пятерочка\"\" главный вход, прямо чрез торговый зал вход в торговую" +
                " галерею. Повернуть налево, пройти 10 метров. Павильон 4\",annabodur@yandex.ru,1.9,0.3,45\n"));
        PickupPointParams params = parser.parse(reader.readNext(), DELIVERY_SERVICE_ID);
        assertThat(params.getName()).isEqualTo("ПВЗ Тест");
        assertThat(params.getDeliveryServiceId()).isEqualTo(DELIVERY_SERVICE_ID);
        assertThat(params.getLocation().getMetro()).isEqualTo("Солнцево");
        assertThat(params.getSchedule().getScheduleDays().stream()
                .filter(sd -> sd.getDayOfWeek() == DayOfWeek.MONDAY)
                .findAny().get().getTimeFrom()).isEqualTo(LocalTime.of(9, 0));
        assertThat(params.getPhone()).isEqualTo("+7 919 123-45-67");
        assertThat(params.getInstruction()).isEqualTo("\"Пятерочка\" главный вход, прямо чрез торговый зал вход в " +
                "торговую галерею. Повернуть налево, пройти 10 метров. Павильон 4");
        assertThat(params.getStoragePeriod()).isEqualTo(3);
        assertThat(params.getCapacity()).isEqualTo(5);
        assertThat(params.getMaxHeight()).isEqualTo(BigDecimal.valueOf(90.0));
        assertThat(params.getCardCompensationRate()).isEqualTo(BigDecimal.valueOf(0.019));
        assertThat(params.getCashCompensationRate()).isEqualTo(BigDecimal.valueOf(0.003));
        assertThat(params.getTransmissionReward()).isEqualTo(BigDecimal.valueOf(45.0));
    }
}
