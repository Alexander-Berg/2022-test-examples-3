package ru.yandex.passport.contact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ContactTestUtils {
    private static final int RANDOM_SEED = 11;
    private static final Random RANDOM = new Random(RANDOM_SEED);

    public static String userJson(String puid, String guid) {
        return String.format(
                "\"user_id\":\"%s\"," +
                        "\"user_uid\":\"%s\"," +
                        " \"id\":\"user_%s\", " +
                        "\"type_p\":\"user\"",
                guid, puid, guid);
    }

    public static String contactJson(String guid, String contactName, String contactGuid) {
        return String.format(
                "\"contact_name\":\"%s\"," +
                        "\"version\":\"0\"," +
                        "\"contact_id\":\"%s\"," +
                        "\"contact_user_id\":\"%s\"," +
                        "\"id\":\"contact_%s\"",
                contactName, contactGuid, guid, contactGuid + guid);
    }

    public static class User {
        private final String puid;
        private final String[] guids;
        private final List<Contact> mutualContacts = new ArrayList<>();

        public User(String puid, String... guids) {
            this.puid = puid;
            this.guids = guids;
        }

        public String puid() {
            return puid;
        }

        public String[] guids() {
            return guids;
        }

        public Contact connect(String name, User other) {
            return connect(name, randomGuidIndex(), other);
        }

        public Contact connect(String name, int guidIndex, User other) {
            return new Contact(name, guids[guidIndex], other.puid(), other.guids[other.randomGuidIndex()]);
        }

        public Pair<Contact, Contact> connectMutually(String name, String otherName, User other) {
            return connectMutually(name, otherName, randomGuidIndex(), other.randomGuidIndex(), other);
        }

        public Pair<Contact, Contact> connectMutually(
                String name,
                String otherName,
                int guidIndex,
                int otherGuidIndex,
                User other)
        {
            String guid = guids[guidIndex];
            String otherGuid = other.guids[otherGuidIndex];

            Contact contact = new Contact(name, guid, other.puid(), otherGuid);
            mutualContacts.add(contact);
            Contact otherContact = new Contact(otherName, otherGuid, puid, guid);
            other.mutualContacts.add(otherContact);

            return new Pair<>(
                    contact,
                    otherContact
            );
        }

        public List<Contact> mutualConnections() {
            return Collections.unmodifiableList(mutualContacts);
        }

        private int randomGuidIndex() {
            return RANDOM.nextInt(guids.length);
        }
    }

    public static class Contact {
        private final String name;
        private final String ownerGuid;
        private final String contactPuid;
        private final String contactGuid;

        public Contact(String name, String ownerGuid, String contactPuid, String contactGuid) {
            this.name = name;
            this.ownerGuid = ownerGuid;
            this.contactPuid = contactPuid;
            this.contactGuid = contactGuid;
        }

        public String name() {
            return name;
        }

        public String ownerGuid() {
            return ownerGuid;
        }

        public String contactPuid() {
            return contactPuid;
        }

        public String contactGuid() {
            return contactGuid;
        }
    }

    public static class Pair<T, U> {
        private final T first;
        private final U second;

        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }

        public T first() {return first;}
        public U second() {return second;}
    }
}
