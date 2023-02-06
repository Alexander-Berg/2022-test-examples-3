package dto.responses.lgw.message.create_order;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Contact {

    @JsonProperty("surname")
    private String surname;

    @JsonProperty("name")
    private String name;
}
