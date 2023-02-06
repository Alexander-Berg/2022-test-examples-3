package ru.yandex.market.checkout.checkouter.order.shop.schedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.shop.ScheduleLine;
import ru.yandex.market.checkout.checkouter.shop.ShopScheduleSerializationHandler;
import ru.yandex.market.checkout.checkouter.shop.ShopsSchedule;
import ru.yandex.market.checkout.checkouter.shop.schedule.ScheduleServiceBean;
import ru.yandex.market.checkout.checkouter.shop.schedule.ShopScheduleDao;

/**
 * Created by tesseract on 15.10.14.
 */

@ExtendWith(MockitoExtension.class)
public class ScheduleValidationTest {

    private final ShopScheduleSerializationHandler shopScheduleSerializationHandler =
            new ShopScheduleSerializationHandler();
    @InjectMocks
    private ScheduleServiceBean scheduleServiceBean;
    @Mock
    private ShopScheduleDao shopScheduleDao;

    public static Stream<Arguments> parameterizedTestData() {

        return Lists.newArrayList(
                new Object[]{"1:30-40,2:10-20,1:10-30", false}, // интервалы пересекаются
                new Object[]{"1:780-1440", true}, // один интервал
                new Object[]{"1:10-20,1:30-40", true}, // интервалы не пересекаются
                new Object[]{"1:10-30,1:20-40", false}, // интервалы пересекаются
                new Object[]{"1:780-1440,2:0-55,2:780-1440,3:0-55,3:780-1440,4:0-55,4:780-1440,5:0-55,5:780-1440," +
                        "6:0-55,6:780-1440,7:0-55,7:780-1440,1:0-55", true} // кейс от mamton https://st.yandex-team
                // .ru/MBI-11915
        ).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void test(String schedule, boolean valid) {
        Collection<ScheduleLine> scheduleCollection = shopScheduleSerializationHandler.deserialize(schedule);

        if (valid) {
            Assertions.assertDoesNotThrow(() -> scheduleServiceBean.updateShopSchedule(1L, scheduleCollection));
        } else {
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> scheduleServiceBean.updateShopSchedule(1L, scheduleCollection));
        }
    }

    @Test
    public void shopsScheduleOverlapTest() {
        ShopsSchedule shopsSchedule = new ShopsSchedule();
        shopsSchedule.getMap().put(582483L,
                new ArrayList<>(shopScheduleSerializationHandler.deserialize("1:540-1140,1:540-1140,1:540-1140")));

        Assertions.assertTrue(Assertions.assertThrows(IllegalArgumentException.class,
                () -> scheduleServiceBean.updateShopsSchedule(shopsSchedule)).getMessage().contains("582483"));
    }
}
