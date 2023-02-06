package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ContactsItem {

    @JsonProperty("phone_contact")
    private PhoneContact phoneContact;

    @JsonProperty("class_name")
    private String className;

    @JsonProperty("email_contact")
    private EmailContact emailContact;
}
