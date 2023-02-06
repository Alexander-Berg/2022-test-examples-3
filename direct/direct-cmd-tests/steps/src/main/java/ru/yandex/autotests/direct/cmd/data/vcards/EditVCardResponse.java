package ru.yandex.autotests.direct.cmd.data.vcards;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;

public class EditVCardResponse {

    @SerializedName("vcard_id")
    private Long vCardId;

    @SerializedName("vcard")
    private ContactInfo vCard;

    public Long getVCardId() {
        return vCardId;
    }

    public EditVCardResponse withVCardId(Long vCardId) {
        this.vCardId = vCardId;
        return this;
    }

    public ContactInfo getVCard() {
        return vCard;
    }

    public EditVCardResponse withVCard(ContactInfo vCard) {
        this.vCard = vCard;
        return this;
    }
}
