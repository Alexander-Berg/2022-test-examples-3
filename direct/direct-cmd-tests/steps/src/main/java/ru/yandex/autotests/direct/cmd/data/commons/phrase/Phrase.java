package ru.yandex.autotests.direct.cmd.data.commons.phrase;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Phrase {

    @SerializedName("phrase")
    private String phrase;

    @SerializedName("is_suspended")
    private String isSuspended;

    @SerializedName("autobudgetPriority")
    private Double autobudgetPriority;

    @SerializedName("is_deleted")
    private Boolean isDeleted;

    @SerializedName("autobroker")
    private Double autobroker;

    @SerializedName("disabled_tragic")
    private Boolean disabledTragic;

    @SerializedName("premium")
    private List<Bid> premium = new ArrayList<>();

    @SerializedName("guarantee")
    private List<Bid> guarantee = new ArrayList<>();

    @SerializedName("larr")
    private String larr;

    @SerializedName("probs")
    private String probs;

    @SerializedName("nobsdata")
    private Integer nobsdata;

    @SerializedName("min_price")
    private String minPrice;

    @SerializedName("context_scope")
    private String contextScope;

    @SerializedName("price")
    private Double price;

    @SerializedName("price_context")
    private Double priceContext;

    @SerializedName("clicks")
    private Double clicks;

    @SerializedName("ctr")
    private Double ctr;

    @SerializedName("ctx_clicks")
    private Double ctxClicks;

    @SerializedName("shows")
    private Double shows;

    @SerializedName("state")
    private String state;

    @SerializedName("rank")
    private Double rank;

    @SerializedName("id")
    private Long id;

    @SerializedName("status")
    private String status;

    @SerializedName("minus_words")
    private List<String> minusWords;

    @SerializedName("showsForecast")
    private String showsForecast;

    @SerializedName("key_words")
    private String keyWords;

    @SerializedName("phrase_unglued_suffix")
    private String phraseUngluedSuffix;

    @SerializedName("verdicts")
    private List<Object> verdicts = new ArrayList<>();

    @SerializedName("fixation")
    private List<Object> fixation = new ArrayList<>();

    @SerializedName("key_words_fix_on")
    private String keyWordsFixOn;

    @SerializedName("key_words_fix_off")
    private String keyWordsFixOff;

    @SerializedName("param1")
    private String param1;

    @SerializedName("param2")
    private String param2;

    @SerializedName("numword")
    private String numWord;

    @SerializedName("norm_phrase")
    private String normPhrase;

    @SerializedName("statusModerate")
    private String statusModerate;

    @SerializedName("no_pokazometer_stat")
    private Integer noPokazometerStat;

    @SerializedName("pokazometer_data")
    private Object pokazometerData;

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public Phrase withPhrase(String phrase) {
        this.phrase = phrase;
        return this;
    }

    public String getSuspended() {
        return isSuspended;
    }

    public Phrase withSuspended(String suspended) {
        isSuspended = suspended;
        return this;
    }

    public Double getAutobudgetPriority() {
        return autobudgetPriority;
    }

    public void setAutobudgetPriority(Double autobudgetPriority) {
        this.autobudgetPriority = autobudgetPriority;
    }

    public Phrase withAutobudgetPriority(Double autobudgetPriority) {
        this.autobudgetPriority = autobudgetPriority;
        return this;
    }

    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    public Phrase withDeleted(Boolean deleted) {
        isDeleted = deleted;
        return this;
    }

    public Double getAutobroker() {
        return autobroker;
    }

    public void setAutobroker(Double autobroker) {
        this.autobroker = autobroker;
    }

    public Phrase withAutobroker(Double autobroker) {
        this.autobroker = autobroker;
        return this;
    }

    public Boolean getDisabledTragic() {
        return disabledTragic;
    }

    public void setDisabledTragic(Boolean disabledTragic) {
        this.disabledTragic = disabledTragic;
    }

    public Phrase withDisabledTragic(Boolean disabledTragic) {
        this.disabledTragic = disabledTragic;
        return this;
    }

    public List<Bid> getPremium() {
        return premium;
    }

    public void setPremium(List<Bid> premium) {
        this.premium = premium;
    }

    public Phrase withPremium(List<Bid> premium) {
        this.premium = premium;
        return this;
    }

    public List<Bid> getGuarantee() {
        return guarantee;
    }

    public void setGuarantee(List<Bid> guarantee) {
        this.guarantee = guarantee;
    }

    public Phrase withGuarantee(List<Bid> guarantee) {
        this.guarantee = guarantee;
        return this;
    }

    public String getLarr() {
        return larr;
    }

    public void setLarr(String larr) {
        this.larr = larr;
    }

    public Phrase withLarr(String larr) {
        this.larr = larr;
        return this;
    }

    public String getProbs() {
        return probs;
    }

    public void setProbs(String probs) {
        this.probs = probs;
    }

    public Phrase withProbs(String probs) {
        this.probs = probs;
        return this;
    }

    public Integer getNobsdata() {
        return nobsdata;
    }

    public void setNobsdata(Integer nobsdata) {
        this.nobsdata = nobsdata;
    }

    public Phrase withNobsdata(Integer nobsdata) {
        this.nobsdata = nobsdata;
        return this;
    }

    public String getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(String minPrice) {
        this.minPrice = minPrice;
    }

    public Phrase withMinPrice(String minPrice) {
        this.minPrice = minPrice;
        return this;
    }

    public String getContextScope() {
        return contextScope;
    }

    public void setContextScope(String contextScope) {
        this.contextScope = contextScope;
    }

    public Phrase withContextScope(String contextScope) {
        this.contextScope = contextScope;
        return this;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Phrase withPrice(Double price) {
        this.price = price;
        return this;
    }

    public Double getPriceContext() {
        return priceContext;
    }

    public void setPriceContext(Double priceContext) {
        this.priceContext = priceContext;
    }

    public Phrase withPriceContext(Double priceContext) {
        this.priceContext = priceContext;
        return this;
    }

    public Double getClicks() {
        return clicks;
    }

    public void setClicks(Double clicks) {
        this.clicks = clicks;
    }

    public Phrase withClicks(Double clicks) {
        this.clicks = clicks;
        return this;
    }

    public Double getCtr() {
        return ctr;
    }

    public void setCtr(Double ctr) {
        this.ctr = ctr;
    }

    public Phrase withCtr(Double ctr) {
        this.ctr = ctr;
        return this;
    }

    public Double getCtxClicks() {
        return ctxClicks;
    }

    public void setCtxClicks(Double ctxClicks) {
        this.ctxClicks = ctxClicks;
    }

    public Phrase withCtxClicks(Double ctxClicks) {
        this.ctxClicks = ctxClicks;
        return this;
    }

    public Double getShows() {
        return shows;
    }

    public void setShows(Double shows) {
        this.shows = shows;
    }

    public Phrase withShows(Double shows) {
        this.shows = shows;
        return this;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Phrase withState(String state) {
        this.state = state;
        return this;
    }

    public Double getRank() {
        return rank;
    }

    public void setRank(Double rank) {
        this.rank = rank;
    }

    public Phrase withRank(Double rank) {
        this.rank = rank;
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Phrase withId(Long id) {
        this.id = id;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Phrase withStatus(String status) {
        this.status = status;
        return this;
    }

    public List<String> getMinusWords() {
        return minusWords;
    }

    public void setMinusWords(List<String> minusWords) {
        this.minusWords = minusWords;
    }

    public Phrase withMinusWords(List<String> minusWords) {
        this.minusWords = minusWords;
        return this;
    }

    public String getShowsForecast() {
        return showsForecast;
    }

    public void setShowsForecast(String showsForecast) {
        this.showsForecast = showsForecast;
    }

    public Phrase withShowsForecast(String showsForecast) {
        this.showsForecast = showsForecast;
        return this;
    }

    public String getKeyWords() {
        return keyWords;
    }

    public void setKeyWords(String keyWords) {
        this.keyWords = keyWords;
    }

    public Phrase withKeyWords(String keyWords) {
        this.keyWords = keyWords;
        return this;
    }

    public String getPhraseUngluedSuffix() {
        return phraseUngluedSuffix;
    }

    public void setPhraseUngluedSuffix(String phraseUngluedSuffix) {
        this.phraseUngluedSuffix = phraseUngluedSuffix;
    }

    public Phrase withPhraseUngluedSuffix(String phraseUngluedSuffix) {
        this.phraseUngluedSuffix = phraseUngluedSuffix;
        return this;
    }

    public List<Object> getVerdicts() {
        return verdicts;
    }

    public void setVerdicts(List<Object> verdicts) {
        this.verdicts = verdicts;
    }

    public Phrase withVerdicts(List<Object> verdicts) {
        this.verdicts = verdicts;
        return this;
    }

    public List<Object> getFixation() {
        return fixation;
    }

    public void setFixation(List<Object> fixation) {
        this.fixation = fixation;
    }

    public Phrase withFixation(List<Object> fixation) {
        this.fixation = fixation;
        return this;
    }

    public String getKeyWordsFixOn() {
        return keyWordsFixOn;
    }

    public void setKeyWordsFixOn(String keyWordsFixOn) {
        this.keyWordsFixOn = keyWordsFixOn;
    }

    public Phrase withKeyWordsFixOn(String keyWordsFixOn) {
        this.keyWordsFixOn = keyWordsFixOn;
        return this;
    }

    public String getKeyWordsFixOff() {
        return keyWordsFixOff;
    }

    public void setKeyWordsFixOff(String keyWordsFixOff) {
        this.keyWordsFixOff = keyWordsFixOff;
    }

    public Phrase withKeyWordsFixOff(String keyWordsFixOff) {
        this.keyWordsFixOff = keyWordsFixOff;
        return this;
    }

    public String getParam1() {
        return param1;
    }

    public void setParam1(String param1) {
        this.param1 = param1;
    }

    public Phrase withParam1(String param1) {
        this.param1 = param1;
        return this;
    }

    public String getParam2() {
        return param2;
    }

    public void setParam2(String param2) {
        this.param2 = param2;
    }

    public Phrase withParam2(String param2) {
        this.param2 = param2;
        return this;
    }

    public Phrase withIsSuspended(String isSuspended) {
        this.isSuspended = isSuspended;
        return this;
    }

    public String getIsSuspended() {
        return isSuspended;
    }

    public void setIsSuspended(String isSuspended) {
        this.isSuspended = isSuspended;
    }

    public String getNumWord() {
        return numWord;
    }

    public void setNumWord(String numWord) {
        this.numWord = numWord;
    }

    public Phrase withNumWord(String numWord) {
        this.numWord = numWord;
        return this;
    }

    public String getNormPhrase() {
        return normPhrase;
    }

    public void setNormPhrase(String normPhrase) {
        this.normPhrase = normPhrase;
    }

    public Phrase withNormPhrase(String normPhrase) {
        this.normPhrase = normPhrase;
        return this;
    }

    public String getStatusModerate() {
        return statusModerate;
    }

    public Phrase withStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
        return this;
    }

    public Integer getNoPokazometerStat() {
        return noPokazometerStat;
    }

    public Phrase withNoPokazometerStat(Integer noPokazometerStat) {
        this.noPokazometerStat = noPokazometerStat;
        return this;
    }

    public Object getPokazometerData() {
        return pokazometerData;
    }

    public Phrase withPokazometerData(Object pokazometerData) {
        this.pokazometerData = pokazometerData;
        return this;
    }
}
