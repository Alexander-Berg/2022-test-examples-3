package toolkit.vault.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Version {
    String comment;

    Map<String, String> valueMap = new HashMap<>();

    @JsonSetter("value")
    void setValue(List<Map<String, String>> values) {
        for (Map<String, String> value : values) {
            valueMap.put(value.get("key"), value.get("value").trim());
        }
    }

    public String getValue(String key) {
        return valueMap.get(key);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Map<String, String> getValueMap() {
        return valueMap;
    }

    public void setValueMap(Map<String, String> valueMap) {
        this.valueMap = valueMap;
    }

    @Override
    public String toString() {
        return "Version{" +
                "comment='" + comment + '\'' +
                ", valueMap=" + valueMap +
                '}';
    }
}
