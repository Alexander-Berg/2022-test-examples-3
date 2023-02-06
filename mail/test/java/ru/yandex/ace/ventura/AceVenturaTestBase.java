package ru.yandex.ace.ventura;

import java.util.Objects;

import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.test.util.TestBase;

public class AceVenturaTestBase extends TestBase {
    protected static void share(
        final AceVenturaCluster cluster,
        final AceVenturaPrefix prefixTo,
        final AceVenturaPrefix ownerPrefix,
        final int listId)
        throws Exception
    {
        cluster.searchBackend().add(
            prefixTo,
            "\"id\":\""
                + AceVenturaFields.sharedUrl(
                String.valueOf(listId),
                ownerPrefix,
                prefixTo)
                + "\",\"av_record_type\": \"shared\","
                + "\"av_shared_owner_uid\": \""
                + ownerPrefix.uid() + "\","
                + "\"av_shared_owner_utype\":\""
                + ownerPrefix.userType().lowName() + "\","
                + "\"av_shared_list_id\": \"" + listId + "\"");
    }

    protected static void addTag(
        final AceVenturaCluster cluster,
        final AceVenturaPrefix prefix,
        final String id,
        final String name)
        throws Exception
    {
        cluster.searchBackend().add(
            prefix,
            "\"id\":\"" + AceVenturaFields.tagUrl(id, prefix) + "\","
                + "\"av_record_type\": \"tag\","
                + "\"av_tag_name\": \"" + name + "\",\"av_tag_id\":" + id);
    }

    protected static String addContact(
        final AceVenturaCluster cluster,
        final AceVenturaPrefix prefix,
        final long cid,
        final String names)
        throws Exception
    {
        return addContact(cluster, prefix, cid, names, names, null, null);
    }

    protected static String addContact(
        final AceVenturaCluster cluster,
        final AceVenturaPrefix prefix,
        final long cid,
        final String names,
        final String aliases,
        final String tags,
        final String phones)
        throws Exception
    {
        return
            addContact(cluster, prefix, cid, 1, names, aliases, tags, phones);
    }

    protected static String addContact(
        final AceVenturaCluster cluster,
        final AceVenturaPrefix prefix,
        final long cid,
        final String names,
        final String aliases,
        final String tags,
        final String phones,
        final String vcard)
        throws Exception
    {
        return
            addContact(
                cluster,
                prefix,
                cid,
                1,
                names,
                aliases,
                tags,
                phones,
                vcard);
    }

    protected static String addContact(
        final AceVenturaCluster cluster,
        final AceVenturaPrefix prefix,
        final long cid,
        final long listId,
        final String names,
        final String aliases,
        final String tags,
        final String phones)
        throws Exception
    {
        return addContact(
            cluster,
            prefix,
            cid,
            listId,
            names,
            aliases,
            tags,
            phones,
            "{}");
    }

    protected static String addContact(
        final AceVenturaCluster cluster,
        final AceVenturaPrefix prefix,
        final long cid,
        final long listId,
        final String names,
        final String aliases,
        final String tags,
        final String phones,
        final String vcard)
        throws Exception
    {
        String record =
            "\"av_list_id\": \"" + listId + "\",\n"
                + "\"av_list_name\": \"Personal\",\n"
                + "\"av_list_type\": \"personal\",\n"
                + "\"av_cid\": \"" + cid + "\",\n"
                + "\"av_user_id\": \"" + prefix.uid() + "\",\n"
                + "\"av_user_type\": \"" + prefix.userType().lowName() + "\",\n"
                + "\"av_vcard\":\"" + vcard
                + "\",\"id\": \"av_contact_" + prefix.uid()
                + '_' + prefix.userType().lowName()
                + '_' + cid + "\","
                + "\"av_names\": \"" + names + "\",\n"
                + "\"av_names_alias\": \"" + aliases + "\",\n"
                + "\"av_record_type\": \"contact\"";
        if (phones != null) {
            record += ",\"av_has_phones\": \"true\",\"av_phones\":\"" + phones
                + "\",\"av_phones_n\":\"" + phones + "\"";
        } else {
            record += ",\"av_has_phones\": \"true\"";
        }

        if (tags != null) {
            record += ",\"av_tags\":\"" + tags + "\"";
        }
        cluster.searchBackend().add(prefix, record);
        return '{' + record + '}';
    }

    protected static void addEmail(
        final AceVenturaCluster cluster,
        final AceVenturaPrefix prefix,
        final long eid,
        final String contactRecord,
        final String email,
        final String login,
        final String domain,
        final String tags)
        throws Exception
    {
        addEmail(cluster, prefix, eid, contactRecord, email, login, domain, tags, -1L);
    }

    protected static void addEmail(
        final AceVenturaCluster cluster,
        final AceVenturaPrefix prefix,
        final long eid,
        final String contactRecord,
        final String email,
        final String login,
        final String domain,
        final String tags,
        final long lastUsage)
        throws Exception
    {
        JsonMap contact =
            TypesafeValueContentHandler.parse(contactRecord).asMap();
        String record = "\"av_domain\": \"" + domain + "\",\n"
            + "\"av_domain_nt\": \"" + domain + "\",\n"
            + "\"av_email\": \"" + email + "\",\n"
            + "\"av_email_cid\": \"" + contact.getLong("av_cid")
            + "\",\"av_email_id\": \"" + eid + "\",\n"
            + "\"av_email_type\": \"\","
            + "\"av_names\":\"" + contact.getString("av_names") + "\","
            + "\"av_names_alias\":\""
            + contact.getString("av_names_alias") + "\","
            + "\"av_has_phones\":\""
            + contact.getString("av_has_phones") + "\","
            + "\"av_list_id\":\""
            + contact.getString("av_list_id") + "\","
            + "\"av_login\": \""+ login + "\",\n"
            + "\"av_record_type\": \"email\",\n"
            + "\"av_revision\": \"178\",\n"
            + "\"av_user_id\": \"" + prefix.uid() + "\",\n"
            + "\"av_user_type\": \"" + prefix.userType().lowName() + "\",\n"
            + "\"id\": \"av_email_" + prefix.uid() + '_'
            + prefix.userType().lowName() + '_' + eid + "\"";

        if (tags != null) {
            record += ",\"av_tags\": \"" + tags + "\"";
        }

        if (lastUsage >= 0) {
            record += ",\"av_last_usage\": " + lastUsage;
        }

        cluster.searchBackend().add(prefix, record);
    }

    protected static String suggestReportRequest(
            String host,
            ContactTestingEnvironment.UserData user,
            int contactId,
            String request,
            String title,
            int ts
    ) {
        return  host
                + "/v1/suggestReport?&user_type="
                + user.type.lowName()
                + "&user_id=" + user.id
                + "&contact_id=" + contactId
                + "&request=" + request
                + "&title=" + title
                + "&ts=" + ts;
    }

    protected static String suggestRequest(
            String host,
            ContactTestingEnvironment.UserData user,
            String sortKey
    ) {
        return host
                + "/v1/suggest?user_id=" + user.id
                + "&user_type=" + user.type.lowName()
                + "&shared=include&limit=10"
                + (Objects.isNull(sortKey) ? "" : "&sort=" + sortKey);
    }

    protected static String suggestRequest(
            String host,
            ContactTestingEnvironment.UserData user
    ) {
        return suggestRequest(host, user, null);
    }
}
