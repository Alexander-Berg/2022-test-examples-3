package ru.yandex.msearch.proxy.suggest.utils;

import java.io.IOException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import ru.yandex.http.test.HttpAssert;

import ru.yandex.http.util.CharsetUtils;

import ru.yandex.msearch.proxy.MsearchProxyCluster;
import ru.yandex.msearch.proxy.MsearchProxyTestBase;

import ru.yandex.msearch.proxy.api.async.ProxyParams;

import ru.yandex.parser.uri.QueryConstructor;

import ru.yandex.search.prefix.LongPrefix;

public class SuggestTestUtil {
    private static final AtomicLong TS =
        new AtomicLong(System.currentTimeMillis() / 1000);
    public static final int CONTACTS_NUMBER_IN_SUGGEST = 10;
    public final String suggestRoute;

    public SuggestTestUtil(final String suggestRoute) {
        this.suggestRoute = suggestRoute;
    }

    public String envelope(final String mid, final String... attrs) {
        StringBuilder sb = new StringBuilder("{\"mid\":\"");
        sb.append(mid);
        sb.append('"');
        for (String attr: attrs) {
            sb.append(',');
            sb.append(attr);
        }
        sb.append('}');
        return new String(sb);
    }

    private String formatField(
        final String field,
        final String dName,
        final String email)
    {
        return '"' + field + "\":\"\\\""
            + dName
            + "\\\" " + '<' + email + ">\n\","
            + '"' + field + "_normalized\":\"" + email + "\n\"";
    }

    public void indexDoc(
        final MsearchProxyCluster cluster,
        final MailUser user,
        final Email email)
        throws Exception
    {
        indexDoc(cluster, user.prefix(), email);
        Thread.sleep(10);
    }

    private void indexDoc(
        final MsearchProxyCluster cluster,
        final long prefix,
        final Email email)
        throws IOException
    {
        String ts = String.valueOf(TS.incrementAndGet());

        String mailFields = "\"received_date\":\"" + ts + "\","
            + formatField("hdr_from", email.fromName(), email.hdrFrom())
            + ","
            + formatField("hdr_to", email.toName(), email.hdrTo());

        if (email.hdrCc() != null) {
            mailFields += ",";
            mailFields += formatField("hdr_cc", email.ccName(), email.hdrCc());
        }

        cluster.backend().add(
            new LongPrefix(prefix),
            MsearchProxyTestBase.doc(prefix, email.mid(), mailFields, ""));
    }

    public SuggestResponse suggestRaw(
        final CloseableHttpClient client,
        final HttpHost host,
        final MailUser user,
        final String text)
        throws Exception
    {
        QueryConstructor qc = new QueryConstructor(suggestRoute);
        qc.append("maildb", "pg");
        qc.append(ProxyParams.UID, String.valueOf(user.prefix()));
        String request = qc.toString() + "&q=" + text;
        try (CloseableHttpResponse response =
                 client.execute(host, new HttpGet(request)))
        {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            String responseTxt = CharsetUtils.toString(response.getEntity());
            System.out.println("Response " + responseTxt);
            return new SuggestResponse(responseTxt);
        }
    }

    public SuggestResponse suggest(
        final CloseableHttpClient client,
        final HttpHost host,
        final MailUser user,
        final String text)
        throws Exception
    {
        QueryConstructor qc = new QueryConstructor(suggestRoute);
        qc.append("maildb", "pg");
        qc.append(ProxyParams.UID, String.valueOf(user.prefix()));
        qc.append("q", text);
        try (CloseableHttpResponse response =
                 client.execute(host, new HttpGet(qc.toString())))
        {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            String responseTxt = CharsetUtils.toString(response.getEntity());
            System.out.println("Response " + responseTxt);
            return new SuggestResponse(responseTxt);
        }
    }

    public String getRandomEmail() {
        return
            RandomStringUtils.randomAlphanumeric(7)
                + "@"
                + RandomStringUtils.randomAlphabetic(5)
                + "."
                + RandomStringUtils.randomAlphabetic(3);
    }

    public Map<String, String> sendFromContacts(
        final MsearchProxyCluster cluster,
        final MailUser user,
        final int num)
        throws Exception
    {
        Map<String, String> contactsToCheck = new LinkedHashMap<>();
        String[] mids = new String[num];
        for (int i = 0; i < num; i++) {
            String displayName = RandomStringUtils.randomAlphanumeric(15);
            String email = getRandomEmail();
            mids[i] = RandomStringUtils.randomNumeric(18);
            Email mail = new Email(mids[i])
                .from(displayName, email)
                .to(user.email(), user.email());

            indexDoc(cluster, user, mail);
            contactsToCheck.put(email, displayName);

        }

        return contactsToCheck;
    }

    public Map<String, String> sendToContactsByRcpts(
        final MsearchProxyCluster cluster,
        final MailUser user,
        final String[] rcpts)
        throws Exception
    {
        Map<String, String> contactsToCheck = new LinkedHashMap<>();
        String[] mids = new String[rcpts.length];
        for (int i = 0; i < rcpts.length; i++) {
            mids[i] = RandomStringUtils.randomNumeric(18);
            String displayName = RandomStringUtils.randomAlphanumeric(15);
            Email email = new Email(mids[i])
                .from(user.email(), user.email())
                .to(displayName, rcpts[i]);
            contactsToCheck.put(rcpts[i], displayName);

            indexDoc(cluster, user, email);
        }

        return contactsToCheck;
    }


    public Map<String, String> sendFromContactsByEmailsAndCCs(
        final MsearchProxyCluster cluster,
        final MailUser user,
        final String[] emails,
        final String[] ccs)
        throws Exception
    {
        Map<String, String> contactsToCheck = new LinkedHashMap<>();
        String[] mids = new String[emails.length];
        for (int i = 0; i < emails.length; i++) {
            String emailDisplayName = RandomStringUtils.randomAlphanumeric(15);
            String ccDisplayName = RandomStringUtils.randomAlphanumeric(15);
            mids[i] = RandomStringUtils.randomNumeric(18);

            //sendMessagesForSuggest(1,2,3,4)
            Email mail = new Email(mids[i])
                .from(emailDisplayName, emails[i])
                .to(user.email(), user.email())
                .cc(ccDisplayName, ccs[i]);
            indexDoc(cluster, user, mail);

            contactsToCheck.put(emails[i], emailDisplayName);
            contactsToCheck.put(ccs[i], ccDisplayName);
        }

        return contactsToCheck;
    }

    public Map<String, String> sendFromContactsByEmailsAndDisplayNames(
        final MsearchProxyCluster cluster,
        final MailUser user,
        final String[] emails,
        final String[] names)
        throws Exception
    {
        Map<String, String> contactsToCheck = new LinkedHashMap<>();
        String[] mids = new String[emails.length];
        for (int i = 0; i < emails.length; i++) {
            String displayName = names[i];
            mids[i] = RandomStringUtils.randomNumeric(18);
            Email mail = new Email(mids[i])
                .from(names[i], emails[i])
                .to(user.email(), user.email());
            contactsToCheck.put(emails[i], displayName);
            indexDoc(cluster, user, mail);
        }

        return contactsToCheck;
    }

    public Map<String, String> sendToContactsByRcptsAndDisplayNames(
        final MsearchProxyCluster cluster,
        final MailUser user,
        final String[] rcpts,
        final String[] names)
        throws Exception
    {
        if (rcpts.length != names.length) {
            throw new Exception(
                "Arrays rcpts and names shouls have the same length!");
        }

        Map<String, String> contactsToCheck = new LinkedHashMap<>();
        String[] mids = new String[rcpts.length];
        for (int i = 0; i < rcpts.length; i++) {
            String displayName = names[i];
            mids[i] = RandomStringUtils.randomNumeric(18);
            Email email = new Email(mids[i])
                .from(user.email(), user.email())
                .to(displayName, rcpts[i]);
            indexDoc(cluster, user, email);
            contactsToCheck.put(rcpts[i], displayName);
        }

        return contactsToCheck;
    }

    public Map<String, String> sendToContacts(
        final MsearchProxyCluster cluster,
        final MailUser user,
        final int num)
        throws Exception
    {
        Map<String, String> contactsToCheck = new LinkedHashMap<>();
        String[] mids = new String[num];
        for (int i = 0; i < num; i++) {
            String displayName = RandomStringUtils.randomAlphanumeric(15);
            String email = getRandomEmail();
            mids[i] = RandomStringUtils.randomNumeric(18);
            Email mail = new Email(mids[i])
                .from(user.email(), user.email())
                .to(displayName, email);

            indexDoc(cluster, user, mail);
            contactsToCheck.put(email, displayName);
        }

        return contactsToCheck;
    }

    public Map<String, String> sendToContactsByRcptsAndCCs(
        final MsearchProxyCluster cluster,
        final MailUser user,
        final String[] rcpts,
        final String[] ccs)
        throws Exception
    {
        Map<String, String> contactsToCheck = new LinkedHashMap<>();
        String[] mids = new String[rcpts.length];
        for (int i = 0; i < rcpts.length; i++) {
            String rcptDisplayName = RandomStringUtils.randomAlphanumeric(15);
            String ccDisplayName = RandomStringUtils.randomAlphanumeric(15);
            contactsToCheck.put(rcpts[i], rcptDisplayName);
            contactsToCheck.put(ccs[i], ccDisplayName);
            mids[i] = RandomStringUtils.randomNumeric(18);
            Email email =
                new Email(mids[i])
                    .from(user.email(), user.email())
                    .to(rcptDisplayName, rcpts[i])
                    .cc(ccDisplayName, ccs[i]);

            indexDoc(cluster, user, email);
        }

        return contactsToCheck;
    }

    public Map<String, String> sendFromContactsByDomains(
        final MsearchProxyCluster cluster,
        final MailUser user,
        final String[] domains)
        throws Exception
    {
        Map<String, String> contactsToCheck = new LinkedHashMap<>();
        String[] mids = new String[domains.length];
        for (int i = 0; i < domains.length; i++) {
            String displayName = RandomStringUtils.randomAlphanumeric(15);

            String email =
                RandomStringUtils.randomAlphabetic(10) + "@" + domains[i];

            mids[i] = RandomStringUtils.randomNumeric(18);
            contactsToCheck.put(email, displayName);
            //sendMessagesForSuggest(1, 2)
            Email mail = new Email(mids[i])
                .from(displayName, email)
                .to(user.email(), user.email());
            indexDoc(cluster, user, mail);
        }

        return contactsToCheck;
    }

    public Map<String, String> sendFromContactsByEmails(
        final MsearchProxyCluster cluster,
        final MailUser user,
        final String[] emails)
        throws Exception
    {
        Map<String, String> contactsToCheck = new LinkedHashMap<>();
        String[] mids = new String[emails.length];
        for (int i = 0; i < emails.length; i++) {
            String displayName = RandomStringUtils.randomAlphanumeric(15);
            mids[i] = RandomStringUtils.randomNumeric(18);
            Email mail = new Email(mids[i])
                .from(displayName, emails[i])
                .to(user.email(), user.email());
            indexDoc(cluster, user, mail);
            contactsToCheck.put(emails[i], displayName);
        }

        return contactsToCheck;
    }

    public Contact dummyContact(final MailUser user) {
        return new Contact(user.email(), user.email().split("@")[0]);
    }

    public List<Contact> getExpContacts(
        final Map<String, String> contactsToCheck)
    {
        return contactsToCheck.entrySet().stream()
            .map(e -> new Contact(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    }

    public static final class Email {
        private String fromName;
        private String toName;
        private String ccName;
        private String hdrFrom;
        private String hdrTo;
        private String hdrCc;
        private String mid;

        public Email(final String mid) {
            this.mid = mid;
        }

        public String hdrFrom() {
            return hdrFrom;
        }

        public Email from(final String name, final String hdrFrom) {
            this.hdrFrom = hdrFrom;
            this.fromName = name;
            return this;
        }

        public String hdrTo() {
            return hdrTo;
        }

        public Email to(final String name, final String hdrTo) {
            this.hdrTo = hdrTo;
            this.toName = name;
            return this;
        }

        public String hdrCc() {
            return hdrCc;
        }

        public Email cc(final String name, final String hdrCc) {
            this.hdrCc = hdrCc;
            this.ccName = name;
            return this;
        }

        public String fromName() {
            return fromName;
        }

        public String toName() {
            return toName;
        }

        public String ccName() {
            return ccName;
        }

        public String mid() {
            return mid;
        }
    }
}
