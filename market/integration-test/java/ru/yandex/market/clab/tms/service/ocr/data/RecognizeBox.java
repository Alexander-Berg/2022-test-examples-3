package ru.yandex.market.clab.tms.service.ocr.data;

import java.util.List;

public class RecognizeBox {

    private PolyCoefs polyCoefs;
    private Color backgroundColor;
    private Color textColor;
    private int lineSizeCategory;
    private List<LanguageWithTexts> languages;
    private double confidence;
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

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    public int getLineSizeCategory() {
        return lineSizeCategory;
    }

    public void setLineSizeCategory(int lineSizeCategory) {
        this.lineSizeCategory = lineSizeCategory;
    }

    public List<LanguageWithTexts> getLanguages() {
        return languages;
    }

    public void setLanguages(List<LanguageWithTexts> languages) {
        this.languages = languages;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
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
