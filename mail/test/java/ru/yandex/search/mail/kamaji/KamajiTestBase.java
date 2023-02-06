package ru.yandex.search.mail.kamaji;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.protocol.HttpCoreContext;

import ru.yandex.blackbox.BlackboxAddress;
import ru.yandex.blackbox.BlackboxDbfield;
import ru.yandex.blackbox.BlackboxEmailsType;
import ru.yandex.blackbox.BlackboxUserIdType;
import ru.yandex.blackbox.BlackboxUserinfo;
import ru.yandex.blackbox.BlackboxUserinfoRequest;
import ru.yandex.dbfields.FilterSearchFields;
import ru.yandex.http.proxy.BasicProxySession;
import ru.yandex.http.proxy.HttpProxy;
import ru.yandex.http.test.MockHttpExchange;
import ru.yandex.http.test.MockServerConnection;
import ru.yandex.json.dom.BasicContainerFactory;
import ru.yandex.json.dom.JsonBoolean;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonLong;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.JsonString;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.dom.ValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.xpath.ValueUtils;
import ru.yandex.logger.DevNullLogger;
import ru.yandex.parser.email.types.MessageType;
import ru.yandex.search.document.mail.FirstlineMailMetaInfo;
import ru.yandex.search.document.mail.JsonFirstlineMailMetaHandler;
import ru.yandex.search.document.mail.MailMetaInfo;
import ru.yandex.search.mail.kamaji.senders.StoreSendersIndexerModule;
import ru.yandex.test.util.TestBase;

public class KamajiTestBase extends TestBase {
    protected static final long DEFAULT_SUID = 90000L;
    // CSOFF: MultipleStringLiterals
    protected static final String FILTER_SEARCH =
        "/filter_search?order=default&full_folders_and_labels=1&uid=";
    protected static final String STORE_ENVELOPE = "store.json";
    protected static final String ADD_IF_NOT_EXISTS = "AddIfNotExists";
    protected static final String PRESERVE_FIELDS = "PreserveFields";
    protected static final String QUEUE_ID_P = "&zoo-queue-id=";
    protected static final String CHANGE_LOG_QUEUE = "change_log";
    protected static final String HTTP_LOCALHOST = "http://localhost:";
    protected static final String NOTIFY = "/notify?mdb=pg";
    protected static final String DELETE = "/delete?prefix=";
    protected static final String MODIFY = "/update?uid=";
    protected static final String UID_PARAM = "&uid=";
    protected static final String PREFIX = "prefix";
    protected static final String MID = "&mid=";
    protected static final String DOCS = "docs";
    protected static final String EMPTY_ENVELOPES = "{\"envelopes\":[]}";
    protected static final String JSON_END = "}]}";

    protected static String blackboxUri(final String filter) {
        return "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
            + "&dbfields=hosts.db_id.2,subscription.suid.2&emails=getall&sid=2"
            + filter;
    }

    protected static String blackboxResponse(
        final long uid,
        final String... addrs)
    {
        return blackboxResponse(uid, DEFAULT_SUID, addrs);
    }

    protected static String blackboxResponse(
        final long uid,
        final long suid,
        final String... addrs)
    {
        StringBuilder sb = new StringBuilder("{\"users\":[{\"id\":\"");
        sb.append(uid);
        sb.append("\",\"uid\":{\"value\":\"");
        sb.append(uid);
        sb.append("\",\"lite\":false,\"hosted\":false},\"login\":\"vp");
        sb.append("\",\"have_password\":true,\"have_hint\":true,\"karma\":{");
        sb.append("\"value\":0},\"karma_status\":{\"value\":6000},");
        sb.append("\"address-list\":[");
        for (int i = 0; i < addrs.length; ++i) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append("{\"address\":\"");
            sb.append(addrs[i]);
            sb.append("\",\"validated\":true,\"default\":true,");
            sb.append("\"rpop\":false,\"unsafe\":false,\"native\":true,");
            sb.append("\"born-date\":\"2003-09-04 21:34:25\"}");
        }
        sb.append("],\"dbfields\":{\"subscription.suid.2\":\"");
        if (suid != 0L) {
            sb.append(suid);
        }
        sb.append("\",\"hosts.db_id.2\":\"pg\"}}]}");
        return new String(sb);
    }

    protected List<BlackboxAddress> blackboxAddress(final String addr)
        throws Exception
    {
        JsonMap address = new JsonMap(BasicContainerFactory.INSTANCE);
        address.put("address", new JsonString(addr));
        address.put("born-date", new JsonString("2014-12-10 23:20:22"));
        address.put("default", JsonBoolean.TRUE);
        address.put("native", JsonBoolean.TRUE);
        address.put("prohibit-restore", JsonBoolean.TRUE);
        address.put("rpop", JsonBoolean.TRUE);
        address.put("unsafe", JsonBoolean.TRUE);
        address.put("validated", JsonBoolean.TRUE);
        return Collections.singletonList(new BlackboxAddress(address));
    }

    protected Map<String, Object> docComplete(final KamajiConfig config) {
        return docComplete(config, Collections.emptyList());
    }

    protected Map<String, Object> docComplete(
        final KamajiConfig config,
        final List<String> additionalPreserveFields)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("AddIfNotExists", true);
        List<String> preserveFields = new ArrayList<>(config.preserveFields());
        preserveFields.addAll(additionalPreserveFields);
        result.put("PreserveFields", preserveFields);
        return result;
    }

    protected static class FilterSearchResponseBuilder {
        private String mid;
        private String threadId;
        private String stid;
        private String fid;
        private String lcn;
        private JsonObject folder;
        private JsonList types;
        private String receivedDate;
        private JsonList from;
        private JsonList to;

        public FilterSearchResponseBuilder() {
        }

        public FilterSearchResponseBuilder mid(final String mid) {
            FilterSearchResponseBuilder fsrb = copy();
            fsrb.mid = mid;
            if (fsrb.threadId == null) {
                fsrb.threadId = mid;
            }

            return fsrb;
        }

        public String mid() {
            return mid;
        }

        public FilterSearchResponseBuilder lcn(final String lcn) {
            FilterSearchResponseBuilder fsrb = copy();
            fsrb.lcn = lcn;
            return fsrb;
        }

        public String lcn() {
            return lcn;
        }

        public FilterSearchResponseBuilder threadId(final String threadId) {
            FilterSearchResponseBuilder fsrb = copy();
            fsrb.threadId = threadId;
            return fsrb;
        }

        public FilterSearchResponseBuilder stid(final String stid) {
            FilterSearchResponseBuilder fsrb = copy();
            fsrb.stid = stid;
            return fsrb;
        }

        public FilterSearchResponseBuilder fid(final String fid) {
            FilterSearchResponseBuilder fsrb = copy();
            fsrb.fid = fid;
            return fsrb;
        }

        public FilterSearchResponseBuilder folder(final JsonObject folder) {
            FilterSearchResponseBuilder fsrb = copy();
            fsrb.folder = folder;
            return fsrb;
        }

        public FilterSearchResponseBuilder folder(
            final String fid,
            final String type,
            final String name)
        {
            JsonMap folder = new JsonMap(BasicContainerFactory.INSTANCE);
            folder.put(FilterSearchFields.NAME, new JsonString(name));
            JsonMap typeMap = new JsonMap(BasicContainerFactory.INSTANCE);
            typeMap.put(FilterSearchFields.TITLE, new JsonString(type));
            folder.put(FilterSearchFields.TYPE, typeMap);
            FilterSearchResponseBuilder fd = folder(folder);
            fd.fid = fid;
            JsonMap symbolicName = new JsonMap(BasicContainerFactory.INSTANCE);
            symbolicName.put(
                FilterSearchFields.TITLE,
                new JsonString(name.toLowerCase(Locale.ROOT)));
            folder.put(FilterSearchFields.SYMBOLIC_NAME, symbolicName);

            return fd;
        }

        public FilterSearchResponseBuilder types(final JsonList types) {
            FilterSearchResponseBuilder fsrb = copy();
            fsrb.types = types;
            return fsrb;
        }

        public FilterSearchResponseBuilder types(final MessageType... types) {
            JsonList list = new JsonList(BasicContainerFactory.INSTANCE);
            for (MessageType type: types) {
                list.add(new JsonLong(type.typeNumber()));
            }

            return types(list);
        }

        public FilterSearchResponseBuilder receivedDate(
            final String receivedDate)
        {
            FilterSearchResponseBuilder fsrb = copy();
            fsrb.receivedDate = receivedDate;
            return fsrb;
        }

        public FilterSearchResponseBuilder from(final JsonList from) {
            FilterSearchResponseBuilder fsrb = copy();
            fsrb.from = from;
            return fsrb;
        }

        public FilterSearchResponseBuilder from(
            final String local,
            final String displayName,
            final String domain)
        {
            FilterSearchResponseBuilder fsrb = copy();
            fsrb.from =
                new JsonList(BasicContainerFactory.INSTANCE);
            fsrb.from.add(buildAddr(local, displayName, domain));
            return fsrb;
        }

        protected JsonMap buildAddr(
            final String local,
            final String displayName,
            final String domain)
        {
            JsonMap map = new JsonMap(BasicContainerFactory.INSTANCE);
            map.put(FilterSearchFields.ADDR_LOCAL, new JsonString(local));
            map.put(FilterSearchFields.ADDR_DOMAIN, new JsonString(domain));
            map.put(
                FilterSearchFields.ADDR_DISPLAY_NAME,
                new JsonString(displayName));
            return map;
        }

        public FilterSearchResponseBuilder setTo(
            final String local,
            final String displayName,
            final String domain)
        {
            FilterSearchResponseBuilder fsrb = copy();
            fsrb.to = new JsonList(BasicContainerFactory.INSTANCE);
            fsrb.to.add(buildAddr(local, displayName, domain));
            return fsrb;
        }

        public FilterSearchResponseBuilder addTo(
            final String local,
            final String displayName,
            final String domain)
        {
            FilterSearchResponseBuilder fsrb = copy();
            fsrb.to.add(buildAddr(local, displayName, domain));
            return fsrb;
        }

        protected JsonObject longOrString(final String value) {
            try {
                return new JsonLong(Long.parseLong(value));
            } catch (NumberFormatException nfe) {
                return new JsonString(value);
            }
        }

        protected FilterSearchResponseBuilder copy() {
            FilterSearchResponseBuilder rb = new FilterSearchResponseBuilder();
            try {
                JsonObject fsrRaw =
                    TypesafeValueContentHandler.parse(
                        JsonType.HUMAN_READABLE.toString(
                            this.toJsonMap()));
                JsonMap fsr = fsrRaw.asMap();
                rb.mid = fsr.getString("mid", null);
                rb.to = fsr.getListOrNull("to");
                rb.stid = fsr.getString("stid", null);
                rb.from = fsr.getListOrNull("from");
                rb.fid = fsr.getString("fid", null);
                rb.folder = fsr.getMapOrNull("folder");
                rb.receivedDate = fsr.getString("receiveDate", null);
                rb.threadId = fsr.getString("threadId", null);
                rb.types = fsr.getListOrNull("types");
                rb.lcn = fsr.getString("revision", null);
            } catch (JsonException je) {
                return null;
            }

            return rb;
        }

        public JsonMap toJsonMap() {
            JsonMap o = new JsonMap(BasicContainerFactory.INSTANCE);
            if (stid != null) {
                o.put("stid", new JsonString(stid));
            }

            if (fid != null) {
                o.put("fid", longOrString(fid));
            }

            if (folder != null) {
                o.put("folder", folder);
            }

            if (from != null) {
                o.put("from", from);
            }

            if (to != null) {
                o.put("to", to);
            }

            if (receivedDate != null) {
                o.put("receiveDate", longOrString(receivedDate));
            }

            if (mid != null) {
                o.put("mid", longOrString(mid));
            }

            if (threadId != null) {
                o.put("threadId", longOrString(threadId));
            }

            if (types != null) {
                o.put("types", types);
            }

            if (lcn != null) {
                o.put("revision", longOrString(lcn));
            }

            return o;
        }

        @Override
        public String toString() {
            JsonMap map = new JsonMap(BasicContainerFactory.INSTANCE);
            JsonList envelopes = new JsonList(BasicContainerFactory.INSTANCE);
            envelopes.add(toJsonMap());
            map.put("envelopes", envelopes);
            return JsonType.HUMAN_READABLE.toString(map);
        }
    }

    protected static BlackboxUserinfo blackboxUserinfo(
        final long uid,
        final String... addr)
        throws Exception
    {
        JsonMap map =
            TypesafeValueContentHandler.parse(
                blackboxResponse(uid, addr)).asMap();
        JsonMap user = map.getList("users").get(0).asMap();

        return new BlackboxUserinfo(
            new BlackboxUserinfoRequest(BlackboxUserIdType.UID, uid)
                .emailsType(BlackboxEmailsType.GETALL)
                .requiredDbfields(BlackboxDbfield.SUID, BlackboxDbfield.MDB),
            user);
    }

    //CSOFF: ParameterNumber
    protected static List<Map<String, Object>> sendersDocs(
        final KamajiCluster cluster,
        final String uri,
        final String data,
        final long uid,
        final String lcn,
        final String fsData,
        final String... addr)
        throws Exception
    {
        JsonMap envelope =
            TypesafeValueContentHandler.parse(fsData)
                .asMap()
                .getList("envelopes").get(0).asMap();

        FirstlineMailMetaInfo meta = new FirstlineMailMetaInfo();
        new JsonFirstlineMailMetaHandler(meta).handle(envelope);
        meta.set(MailMetaInfo.UID, String.valueOf(uid));
        meta.set(MailMetaInfo.LCN, lcn);
        return StoreSendersIndexerModule.INSTANCE.indexDocuments(
            createFakeContext(
                cluster,
                meta,
                uri,
                data,
                uid,
                addr),
            Collections.emptyList());
    }

    protected static KamajiIndexationContext createFakeContext(
        final KamajiCluster cluster,
        final MailMetaInfo meta,
        final String uri,
        final String data,
        final long uid,
        final String... addr)
        throws Exception
    {
        HttpCoreContext coreContext = HttpCoreContext.create();
        coreContext.setAttribute(
            "http.connection",
            new MockServerConnection());
        coreContext.setAttribute(HttpProxy.LOGGER, DevNullLogger.INSTANCE);
        return new KamajiIndexationContext(
            new ChangeContext(
                cluster.kamaji(),
                new BasicProxySession(
                    cluster.kamaji(),
                    new MockHttpExchange(uri),
                    coreContext),
                ValueUtils.asMap(
                    ValueContentHandler.parse(data))),
            null,
            meta,
            blackboxUserinfo(uid, addr),
            null);
    }
    // CSON: ParameterNumber
    // CSON: MultipleStringLiterals
}
