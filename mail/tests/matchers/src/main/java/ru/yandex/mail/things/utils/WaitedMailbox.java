package ru.yandex.mail.things.utils;

import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;
import ru.yandex.mail.tests.hound.generated.Envelope;
import ru.yandex.mail.tests.hound.generated.messagesbyfolder.ApiMessagesByFolder;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.tests.hound.MessagesByFolder.messagesByFolder;


public class WaitedMailbox {
    private UserCredentials authClient;
    private String fid;
    private String subject;

    WaitedMailbox(String subj, String fid, UserCredentials authClient) {
        this.subject = subj;
        this.authClient = authClient;
        this.fid = fid;
    }

    public String getMid() {
        return messagesByFolder(defaultParams().post(shouldBe(HoundResponses.ok200())))
                .firstMidBySubject(subject);
    }

    public Optional<Envelope> getEnvelope() {
        return messagesByFolder(defaultParams().post(shouldBe(HoundResponses.ok200())))
                .envelopesWithSubject(subject)
                .findFirst();
    }

    public List<Envelope> getEnvelopes() {
        return messagesByFolder(defaultParams().post(shouldBe(HoundResponses.ok200())))
                .envelopesWithSubject(subject)
                .collect(Collectors.toList());
    }

    public List<Envelope> getEnvelopes(String fid) {
        return messagesByFolder(defaultParams().withFid(fid).post(shouldBe(HoundResponses.ok200())))
                .envelopesWithSubject(subject)
                .collect(Collectors.toList());
    }

    public String getTid() {
        return messagesByFolder(
                defaultParams()
                        .post(shouldBe(HoundResponses.ok200()))
        )
                .envelopesWithSubject(subject)
                .findFirst()
                .map(Envelope::getThreadId)
                .get();
    }

    public String getFirstMidIf(Predicate<Envelope> predicate) {
        return messagesByFolder(defaultParams().post(shouldBe(HoundResponses.ok200())))
                .firstMidIf(predicate);
    }

    public List<String> getMids() {
        return messagesByFolder(defaultParams().post(shouldBe(HoundResponses.ok200())))
                .envelopesWithSubject(subject)
                .map(Envelope::getMid)
                .collect(Collectors.toList());
    }

    private ApiMessagesByFolder defaultParams() {
        return HoundApi.apiHound(HoundProperties.properties().houndUri(), props().getCurrentRequestId())
                .messagesByFolder()
                .withUid(authClient.account().uid())
                .withFid(fid)
                .withFirst("0")
                .withCount("10");
    }
}
