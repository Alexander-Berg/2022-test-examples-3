package ru.yandex.autotests.direct.httpclient.data.campaigns;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import ru.yandex.autotests.direct.httpclient.data.adjustment.rates.HierarchicalMultipliers;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientParametersException;

import java.io.IOException;

/**
 * Created by aleran on 06.08.2015.
 */
public class CampaignGroupBean {

    @JsonPath(responsePath = "hierarchical_multipliers")
    private HierarchicalMultipliers hierarchicalMultipliers;

    public HierarchicalMultipliers getHierarchicalMultipliers() {
        return hierarchicalMultipliers;
    }

    public void setHierarchicalMultipliers(HierarchicalMultipliers hierarchicalMultipliers) {
        this.hierarchicalMultipliers = hierarchicalMultipliers;
    }

    public CampaignGroupBean withHierarchicalMultipliers(HierarchicalMultipliers hierarchicalMultipliers){
        this.hierarchicalMultipliers = hierarchicalMultipliers;
        return this;
    }

    public String toJson() {
        String json;
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
            json = mapper.writeValueAsString(this);
        } catch (IOException e) {
            throw new BackEndClientParametersException("Object parsing exception", e);
        }
        return json;
    }

    public String toString() {
        return toJson();
    }
}
