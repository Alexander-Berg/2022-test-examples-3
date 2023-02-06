package ru.yandex.market.pers.author.video;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.pers.author.client.ComplaintInfo;
import ru.yandex.market.pers.author.video.modState.VhModState;
import ru.yandex.market.pers.author.video.model.VideoInfo;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.telegram.TelegramBotClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VideoControllerCommonTest extends VideoControllerTest {

    @Autowired
    private TelegramBotClient telegramBotClient;

    @Autowired
    private ReportService reportService;

    @Value("${pers.author.telegram.export.model.video.group}")
    private String chatId;

    private static final String MODEL_NAME = "Смартфон Xiaomi Redmi Note 8 Pro 6/128GB";

    @Test
    public void testComplaintMessageToTelegram() throws Exception {
        // mock report
        Model model = new Model();
        model.setName(MODEL_NAME);
        when(reportService.getModelById(eq(MODEL_ID))).thenReturn(Optional.of(model));

        // create video
        String videoId = VIDEO_ID;
        videoMvcMocks.saveVideoByUid(videoId, TITLE, MODEL_ID, UID);

        // check callback for READY status
        String bodyCallback = getCallbackBody(videoId, VhModState.READY);
        videoMvcMocks.callback(bodyCallback, status().is2xxSuccessful());

        ArgumentCaptor<String> messageCapture = ArgumentCaptor.forClass(String.class);
        String bodyComplaint = fileToString("/data/complaint_video.json");
        videoMvcMocks.sendComplaint(getId(videoId), bodyComplaint, status().is2xxSuccessful());
        verify(telegramBotClient, times(2)).sendBotMessage(eq(chatId), messageCapture.capture());

        VideoInfo videoInfo = videoService.getVideoByVideoId(videoId);
        final String expectedMessage = String.format(
            "<b>ЖАЛОБА НА ВИДЕО</b>\n" +
                "    Дата создания: %s\n" +
                "    Автор жалобы: UID 666777\n" +
                "    Причина: <i>Очень веская причина</i>\n\n" +
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
            getComplaintTime(bodyComplaint),
            videoInfo.getId(),
            new SimpleDateFormat("yyyy-MM-dd HH:mm").format(videoInfo.getCrTime().toEpochMilli()),
            MODEL_NAME,
            TITLE,
            MODEL_ID);

        List<String> messages = messageCapture.getAllValues();
        Assertions.assertEquals(2, messages.size());
        // because first msg about video
        Assertions.assertEquals(expectedMessage, messages.get(1));
    }

    private String getComplaintTime(String bodyComplaint) throws IOException {
        ComplaintInfo complaintInfo = new ObjectMapper().readValue(bodyComplaint, ComplaintInfo.class);
        Date complaintDate = complaintInfo.getComplaintDate();
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(complaintDate.getTime());
    }
}
