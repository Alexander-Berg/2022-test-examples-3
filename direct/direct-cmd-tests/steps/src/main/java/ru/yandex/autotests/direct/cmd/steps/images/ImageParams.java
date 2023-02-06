package ru.yandex.autotests.direct.cmd.steps.images;

import ru.yandex.autotests.direct.cmd.util.ImageUtils;

public class ImageParams {

    private ImageUtils.ImageFormat format;
    private int width;
    private int height;
    private int resizeX1;
    private int resizeY1;
    private int resizeX2;
    private int resizeY2;

    public ImageUtils.ImageFormat getFormat() {
        return format;
    }

    public ImageParams withFormat(ImageUtils.ImageFormat format) {
        this.format = format;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public ImageParams withWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public ImageParams withHeight(int height) {
        this.height = height;
        return this;
    }

    public int getResizeX1() {
        return resizeX1;
    }

    public ImageParams withResizeX1(int resizeX1) {
        this.resizeX1 = resizeX1;
        return this;
    }

    public int getResizeY1() {
        return resizeY1;
    }

    public ImageParams withResizeY1(int resizeY1) {
        this.resizeY1 = resizeY1;
        return this;
    }

    public int getResizeX2() {
        return resizeX2;
    }

    public ImageParams withResizeX2(int resizeX2) {
        this.resizeX2 = resizeX2;
        return this;
    }

    public int getResizeY2() {
        return resizeY2;
    }

    public ImageParams withResizeY2(int resizeY2) {
        this.resizeY2 = resizeY2;
        return this;
    }
}
