package ru.yandex.market.logistics.management.service.client;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.queue.producer.PechkinNotificationTaskProducer;
import ru.yandex.market.logistics.management.service.notification.email.DayOffNotification;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.noActiveAsyncThreads;

@CleanDatabase
@Sql("/data/service/client/dayOff_notify_prepare_data.sql")
class DayOffNotificationServiceTest extends AbstractContextualTest {

    private static final String CREATE_URL = "https://sender-test/create-template-id/send";
    private static final String DELETE_URL = "https://sender-test/delete-template-id/send";
    private static final String EMAIL_TO = "test@test.test";

    @Autowired
    @Qualifier("yandexSenderRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("commonExecutor")
    private ThreadPoolTaskExecutor asyncExecutor;

    @Autowired
    private DayOffNotificationService dayOffNotificationService;

    @Autowired
    private TestableClock clock;

    @Autowired
    private PechkinHttpClient pechkinHttpClient;

    @Autowired
    private PechkinNotificationTaskProducer pechkinNotificationTaskProducer;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void setup() {
        clock.setFixed(LocalDate.of(2019, 5, 3).atStartOfDay(
            ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
        Mockito.doNothing().when(pechkinHttpClient).sendMessage(Mockito.any());
        Mockito.reset(pechkinHttpClient);
        MockitoAnnotations.initMocks(this);
        Mockito.doNothing().when(pechkinNotificationTaskProducer).produceTask(Mockito.any());
    }

    @Test
        //todo verify не работает до спринг 5.2 на асинк запросах,
        // пишет ошибки в лог, тест не падает
        // https://github.com/spring-projects/spring-framework/issues/21799
    void notificationProperlySent() throws JsonProcessingException {
        mockRestServiceServer.expect(requestTo(equalTo(CREATE_URL)))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Basic dG9rZW46"))
            .andExpect(content().formData(getFormData()))
            .andRespond(withSuccess(
                TestUtil
                    .pathToJson("data/service/notification/email/yandex_sender_ok_response.json"),
                MediaType.APPLICATION_JSON
            ));

        dayOffNotificationService.notifyDayOff(1L, LocalDate.of(2019, 5, 4), true);
        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));

        mockRestServiceServer.verify();
        Mockito.verify(pechkinHttpClient, Mockito.times(1)).sendMessage(Mockito.any());
    }

    @Test
    void testNotificationProperlySentWithoutDeliveryType() {
        dayOffNotificationService.notifyDayOff(4L, LocalDate.of(2019, 5, 4), true);
        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));

        Mockito.verify(pechkinHttpClient, Mockito.times(1)).sendMessage(Mockito.any());
    }

    @Test
    void testNotificationNotSentWhenDayOffExists() throws Exception {
        mockRestServiceServer.expect(never(), requestTo(equalTo(CREATE_URL)));

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partner-capacities/1/days-off?day=2019-05-03")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));

        mockRestServiceServer.verify();
        Mockito.verify(pechkinHttpClient, Mockito.never()).sendMessage(Mockito.any());
    }

    @Test
    void testNotificationNotSentWhenDayOffInThePast() {
        clock.clearFixed();
        clock.setFixed(LocalDate.of(2019, 5, 5).atStartOfDay(
            ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        mockRestServiceServer.expect(never(), requestTo(equalTo(CREATE_URL)));

        dayOffNotificationService.notifyDayOff(1L, LocalDate.of(2019, 5, 4), true);
        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));

        mockRestServiceServer.verify();
        Mockito.verify(pechkinHttpClient, Mockito.never()).sendMessage(Mockito.any());
    }

    @Test
    void testNotificationSentWhenDayOffIsCurrent() throws Exception {
        clock.clearFixed();
        clock.setFixed(LocalDate.of(2019, 5, 4).atStartOfDay(
            ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        mockRestServiceServer.expect(times(1), requestTo(equalTo(CREATE_URL)))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Basic dG9rZW46"))
            .andExpect(content().formData(getFormData()))
            .andRespond(withSuccess(
                TestUtil
                    .pathToJson("data/service/notification/email/yandex_sender_ok_response.json"),
                MediaType.APPLICATION_JSON
            ));

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partner-capacities/1/days-off")
                .param("day", "2019-05-04")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/newDayOff_response.json"));

        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));

        mockRestServiceServer.verify();
        Mockito.verify(pechkinHttpClient, Mockito.times(1)).sendMessage(Mockito.any());
    }

    @Test
    void testNotificationSentWhenDayOffIsNotWarehouse() throws Exception {
        clock.clearFixed();
        clock.setFixed(LocalDate.of(2019, 5, 4).atStartOfDay(
            ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        mockRestServiceServer.expect(times(1), requestTo(equalTo(CREATE_URL)))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Basic dG9rZW46"))
            .andExpect(content().formData(getFormDataDs()))
            .andRespond(withSuccess(
                TestUtil
                    .pathToJson("data/service/notification/email/yandex_sender_ok_response.json"),
                MediaType.APPLICATION_JSON
            ));

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partner-capacities/5/days-off")
                .param("day", "2019-05-04")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/newDayOff_response_other.json"));

        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));

        mockRestServiceServer.verify();
        Mockito.verify(pechkinHttpClient).sendMessage(Mockito.any());
    }

    @Test
    void createCapacityDayOffNotificationFailed() throws Exception {
        mockRestServiceServer.expect(times(3), requestTo(equalTo(CREATE_URL)))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Basic dG9rZW46"))
            .andExpect(content().formData(getFormData()))
            .andRespond(withSuccess(
                TestUtil
                    .pathToJson("data/service/notification/email/yandex_sender_error_response.json"),
                MediaType.APPLICATION_JSON
            ));

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partner-capacities/1/days-off")
                .param("day", "2019-05-04")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/newDayOff_response.json"));

        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));

        mockRestServiceServer.verify();
        Mockito.verify(pechkinHttpClient, Mockito.times(1)).sendMessage(Mockito.any());
    }

    @Test
    void testDeleteNotificationPropertySent() throws Exception {
        mockRestServiceServer.expect(once(), requestTo(equalTo(DELETE_URL)));

        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/externalApi/partner-capacities/1/days-off?day=2019-05-03")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));

        mockRestServiceServer.verify();
        Mockito.verify(pechkinHttpClient, Mockito.times(1)).sendMessage(Mockito.any());
    }

    @Test
    void testDeleteNotificationNotSentWhenNotFound() throws Exception {
        mockRestServiceServer.expect(never(), requestTo(equalTo(DELETE_URL)));

        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/externalApi/partner-capacities/0/days-off")
                .param("day", "2019-05-04")
        )
            .andExpect(status().isNotFound());

        await().atMost(1, TimeUnit.SECONDS).until(noActiveAsyncThreads(asyncExecutor));

        mockRestServiceServer.verify();
        Mockito.verify(pechkinHttpClient, Mockito.never()).sendMessage(Mockito.any());
    }

    @Test
    void testStatisticsNotificationTaskProduced() {
        clock.setFixed(LocalDate.of(2019, 5, 1).atStartOfDay(
            ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        dayOffNotificationService
            .findAndNotifyConsecutiveDayOffsForPartnerType(PartnerType.DELIVERY, CapacityService.DELIVERY);

        ArgumentCaptor<MessageDto> argumentCaptor = ArgumentCaptor.forClass(MessageDto.class);
        Mockito.verify(pechkinNotificationTaskProducer, Mockito.times(1))
            .produceTask(argumentCaptor.capture());

        MessageDto sentMessageDto = argumentCaptor.getValue();
        softly.assertThat(sentMessageDto)
            .as("MessageDto was generated")
            .isNotNull();

        softly.assertThat(sentMessageDto.getChannel())
            .as("Channel was chosen correctly")
            .isEqualTo("Delivery_capacity");

        softly.assertThat(sentMessageDto.getMessage())
            .as("Message generated correctly")
            .isEqualTo("\uD83D\uDD25\uD83D\uDD25\uD83D\uDD25 - Сроки доставки\n" +
                "По этим партнерам капасити заполнено на 3 дня вперед (включая сегодняшнюю дату). Нужно " +
                "договориться с партнерами об увеличении капасити, забитое капасити негативно влияет на сроки" +
                " доставки.\n" +
                "\n" +
                "1.[DeliveryService1 (2)](https://lms.market.yandex-team.ru/lms/partner/2) (capacity: " +
                "[400](https://lms-admin.market.yandex-team.ru/lms/partner-capacity/5))\n");
    }

    @Test
    void testStatisticsNotificationTaskNotProducedWhenNotFound() {
        clock.setFixed(
            LocalDate.of(2019, 5, 2).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        dayOffNotificationService
            .findAndNotifyConsecutiveDayOffsForPartnerType(PartnerType.DELIVERY, CapacityService.DELIVERY);

        Mockito.verify(pechkinNotificationTaskProducer, Mockito.never()).produceTask(Mockito.any());
    }

    private MultiValueMap<String, String> getFormData() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(getDayOffNotification());
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("args", payload);
        body.add("to_email", EMAIL_TO);
        return body;
    }

    private MultiValueMap<String, String> getFormDataDs() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(getDayOffNotificationDs());
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("args", payload);
        body.add("to_email", EMAIL_TO);
        return body;
    }

    private DayOffNotification getDayOffNotification() {
        return new DayOffNotification(
            "Fulfillment",
            "Fulfillment service 1",
            "1",
            "Москва и Московская область",
            "1",
            "Москва",
            "213",
            "2019-05-04",
            "1",
            "100",
            "Курьерка",
            "Beru"
        );
    }

    private DayOffNotification getDayOffNotificationDs() {
        return new DayOffNotification(
            "DeliveryService",
            "Delivery Service 1",
            "2",
            "",
            "41",
            "",
            "42",
            "2019-05-04",
            "5",
            "400",
            null,
            "Yandex Delivery"
        );
    }
}
