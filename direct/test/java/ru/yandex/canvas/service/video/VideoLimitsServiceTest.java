package ru.yandex.canvas.service.video;

import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.VideoLimitsInterface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class VideoLimitsServiceTest {
    @Autowired
    SessionParams sessionParams;

    @Autowired
    AuthRequestParams requestParams;

    @Autowired
    DirectService directService;

    @Autowired
    VideoLimitsService videoLimitsService;

    @TestConfiguration
    public static class TestConf {

        @MockBean
        SessionParams sessionParams;

        @MockBean
        AuthRequestParams requestParams;

        @MockBean
        DirectService directService;

        @Bean
        public VideoLimitsService videoLimitsService(AuthRequestParams authRequestParams,
                                                     DirectService directService) {
            return new VideoLimitsService(authRequestParams, directService);
        }
    }

    @Test
    public void checkLongFeature() {
        Mockito.when(requestParams.getUserId()).thenReturn(Optional.of(12L));
        Mockito.when(requestParams.getClientId()).thenReturn(Optional.of(10L));
        Mockito.when(directService.getFeatures(any(), any())).thenReturn(ImmutableSet.of("cpm_video_long_duration"));
        Mockito.when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPM);
        when(sessionParams.isPresent()).thenReturn(true);

        VideoLimitsInterface videoLimitsInterface = videoLimitsService.getLimits(VideoCreativeType.CPM, null);

        assertEquals(videoLimitsInterface.getDurationMax(), Double.valueOf(120.0));
        assertEquals(videoLimitsInterface.getDurationCaptureStop(), Double.valueOf(120.0));
        assertEquals(videoLimitsInterface.getDurationMin(), Double.valueOf(5.0));
    }

    @Test
    public void checkLongCPCFeature() {
        Mockito.when(requestParams.getUserId()).thenReturn(Optional.of(12L));
        Mockito.when(requestParams.getClientId()).thenReturn(Optional.of(10L));
        Mockito.when(directService.getFeatures(any(), any())).thenReturn(ImmutableSet.of("cpm_video_long_duration"));
        Mockito.when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPC);
        when(sessionParams.isPresent()).thenReturn(true);

        VideoLimitsInterface videoLimitsInterface = videoLimitsService.getLimits(VideoCreativeType.CPC, null);

        assertEquals(videoLimitsInterface.getDurationMax(), Double.valueOf(60.0));
        assertEquals(videoLimitsInterface.getDurationCaptureStop(), Double.valueOf(60.0));
        assertEquals(videoLimitsInterface.getDurationMin(), Double.valueOf(5.0));

    }

    @Test
    public void checkWithoutLongFeature() {
        Mockito.when(requestParams.getUserId()).thenReturn(Optional.of(12L));
        Mockito.when(requestParams.getClientId()).thenReturn(Optional.of(10L));
        Mockito.when(directService.getFeatures(any(), any())).thenReturn(ImmutableSet.of());
        Mockito.when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPM);
        when(sessionParams.isPresent()).thenReturn(true);

        VideoLimitsInterface videoLimitsInterface = videoLimitsService.getLimits(VideoCreativeType.CPM, null);

        assertEquals(videoLimitsInterface.getDurationMax(), Double.valueOf(60.0));
        assertEquals(videoLimitsInterface.getDurationCaptureStop(), Double.valueOf(60.0));
        assertEquals(videoLimitsInterface.getDurationMin(), Double.valueOf(5.0));
    }

    @Test
    public void checkLongCPMIndoorFeature() {
        Mockito.when(requestParams.getUserId()).thenReturn(Optional.of(12L));
        Mockito.when(requestParams.getClientId()).thenReturn(Optional.of(10L));
        Mockito.when(directService.getFeatures(any(), any())).thenReturn(ImmutableSet.of());
        Mockito.when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPM_INDOOR);
        when(sessionParams.isPresent()).thenReturn(true);

        VideoLimitsInterface videoLimitsInterface = videoLimitsService.getLimits(VideoCreativeType.CPM_INDOOR, null);

        assertThat(videoLimitsInterface.getAllowedVideoHwRatio()).contains("16:9", "9:16");
    }

    @Test
    public void checkYndxFrontpageLimits() {
        Mockito.when(requestParams.getUserId()).thenReturn(Optional.of(12L));
        Mockito.when(requestParams.getClientId()).thenReturn(Optional.of(10L));
        Mockito.when(directService.getFeatures(any(), any())).thenReturn(ImmutableSet.of());
        Mockito.when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPM_YNDX_FRONTPAGE);
        when(sessionParams.isPresent()).thenReturn(true);

        VideoLimitsInterface limits = videoLimitsService.getLimits(VideoCreativeType.CPM_YNDX_FRONTPAGE, 406L);

        assertThat(limits.getDurationMin()).isEqualTo(5.0);
        assertThat(limits.getDurationMax()).isEqualTo(15.0);
    }
}
