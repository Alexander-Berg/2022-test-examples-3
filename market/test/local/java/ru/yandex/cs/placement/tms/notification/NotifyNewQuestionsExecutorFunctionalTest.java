package ru.yandex.cs.placement.tms.notification;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;
import ru.yandex.vendor.notification.VendorNotificationParameterFormatter;
import ru.yandex.vendor.questions.QuestionInfo;
import ru.yandex.vendor.questions.dao.QuestionsNotificationDao;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class NotifyNewQuestionsExecutorFunctionalTest extends AbstractCsPlacementTmsFunctionalTest {
    @Autowired
    private WireMockServer blackboxMock;
    @Autowired
    private NotifyNewQuestionsExecutor notifyNewQuestionsExecutor;
    @Autowired
    private VendorNotificationParameterFormatter vendorNotificationParameterFormatter;
    @Autowired
    private QuestionsNotificationDao questionsNotificationDao;
    @Autowired
    private Clock clock;

    @BeforeEach
    void beforeEachTest() {
        doReturn("2021")
                .when(vendorNotificationParameterFormatter)
                .year();
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/notification/NotifyNewQuestionsExecutorFunctionalTest/before.csv",
            after = "/ru/yandex/cs/placement/tms/notification/NotifyNewQuestionsExecutorFunctionalTest/after.csv",
            dataSource = "vendorDataSource"
    )
    void testJob() {
        when(clock.instant()).thenReturn(
                TimeUtil.toInstant(LocalDateTime.of(2021, Month.SEPTEMBER, 15, 0, 0))
        );

        when(questionsNotificationDao.getQuestions(any()))
                .thenReturn(List.of(
                        QuestionInfo.builder()
                                .withDate("15-09-2021")
                                .withModelTitle("Диваны")
                                .withModelUrl("https://market.yandex.ru/product/2")
                                .withUserId(1L)
                                .withBrandId(1L)
                                .withCategoryId(90401L)
                                .withText("Хороший диван")
                                .build()
                ));

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(
                        getStringResource("/blackbox_response.json"))));

        notifyNewQuestionsExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/notification/NotifyNewQuestionsExecutorFunctionalTest/testMultiBrand/before.csv",
            after = "/ru/yandex/cs/placement/tms/notification/NotifyNewQuestionsExecutorFunctionalTest/testMultiBrand/after.csv",
            dataSource = "vendorDataSource"
    )
    void testMultiBrand() {
        when(clock.instant()).thenReturn(
                TimeUtil.toInstant(LocalDateTime.of(2021, Month.SEPTEMBER, 15, 0, 0))
        );

        when(questionsNotificationDao.getQuestions(any()))
                .thenReturn(List.of(
                        QuestionInfo.builder()
                                .withDate("15-09-2021")
                                .withModelTitle("Дрель хорошая")
                                .withModelUrl("https://market.yandex.ru/product/2")
                                .withUserId(1L)
                                .withBrandId(1L)
                                .withCategoryId(102L)
                                .withText("Очень хорошая дрель, как вам удалось сделать такую хорошую дрель?")
                                .build(),
                        QuestionInfo.builder()
                                .withDate("15-09-2021")
                                .withModelTitle("Холодильник LG 10021")
                                .withModelUrl("https://market.yandex.ru/product/2")
                                .withUserId(1L)
                                .withBrandId(1L)
                                .withCategoryId(33L)
                                .withText("Воткнул в розетку а он не работает, что делать?")
                                .build(),
                        QuestionInfo.builder()
                                .withDate("15-09-2021")
                                .withModelTitle("Лопата обыкновенная")
                                .withModelUrl("https://market.yandex.ru/product/2")
                                .withUserId(1L)
                                .withBrandId(1L)
                                .withCategoryId(100L)
                                .withText("Резать салат неудобно, можете что то с этим сделать?")
                                .build()

                ));

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(
                        getStringResource("/blackbox_response.json"))));

        notifyNewQuestionsExecutor.doJob(null);
    }
}
