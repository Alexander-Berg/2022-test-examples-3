package ru.yandex.mail.tests.hound;

import io.restassured.response.Response;
import ru.yandex.mail.tests.hound.generated.Envelope;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class MessagesByFolder extends OperationWithEnvelopes {
    private MessagesByFolder(String responseAsString) {
        super(responseAsString);
    }

    public static MessagesByFolder messagesByFolder(Response response) {
        return new MessagesByFolder(response.asString());
    }

    public String firstMidBySubject(String subj) {
        return envelopes()
                .stream()
                .filter
                        (e -> e
                                .getSubjectInfo()
                                .getSubject()
                                .equals(subj))
                .map(Envelope::getMid)
                .findFirst()
                .orElse(null);
    }

    public List<String> mids() {
        return envelopes()
                .stream()
                .map(Envelope::getMid)
                .collect(toList());
    }

    public String firstMidIf(Predicate<Envelope> predicate) {
        return envelopes()
                .stream()
                .filter(predicate)
                .map(Envelope::getMid)
                .findFirst()
                .orElse(null);
    }

    public Long receivedDate(String mid) {
        return envelopes()
                .stream()
                .filter(
                        (e -> e
                                .getMid()
                                .equals(mid))
                )
                .map(Envelope::getReceiveDate)
                .findFirst()
                .orElse(null);
    }

    public Stream<Envelope> envelopesWithSubject(String subject) {
        return envelopes()
                .stream()
                .filter(e -> e.getSubject().equals(subject));
    }

    public Integer countIf(Predicate<Envelope> predicate) {
        List<Envelope> envelope = envelopes();

        if (envelope.isEmpty()) {
            return null;
        }

        return (int)envelope
                .stream()
                .filter(predicate)
                .count();
    }
}
