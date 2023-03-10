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
                                .withModelTitle("????????????")
                                .withModelUrl("https://market.yandex.ru/product/2")
                                .withUserId(1L)
                                .withBrandId(1L)
                                .withCategoryId(90401L)
                                .withText("?????????????? ??????????")
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
                                .withModelTitle("?????????? ??????????????")
                                .withModelUrl("https://market.yandex.ru/product/2")
                                .withUserId(1L)
                                .withBrandId(1L)
                                .withCategoryId(102L)
                                .withText("?????????? ?????????????? ??????????, ?????? ?????? ?????????????? ?????????????? ?????????? ?????????????? ???????????")
                                .build(),
                        QuestionInfo.builder()
                                .withDate("15-09-2021")
                                .withModelTitle("?????????????????????? LG 10021")
                                .withModelUrl("https://market.yandex.ru/product/2")
                                .withUserId(1L)
                                .withBrandId(1L)
                                .withCategoryId(33L)
                                .withText("?????????????? ?? ?????????????? ?? ???? ???? ????????????????, ?????? ?????????????")
                                .build(),
                        QuestionInfo.builder()
                                .withDate("15-09-2021")
                                .withModelTitle("???????????? ????????????????????????")
                                .withModelUrl("https://market.yandex.ru/product/2")
                                .withUserId(1L)
                                .withBrandId(1L)
                                .withCategoryId(100L)
                                .withText("???????????? ?????????? ????????????????, ???????????? ?????? ???? ?? ???????? ???????????????")
                                .build()

                ));

        blackboxMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(
                        getStringResource("/blackbox_response.json"))));

        notifyNewQuestionsExecutor.doJob(null);
    }
}
