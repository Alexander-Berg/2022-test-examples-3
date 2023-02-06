package ru.yandex.market.clab.tms.service.ocr.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FullText {
    @JsonProperty("LineSizeCategory")
    private int lineSizeCategory;

    @JsonProperty("Confidence")
    private double confidence;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("Text")
    private String text;

    public int getLineSizeCategory() {
        return lineSizeCategory;
    }

    public void setLineSizeCategory(int lineSizeCategory) {
        this.lineSizeCategory = lineSizeCategory;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
