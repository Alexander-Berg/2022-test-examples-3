package dto.responses.lgw.message.create_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Sender {

    @JsonProperty("ogrn")
    private String ogrn;

    @JsonProperty("incorporation")
    private String incorporation;

    @JsonProperty("name")
    private String name;

    @JsonProperty("inn")
    private String inn;

    @JsonProperty("phones")
    private List<PhonesItem> phones;

    @JsonProperty("id")
    private Id id;

    @JsonProperty("legalForm")
    private Integer legalForm;

    @JsonProperty("taxation")
    private Integer taxation;

    @JsonProperty("url")
    private String url;

    @JsonProperty("email")
    private String email;
}
