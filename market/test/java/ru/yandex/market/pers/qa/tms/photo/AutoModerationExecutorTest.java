package ru.yandex.market.pers.qa.tms.photo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.avatarnica.AvararnicaInfoResponse;
import ru.yandex.market.pers.qa.client.avatarnica.AvatarnicaClient;
import ru.yandex.market.pers.qa.model.Photo;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.service.PhotoService;
import ru.yandex.market.util.FormatUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AutoModerationExecutorTest extends PersQaTmsTest {
    @Autowired
    AutoModerationExecutor executor;
    @Autowired
    AvatarnicaClient avatarnicaClient;
    @Autowired
    private ComplexMonitoring complicatedMonitoring;
    @Autowired
    private PhotoService photoService;
    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    @Test
    void testModerationMonitor() {
        int to = 6;
        for (int i = 0; i < to; i++) {
            photoService.createPhoto(buildPhoto("name" + i, i));
        }
        when(avatarnicaClient.getInfo(anyString(), anyString(), anyString()))
            .thenThrow(RuntimeException.class);
        executor.moderate();
        assertEquals(MonitoringStatus.WARNING, complicatedMonitoring.getResult().getStatus());
        assertEquals("WARN {PhotoAutoModerator: Avatarnica doesn't return any verdict for " + to + " photos.}",
            complicatedMonitoring.getResult().getMessage());
    }

    @Test
    void testSavePhotoCvVerdict() {
        String firstImageName = "name0";
        String secondImageName = "name1";
        AvararnicaInfoResponse avatarnicaInfoFirstResponse = new AvararnicaInfoResponse(true, 0, 10, 30);
        AvararnicaInfoResponse avatarnicaInfoSecondResponse = new AvararnicaInfoResponse(true, 0, 33, 66);

        photoService.createPhoto(buildPhoto(firstImageName, 0));
        photoService.createPhoto(buildPhoto(secondImageName, 1));
        when(avatarnicaClient.getInfo(anyString(), anyString(), eq(firstImageName)))
            .thenReturn(avatarnicaInfoFirstResponse);
        when(avatarnicaClient.getInfo(anyString(), anyString(), eq(secondImageName)))
            .thenReturn(avatarnicaInfoSecondResponse);

        executor.moderate();

        assertEquals(avatarnicaInfoFirstResponse.getCvResponse(), getAvatarnicaVerdict(firstImageName).getCvResponse());
        assertEquals(avatarnicaInfoSecondResponse.getCvResponse(), getAvatarnicaVerdict(secondImageName).getCvResponse());
    }

    private AvararnicaInfoResponse getAvatarnicaVerdict(String imageName) {
        String result = jdbcTemplate.queryForObject("select cv_verdict from qa.photo where image_name = ?", String.class, imageName);
        return FormatUtils.fromJson(result, AvararnicaInfoResponse.class);
    }

    private Photo buildPhoto(String imageName, int order) {
        return new Photo(QaEntityType.QUESTION, "1", "ns", "gr", imageName, order);
    }
}
