package ru.yandex.market.clab.tms.service.ocr.data;

import java.util.List;

public class Text {
    private double rank;
    private List<Word> words;
    private String text;

    public double getRank() {
        return rank;
    }

    public void setRank(double rank) {
        this.rank = rank;
    }

    public List<Word> getWords() {
        return words;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
