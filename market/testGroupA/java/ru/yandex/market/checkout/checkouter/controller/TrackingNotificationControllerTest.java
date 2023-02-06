package ru.yandex.market.checkout.checkouter.controller;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.config.web.ViewsConfig;
import ru.yandex.market.checkout.checkouter.controllers.oms.TrackerNotificationController;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.CheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.delivery.track.TrackingService;
import ru.yandex.market.checkout.checkouter.order.delivery.track.checkpoint.TrackCheckpointService;
import ru.yandex.market.checkout.checkouter.service.TrackerNotificationService;
import ru.yandex.market.checkout.checkouter.service.TrackerNotificationServiceImpl;
import ru.yandex.market.checkout.checkouter.storage.returns.ReturnDeliveryDao;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * На самом деле тестит TrackingNotificationController + TrackerNotificationServiceImpl, потому что контроллер слишком
 * тоненький.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextHierarchy({
        @ContextConfiguration(classes = TrackingNotificationControllerTest.TrackingNotificationConfiguration.class),
        @ContextConfiguration(classes = TrackingNotificationControllerTest.TrackingNotificationWebConfigutaion.class)
})
public class TrackingNotificationControllerTest {

    private static final String TRACK_CODE = "TestTrackNo4321";
    private static final long DELIVERY_SERVICE_ID = 100503L;
    private static final long RETURN_DELIVERY_ID = 101;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private TrackCheckpointService trackCheckpointService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private CheckouterProperties checkouterProperties;

    @Autowired
    private QueuedCallService queuedCallService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        Mockito.reset(trackCheckpointService, trackCheckpointService, transactionTemplate);
    }

    @Test
    public void testEmptyIsOk() throws Exception {
        mockMvc.perform(post("/notify-tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveNotCalledIfTrackWasNotFound() throws Exception {
        when(trackingService.findTracksByTrackerIds((Collection<Long>) MockitoHamcrest.argThat(
                Matchers.hasItems(100500L))))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(post("/notify-tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\n" +
                        "  \"deliveryTrackMeta\":{\n" +
                        "    \"id\":100500,\n" +
                        "    \"trackNo\":\"TestTrackNo4321\",\n" +
                        "    \"backUrl\":\"http://checkouter.tst.vs.market.yandex.net:39001/\",\n" +
                        "    \"deliveryServiceId\":100503,\n" +
                        "    \"consumerId\":100502,\n" +
                        "    \"sourceId\":100501,\n" +
                        "    \"startDate\":null,\n" +
                        "    \"lastUpdatedDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"lastNotifySuccessDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"nextRequestDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"orderId\":\"TestOrderId1234\",\n" +
                        "    \"deliveryTrackStatus\":\"STARTED\"\n" +
                        "  },\n" +
                        "  \"deliveryTrackCheckpoints\":[\n" +
                        "    {\n" +
                        "      \"id\":112212,\n" +
                        "      \"trackId\":100500,\n" +
                        "      \"country\":\"RUSSIA\",\n" +
                        "      \"city\":\"MOSCOW\",\n" +
                        "      \"location\":\"MOSCOW, TX, 7976\",\n" +
                        "      \"message\":\"bla-bla\",\n" +
                        "      \"checkpointStatus\":\"DELIVERED\",\n" +
                        "      \"zipCode\":\"123456\",\n" +
                        "      \"checkpointDate\":\"2017-02-27 04:35:26\",\n" +
                        "      \"deliveryCheckpointStatus\": 1\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("NOT_FOUND"));

        Mockito.verify(trackCheckpointService, Mockito.times(0)).insertTrackCheckpoints(any(), any());

    }

    @Test
    public void testSaveIsCalledIfFound() throws Exception {
        Track track = createTrack();
        //
        when(trackingService.findTracksByTrackerIds((Collection<Long>) MockitoHamcrest.argThat(
                Matchers.hasItems(100500L))))
                .thenReturn(Collections.singletonList(track));

        when(trackCheckpointService.findByTrackerIds(Mockito.anyCollectionOf(Long.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(post("/notify-tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\n" +
                        "  \"deliveryTrackMeta\":{\n" +
                        "    \"id\":100500,\n" +
                        "    \"trackNo\":\"TestTrackNo4321\",\n" +
                        "    \"backUrl\":\"http://checkouter.tst.vs.market.yandex.net:39001/\",\n" +
                        "    \"deliveryServiceId\":100503,\n" +
                        "    \"consumerId\":100502,\n" +
                        "    \"sourceId\":100501,\n" +
                        "    \"startDate\":null,\n" +
                        "    \"lastUpdatedDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"lastNotifySuccessDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"nextRequestDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"orderId\":\"TestOrderId1234\",\n" +
                        "    \"deliveryTrackStatus\":\"STARTED\"\n" +
                        "  },\n" +
                        "  \"deliveryTrackCheckpoints\":[\n" +
                        "    {\n" +
                        "      \"id\":112212,\n" +
                        "      \"trackId\":100500,\n" +
                        "      \"country\":\"RUSSIA\",\n" +
                        "      \"city\":\"MOSCOW\",\n" +
                        "      \"location\":\"MOSCOW, TX, 7976\",\n" +
                        "      \"message\":\"bla-bla\",\n" +
                        "      \"checkpointStatus\":\"DELIVERED\",\n" +
                        "      \"zipCode\":\"123456\",\n" +
                        "      \"checkpointDate\":\"2017-02-27 04:35:26\",\n" +
                        "      \"deliveryCheckpointStatus\": 1\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("OK"));

        ArgumentCaptor<List> checkpointsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(trackCheckpointService)
                .insertTrackCheckpoints(any(), checkpointsCaptor.capture());

        List<TrackCheckpoint> trackCheckpoints = checkpointsCaptor.getValue();
        Assertions.assertEquals(1, trackCheckpoints.size());
        TrackCheckpoint trackCheckpoint = Iterables.getOnlyElement(trackCheckpoints);
        Assertions.assertEquals(112212L, trackCheckpoint.getTrackerCheckpointId());
        Assertions.assertEquals("MOSCOW", trackCheckpoint.getCity());
        Assertions.assertEquals(1, trackCheckpoint.getDeliveryCheckpointStatus().intValue());

        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, Calendar.FEBRUARY, 27, 04, 35, 26);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
        Assertions.assertEquals(calendar.getTime(), trackCheckpoint.getCheckpointDate());
    }

    @Test
    public void shouldSortCheckpointsByDateBeforeInserting() throws Exception {
        Track track = createTrack();
        //
        when(trackingService.findTracksByTrackerIds((Collection<Long>) MockitoHamcrest.argThat(
                Matchers.hasItems(100500L))))
                .thenReturn(Collections.singletonList(track));

        when(trackCheckpointService.findByTrackerIds(Mockito.anyCollectionOf(Long.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(post("/notify-tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\n" +
                        "  \"deliveryTrackMeta\":{\n" +
                        "    \"id\":100500,\n" +
                        "    \"trackNo\":\"TestTrackNo4321\",\n" +
                        "    \"backUrl\":\"http://checkouter.tst.vs.market.yandex.net:39001/\",\n" +
                        "    \"deliveryServiceId\":100503,\n" +
                        "    \"consumerId\":100502,\n" +
                        "    \"sourceId\":100501,\n" +
                        "    \"startDate\":null,\n" +
                        "    \"lastUpdatedDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"lastNotifySuccessDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"nextRequestDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"orderId\":\"TestOrderId1234\",\n" +
                        "    \"deliveryTrackStatus\":\"STARTED\"\n" +
                        "  },\n" +
                        "  \"deliveryTrackCheckpoints\":[\n" +
                        "    {\n" +
                        "      \"id\":112213,\n" +
                        "      \"trackId\":100500,\n" +
                        "      \"country\":\"RUSSIA\",\n" +
                        "      \"city\":\"MOSCOW\",\n" +
                        "      \"location\":\"MOSCOW, TX, 7976\",\n" +
                        "      \"message\":\"bla-bla\",\n" +
                        "      \"checkpointStatus\":\"DELIVERED\",\n" +
                        "      \"zipCode\":\"123456\",\n" +
                        "      \"checkpointDate\":\"2017-02-27 07:35:26\",\n" +
                        "      \"deliveryCheckpointStatus\": 1\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"id\":112212,\n" +
                        "      \"trackId\":100500,\n" +
                        "      \"country\":\"RUSSIA\",\n" +
                        "      \"city\":\"MOSCOW\",\n" +
                        "      \"location\":\"MOSCOW, TX, 7976\",\n" +
                        "      \"message\":\"bla-bla\",\n" +
                        "      \"checkpointStatus\":\"DELIVERED\",\n" +
                        "      \"zipCode\":\"123456\",\n" +
                        "      \"checkpointDate\":\"2017-02-27 04:35:26\",\n" +
                        "      \"deliveryCheckpointStatus\": 1\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("OK"));

        ArgumentCaptor<List> checkpointsCaptor = ArgumentCaptor.forClass(List.class);
        // MARKETCHECKOUT-5500: только одно событие на группу чекпоинтов.
        Mockito.verify(trackCheckpointService, Mockito.times(1))
                .insertTrackCheckpoints(any(), checkpointsCaptor.capture());

        List<List> allValues = checkpointsCaptor.getAllValues();
        assertThat(allValues, hasSize(1));

        List<TrackCheckpoint> trackCheckpoints = Iterables.getOnlyElement(allValues);
        assertThat(trackCheckpoints, hasSize(2));

        TrackCheckpoint firstInsertedCheckpoint = trackCheckpoints.get(0);
        Assertions.assertEquals(112212L, firstInsertedCheckpoint.getTrackerCheckpointId());
        TrackCheckpoint secondInsertedCheckpoint = trackCheckpoints.get(1);
        Assertions.assertEquals(112213L, secondInsertedCheckpoint.getTrackerCheckpointId());
    }

    @Test
    public void testBigNumber() throws Exception {
        Track track = new Track();
        track.setTrackerId(100500L);
        track.setOrderId(123L);
        track.setTrackCode(TRACK_CODE);
        track.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        //
        when(trackingService.findTracksByTrackerIds((Collection<Long>) MockitoHamcrest.argThat(
                Matchers.hasItems(100500L))))
                .thenReturn(Collections.singletonList(track));

        mockMvc.perform(post("/notify-tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\n" +
                        "  \"deliveryTrackMeta\":{\n" +
                        "    \"id\":100500,\n" +
                        "    \"trackNo\":\"TestTrackNo4321\",\n" +
                        "    \"backUrl\":\"http://checkouter.tst.vs.market.yandex.net:39001/\",\n" +
                        "    \"deliveryServiceId\":100503,\n" +
                        "    \"consumerId\":100502,\n" +
                        "    \"sourceId\":100501,\n" +
                        "    \"startDate\":null,\n" +
                        "    \"lastUpdatedDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"lastNotifySuccessDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"nextRequestDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"orderId\":\"TestOrderId1234\",\n" +
                        "    \"deliveryTrackStatus\":\"STARTED\"\n" +
                        "  },\n" +
                        "  \"deliveryTrackCheckpoints\":[\n" +
                        "    {\n" +
                        "      \"id\":112212,\n" +
                        "      \"trackId\":100500,\n" +
                        "      \"country\":\"RUSSIA\",\n" +
                        "      \"city\":\"MOSCOW\",\n" +
                        "      \"location\":\"MOSCOW, TX, 7976\",\n" +
                        "      \"message\":\"bla-bla\",\n" +
                        "      \"checkpointStatus\":\"DELIVERED\",\n" +
                        "      \"zipCode\":\"123456\",\n" +
                        "      \"checkpointDate\":\"2017-02-27 04:35:26\",\n" +
                        "      \"deliveryCheckpointStatus\": 1000000000000000000000\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFloat() throws Exception {
        Track track = new Track();
        track.setTrackerId(100500L);
        track.setOrderId(123L);
        track.setTrackCode(TRACK_CODE);
        track.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        //
        when(trackingService.findTracksByTrackerIds((Collection<Long>) MockitoHamcrest.argThat(
                Matchers.hasItems(100500L))))
                .thenReturn(Collections.singletonList(track));

        mockMvc.perform(post("/notify-tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\n" +
                        "  \"deliveryTrackMeta\":{\n" +
                        "    \"id\":100500,\n" +
                        "    \"trackNo\":\"TestTrackNo4321\",\n" +
                        "    \"backUrl\":\"http://checkouter.tst.vs.market.yandex.net:39001/\",\n" +
                        "    \"deliveryServiceId\":100503,\n" +
                        "    \"consumerId\":100502,\n" +
                        "    \"sourceId\":100501,\n" +
                        "    \"startDate\":null,\n" +
                        "    \"lastUpdatedDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"lastNotifySuccessDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"nextRequestDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"orderId\":\"TestOrderId1234\",\n" +
                        "    \"deliveryTrackStatus\":\"STARTED\"\n" +
                        "  },\n" +
                        "  \"deliveryTrackCheckpoints\":[\n" +
                        "    {\n" +
                        "      \"id\":112212,\n" +
                        "      \"trackId\":100500,\n" +
                        "      \"country\":\"RUSSIA\",\n" +
                        "      \"city\":\"MOSCOW\",\n" +
                        "      \"location\":\"MOSCOW, TX, 7976\",\n" +
                        "      \"message\":\"bla-bla\",\n" +
                        "      \"checkpointStatus\":\"DELIVERED\",\n" +
                        "      \"zipCode\":\"123456\",\n" +
                        "      \"checkpointDate\":\"2017-02-27 04:35:26\",\n" +
                        "      \"deliveryCheckpointStatus\": 123.456\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNullDeliveryCheckpointStatus() throws Exception {
        Track track = new Track();
        track.setTrackerId(100500L);
        track.setOrderId(123L);
        track.setTrackCode(TRACK_CODE);
        track.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        //
        when(trackingService.findTracksByTrackerIds((Collection<Long>) MockitoHamcrest.argThat(
                Matchers.hasItems(100500L))))
                .thenReturn(Collections.singletonList(track));

        mockMvc.perform(post("/notify-tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\n" +
                        "  \"deliveryTrackMeta\":{\n" +
                        "    \"id\":100500,\n" +
                        "    \"trackNo\":\"TestTrackNo4321\",\n" +
                        "    \"backUrl\":\"http://checkouter.tst.vs.market.yandex.net:39001/\",\n" +
                        "    \"deliveryServiceId\":100503,\n" +
                        "    \"consumerId\":100502,\n" +
                        "    \"sourceId\":100501,\n" +
                        "    \"startDate\":null,\n" +
                        "    \"lastUpdatedDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"lastNotifySuccessDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"nextRequestDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"orderId\":\"TestOrderId1234\",\n" +
                        "    \"deliveryTrackStatus\":\"STARTED\"\n" +
                        "  },\n" +
                        "  \"deliveryTrackCheckpoints\":[\n" +
                        "    {\n" +
                        "      \"id\":112212,\n" +
                        "      \"trackId\":100500,\n" +
                        "      \"country\":\"RUSSIA\",\n" +
                        "      \"city\":\"MOSCOW\",\n" +
                        "      \"location\":\"MOSCOW, TX, 7976\",\n" +
                        "      \"message\":\"bla-bla\",\n" +
                        "      \"checkpointStatus\":\"DELIVERED\",\n" +
                        "      \"zipCode\":\"123456\",\n" +
                        "      \"checkpointDate\":\"2017-02-27 04:35:26\",\n" +
                        "      \"deliveryCheckpointStatus\": null\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}]"))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("Если получаем возвратный трек, то пишем в тикет ЕО")
    public void testOperatorWindowIsCalledIfReturnTrackUpdated() throws Exception {
        Track track = createTrack();
        track.setDeliveryServiceType(DeliveryServiceType.RETURN_DELIVERY);
        track.setReturnDeliveryId(RETURN_DELIVERY_ID);
        //
        when(trackingService.findTracksByTrackerIds((Collection<Long>) MockitoHamcrest.argThat(
                Matchers.hasItems(100500L))))
                .thenReturn(Collections.singletonList(track));

        when(trackCheckpointService.findByTrackerIds(Mockito.anyCollection()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(post("/notify-tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\n" +
                        "  \"deliveryTrackMeta\":{\n" +
                        "    \"id\":100500,\n" +
                        "    \"trackNo\":\"TestTrackNo4321\",\n" +
                        "    \"backUrl\":\"http://checkouter.tst.vs.market.yandex.net:39001/\",\n" +
                        "    \"deliveryServiceId\":100503,\n" +
                        "    \"consumerId\":100502,\n" +
                        "    \"sourceId\":100501,\n" +
                        "    \"startDate\":null,\n" +
                        "    \"lastUpdatedDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"lastNotifySuccessDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"nextRequestDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"orderId\":\"TestOrderId1234\",\n" +
                        "    \"deliveryTrackStatus\":\"STARTED\"\n" +
                        "  },\n" +
                        "  \"deliveryTrackCheckpoints\":[\n" +
                        "    {\n" +
                        "      \"id\":112212,\n" +
                        "      \"trackId\":100500,\n" +
                        "      \"country\":\"RUSSIA\",\n" +
                        "      \"city\":\"MOSCOW\",\n" +
                        "      \"location\":\"MOSCOW, TX, 7976\",\n" +
                        "      \"message\":\"bla-bla\",\n" +
                        "      \"checkpointStatus\":\"DELIVERED\",\n" +
                        "      \"zipCode\":\"123456\",\n" +
                        "      \"checkpointDate\":\"2017-02-27 04:35:26\",\n" +
                        "      \"deliveryCheckpointStatus\": 1\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("OK"));

        ArgumentCaptor<List> checkpointsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(trackCheckpointService)
                .insertReturnTrackCheckpoints(any(), checkpointsCaptor.capture());

        List<TrackCheckpoint> trackCheckpoints = checkpointsCaptor.getValue();
        Assertions.assertEquals(1, trackCheckpoints.size());
        TrackCheckpoint trackCheckpoint = Iterables.getOnlyElement(trackCheckpoints);
        Assertions.assertEquals(CheckpointStatus.DELIVERED, trackCheckpoint.getCheckpointStatus());
        ArgumentCaptor<TransactionCallback<Object>> transactionCallbackArgumentCaptor =
                ArgumentCaptor.forClass(TransactionCallback.class);
        Mockito.verify(transactionTemplate).execute(transactionCallbackArgumentCaptor.capture());


    }

    @Test
    @DisplayName("Если получаем возвратный трек не с тем статусом, то не пишем в тикет ЕО")
    public void testOwNotCalledOnWrongCheckpointStatus() throws Exception {
        Track track = createTrack();
        track.setDeliveryServiceType(DeliveryServiceType.RETURN_DELIVERY);
        track.setReturnDeliveryId(RETURN_DELIVERY_ID);
        //
        when(trackingService.findTracksByTrackerIds((Collection<Long>) MockitoHamcrest.argThat(
                Matchers.hasItems(100500L))))
                .thenReturn(Collections.singletonList(track));

        when(trackCheckpointService.findByTrackerIds(Mockito.anyCollection()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(post("/notify-tracks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\n" +
                        "  \"deliveryTrackMeta\":{\n" +
                        "    \"id\":100500,\n" +
                        "    \"trackNo\":\"TestTrackNo4321\",\n" +
                        "    \"backUrl\":\"http://checkouter.tst.vs.market.yandex.net:39001/\",\n" +
                        "    \"deliveryServiceId\":100503,\n" +
                        "    \"consumerId\":100502,\n" +
                        "    \"sourceId\":100501,\n" +
                        "    \"startDate\":null,\n" +
                        "    \"lastUpdatedDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"lastNotifySuccessDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"nextRequestDate\":\"2017-02-27 04:35:26\",\n" +
                        "    \"orderId\":\"TestOrderId1234\",\n" +
                        "    \"deliveryTrackStatus\":\"STARTED\"\n" +
                        "  },\n" +
                        "  \"deliveryTrackCheckpoints\":[\n" +
                        "    {\n" +
                        "      \"id\":112212,\n" +
                        "      \"trackId\":100500,\n" +
                        "      \"country\":\"RUSSIA\",\n" +
                        "      \"city\":\"MOSCOW\",\n" +
                        "      \"location\":\"MOSCOW, TX, 7976\",\n" +
                        "      \"message\":\"bla-bla\",\n" +
                        "      \"checkpointStatus\":\"INFO_RECEIVED\",\n" +
                        "      \"zipCode\":\"123456\",\n" +
                        "      \"checkpointDate\":\"2017-02-27 04:35:26\",\n" +
                        "      \"deliveryCheckpointStatus\": 10\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("OK"));

        ArgumentCaptor<List> checkpointsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(trackCheckpointService)
                .insertReturnTrackCheckpoints(any(), checkpointsCaptor.capture());

        List<TrackCheckpoint> trackCheckpoints = checkpointsCaptor.getValue();
        Assertions.assertEquals(1, trackCheckpoints.size());
        TrackCheckpoint trackCheckpoint = Iterables.getOnlyElement(trackCheckpoints);
        Assertions.assertEquals(CheckpointStatus.INFO_RECEIVED, trackCheckpoint.getCheckpointStatus());
        //убеждаемся, что с Mock'ом queuedCallService ничто не взаимодействовало
        Mockito.verifyNoMoreInteractions(queuedCallService);
    }

    /**
     * Application config
     */
    @Configuration
    public static class TrackingNotificationConfiguration {

        @Bean
        public OrderService orderService() {
            return Mockito.mock(OrderService.class);
        }

        @Bean
        public TrackingService trackingService() {
            return Mockito.mock(TrackingService.class);
        }

        @Bean
        public TrackCheckpointService trackCheckpointService() {
            return Mockito.mock(TrackCheckpointService.class);
        }

        @Bean
        public QueuedCallService queuedCallService() {
            return Mockito.mock(QueuedCallService.class);
        }

        @Bean
        public TransactionTemplate transactionTemplate() {
            return Mockito.mock(TransactionTemplate.class);
        }

        @Bean
        public CheckouterProperties checkouterProperties() {
            return new CheckouterPropertiesImpl();
        }

        @Bean
        public ReturnDeliveryDao returnDeliveryDao() {
            return Mockito.mock(ReturnDeliveryDao.class);
        }

        @Bean
        @Autowired
        public TrackerNotificationServiceImpl trackerNotificationService(
                TrackingService trackingService,
                TrackCheckpointService trackCheckpointService,
                QueuedCallService queuedCallService,
                TransactionTemplate transactionTemplate,
                CheckouterProperties checkouterProperties,
                ReturnDeliveryDao returnDeliveryDao
        ) {
            return new TrackerNotificationServiceImpl(trackingService, trackCheckpointService, queuedCallService,
                    transactionTemplate, checkouterProperties, returnDeliveryDao);
        }
    }

    @Nonnull
    private Track createTrack() {
        Track track = new Track();
        track.setId(111L);
        track.setTrackerId(100500L);
        track.setOrderId(123L);
        track.setTrackCode(TRACK_CODE);
        track.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        track.setShipmentId(45L);
        return track;
    }

    /**
     * "Web config". Инклюдим views, чтобы подтянулись наши сериализаторы/десериализаторы.
     */
    @Import(ViewsConfig.class)
    @ImportResource({"classpath:int-test-views.xml"})
    @Configuration
    public static class TrackingNotificationWebConfigutaion extends AbstractControllerContext {

        @Bean
        @Autowired
        public TrackerNotificationController controller(TrackerNotificationService trackerNotificationService) {
            return new TrackerNotificationController(trackerNotificationService);
        }
    }
}
