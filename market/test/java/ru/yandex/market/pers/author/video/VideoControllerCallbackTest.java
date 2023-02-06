package ru.yandex.market.pers.author.video;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.pers.author.video.modState.CompositeModState;
import ru.yandex.market.pers.author.client.api.model.VideoModState;
import ru.yandex.market.pers.author.video.modState.VhModState;
import ru.yandex.market.pers.author.video.model.VideoInfo;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.telegram.TelegramBotClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VideoControllerCallbackTest extends VideoControllerTest {

    @Autowired
    private TelegramBotClient telegramBotClient;

    @Autowired
    private ReportService reportService;

    @Value("${pers.author.telegram.export.model.video.group}")
    private String chatId;

    private static final String MODEL_NAME = "Смартфон Xiaomi Redmi Note 8 Pro 6/128GB";

    @Test
    public void testCallbackRequestAllStates() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        for (VhModState state : VhModState.values()) {
            String body = getCallbackBody(videoId, state);
            videoMvcMocks.callback(body, status().is2xxSuccessful());
            checkLastVhModState(videoId, state);
            checkModState(videoId, CompositeModState.calcModState(Optional.of(state), Optional.empty()));
        }
    }

    @Test
    public void testCallbackNewBeforeCreate() throws Exception {
        String videoId = VIDEO_ID;
        final VhModState vhModState = VhModState.NEW;
        String body = getCallbackBody(videoId, vhModState);
        videoMvcMocks.callback(body, status().is2xxSuccessful());
        checkLastVhModState(videoId, vhModState);

        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        checkLastVhModState(videoId, vhModState);
        checkModState(videoId, VideoModState.NEW);
    }

    @Test
    public void testCallbackReadyBeforeCreate() throws Exception {
        String videoId = VIDEO_ID;
        final VhModState vhModState = VhModState.READY;
        String body = getCallbackBody(videoId, vhModState);
        String metaInfo = getCallbackMeta();

        videoMvcMocks.callback(body, status().is2xxSuccessful());
        checkLastVhModState(videoId, vhModState);
        final List<Long> states = jdbcTemplate.queryForList("select mod_state from pers.video where video_id = ?",
                Long.class, videoId);
        assertEquals(states.size(), 1);
        assertNull(states.get(0));
        checkMetaInfo(videoId, metaInfo);

        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        checkLastVhModState(videoId, vhModState);
        checkModState(videoId, VideoModState.READY);
        checkMetaInfo(videoId, metaInfo);
    }

    @Test
    public void testCallbackErrorBeforeCreate() throws Exception {
        String videoId = VIDEO_ID;
        final VhModState vhModState = VhModState.ERROR;
        String body = getCallbackBody(videoId, vhModState);

        videoMvcMocks.callback(body, status().is2xxSuccessful());
        checkLastVhModState(videoId, vhModState);

        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        checkLastVhModState(videoId, vhModState);
        checkModState(videoId, VideoModState.REJECTED);
    }

    @Test
    public void testCallbackWithoutVideoId() throws Exception {
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        String callbackBody = fileToString("/data/callback_ready_without_video.json");
        videoMvcMocks.callback(callbackBody, status().is4xxClientError());
        checkModState(videoId, VideoModState.NEW);
        checkMetaInfo(videoId, null);
    }

    @Test
    public void testSendToTelegram() throws Exception {
        // create video
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        verify(telegramBotClient, times(0)).sendBotMessage(anyString(), anyString());

        // check callback for different statuses exclude READY
        for (VhModState state : VhModState.values()) {
            if (state != VhModState.READY) {
                String body = getCallbackBody(videoId, state);
                videoMvcMocks.callback(body, status().is2xxSuccessful());
                verify(telegramBotClient, times(0)).sendBotMessage(anyString(), anyString());
            }
        }

        // check callback for READY status
        String body = getCallbackBody(videoId, VhModState.READY);
        videoMvcMocks.callback(body, status().is2xxSuccessful());
        verify(telegramBotClient, times(1)).sendBotMessage(eq(chatId), anyString());
    }

    @Test
    public void testNotRepeatedSendToTelegram() throws Exception {
        // create video
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        verify(telegramBotClient, times(0)).sendBotMessage(anyString(), anyString());

        // check callback for READY status
        String body = getCallbackBody(videoId, VhModState.READY);
        videoMvcMocks.callback(body, status().is2xxSuccessful());
        verify(telegramBotClient, times(1)).sendBotMessage(eq(chatId), anyString());

        // NOT send again if status READY
        videoMvcMocks.callback(body, status().is2xxSuccessful());
        verify(telegramBotClient, times(1)).sendBotMessage(eq(chatId), anyString());
    }

    @Test
    public void testNotSendDeletedToTelegram() throws Exception {
        // create video
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);
        long id = getId(videoId);
        verify(telegramBotClient, times(0)).sendBotMessage(anyString(), anyString());
        videoMvcMocks.deleteVideoById(id, UID, status().is2xxSuccessful());

        // check for READY status
        String body = getCallbackBody(videoId, VhModState.READY);
        videoMvcMocks.callback(body, status().is2xxSuccessful());
        verify(telegramBotClient, times(0)).sendBotMessage(eq(chatId), anyString());
    }

    @Test
    public void testMessageSendToTelegram() throws Exception {
        // mock report
        Model model = new Model();
        model.setName(MODEL_NAME);
        when(reportService.getModelById(eq(MODEL_ID))).thenReturn(Optional.of(model));

        // create video
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);

        // check callback for READY status
        ArgumentCaptor<String> messageCapture = ArgumentCaptor.forClass(String.class);
        String body = getCallbackBody(videoId, VhModState.READY);
        videoMvcMocks.callback(body, status().is2xxSuccessful());
        verify(telegramBotClient, times(1)).sendBotMessage(eq(chatId), messageCapture.capture());

        VideoInfo videoInfo = videoService.getVideoByVideoId(videoId);
        final String expectedMessage = String.format(
                "Видео #%s от %s\n" +
                        "    <b>%s</b>\n" +
                        "\n" +
                        "<i>%s</i>\n" +
                        "\n" +
                        "<b>Id видео:</b> video_id_111\n" +
                        "Посмотреть видео можно по ссылке:\n" +
                        "https://frontend.vh.yandex.ru/player/vByWf3NfpKn4\n" +
                        "Ссылка на модель:\n" +
                        "https://market.yandex.ru/product/%s",
                videoInfo.getId(),
                new SimpleDateFormat("yyyy-MM-dd HH:mm").format(videoInfo.getCrTime().toEpochMilli()),
                MODEL_NAME,
                TITLE,
                MODEL_ID);

        List<String> messages = messageCapture.getAllValues();
        Assertions.assertEquals(1, messages.size());
        Assertions.assertEquals(expectedMessage, messages.get(0));
    }

    @Test
    public void testCallbackWithShortId() throws Exception {
        // create video
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);

        //callback with short_id differ from short_id from url
        String callbackTemplate = fileToString("/data/callback_template_with_short_id.json");
        String body = String.format(callbackTemplate, videoId, VhModState.READY.getName());
        videoMvcMocks.callback(body, status().is2xxSuccessful());

        //get short_id from callback
        VideoInfo videoInfo = videoService.getVideoByVideoId(videoId);
        assertEquals("123456", videoInfo.getMetaInfo().getShortId());

    }
}
