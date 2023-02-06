package ru.yandex.direct.core.testing.data;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public enum TestFfmpegResolution {
    R_21_320p_576("2:1", "21_320p_576", 576, 320, 700, 64),
    R_21_320p("2:1", "21_320p_640", 640, 320, 800, 64),
    R_21_360p("2:1", "21_360p", 720, 360, 800, 64),
    R_21_720p("2:1", "21_720p", 1440, 720, 3200, 128),
    R_21_900p("2:1", "21_900p", 1800, 900, 5000, 128),

    R_31_288p("3:1", "31_288p", 864, 288, 1200, 64),
    R_31_300p_940("3:1", "31_300p_940", 940, 300, 2000, 64),
    R_31_320p_940("3:1", "31_320p_940", 940, 320, 2000, 64),
    R_31_400p_1216("3:1", "31_400p_1216", 1216, 400, 2000, 64),
    R_31_416p("3:1", "31_416p", 1248, 416, 2000, 64),
    R_31_576p("3:1", "31_576p", 1728, 576, 3200, 128),
    R_31_600p_1920("3:1", "31_600p_1920", 1920, 600, 4200, 128),

    R_916_1920p("9:16", "916_1920p", 1080, 1920, 2500, 128);

    private final String ratio;
    private final String suffix;
    private final int width;
    private final int height;
    private final int videoBitrate;
    private final int audioBitrate;
    private final String stringValue;

    TestFfmpegResolution(String ratio, String suffix, int width, int height, int videoBitrate, int audioBitrate) {
        this.ratio = ratio;
        this.suffix = suffix;
        this.width = width;
        this.height = height;
        this.videoBitrate = videoBitrate;
        this.audioBitrate = audioBitrate;
        this.stringValue = String.format("[\"%s\", [\"%d\", \"%d\", \"%d\", \"%d\"]]",
                suffix, width, height, videoBitrate, audioBitrate);
    }

    public String getRatio() {
        return ratio;
    }

    public String getSuffix() {
        return suffix;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }

    @Override
    public String toString() {
        return stringValue;
    }

}
