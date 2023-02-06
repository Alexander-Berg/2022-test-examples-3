package ru.yandex.chemodan.videostreaming.framework.hls.ffmpeg.transcoding;

import ru.yandex.chemodan.videostreaming.framework.ffmpeg.ffprobe.FFprobeSharedFixtures;
import ru.yandex.chemodan.videostreaming.framework.hls.HlsResource;
import ru.yandex.chemodan.videostreaming.framework.media.MediaInfo;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class TestHelper {
    public static HlsResource buildHlsResource() {
        return new HlsResource(new MediaInfo(FFprobeSharedFixtures.INFO_1HR_1080P_STEREO, "{}"));
    }
}
