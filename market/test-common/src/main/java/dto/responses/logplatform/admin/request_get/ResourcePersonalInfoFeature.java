package dto.responses.logplatform.admin.request_get;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ResourcePersonalInfoFeature {

    @JsonProperty("patronymic")
    private String patronymic;

    @JsonProperty("yandex_uid")
    private String yandexUid;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("contacts")
    private List<ContactsItem> contacts;
}
