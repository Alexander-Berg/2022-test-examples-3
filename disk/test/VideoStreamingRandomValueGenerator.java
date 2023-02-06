package ru.yandex.chemodan.app.videostreaming.test;

import java.math.BigDecimal;

import org.mockito.Mockito;

import ru.yandex.chemodan.app.videostreaming.MpfsSourceMeta;
import ru.yandex.chemodan.app.videostreaming.cache.MpfsSegmentMeta;
import ru.yandex.chemodan.app.videostreaming.cache.SegmentCacheId;
import ru.yandex.chemodan.videostreaming.framework.ffmpeg.ffprobe.FFprobeFormat;
import ru.yandex.chemodan.videostreaming.framework.ffmpeg.ffprobe.FFprobeInfo;
import ru.yandex.chemodan.videostreaming.framework.hls.HlsStreamQuality;
import ru.yandex.chemodan.videostreaming.framework.media.MediaInfo;
import ru.yandex.chemodan.videostreaming.framework.media.units.MediaTime;
import ru.yandex.commune.random.RandomValueGenerator;
import ru.yandex.inside.mds.MdsFileKey;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.reflection.TypeX;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class VideoStreamingRandomValueGenerator extends RandomValueGenerator {
    @Override
    public Object randomValueImpl(TypeX type) {
        Random2 rnd = Random2.threadLocal();
        if (type.erasure().is(MpfsSegmentMeta.class)) {
            return randomMpfsSegmentMeta();
        } else if (type.erasure().is(SegmentCacheId.class)) {
            return randomSegmentCacheId();
        } else if (type.erasure().is(MpfsSourceMeta.class)) {
            return randomMpfsSourceMeta();
        } else if (type.erasure().is(MulcaId.class)) {
            return randomMulcaId();
        } else if (type.erasure().is(MdsFileKey.class)) {
            return randomMdsFileKey();
        } else if (type.erasure().is(FFprobeInfo.class)) {
            return randomFFprobeInfo(rnd);
        } else if (type.erasure().is(MediaInfo.class)) {
            return randomMediaInfo(rnd);
        } else if (type.erasure().is(MediaTime.class)) {
            return new MediaTime(r.nextLong(3 * 60 * 60 * MediaTime.MICROS_PER_SECOND));
        } else {
            return super.randomValueImpl(type);
        }
    }

    private static MpfsSourceMeta randomMpfsSourceMeta() {
        return new MpfsSourceMeta(randomMulcaId());
    }

    private static MulcaId randomMulcaId() {
        return MulcaId.valueOf(Random2.threadLocal().nextString(20), "");
    }

    private static MdsFileKey randomMdsFileKey() {
        return new MdsFileKey(Random2.threadLocal().nextInt(), Random2.threadLocal().nextString(20));
    }

    private static FFprobeInfo randomFFprobeInfo(Random2 rnd) {
        return new FFprobeInfo(
                FFprobeFormat.builder()
                        .name(rnd.nextString(32))
                        .duration(new MediaTime(rnd.nextLong()))
                        .probeScore(rnd.nextInt())
                        .build()
        );
    }

    private static MediaInfo randomMediaInfo(Random2 rnd) {
        return new MediaInfo(randomFFprobeInfo(rnd), "{}");
    }

    private static MpfsSegmentMeta randomMpfsSegmentMeta() {
        MpfsSegmentMeta mock = Mockito.mock(MpfsSegmentMeta.class);
        // DO NOT inline! Otherwise you'll get stubbing error.
        SegmentCacheId segmentCacheId = randomSegmentCacheId();
        Mockito.when(mock.getCacheId())
                .thenReturn(segmentCacheId);
        return mock;
    }

    private static SegmentCacheId randomSegmentCacheId() {
        SegmentCacheId mock = Mockito.mock(SegmentCacheId.class);
        Mockito.when(mock.getMulcaIdWithTag())
                .thenReturn(Random2.threadLocal().nextString(20));
        Mockito.when(mock.getQualityStr())
                .thenReturn(Random2.threadLocal().nextEnum(HlsStreamQuality.class).toString());
        Mockito.when(mock.getIndex())
                .thenReturn(Random2.threadLocal().nextInt());
        Mockito.when(mock.getDurationInSeconds())
                .thenReturn(new BigDecimal("5.0"));
        return mock;
    }
}
