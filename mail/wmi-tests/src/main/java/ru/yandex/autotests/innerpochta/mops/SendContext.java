package ru.yandex.autotests.innerpochta.mops;

import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.WaitedMailbox;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder.messagesByFolder;


class SendContext {
    private Envelope envelope;
    private HttpClientManagerRule rule;
    private WaitedMailbox waited;

    SendContext(WaitedMailbox waited, HttpClientManagerRule rule) {
        this.waited = waited;
        this.rule = rule;

        Optional<Envelope> opt = waited.getEnvelope();
        assertThat(opt.isPresent(), is(true));
        this.envelope = opt.get();
    }

    String subject() {
        return envelope.getSubject();
    }

    String firstMid() {
        return envelope.getMid();
    }

    String firstSentMid(String fid) {
        return messagesByFolder(MessagesByFolderObj
                    .empty()
                    .setUid(this.rule.account().uid())
                    .setFid(fid)
                    .setFirst("0")
                    .setCount("10")
                )
                .get()
                .via(this.rule)
                .firstMidBySubject(this.envelope.getSubject());
    }

    String firstTid() {
        return envelope.getThreadId();
    }

    List<String> mids() {
        return waited.getEnvelopes()
                .stream()
                .map(Envelope::getMid)
                .collect(Collectors.toList());
    }

    List<String> mids(String fid) {
        return waited.getEnvelopes(fid)
                .stream()
                .map(Envelope::getMid)
                .collect(Collectors.toList());
    }

    public Envelope message() {
        return envelope;
    }
}