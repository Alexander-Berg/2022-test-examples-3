package ru.yandex.autotests.innerpochta.imap.structures;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static ch.lambdaj.collection.LambdaCollections.with;
import static com.google.common.base.Joiner.on;
import static com.sun.mail.imap.protocol.BASE64MailboxDecoder.decode;
import static java.lang.String.format;

public class ListItem implements Cloneable {
    private final String reference;
    private Set<String> flags = new HashSet<>();
    private String name;

    public ListItem(String reference, String name, String... flags) {
        this.reference = reference;
        this.name = name;
        Collections.addAll(this.flags, flags);
    }

    public ListItem(String reference, String name, Set<String> flags) {
        this.reference = reference;
        this.name = name;
        this.flags = flags;
    }

    public Set<String> getFlags() {
        return flags;
    }

    public String getReference() {
        return reference;
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        name = newName;
    }

    public String toString() {
        return format("<%s> с флагами [%s] и разделителем %s",
                name.startsWith("&B") || name.startsWith("\"&B") ? format("%s (%s)", decode(name), name) : name,
                on(' ').join(flags),
                reference);
    }

    public ListItem clone() {
        return new ListItem(reference, name, with(flags).clone());
    }
}
