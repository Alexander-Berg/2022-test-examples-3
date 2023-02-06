package dto.responses.tm.admin.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Autocomplete {

    @JsonProperty("queryParamName")
    private String queryParamName;

    @JsonProperty("optionsSlug")
    private String optionsSlug;

    @JsonProperty("titleFieldName")
    private String titleFieldName;

    @JsonProperty("hint")
    private String hint;

    @JsonProperty("multiple")
    private Boolean multiple;

    @JsonProperty("pageSize")
    private Integer pageSize;

    @JsonProperty("idFieldName")
    private String idFieldName;

    @JsonProperty("authorities")
    private List<String> authorities;
}
