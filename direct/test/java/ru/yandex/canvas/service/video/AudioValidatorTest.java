package ru.yandex.canvas.service.video;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.exceptions.ValidationErrorsException;
import ru.yandex.canvas.model.stillage.StillageFileInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AudioValidatorTest {
    @Autowired
    VideoLimitsService videoLimitsService;

    private AudioUploadService.MetaDataInfo makeMetaDataInfo() {
        return new AudioUploadService.MetaDataInfo()
                .setDuration(15.386122)
                .setBitrate(98173L)
                .setStream(new AudioUploadService.MetaDataInfo.MetaDataInfoStream()
                    .setBaseInfo(new AudioUploadService.MetaDataInfo.MetaDataInfoStreamBaseInfo()
                        .setBitrate(98173L)
                        .setChannelCount(1L)
                        .setChannelLayout("mono")
                        .setSampleRate(44100L)
                        .setCodec("mp3")
                    ));
    }

    private StillageFileInfo makeStillageFileInfo() {
        StillageFileInfo info = new StillageFileInfo();
        info.setFileSize(188814);
        info.setContentGroup("AUDIO");
        info.setMimeType("audio/mpeg");
        return info;
    }

    private List<String> runValidator(StillageFileInfo stillageFileInfo, AudioUploadService.MetaDataInfo metaDataInfo) {
        AudioValidator validator = new AudioValidator(stillageFileInfo, metaDataInfo,
                videoLimitsService.getLimits(VideoCreativeType.CPM_AUDIO, 301L));

        try {
            validator.validate();
        } catch (ValidationErrorsException e) {
            return e.getMessages();
        }

        return Collections.emptyList();
    }

    @Test
    public void allCorrectTest() {
        assertThat(runValidator(makeStillageFileInfo(), makeMetaDataInfo()), empty());
    }

    @Test
    public void invalidContentGroupTest() {
        StillageFileInfo info = makeStillageFileInfo();
        info.setContentGroup("TEXT");

        assertThat(runValidator(info, makeMetaDataInfo()), not(empty()));
    }

    @Test
    public void invalidDurationTest() {
        assertThat(runValidator(makeStillageFileInfo(), makeMetaDataInfo().setDuration(3.1)), not(empty()));
        assertThat(runValidator(makeStillageFileInfo(), makeMetaDataInfo().setDuration(37.1)), not(empty()));
        assertThat(runValidator(makeStillageFileInfo(), makeMetaDataInfo().setDuration(30.6)), not(empty()));
        assertThat(runValidator(makeStillageFileInfo(), makeMetaDataInfo().setDuration(4.99)), not(empty()));
    }

    @Test
    public void invalidCodec() {
        AudioUploadService.MetaDataInfo metaDataInfo = makeMetaDataInfo();
        metaDataInfo.getStream().getBaseInfo().setCodec("h264");

        assertThat(runValidator(makeStillageFileInfo(), metaDataInfo), not(empty()));
    }

    @Test
    public void invalidSampleRate() {
        AudioUploadService.MetaDataInfo metaDataInfo = makeMetaDataInfo();
        metaDataInfo.getStream().getBaseInfo().setSampleRate(32000L);

        assertThat(runValidator(makeStillageFileInfo(), metaDataInfo), not(empty()));
    }

    @Test
    public void invalidFileSize() {
        StillageFileInfo info = makeStillageFileInfo();
        info.setFileSize(10406738);

        assertThat(runValidator(info, makeMetaDataInfo()), not(empty()));
    }
}
