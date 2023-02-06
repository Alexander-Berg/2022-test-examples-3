package ru.yandex.direct.api.v5.entity.campaigns.converter

import com.yandex.direct.api.v5.campaigns.TimeTargetingAdd
import com.yandex.direct.api.v5.campaigns.TimeTargetingOnPublicHolidays
import com.yandex.direct.api.v5.general.ArrayOfString
import com.yandex.direct.api.v5.general.YesNoEnum
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.libs.timetarget.TimeTarget

@RunWith(JUnitParamsRunner::class)
class CampaignDataConverterTest {

    fun toTimeTargetTestData() = listOf(
            listOf(
                    "Тест 1",
                    TimeTargetingAdd()
                            .withHolidaysSchedule(
                                    TimeTargetingOnPublicHolidays()
                                            .withSuspendOnHolidays(YesNoEnum.YES))
                            .withConsiderWorkingWeekends(YesNoEnum.YES),

                    // Здесь и ниже значение TimeTarget ядра - это то что оказалось в базе после того, как исполнили
                    // соответствующий запрос через перл. Возможно стоит переписать на конструирование объекта
                    // TimeTarget, чтобы тест был более читаемым
                    TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMN" +
                            "OPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7AB" +
                            "CDEFGHIJKLMNOPQRSTUVWX89")
            ),

            listOf(
                    "Тест 2",
                    TimeTargetingAdd()
                            .withHolidaysSchedule(
                                    TimeTargetingOnPublicHolidays()
                                            .withSuspendOnHolidays(YesNoEnum.YES))
                            .withConsiderWorkingWeekends(YesNoEnum.NO),

                    TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMN" +
                            "OPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7AB" +
                            "CDEFGHIJKLMNOPQRSTUVWX8")
            ),

            listOf(
                    "Тест 3",
                    TimeTargetingAdd()
                            .withConsiderWorkingWeekends(YesNoEnum.NO),

                    TimeTarget.parseRawString(";p:o")
            ),

            listOf(
                    "Тест 4",
                    TimeTargetingAdd()
                            .withConsiderWorkingWeekends(YesNoEnum.YES),

                    TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMN" +
                            "OPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7AB" +
                            "CDEFGHIJKLMNOPQRSTUVWX9;p:o")
            ),

            listOf(
                    "Тест 5",
                    TimeTargetingAdd()
                            .withHolidaysSchedule(
                                    TimeTargetingOnPublicHolidays()
                                            .withSuspendOnHolidays(YesNoEnum.NO)
                                            .withBidPercent(50)
                                            .withStartHour(9)
                                            .withEndHour(23))
                            .withConsiderWorkingWeekends(YesNoEnum.NO)
                            .withSchedule(ArrayOfString().withItems(listOf(
                                    "2,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190," +
                                            "200,150,100"
                            ))),

                    TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWpX3A" +
                            "BCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMN" +
                            "OPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX8JfKfLfMfNfOfPfQfRfSfTfUfVfWf;p:o")
            ),

            listOf(
                    "Тест 6",
                    TimeTargetingAdd()
                            .withHolidaysSchedule(
                                    TimeTargetingOnPublicHolidays()
                                            .withSuspendOnHolidays(YesNoEnum.NO)
                                            .withBidPercent(10)
                                            .withStartHour(0)
                                            .withEndHour(1))
                            .withConsiderWorkingWeekends(YesNoEnum.NO)
                            .withSchedule(ArrayOfString().withItems(listOf(
                                    "1,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190," +
                                            "200,150,100",
                                    "2,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190," +
                                            "200,150,100",
                                    "3,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190," +
                                            "200,150,100",
                                    "4,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190," +
                                            "200,150,100",
                                    "5,0,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190," +
                                            "200,150,0",
                                    "6,10,20,30,50,100,100,100,100,100,100,100,10,20,30,50,20,30,40,90,80,70,60,50," +
                                            "100",
                                    "7,10,20,30,50,100,100,100,100,100,100,100,10,20,30,50,20,30,40,90,80,70,60,50," +
                                            "100"
                            ))),

                    TimeTarget.parseRawString("1ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWpX2ABCDEFGHIJbKcLdMeNOfPgQhRq" +
                            "SrTsUtVuWpX3ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWpX4ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWpX5" +
                            "BCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWp6AbBcCdDfEFGHIJKLbMcNdOfPcQdReSjTiUhVgWfX7AbBcCdDfEFG" +
                            "HIJKLbMcNdOfPcQdReSjTiUhVgWfX8Ab;p:o")
            )

    )

    @Test
    @Parameters(method = "toTimeTargetTestData")
    @TestCaseName("{0}")
    fun toTimeTargetTest(@SuppressWarnings("unused") testDescription: String,
                         apiTimeTarget: TimeTargetingAdd,
                         expectedTimeTarget: TimeTarget) {
        Assertions.assertThat(toTimeTarget(apiTimeTarget)).isEqualTo(expectedTimeTarget)
    }

}