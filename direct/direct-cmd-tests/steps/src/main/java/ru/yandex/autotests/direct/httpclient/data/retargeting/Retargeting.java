package ru.yandex.autotests.direct.httpclient.data.retargeting;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientParametersException;

import java.io.IOException;
import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 29.09.14
 */
public class Retargeting {

    @JsonProperty("ret_cond_id")
    private String retargetingConditionID = "";

    @JsonProperty("condition_name")
    private String conditionName;

    @JsonProperty("condition_desc")
    private String conditionDesc;

    @JsonProperty("condition")
    private List<Condition> conditions;

    @JsonProperty("ret_cond_id")
    public String getRetargetingConditionID() {
        return retargetingConditionID;
    }

    @JsonProperty("ret_cond_id")
    public void setRetargetingConditionID(String retargetingConditionID) {
        this.retargetingConditionID = retargetingConditionID;
    }

    public String getConditionName() {
        return conditionName;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public String getConditionDesc() {
        return conditionDesc;
    }

    public void setConditionDesc(String conditionDesc) {
        this.conditionDesc = conditionDesc;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public String toJson() {
        String json;
        try {
            ObjectMapper mapper  = new ObjectMapper();
            mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE));
            json = mapper.writeValueAsString(this);
        } catch (IOException e) {
            throw new BackEndClientParametersException("Object parsing exception", e);
        }
        return json;
    }
}
