package ru.yandex.market.clab.tms.service.ocr.data;

public class Word {
    private PolyCoefs polyCoefs;
    private String meta;
    private String word;
    private boolean hyp;
    private int x;
    private int y;
    private int w;
    private int h;

    public PolyCoefs getPolyCoefs() {
        return polyCoefs;
    }

    public void setPolyCoefs(PolyCoefs polyCoefs) {
        this.polyCoefs = polyCoefs;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public boolean isHyp() {
        return hyp;
    }

    public void setHyp(boolean hyp) {
        this.hyp = hyp;
    }

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
}
