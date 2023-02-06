package ru.yandex.ace.ventura;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class ContactTestingEnvironment extends AceVenturaTestBase {
    private final AceVenturaCluster cluster;

    private final HashMap<Integer, ContactData> contactToData = new HashMap<>();
    private final HashMap<Long, UserData> userToData = new HashMap<>();
    private final HashMap<Long, Set<Integer>> userToContacts = new HashMap<>();

    public ContactTestingEnvironment(AceVenturaCluster cluster, UserData[] users) throws Exception {
        this.cluster = cluster;

        for (UserData user: users) {
            addUser(user);
        }
    }

    private void addUser(final UserData user) throws Exception {
        userToData.put(user.id, user);

        AceVenturaPrefix prefix = new AceVenturaPrefix(user.id, user.type);
        Set<Integer> contacts = new HashSet<>();
        for (ContactData contact: user.contacts) {
            String contactStr = addContact(
                    cluster,
                    prefix,
                    contact.contactId,
                    contact.listId,
                    contact.getNames(),
                    contact.getAliases(),
                    contact.getTagsIds(),
                    contact.phone);
            contacts.add(contact.contactId);
            contactToData.put(contact.contactId, contact);
            for (EmailData email: contact.emails) {
                addEmail(
                        cluster,
                        prefix,
                        email.id,
                        contactStr,
                        email.value,
                        email.login,
                        email.domain,
                        email.getTags()
                );
            }
        }
        userToContacts.put(user.id, contacts);
    }

    public void useContact(int contactId, long emailId, int ts) {
        ContactData contact = contactToData.get(contactId);
        for (EmailData email: contact.emails) {
            if (email.id == emailId) {
                email.lastUsage = Math.max(ts, email.lastUsage);
                contact.lastUsage = Math.max(ts, contact.lastUsage);
                return;
            }
        }
    }

    private String getUserType(long id) {
        return userToData.get(id).type.lowName();
    }

    public void share(long idFrom, long idTo, int listId) throws Exception {
        UserData userFrom = userToData.get(idFrom);
        UserData userTo = userToData.get(idTo);

        AceVenturaPrefix prefixFrom = new AceVenturaPrefix(userFrom.id, userFrom.type);
        AceVenturaPrefix prefixTo = new AceVenturaPrefix(userTo.id, userTo.type);

        share(cluster, prefixTo, prefixFrom, listId);

        for (ContactData contact: userFrom.contacts) {
            if (contact.listId == listId) {
                Set<Integer> contacts = userToContacts.get(idTo);
                contacts.add(contact.contactId);
            }
        }
    }

    // Adding tags of contact (not of email) may be a bug
    protected String buildSingleEmail(EmailData email, int[] contactTags) {
        return "{" +
                "\"id\":" + email.id + "," +
                "\"value\":\"" + email.value + "\", " +
                "\"last_usage\":" + email.lastUsage + ", " +
                "\"tags\":[" +
                Arrays.stream(contactTags)
                        .mapToObj(Integer::toString)
                        .collect(Collectors.joining(", ")) +
                "]" +
                "}";
    }

    protected String buildSingleContact(ContactData contact) {
        return "{" +
                "\"contact_owner_user_id\":" + contact.ownerId + ", " +
                "\"contact_owner_user_type\":\"" + getUserType(contact.ownerId) + "\", " +
                "\"contact_id\":" + contact.contactId + ", " +
                "\"list_id\":" + contact.listId + ", " +
                "\"revision\":\"" + contact.revision + "\", " +
                "\"tag_ids\":[" +
                Arrays.stream(contact.tagsIds)
                        .mapToObj(Integer::toString)
                        .collect(Collectors.joining(", ")) +
                "], " +
                "\"emails\":[" +
                Arrays.stream(contact.emails)
                        .map(x -> buildSingleEmail(x, contact.tagsIds))
                        .collect(Collectors.joining(", ")) +
                "], " +
                "\"vcard\": {\"emails\":[" +
                Arrays.stream(contact.emails)
                        .map(x -> "{\"email\":\"" + x.value + "\"}")
                        .collect(Collectors.joining(", ")) +
                "]}" +
                "}";
    }

    public String buildExpectedResponse(long userId) {
        return "{\"contacts\":[" +
                userToContacts.get(userId).stream().map(contactToData::get)
                        .sorted(Comparator.comparingInt(x -> ((ContactData) x).lastUsage).reversed())
                        .map(this::buildSingleContact)
                        .collect(Collectors.joining(", ")) +
                "], " +
                "\"tags\":[]" +
                "}";
    }

    protected static class UserData {
        public final long id;
        public final UserType type;
        public final ContactData[] contacts;

        private static long defaultId = 0;

        public UserData(long id, ContactData[] contacts, UserType type) {
            this.id = id;
            this.contacts = contacts;
            this.type = type;

            defaultId = Math.max(defaultId, id + 1);
        }

        public UserData(long id, ContactData[] contacts) {
            this(id, contacts, UserType.PASSPORT_USER);
        }

        public UserData(ContactData[] contacts) {
            this(defaultId, contacts, UserType.PASSPORT_USER);
        }
    }

    public static class EmailData {
        private static final int[] DEFAULT_TAGS = new int[]{1, 11};
        private static final String DEFAULT_DOMAIN = "yandex.ru";

        public final long id;
        public final String value;
        public final String login;
        public final String domain;
        public int lastUsage;
        public final int[] tags;

        private static long defaultId = 0;

        EmailData(long id, String value, String login, String domain, int[] tags) {
            this.id = id;
            this.value = value;
            this.login = login;
            this.domain = domain;
            this.lastUsage = 0;
            this.tags = tags;

            defaultId = Math.max(defaultId, id + 1);
        }

        EmailData(long id, String login, String domain) {
            this(id, login + '@' + domain, login, domain, DEFAULT_TAGS);
        }

        EmailData(long id, String login) {
            this(id, login, DEFAULT_DOMAIN);
        }

        EmailData(String login) {
            this(defaultId, login);
        }

        public String getTags() {
            return Arrays.stream(tags).mapToObj(Integer::toString).collect(Collectors.joining("\n"));
        }
    }

    public static class ContactData {
        private static final int DEFAULT_LIST_ID = 1;
        private static final int[] DEFAULT_TAGS_IDS = new int[]{20, 21};
        private static final String DEFAULT_REVISION = "";

        private static final String DELIMITER = "\n";

        private static int defaultId = 0;

        public final long ownerId;
        public final int contactId;
        public final int listId;
        public final String revision;
        public final int[] tagsIds;
        public final String[] names;
        public final String[] aliases;
        public EmailData[] emails;
        public final String phone;
        public int lastUsage = 0;

        ContactData(
                long ownerId,
                int contactId,
                int listId,
                String revision,
                int[] tagsIds,
                String[] names,
                String[] aliases,
                EmailData[] emails,
                String phone) {
            this.ownerId = ownerId;
            this.contactId = contactId;
            this.listId = listId;
            this.revision = revision;
            this.tagsIds = tagsIds;
            this.names = names;
            this.aliases = aliases;
            this.emails = emails;
            this.phone = phone;

            defaultId = Math.max(defaultId, contactId + 1);
        }

        ContactData(
                long ownerId,
                int contactId,
                int listId,
                String revision,
                int[] tagsIds,
                String[] names,
                String[] aliases,
                EmailData[] emails
        ) {
            this(ownerId, contactId, listId, revision, tagsIds, names, aliases, emails, null);
        }

        ContactData(
                long ownerId,
                int contactId,
                int listId,
                String[] names,
                String[] aliases,
                EmailData[] emails
        ) {
            this(ownerId, contactId, listId, DEFAULT_REVISION, DEFAULT_TAGS_IDS, names, aliases, emails);
        }

        ContactData(
                long ownerId,
                int contactId,
                String[] names,
                String[] aliases,
                EmailData[] emails
        ) {
            this(ownerId, contactId, DEFAULT_LIST_ID, DEFAULT_REVISION, DEFAULT_TAGS_IDS, names, aliases, emails);
        }

        ContactData(
                long ownerId,
                String[] names,
                String[] aliases,
                EmailData[] emails
        ) {
            this(ownerId, defaultId, names, aliases, emails);
        }

        private String arrToString(String[] arr) {
            return String.join(DELIMITER, arr);
        }

        public String getNames() {
            return arrToString(names);
        }

        public String getTagsIds() {
            return arrToString(Arrays.stream(tagsIds).mapToObj(Integer::toString).toArray(String[]::new));
        }

        public String getAliases() {
            return arrToString(aliases);
        }
    }
}
