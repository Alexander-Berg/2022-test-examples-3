package ru.yandex.iex.proxy;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;

import ru.yandex.blackbox.BlackboxUserinfo;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.writer.JsonType;

public final class IexProxyTestMocks {
    public static final String FILTER_SEARCH_URI = "/filter_search?";
    public static final String ENVELOPES = "envelopes";

    private IexProxyTestMocks() {
    }

    //CSOFF: MultipleStringLiterals
    public static String pgNotifyPost(final long uid, final String mid) {
        return "{\"select_date\":\"1492202195.306\",\"uid\":\"" + uid + "\","
            + "\"pgshard\":\"2094\",\"lcn\":\"9179\",\"fresh_count\":\"0\","
            + "\"operation_date\":\"1492202195.239788\",\"operation_id\":"
            + "\"3555057022\",\"change_type\":\"store\",\"useful_new_messages\""
            + ":\"1252\",\"changed\":[{\"hdr_message_id\":\"<123@some>\","
            + "\"fid\":2,\"deleted\":false,\"mid\":" + mid + ",\"recent\":true,"
            + "\"lids\":[118,48,63,65],\"seen\":false,\"tid\":" + mid + "}]}";
    }

    public static String filterSearchUri(final long uid, final String mid) {
        return FILTER_SEARCH_URI
            + (BlackboxUserinfo.corp(uid) ? IexProxy.CORP_FILTER_SEARCH_PARAMS : IexProxy.FILTER_SEARCH_PARAMS)
            + "&uid=" + uid + "&mdb=pg&mids=" + mid;
    }

    public static String filterSearchUri(final long uid, final String... mids) {
        StringBuilder sb = new StringBuilder(FILTER_SEARCH_URI);
        sb.append(BlackboxUserinfo.corp(uid) ? IexProxy.CORP_FILTER_SEARCH_PARAMS : IexProxy.FILTER_SEARCH_PARAMS);
        sb.append("&uid=").append(uid).append("&mdb=pg");
        for (String mid : mids) {
            sb.append("&mids=").append(mid);
        }
        return sb.toString();
    }

    public static String filterSearchUri(final long uid, final Collection<String> mids) {
        StringBuilder sb = new StringBuilder(FILTER_SEARCH_URI);
        sb.append(BlackboxUserinfo.corp(uid) ? IexProxy.CORP_FILTER_SEARCH_PARAMS : IexProxy.FILTER_SEARCH_PARAMS);
        sb.append("&uid=").append(uid).append("&mdb=pg");
        for (String mid : mids) {
            sb.append("&mids=").append(mid);
        }
        return sb.toString();
    }

    public static String filterSearchPgResponse(
        final String mid,
        final String stid,
        final String types)
    {
        return filterSearchPgResponse(mid, stid, types, "user");
    }

    //CSOFF: ParameterNumber
    public static String filterSearchPgResponse(
        final String mid,
        final String stid,
        final String types,
        final String folderType)
    {
        return filterSearchPgResponse(mid, stid, types, folderType, "login", "d.c");
    }

    //CSOFF: ParameterNumber
    public static String filterSearchPgResponse(
        final String mid,
        final String stid,
        final String types,
        final String folderType,
        final String localFrom,
        final String domainFrom)
    {
        return filterSearchPgResponse(mid, stid, types, folderType, "login", "d.c", "Subject");
    }

    //CSOFF: ParameterNumber
    public static String filterSearchPgResponse(
        final String mid,
        final String stid,
        final String types,
        final String folderType,
        final String localFrom,
        final String domainFrom,
        final String subject)
    {
        return
            "{\"envelopes\":["
                + "{\"mid\":\"" + mid + "\",\"fid\":\"2\",\"threadId\":\"\","
                    + "\"revision\":9179,\"date\":1492202194,"
                    + "\"receiveDate\":1492202195,"
                    + "\"from\":[{\"local\":\"" + localFrom + "\",\"domain\":\""
                    + domainFrom + "\"," + "\"displayName\":\"Name\"}],"
                    + "\"replyTo\":[{\"local\":\"" + localFrom + "\",\"domain\":\""
                    + domainFrom + "\","
                        + "\"displayName\":\"RName\"}],"
                    + "\"subject\":\"" + subject + "\","
                    + "\"subjectInfo\":{\"type\":\"\",\"prefix\":\"\","
                        + "\"subject\":\"" + subject + "\",\"postfix\":\"\","
                        + "\"isSplitted\":true},"
                    + "\"cc\":[],\"bcc\":[],\"hdrStatus\":\"\","
                    + "\"to\":[{\"local\":\"123\",\"domain\":\"yandex.ru\","
                        + "\"displayName\":\"dName\"}],"
                    + "\"hdrLastStatus\":\"\",\"uidl\":\"\","
                    + "\"imapId\":\"15020\",\"ImapModSeq\":\"\","
                    + "\"stid\":\"" + stid + "\","
                    + "\"firstline\":\"Firstline!!!\",\"inReplyTo\":\"\","
                    + "\"references\":\"\",\"rfcId\":\"<123@some>\","
                    + "\"size\":40764,\"threadCount\":0,"
                    + "\"extraData\":\"123@yandex.ru\",\"newCount\":0,"
                    + "\"attachmentsCount\":0,\"attachmentsFullSize\":0,"
                    + "\"attachments\":[],"
                    + "\"labels\":[\"118\",\"48\",\"63\",\"65\","
                        + "\"FAKE_POSTMASTER_LBL\",\"FAKE_RECENT_LBL\","
                        + "\"FAKE_SPAM_LBL\"],"
                    + "\"specialLabels\":[],"
                    + "\"types\":[" + types + "],"
                    + "\"folder\":{\"name\":\"Spam\",\"isUser\":false,"
                        + "\"isSystem\":true,"
                        + "\"type\":{\"code\":3,\"title\":\"system\"},"
                        + "\"symbolicName\":{\"code\":4,\"title\":\""
                            + folderType + "\"},"
                        + "\"bytes\":9979339,\"messagesCount\":337,"
                        + "\"newMessagesCount\":257,"
                        + "\"recentMessagesCount\":337,"
                        + "\"unvisited\":false,"
                        + "\"folderOptions\":{\"getPosition\":0},"
                        + "\"position\":0,\"parentId\":\"0\","
                        + "\"pop3On\":\"0\",\"scn\":\"0\","
                        + "\"creationTime\":\"1247354537\","
                        + "\"subscribed\":\"1\",\"shared\":\"0\"},"
                    + "\"labelsInfo\":{\"118\":{\"name\":\"55\","
                        + "\"creationTime\":\"1476173815\",\"color\":\"\","
                        + "\"isUser\":false,\"isSystem\":false,\"type\":{"
                            + "\"code\":9,\"title\":\"so\"},"
                        + "\"symbolicName\":{\"code\":0,\"title\":\"\"},"
                        + "\"messagesCount\":139},"
                        + "\"48\":{\"name\":\"vtnrf0datingsite\","
                            + "\"creationTime\":\"1361380884\",\"color\":\"\","
                            + "\"isUser\":false,\"isSystem\":false,"
                            + "\"type\":{\"code\":2,\"title\":\"social\"},"
                            + "\"symbolicName\":{\"code\":0,\"title\":\"\"},"
                            + "\"messagesCount\":23},"
                        + "\"63\":{\"name\":\"13\","
                            + "\"creationTime\":\"1456441383\",\"color\":\"\","
                            + "\"isUser\":false,\"isSystem\":false,"
                            + "\"type\":{\"code\":9,\"title\":\"so\"},"
                            + "\"symbolicName\":{\"code\":0,\"title\":\"\"},"
                            + "\"messagesCount\":847},"
                        + "\"65\":{\"name\":\"15\","
                            + "\"creationTime\":\"1456441383\",\"color\":\"\","
                            + "\"isUser\":false,\"isSystem\":false,"
                            + "\"type\":{\"code\":9,\"title\":\"so\"},"
                            + "\"symbolicName\":{\"code\":0,\"title\":\"\"},"
                            + "\"messagesCount\":23},"
                        + "\"FAKE_POSTMASTER_LBL\":{"
                            + "\"name\":\"FAKE_POSTMASTER_LBL\","
                            + "\"creationTime\":\"\",\"color\":\"\","
                            + "\"isUser\":false,\"isSystem\":true,"
                            + "\"type\":{\"code\":3,\"title\":\"system\"},"
                            + "\"symbolicName\":{\"code\":25,"
                                + "\"title\":\"postmaster_label\"},"
                            + "\"messagesCount\":0},"
                        + "\"FAKE_RECENT_LBL\":{\"name\":\"FAKE_RECENT_LBL\","
                            + "\"creationTime\":\"\",\"color\":\"\","
                            + "\"isUser\":false,\"isSystem\":true,"
                            + "\"type\":{\"code\":3,\"title\":\"system\"},"
                            + "\"symbolicName\":{\"code\":13,"
                                + "\"title\":\"recent_label\"},"
                            + "\"messagesCount\":0},"
                        + "\"FAKE_SPAM_LBL\":{\"name\":\"FAKE_SPAM_LBL\","
                            + "\"creationTime\":\"\",\"color\":\"\","
                            + "\"isUser\":false,\"isSystem\":true,"
                            + "\"type\":{\"code\":3,\"title\":\"system\"},"
                            + "\"symbolicName\":{\"code\":11,"
                                + "\"title\":\"spam_label\"},"
                            + "\"messagesCount\":0}"
                        + "},\"specialLabelsInfo\":{}"
                    + "}]}";
    }
    //CSON: ParameterNumber

    public static void cokemulatorIexlibMock(
        final IexProxyCluster cluster,
        final String file)
        throws URISyntaxException
    {
        FileEntity entityFromCoke = new FileEntity(
            new File(IexProxyTestMocks.class.getResource(file).toURI()),
            ContentType.APPLICATION_JSON);
        cluster.cokemulatorIexlib().add(
            "/process?*",
            new StaticHttpResource(HttpStatus.SC_OK, entityFromCoke));
    }

    // CSOFF: ParameterNumber
    public static void filterSearchMock(
        final IexProxyCluster cluster,
        final String file,
        final long uid,
        final String... mids)
        throws URISyntaxException, BadRequestException
    {
        filterSearchMock(
            cluster,
            uid,
            new File(IexProxyTestMocks.class.getResource(file).toURI()),
            mids);
    }
    // CSON: ParameterNumber

    // CSOFF: ParameterNumber
    public static void filterSearchMock(
        final IexProxyCluster cluster,
        final List<String> files,
        final long uid,
        final String... mids)
        throws URISyntaxException, BadRequestException, IOException, JsonException
    {
        JsonMap body = null;
        for (String file : files) {
            JsonMap cbody = TypesafeValueContentHandler.parse(
                Files.readString(Paths.get(IexProxyTestMocks.class.getResource(file).toURI()))
            ).asMap();
            if (body == null) {
                body = cbody;
            } else {
                body.getList(ENVELOPES).addAll(cbody.getList(ENVELOPES));
            }
        }
        filterSearchMock(cluster, uid, JsonType.NORMAL.toString(body), mids);
    }
    // CSON: ParameterNumber

    public static void filterSearchMock(
        final IexProxyCluster cluster,
        final long uid,
        final String body,
        final String... mids)
    {
        String uri = filterSearchUri(uid, mids);
        if (BlackboxUserinfo.corp(uid)) {
            cluster.corpFilterSearch().add(uri, body, ContentType.APPLICATION_JSON);
        } else {
            cluster.filterSearch().add(uri, body, ContentType.APPLICATION_JSON);
        }
    }

    public static void filterSearchMock(
        final IexProxyCluster cluster,
        final long uid,
        final String body,
        final Collection<String> mids)
    {
        String uri = filterSearchUri(uid, mids);
        if (BlackboxUserinfo.corp(uid)) {
            cluster.corpFilterSearch().add(uri, body, ContentType.APPLICATION_JSON);
        } else {
            cluster.filterSearch().add(uri, body, ContentType.APPLICATION_JSON);
        }
    }

    public static void filterSearchMock(
        final IexProxyCluster cluster,
        final long uid,
        final File file,
        final String... mids)
    {
        String uri = filterSearchUri(uid, mids);
        if (BlackboxUserinfo.corp(uid)) {
            cluster.corpFilterSearch().add(uri, file, ContentType.APPLICATION_JSON);
        } else {
            cluster.filterSearch().add(uri, file, ContentType.APPLICATION_JSON);
        }
    }
}
