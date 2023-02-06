package ru.yandex.market.checkout.checkouter.shop;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.shop.schedule.ShopScheduleDao;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class ShopControllerScheduleTest extends AbstractWebTestBase {

    private static final long SHOP_ID = 242102L;
    private static final ShopMetaData META = ShopSettingsHelper.getDefaultMeta();

    @Autowired
    private TestSerializationService testSerializationService;
    @Autowired
    private ShopScheduleDao shopScheduleDao;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                new Schedule(),
                prepareCorrectSchedule()
        )
                .map(ss -> new Object[]{ss})
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    private static Schedule prepareCorrectSchedule() {
        return new Schedule(Arrays.asList(
                new ScheduleLine(1, 10, 1430),
                new ScheduleLine(3, 40, 1310),
                new ScheduleLine(5, 40, 1240),
                new ScheduleLine(7, 100, 1340)
        ));
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldPutAndReadSchedule(Schedule shopsSchedule) throws Exception {
        mockMvc.perform(put("/shops/{shopId}", SHOP_ID)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(META)));

        mockMvc.perform(
                put("/shop/{shopId}/schedule/", SHOP_ID)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(testSerializationService.serializeCheckouterObject(shopsSchedule))
        ).andExpect(status().isOk());

        String scheduleString = mockMvc.perform(
                get("/shop/{shopId}/schedule/", SHOP_ID)
        )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Schedule schedule = testSerializationService.deserializeCheckouterObject(
                scheduleString, Schedule.class
        );

        assertThat(schedule, is(shopsSchedule));
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldPutAndReadScheduleFromCache(Schedule shopsSchedule) throws Exception {
        mockMvc.perform(put("/shops/{shopId}", SHOP_ID)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(META)));

        mockMvc.perform(
                put("/shop/{shopId}/schedule/", SHOP_ID)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(testSerializationService.serializeCheckouterObject(shopsSchedule))
        ).andExpect(status().isOk());

        // прогреваем кэш
        mockMvc.perform(
                get("/shop/{shopId}/schedule/", SHOP_ID)
        )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        transactionTemplate.execute(tc -> {
            // удаляем расписание в БД
            shopScheduleDao.updateByShopId(SHOP_ID, Collections.emptyList());
            return null;
        });

        // теперь должно доставаться из кэша
        String scheduleString = mockMvc.perform(
                get("/shop/{shopId}/schedule/", SHOP_ID)
        )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Schedule schedule = testSerializationService.deserializeCheckouterObject(
                scheduleString, Schedule.class
        );

        assertThat(schedule, is(shopsSchedule));
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void saveInDatabase(Schedule shopsSchedule) throws Exception {
        // Тест нужно выпилить после полного перехода к считыванию расписаний из базы
        mockMvc.perform(put("/shops/{shopId}", SHOP_ID)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(META)));

        mockMvc.perform(
                put("/shop/{shopId}/schedule/", SHOP_ID)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(testSerializationService.serializeCheckouterObject(shopsSchedule))
        ).andExpect(status().isOk());


        List<ScheduleLine> byShopId = shopScheduleDao.findByShopId(SHOP_ID);

        assertThat(new Schedule(byShopId), is(shopsSchedule));
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldOverrideInDatabase(Schedule shopsSchedule) throws Exception {
        mockMvc.perform(put("/shops/{shopId}", SHOP_ID)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(META)));

        mockMvc.perform(
                put("/shop/{shopId}/schedule/", SHOP_ID)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(testSerializationService.serializeCheckouterObject(shopsSchedule))
        ).andExpect(status().isOk());

        mockMvc.perform(
                put("/shop/{shopId}/schedule/", SHOP_ID)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(testSerializationService.serializeCheckouterObject(shopsSchedule))
        ).andExpect(status().isOk());

        Schedule fromDatabase = new Schedule(shopScheduleDao.findByShopId(SHOP_ID));
        assertThat(fromDatabase, is(shopsSchedule));
    }
}
