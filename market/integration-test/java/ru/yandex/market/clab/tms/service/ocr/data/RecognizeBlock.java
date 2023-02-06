package ru.yandex.market.clab.tms.service.ocr.data;

import java.util.List;

public class RecognizeBlock {

    private int x;
    private int y;
    private int w;
    private int h;
    private int rx;
    private int ry;
    private int rw;
    private int rh;
    private int angle;
    private List<RecognizeBox> boxes;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getW() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }

    public int getRx() {
        return rx;
    }

    public void setRx(int rx) {
        this.rx = rx;
    }

    public int getRy() {
        return ry;
    }

    public void setRy(int ry) {
        this.ry = ry;
    }

    public int getRw() {
        return rw;
    }

    public void setRw(int rw) {
        this.rw = rw;
    }

    public int getRh() {
        return rh;
    }

    public void setRh(int rh) {
        this.rh = rh;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public List<RecognizeBox> getBoxes() {
        return boxes;
    }

    public void setBoxes(List<RecognizeBox> boxes) {
        this.boxes = boxes;
    }
}
