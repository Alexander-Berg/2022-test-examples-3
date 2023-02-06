package ru.yandex.autotests.direct.cmd.data.phrases;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

import java.util.List;

public class AjaxCheckMinusWordsRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private String campaignId;

    @SerializeKey("json_key_words-0")
    @SerializeBy(ValueToJsonSerializer.class)
    private List<String> jsonKeyWords;

    @SerializeKey("adgroup_ids")
    private String adgroupIds;

    @SerializeKey("on_success_save")
    private String onSuccessSave;

    @SerializeKey("json_minus_words-0")
    @SerializeBy(ValueToJsonSerializer.class)
    private List<String> jsonMinusWords;

    public String getOnSuccessSave() {
        return onSuccessSave;
    }

    public AjaxCheckMinusWordsRequest withOnSuccessSave(String onSuccessSave) {
        this.onSuccessSave = onSuccessSave;
        return this;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public AjaxCheckMinusWordsRequest withCampaignId(String campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    public String getAdgroupIds() {
        return adgroupIds;
    }

    public AjaxCheckMinusWordsRequest withAdgroupIds(String adgroupIds) {
        this.adgroupIds = adgroupIds;
        return this;
    }

    public List<String> getJsonMinusWords() {
        return jsonMinusWords;
    }

    public AjaxCheckMinusWordsRequest withJsonMinusWords(List<String> jsonMinusWords) {
        this.jsonMinusWords = jsonMinusWords;
        return this;
    }

    public List<String> getJsonKeyWords() {
        return jsonKeyWords;
    }

    public AjaxCheckMinusWordsRequest withJsonKeyWords(List<String> jsonKeyWords) {
        this.jsonKeyWords = jsonKeyWords;
        return this;
    }
}
