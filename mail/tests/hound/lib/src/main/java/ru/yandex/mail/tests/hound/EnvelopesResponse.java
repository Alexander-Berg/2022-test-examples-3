package ru.yandex.mail.tests.hound;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ru.yandex.mail.tests.hound.generated.Envelope;

import java.util.List;

public class EnvelopesResponse {
    @SerializedName("envelopes")
    @Expose
    private List<Envelope> envelopes;

    public List<Envelope> getEnvelopes() {
        return envelopes;
    }

    public void setEnvelopes(List<Envelope> envelopes) {
        this.envelopes = envelopes;
    }
}
