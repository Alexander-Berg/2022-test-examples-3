package ru.yandex.autotests.innerpochta.imap.consts;

import java.util.ArrayList;

import static com.google.common.base.Joiner.on;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;

/**
 * Created by kurau on 23.04.14.
 */
public class Headers {
    private ArrayList<String> items;

    private Headers() {
        this.items = new ArrayList<>();
    }

    public static Headers headers() {
        return new Headers();
    }

    public Headers received() {
        items.add("Received");
        return this;
    }

    public Headers xYandexTimemark() {
        items.add("X-Yandex-Timemark");
        return this;
    }

    public Headers xYandexFront() {
        items.add("X-Yandex-Front");
        return this;
    }

    public Headers xYandexSpam() {
        items.add("X-Yandex-Spam");
        return this;
    }

    public Headers from() {
        items.add("From");
        return this;
    }

    public Headers bcc() {
        items.add("Bcc");
        return this;
    }

    public Headers subject() {
        items.add("Subject");
        return this;
    }

    public Headers to() {
        items.add("To");
        return this;
    }

    public Headers cc() {
        items.add("Cc");
        return this;
    }

    public Headers mimeVersion() {
        items.add("MIME-Version");
        return this;
    }

    public Headers messageID() {
        items.add("Message-ID");
        return this;
    }

    public Headers date() {
        items.add("Date");
        return this;
    }

    public Headers priority() {
        items.add("Priority");
        return this;
    }

    public Headers xPriority() {
        items.add("X-Priority");
        return this;
    }

    public Headers xUniformTypeIdentifier() {
        items.add("x-uniform-type-identifier");
        return this;
    }

    public Headers xUniversallyUniqueIdentifier() {
        items.add("x-universally-unique-identifier");
        return this;
    }

    public Headers references() {
        items.add("References");
        return this;
    }

    public Headers newsGroups() {
        items.add("Newsgroups");
        return this;
    }

    public Headers inReplyTo() {
        items.add("In-Reply-To");
        return this;
    }

    public Headers xMailer() {
        items.add("X-Mailer");
        return this;
    }

    public Headers contentType() {
        items.add("Content-Type");
        return this;
    }

    public Headers returnPath() {
        items.add("Return-Path");
        return this;
    }

    public Headers xYandexForward() {
        items.add("X-Yandex-Forward");
        return this;
    }

    public String inLine() {
        return roundBraceList(items);
    }

    public String toString() {
        return String.format("%s", on(' ').join(items));
    }
}
