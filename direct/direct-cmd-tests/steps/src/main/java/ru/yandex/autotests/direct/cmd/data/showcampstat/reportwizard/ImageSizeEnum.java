package ru.yandex.autotests.direct.cmd.data.showcampstat.reportwizard;

public enum ImageSizeEnum {
    SIZE_728X90("728x90"),
    SIZE_240X400("240x400"),
    SIZE_300X250("300x250"),
    SIZE_300X600("300x600"),
    SIZE_336X280("336x280"),
    SIZE_300X500("300x500"),
    SIZE_970X250("970x250"),
    SIZE_640X100("640x100"),
    SIZE_640X200("640x200"),
    SIZE_640X960("640x960"),
    SIZE_960X640("960x640");

    private String value;

    ImageSizeEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
