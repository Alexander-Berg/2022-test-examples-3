package ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections;

import com.google.gson.*;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.CmdBeanBuilder;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 11.06.15.
 */
public class AjaxApplyRejectCorrectionsPhrasesBean extends CmdBeanBuilder {

    private static final String JSON_PHRASES_PATH = "json_phrases";

    @JsonPath(requestPath = JSON_PHRASES_PATH)
    private JsonObject jsonPhrases;

    public void addGroupPhrasesForStopWordsFixation(String adgroupId, AjaxApplyRejectCorrectionsPhrase phraseBuilder) {
        if (jsonPhrases == null) {
            jsonPhrases = new JsonObject();
        }
        JsonObject phrase = new JsonObject();
        JsonArray phraseArray = new JsonArray();
        JsonArray phrasesArray = new JsonArray();
        phraseArray.add(new JsonPrimitive(phraseBuilder.getCorrectedPhrase()));
        phraseArray.add(new JsonPrimitive(phraseBuilder.getOriginalPhrase()));
        phrasesArray.add(phraseArray);
        if (jsonPhrases.get(adgroupId) == null) {
            phrase.add(phraseBuilder.getPhraseId(), phrasesArray);
            jsonPhrases.add(adgroupId, phrase);
        } else {
            jsonPhrases.get(adgroupId).getAsJsonObject().add(phraseBuilder.getPhraseId(), phrasesArray);
        }
    }

    public void addGroupPhrasesForUnglue(String adgroupId, AjaxApplyRejectCorrectionsPhrase phraseBuilder) {
        if (jsonPhrases == null) {
            jsonPhrases = new JsonObject();
        }
        JsonObject phrase = new JsonObject();
        if (jsonPhrases.get(adgroupId) == null) {
            phrase.addProperty(phraseBuilder.getPhraseId(), phraseBuilder.getMinusWords());
            jsonPhrases.add(adgroupId, phrase);
        } else {
            jsonPhrases.get(adgroupId).getAsJsonObject().addProperty(phraseBuilder.getPhraseId(), phraseBuilder.getMinusWords());
        }
    }

    public void setGroupPhrasesForStopWordsFixation(String adgroupId, AjaxApplyRejectCorrectionsPhrase phraseBuilder) {
        jsonPhrases = new JsonObject();
        addGroupPhrasesForStopWordsFixation(adgroupId, phraseBuilder);
    }

    @Override
    public String toJson() {
        String nameValuePair = super.toJson();
        if (jsonPhrases == null) {
            return nameValuePair;
        }
        JsonElement jsonTree = new JsonParser().parse(nameValuePair);
        return jsonTree.getAsJsonObject().get(JSON_PHRASES_PATH).toString();
    }

}
