package ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections;

/**
 * Created by shmykov on 11.06.15.
 */
public class AjaxApplyRejectCorrectionsPhrase {

    private String phraseId;

    private String originalPhrase;

    private String correctedPhrase;

    private String minusWords;

    public String getOriginalPhrase() {
        return originalPhrase;
    }

    public void setOriginalPhrase(String originalPhrase) {
        this.originalPhrase = originalPhrase;
    }

    public String getPhraseId() {
        return phraseId;
    }

    public void setPhraseId(String phraseId) {
        this.phraseId = phraseId;
    }

    public String getCorrectedPhrase() {
        return correctedPhrase;
    }

    public void setCorrectedPhrase(String correctedPhrase) {
        this.correctedPhrase = correctedPhrase;
    }

    public String getMinusWords() {
        return minusWords;
    }

    public void setMinusWords(String minusWords) {
        this.minusWords = minusWords;
    }
}