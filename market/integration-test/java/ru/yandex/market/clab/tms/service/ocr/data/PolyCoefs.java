package ru.yandex.market.clab.tms.service.ocr.data;

public class PolyCoefs {
    private int a0;
    private int a1;
    private double a2;
    private double a3;
    private double hh;
    private boolean fromXtoY;

    public int getA0() {
        return a0;
    }

    public void setA0(int a0) {
        this.a0 = a0;
    }

    public int getA1() {
        return a1;
    }

    public void setA1(int a1) {
        this.a1 = a1;
    }

    public double getA2() {
        return a2;
    }

    public void setA2(double a2) {
        this.a2 = a2;
    }

    public double getA3() {
        return a3;
    }

    public void setA3(double a3) {
        this.a3 = a3;
    }

    public double getHh() {
        return hh;
    }

    public void setHh(double hh) {
        this.hh = hh;
    }

    public boolean isFromXtoY() {
        return fromXtoY;
    }

    public void setFromXtoY(boolean fromXtoY) {
        this.fromXtoY = fromXtoY;
    }
}
