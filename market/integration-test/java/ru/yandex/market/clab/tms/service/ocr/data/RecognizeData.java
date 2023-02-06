package ru.yandex.market.clab.tms.service.ocr.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RecognizeData {

    private List<RecognizeBlock> blocks;
    private TimeLimit timeLimit;
    private int rotate;
    private ImgSize imgsize;
    private List<FullText> fulltext;
    private String lang;
    @JsonProperty("aggregated_stat")
    private int aggregatedStat;
    @JsonProperty("max_line_confidence")
    private double maxLineConfidence;

    public List<RecognizeBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<RecognizeBlock> blocks) {
        this.blocks = blocks;
    }

    public TimeLimit getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(TimeLimit timeLimit) {
        this.timeLimit = timeLimit;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    public ImgSize getImgsize() {
        return imgsize;
    }

    public void setImgsize(ImgSize imgsize) {
        this.imgsize = imgsize;
    }

    public List<FullText> getFulltext() {
        return fulltext;
    }

    public void setFulltext(List<FullText> fulltext) {
        this.fulltext = fulltext;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public int getAggregatedStat() {
        return aggregatedStat;
    }

    public void setAggregatedStat(int aggregatedStat) {
        this.aggregatedStat = aggregatedStat;
    }

    public double getMaxLineConfidence() {
        return maxLineConfidence;
    }

    public void setMaxLineConfidence(double maxLineConfidence) {
        this.maxLineConfidence = maxLineConfidence;
    }
}
