package ru.yandex.autotests.market.billing.backend.deliveryCalendarExportExecutor;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.billing.backend.data.deliveryholidays.DeliveryHolidaysProvider;
import ru.yandex.autotests.market.billing.backend.steps.DeliveryCalendarExportExecutorSteps;
import ru.yandex.autotests.market.billing.beans.deliveryholidays.Calendars;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

/**
 * Created by zajic on 25.08.16.
 */
@Aqua.Test
@Features("deliveryCalendarExportExecutor")
@Title("Проверка выгрузки календаря доставки")
@Issue("https://st.yandex-team.ru/AUTOTESTMARKET-2014")
public class DeliveryCalendarExportExecutorTest {
    private DeliveryCalendarExportExecutorSteps steps = new DeliveryCalendarExportExecutorSteps();
    private Calendars calendars;

    @Before
    public void setUp(){
        //Получаем файл delivery_holidays.xml из эллиптикса и мапим его на Calendars
        calendars = DeliveryHolidaysProvider.get();
    }

    @Test
    public void compareDeliveryHolidaysXMLWithBilling(){
        steps.compareWithBilling(calendars);
    }
}