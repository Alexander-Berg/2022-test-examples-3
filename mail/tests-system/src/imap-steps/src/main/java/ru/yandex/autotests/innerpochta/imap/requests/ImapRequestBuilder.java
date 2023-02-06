package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.responses.ImapResponse;

public interface ImapRequestBuilder<T extends ImapResponse<?>> {
    ImapRequest<T> build(String tag);
}
