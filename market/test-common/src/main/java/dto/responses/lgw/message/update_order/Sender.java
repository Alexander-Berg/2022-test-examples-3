package dto.responses.lgw.message.update_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Sender {

    @JsonProperty("ogrn")
    private String ogrn;

    @JsonProperty("incorporation")
    private String incorporation;

    @JsonProperty("inn")
    private String inn;

    @JsonProperty("name")
    private String name;

    @JsonProperty("phones")
    private List<PhonesItem> phones;

    @JsonProperty("id")
    private Id id;

    @JsonProperty("legalForm")
    private int legalForm;

    @JsonProperty("taxation")
    private int taxation;

    @JsonProperty("url")
    private String url;
}
