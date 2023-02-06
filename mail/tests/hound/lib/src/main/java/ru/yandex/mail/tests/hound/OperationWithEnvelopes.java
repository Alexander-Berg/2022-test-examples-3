package ru.yandex.mail.tests.hound;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import ru.yandex.mail.tests.hound.generated.Envelope;

import java.util.List;

public class OperationWithEnvelopes {
    protected String respAsString;

    public List<Envelope> envelopes() {
        try {
            return new Gson().fromJson(respAsString, EnvelopesResponse.class).getEnvelopes();
        } catch (JsonSyntaxException e) {
            throw new AssertionError("Не удалось распарсить JSON", e);
        }
    }

    OperationWithEnvelopes(String respAsString) {
        this.respAsString = respAsString;
    }
}
