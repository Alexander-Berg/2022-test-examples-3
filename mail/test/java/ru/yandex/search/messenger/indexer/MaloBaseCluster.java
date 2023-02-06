package ru.yandex.search.messenger.indexer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import NMessengerProtocol.Client;
import NMessengerProtocol.Message;
import com.google.protobuf.CodedOutputStream;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.cityhash.CityHashingArrayOutputStream;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.NotImplementedException;
import ru.yandex.json.dom.BasicContainerFactory;
import ru.yandex.json.dom.JsonBoolean;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonLong;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.JsonString;
import ru.yandex.json.dom.PositionSavingContainerFactory;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.search.messenger.indexer.user.settings.SearchBucketSubType;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;

public abstract class MaloBaseCluster implements GenericAutoCloseable<IOException> {
    private static final String CHAT_TOPIC = "rt3.messenger--mssngr--prod-search-chats";
    private static final String USERS_TOPIC = "rt3.messenger--mssngr--prod-search-users";
    private static final String CONTACTS_TOPIC = "rt3.messenger--mssngr--prod-search-contacts";
    private static final String SEARCH_BUCKET_TOPIC = "rt3.messenger--mssngr--prod-search-buckets";
    private static final String MESSAGES_TOPIC = "rt3.messenger--mssngr--test-search-messages";
    private static final String CHAT_MEMBERS_TOPIC
        = "rt3.messenger--mssngr--prod-search-chat-members";

    private static final String CHATS_CONFIG =
        Paths.getSourcePath(
            "mail/search/messenger/chats_backend/files/chats_backend.conf");
    private static final String MESSAGES_CONFIG =
        Paths.getSourcePath(
            "mail/search/messenger/messages_backend/files/messages_backend.conf");

    protected final boolean autoFlush;
    protected final StaticServer router;
    protected final StaticServer metaApi;
    protected final StaticServer rca;
    protected final StaticServer producer;
    protected final TestSearchBackend chatsBackend;
    protected final TestSearchBackend messagesBackend;
    protected final GenericAutoCloseableChain<IOException> chain;

    public MaloBaseCluster(final TestBase base) throws Exception {
        this(base, true);
    }

    public MaloBaseCluster(final TestBase base, final boolean autoFlush) throws Exception {
        this.autoFlush = autoFlush;
        System.setProperty("TVM_API_HOST", "");
        System.setProperty("TVM_CLIENT_ID", "");
        System.setProperty("TVM_ALLOWED_SRCS", "");
        System.setProperty("SECRET", "");
        System.setProperty("SERVER_NAME", "");
        System.setProperty("JKS_PASSWORD", "");
        System.setProperty("INDEX_PATH", "");
        System.setProperty("INDEX_DIR", "");
        System.setProperty("MAIL_SEARCH_TVM_ID", "0");
        System.setProperty("YT_ACCESS_LOG", "");
        System.setProperty("SERVICE_CONFIG", "null.conf");
        System.setProperty("BSCONFIG_IPORT", "0");
        System.setProperty("BSCONFIG_IDIR", ".");
        System.setProperty("OLD_SEARCH_PORT", "0");
        System.setProperty("SEARCH_PORT", "0");
        System.setProperty("INDEX_PORT", "0");
        System.setProperty("DUMP_PORT", "0");
        System.setProperty("CONSUMER_PORT", "0");
        System.setProperty("CMNT_API", "empty-mail-producer");
        System.setProperty("MOXY_HOST", "localhost");
        System.setProperty("MESSENGER_ROUTER_TVM_CLIENT_ID", "");
        System.setProperty("MESSENGER_ROUTER_HOST", "localhost");
        System.setProperty("MAIL_PRODUCER", "empty-mail-producer");
        System.setProperty("CHATS_SERVICE", "messenger_chats");
        System.setProperty("USERS_SERVICE", "messenger_users");
        System.setProperty("MESSAGES_SERVICE", "messenger_messages");
        System.setProperty("TASKS_SERVICE", "tasks_service");
        System.setProperty("META_API_HOST", "");
        System.setProperty("META_API_TVM_CLIENT_ID", "");
        System.setProperty("TVM_CLIENT_ID", "");
        System.setProperty("CPU_CORES", "2");
        System.setProperty("SEARCH_THREADS", "2");
        System.setProperty("MERGE_THREADS", "2");
        System.setProperty("LIMIT_SEARCH_REQUESTS", "10");
        System.setProperty("LIMIT_FORWARD_REQUESTS", "10");
        System.setProperty("INDEX_THREADS", "2");
        System.setProperty("MOXY_TVM_CONF", "moxy-notvm.conf");
        System.setProperty("HOSTNAME", "localhost");
        System.setProperty("NGINX_SSL_PORT", "0");
        System.setProperty("FULL_LOG_LEVEL", "all");
        System.setProperty("BLOCK_CASHE_SIZE", "200M");
        System.setProperty("COMPRESSED_CASHE_SIZE", "200M");
        System.setProperty("LIMIT_RECENT_REQUESTS", "5");

        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            Path tmpDir1 = Files.createTempDirectory(base.testName.getMethodName());
            Path tmpDir2 = Files.createTempDirectory(base.testName.getMethodName());
            IniConfig chatsConfig =
                TestSearchBackend.patchConfig(
                    tmpDir1,
                    true,
                    new IniConfig(new File(CHATS_CONFIG)));
            chatsConfig.put("shards", "1");
            IniConfig mesConfig =
                TestSearchBackend.patchConfig(
                    tmpDir2,
                    true,
                    new IniConfig(new File(MESSAGES_CONFIG)));

            chatsBackend = new TestSearchBackend(tmpDir1, true, chatsConfig);
            messagesBackend = new TestSearchBackend(tmpDir2, true, mesConfig);

            chain.get().add(chatsBackend);
            chain.get().add(messagesBackend);

            producer = new StaticServer(Configs.baseConfig("ProducerStaticServer"));
            chain.get().add(producer);
            producer.start();

            metaApi = new StaticServer(Configs.baseConfig("MetaApi"));
            chain.get().add(metaApi);
            metaApi.start();
            addChatOrgsResolve();

            router = new StaticServer(Configs.baseConfig("Router"));
            chain.get().add(router);
            router.start();

            rca = new StaticServer(Configs.baseConfig("Rca"));
            chain.get().add(rca);
            rca.start();

            this.chain = chain.release();
        }
    }

    public void addChatOrgsResolve() {
        metaApi.add("/meta_api/", "{\"status\":\"ok\", \"data\":{}}");
    }
    @Override
    public void close() throws IOException {
        chain.close();
    }

    public abstract Malo malo();

    public StaticServer router() {
        return router;
    }

    public StaticServer rca() {
        return rca;
    }

    public StaticServer metaApi() {
        return metaApi;
    }

    public TestSearchBackend chatsBackend() {
        return chatsBackend;
    }

    public TestSearchBackend messagesBackend() {
        return messagesBackend;
    }

    public void chatMembers(
        final String chatId,
        final Collection<String> added,
        final Collection<String> removed,
        final long newVersion,
        final long prevVersion)
        throws Exception
    {
        JsonMap data = new JsonMap(BasicContainerFactory.INSTANCE);
        data.put("version", new JsonLong(newVersion));
        JsonList removedJL = new JsonList(BasicContainerFactory.INSTANCE);
        removed.stream().map(JsonString::new).forEach(removedJL::add);
        JsonList addedJL = new JsonList(BasicContainerFactory.INSTANCE);
        added.stream().map(JsonString::new).forEach(addedJL::add);

        data.put("removed", removedJL);
        data.put("added", addedJL);

        JsonMap result = new JsonMap(BasicContainerFactory.INSTANCE);
        result.put("data", data);
        result.put("status", new JsonString("ok"));

        metaApi().add(
            "/meta_api/",
            new ExpectingHttpItem(
                new StringChecker("request={\"method\":\"get_chat_members_diff\"," +
                    "\"params\":{\"chat_id\":\"" + chatId + "\"," +
                    "\"version\":" + prevVersion + "}}"),
                JsonType.NORMAL.toString(result)));
        HttpPost chatPost = new HttpPost(
            malo().host()
                + "/?&partition=20&offset=1&seqNo=1&topic=" + CHAT_MEMBERS_TOPIC);
        chatPost.setEntity(
            new NByteArrayEntity(
                MaloTest.topicDoc(
                    chatId,
                    "chat_members",
                    1),
                ContentType.DEFAULT_BINARY));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, chatPost);
    }

    public JsonMap pvpChatData(
        final String member1,
        final String member2,
        final long version)
        throws Exception
    {
        JsonMap data = TypesafeValueContentHandler.parse(
            new InputStreamReader(
                MaloTest.class.getResourceAsStream(
                    "default_private_chat_data.json"))).asMap();
        String chatId = member1 + '_' + member2;
        data.put("chat_id", new JsonString(chatId));
        data.put("version", new JsonLong(version));
        data.put("member1", new JsonString(member1));
        data.put("member2", new JsonString(member2));
        return data;
    }

    public JsonMap updatePvpChat(
        final String member1,
        final String member2,
        final long version)
        throws Exception
    {
        JsonMap data = pvpChatData(member1, member2, version);
        updateChat(data.getString("chat_id"), data);
        return data;
    }

    public JsonMap updateChat(final String chatId) throws Exception {
        return updateChat(chatId, 1L);
    }

    public JsonMap updateChat(final String chatId, final long version) throws Exception {
        JsonMap data = groupChatData(chatId, version);
        updateChat(chatId, data);
        return data;
    }

    public JsonMap groupChatData(final String chatId, final long version) throws Exception {
        JsonMap data = TypesafeValueContentHandler.parse(
            new InputStreamReader(
                MaloTest.class.getResourceAsStream(
                    "default_chat_data.json"))).asMap();
        data.put("chat_id", new JsonString(chatId));
        data.put("version", new JsonLong(version));
        return data;
    }

    public void updateChat(final String chatId, final JsonMap data) throws Exception {
        metaApi().add(
            "/meta_api/",
            new ExpectingHttpItem(
                new StringChecker("request={\"method\":\"get_chat\"," +
                    "\"params\":{\"chat_id\":\"" + chatId + "\"," +
                    "\"disable_members\":true}}"),
                "{\"status\":\"ok\",\"data\":" + JsonType.NORMAL.toString(data) + "}"));
        HttpPost chatPost = new HttpPost(
            malo().host()
                + "/?&partition=20&offset=1&seqNo=1&topic=" + CHAT_TOPIC);
        chatPost.setEntity(
            new NByteArrayEntity(
                MaloTest.topicDoc(
                    chatId,
                    "chat",
                    1),
                ContentType.DEFAULT_BINARY));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, chatPost);
        if (autoFlush) {
            chatsBackend().flush();
        }
    }

    public void hiddenChats(
        final String guid,
        final Map<String, Long> chats)
        throws Exception
    {
        hiddenChats(guid, chats, 1L);
    }

    public void hiddenChats(
        final String guid,
        final Map<String, Long> chats,
        final long version)
        throws Exception
    {
        JsonMap map = new JsonMap(PositionSavingContainerFactory.INSTANCE);
        for (Map.Entry<String, Long> entry: chats.entrySet()) {
            map.put(entry.getKey(), new JsonLong(entry.getValue()));
        }

        searchBucket(guid, SearchBucketSubType.HIDDEN_PRIVATE_CHATS.bucketName(), map, version);
    }

    public void setUserPrivacy(
        final String guid,
        final int search)
        throws Exception
    {
        setUserPrivacy(guid, search, 1L);
    }

    public void setUserPrivacy(
        final String guid,
        final int search,
        final long version)
        throws Exception
    {
        JsonMap map = new JsonMap(BasicContainerFactory.INSTANCE);
        map.put("search", new JsonLong(search));
        map.put("online_status", new JsonLong(0));
        map.put("private_chats", new JsonLong(0));
        map.put("calls", new JsonLong(0));
        map.put("invites", new JsonLong(0));
        searchBucket(guid, SearchBucketSubType.PRIVACY.bucketName(), map, version);
    }

    public void searchBucket(
        final String guid,
        final String bucketName,
        final JsonMap bucketValue,
        final long version)
        throws Exception
    {
        metaApi().add(
            "/meta_api/",
            new ExpectingHttpItem(
                new StringChecker("request={\"method\":\"get_bucket\"," +
                    "\"params\":{\"guid\":\"" + guid + "\"," +
                    "\"bucket_name\":\"" + bucketName + "\"}}"),
                "{\n" +
                    "  \"data\": {\n" +
                    "    \"bucket\": {\n" +
                    "      \"version\": " + version + ",\n" +
                    "      \"bucket_value\": " + JsonType.HUMAN_READABLE.toString(bucketValue)
                    + ",\n" +
                    "      \"bucket_name\": \"hidden_private_chats\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"status\": \"ok\"\n" +
                    "}"));
        HttpPost sbPost = new HttpPost(
            malo().host()
                + "/?&partition=20&offset=1&seqNo=1&topic=" + SEARCH_BUCKET_TOPIC);
        sbPost.setEntity(
            new NByteArrayEntity(
                MaloTest.topicDoc(
                    guid,
                    bucketName,
                    1),
                ContentType.DEFAULT_BINARY));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, sbPost);
        if (autoFlush) {
            chatsBackend().flush();
        }
    }

    public void lastSeen(final Map<String, Long> lastSeen) throws Exception {
        lastSeen(lastSeen, 1);
    }

    public void lastSeen(
        final Map<String, Long> lastSeen,
        final int requestsCount)
        throws Exception
    {
        HttpRequestHandler handler = new HttpRequestHandler() {
            @Override
            public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context)
                throws HttpException, IOException
            {
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntity entity =
                        ((HttpEntityEnclosingRequest) request).getEntity();
                    InputStream stream =
                        entity.getContent();

                    long skipped =
                        stream.skip(CityHashingArrayOutputStream.THEADER_SIZE);
                    if (skipped != CityHashingArrayOutputStream.THEADER_SIZE) {
                        throw new HttpException("Failed to skip header");
                    }

                    Message.TLastSeenRequest lastSeenRequest =
                        Message.TLastSeenRequest.parseFrom(stream);

                    Set<String> requestedGuids =
                        new LinkedHashSet<>(lastSeenRequest.getGuidsList());
                    //Assert.assertEquals(guids, requestedGuids);

                    Message.TLastSeenResponse.Builder builder =
                        Message.TLastSeenResponse.newBuilder();
                    for (String reqGuid: requestedGuids) {
                        Long ts = lastSeen.get(reqGuid);
                        if (ts != null) {
                            Client.TLastSeenInfo info =
                                Client.TLastSeenInfo.newBuilder().setUser(
                                    Client.TUserInfo.newBuilder().setGuid(reqGuid).build()).setTimestamp(ts).build();
                            builder.addLastSeenInfos(info);
                        }
                    }

                    Message.TLastSeenResponse lsResponse = builder.build();
                    CityHashingArrayOutputStream out = new CityHashingArrayOutputStream();
                    out.reset();
                    CodedOutputStream googleOut = CodedOutputStream.newInstance(out);
                    lsResponse.writeTo(googleOut);
                    googleOut.flush();

                    final byte[] responseData = out.toByteArrayWithVersion(2);

                    response.setEntity(new ByteArrayEntity(responseData));
                } else {
                    throw new NotImplementedException(
                        "Entity enclosing request expected");
                }
            }
        };
        HttpRequestHandler[] handlers = new HttpRequestHandler[requestsCount];
        Arrays.fill(handlers, handler);
        router().add("/last_seen", handlers);
    }

    public JsonMap addUser(
        final String guid,
        final String displayName) throws Exception
    {
        return addUser(guid, displayName, null, 0L);
    }

    public JsonMap addUser(
        final String guid,
        final String displayName,
        final String nickname,
        final long version) throws Exception
    {
        return addUser(guid, displayName, nickname, false, false, version);
    }

    public static JsonMap defaultUser(
        final String guid,
        final String displayName,
        final long version)
        throws Exception
    {
        JsonMap map = new JsonMap(BasicContainerFactory.INSTANCE);
        map.put("passport_display_name", new JsonString(displayName));
        map.put("display_name", new JsonString(displayName));
        map.put("guid", new JsonString(guid));
        map.put("version", new JsonLong(version));
        map.put("org_id", new JsonLong(0));
        map.put("is_robot", JsonBoolean.FALSE);
        map.put("is_display_restricted", JsonBoolean.FALSE);
        return map;
    }

    public JsonMap addUser(
        final String guid,
        final String displayName,
        final String nickname,
        final boolean robot,
        final boolean displayRestricted,
        final long version) throws Exception
    {
        JsonMap map = defaultUser(guid, displayName, version);
        if (nickname != null) {
            map.put("nickname", new JsonString(nickname));
        }
        map.put("is_robot", JsonBoolean.valueOf(robot));
        map.put(
            "is_display_restricted",
            JsonBoolean.valueOf(displayRestricted));

        addUser(guid, map);
        return map;
    }

    public void addUser(
        final String guid,
        final JsonMap user)
        throws Exception
    {
        metaApi().add(
            "/meta_api/",
            new ExpectingHttpItem(
                new StringChecker("request={\"method\":\"get_user\"," +
                    "\"params\":{\"guid\":\"" + guid + "\"}}"),
                "{\"status\":\"ok\",\"data\":" + JsonType.NORMAL.toString(user) + "}"));
        HttpPost userPost = new HttpPost(
            malo().host()
                + "/?&partition=20&offset=1&seqNo=1&topic=" + USERS_TOPIC);
        userPost.setEntity(
            new NByteArrayEntity(
                MaloTest.topicDoc(
                    guid,
                    "users",
                    1),
                ContentType.DEFAULT_BINARY));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, userPost);
        if (autoFlush) {
            chatsBackend().flush();
        }
    }

    public void addContact(
        final String owner,
        final String contactId,
        final String contactName,
        final long version,
        final long prevVersion)
        throws Exception
    {
        updateContact(
            owner,
            contactId,
            contactName,
            false,
            version,
            prevVersion);
    }

    public void addContact(
        final String owner,
        final String contactId,
        final String contactName,
        final long version)
        throws Exception
    {
        updateContact(
            owner,
            contactId,
            contactName,
            false,
            version,
            0L);
    }

    public void updateContact(
        final String owner,
        final String contactId,
        final String contactName,
        final boolean deleted,
        final long version,
        final long prevVersion)
        throws Exception
    {
        JsonList list = new JsonList(BasicContainerFactory.INSTANCE);
        JsonMap contact = new JsonMap(BasicContainerFactory.INSTANCE);
        contact.put("contact_name", new JsonString(contactName));
        contact.put("guid", new JsonString(contactId));
        contact.put("version", new JsonLong(version));
        contact.put("deleted", JsonBoolean.valueOf(deleted));
        list.add(contact);
        updateContacts(owner, list, prevVersion);
    }

    public void updateContacts(
        final String owner,
        final List<? extends JsonObject> contacts,
        final long prevVersion)
        throws Exception
    {
        metaApi().add(
            "/meta_api/",
            new ExpectingHttpItem(
                new StringChecker("request={\"method\":\"get_contacts\"," +
                    "\"params\":{\"guid\":\"" + owner + "\"," +
                    "\"version\":" + prevVersion +  "}}"),
                "{\"status\":\"ok\",\"data\":" + JsonType.NORMAL.toString(contacts) + "}"));
        HttpPost contactsPost = new HttpPost(
            malo().host()
                + "/?&partition=20&offset=1&seqNo=1&topic=" + CONTACTS_TOPIC);
        contactsPost.setEntity(
            new NByteArrayEntity(
                MaloTest.topicDoc(
                    owner,
                    "contacts",
                    1),
                ContentType.DEFAULT_BINARY));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, contactsPost);
        if (autoFlush) {
            messagesBackend().flush();
        }
    }

    public void addMessage(
        final String chatId,
        final long ts,
        final String fromGuid)
        throws Exception
    {
        addTextMessage(chatId, ts, fromGuid, "");
    }

    public void addMessage(
        final Message.TMessageInfoResponse messageResp,
        final String chatId,
        final long ts)
        throws Exception
    {
        HttpPost messagePost = new HttpPost(
            malo().host()
                + "/?&partition=20&offset=1&seqNo=1&topic=" + MESSAGES_TOPIC);
        messagePost.setEntity(
            new NByteArrayEntity(
                Message.TOutMessageRef.newBuilder()
                    .setChatId(chatId)
                    .setTimestamp(ts)
                    .build()
                    .toByteArray(),
                ContentType.DEFAULT_BINARY));

        history(chatId, Message.TChatCounters.newBuilder().build());

        router().add("/message_info", new HttpRequestHandler() {
            @Override
            public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context)
                throws HttpException, IOException
            {
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntity entity =
                        ((HttpEntityEnclosingRequest) request).getEntity();
                    InputStream stream =
                        entity.getContent();

                    long skipped =
                        stream.skip(CityHashingArrayOutputStream.THEADER_SIZE);
                    if (skipped != CityHashingArrayOutputStream.THEADER_SIZE) {
                        throw new HttpException("Failed to skip header");
                    }
                    Message.TMessageInfoRequest infoRequest =
                        Message.TMessageInfoRequest.parseFrom(stream);
                    System.out.println("Message request "
                        + new JsonFormat().printToString(infoRequest));

                    CityHashingArrayOutputStream out = new CityHashingArrayOutputStream();
                    out.reset();
                    CodedOutputStream googleOut = CodedOutputStream.newInstance(out);
                    messageResp.writeTo(googleOut);
                    googleOut.flush();

                    final byte[] responseData = out.toByteArrayWithVersion(2);

                    response.setEntity(new ByteArrayEntity(responseData));
                } else {
                    throw new NotImplementedException(
                        "Entity enclosing request expected");
                }
            }
        });

        HttpAssert.assertStatusCode(HttpStatus.SC_OK, messagePost);

        if (autoFlush) {
            messagesBackend().flush();
            chatsBackend().flush();
        }
    }

    public void addTextMessage(
        final String chatId,
        final long ts,
        final String fromGuid,
        final String text)
        throws Exception
    {
        Message.TMessageInfoResponse.Builder builder =
            Message.TMessageInfoResponse.newBuilder();
        new JsonFormat().merge(
            MaloTest.class.getResourceAsStream(
                "default_text_message.json"),
            builder);

        Client.TServerMessage.Builder smb =
            builder.getMessageBuilder().getServerMessageBuilder();
        smb.getClientMessageBuilder().getPlainBuilder().setChatId(chatId);
        smb.getServerMessageInfoBuilder().getFromBuilder().setGuid(fromGuid);
        smb.getServerMessageInfoBuilder().setTimestamp(ts);
        smb.getClientMessageBuilder().getPlainBuilder().getTextBuilder().setMessageText(text);

        addMessage(builder.build(), chatId, ts);
    }

    public void clearChatHistory(
        final String guid,
        final String chatId,
        final long ts)
        throws Exception
    {
        HttpPost messagePost = new HttpPost(
            malo().host()
                + "/?&partition=20&offset=1&seqNo=1&topic=" + MESSAGES_TOPIC);
        Message.TOutMessageRef messageRef = Message.TOutMessageRef.newBuilder()
            .setChatId(chatId)
            .setTimestamp(ts)
            .setNonHistoryMessage(
                Client.TServerMessage.newBuilder()
                    .setClientMessage(
                        Client.TClientMessage.newBuilder().setClearUserHistory(
                            Client.TClearUserHistory.newBuilder().setChatId(chatId).build()).build())
                    .setServerMessageInfo(
                        Client.TServerMessage.TServerMessageInfo.newBuilder().setFrom(
                            Client.TUserInfo.newBuilder().setGuid(guid).build()).build())
                    .build()).build();
        messagePost.setEntity(
            new NByteArrayEntity(
                messageRef.toByteArray(),
                ContentType.DEFAULT_BINARY));

        HttpAssert.assertStatusCode(HttpStatus.SC_OK, messagePost);

        if (autoFlush) {
            messagesBackend().flush();
            chatsBackend().flush();
        }
    }

    public void history(
        final String chatId,
        final Message.TChatCounters counters)
        throws Exception
    {
        history(Collections.singletonMap(chatId, counters));
    }

    public void history(
        final Map<String, Message.TChatCounters> counters)
        throws Exception
    {
        Message.THistoryResponse.Builder builder = Message.THistoryResponse.newBuilder();
        List<Message.TChatHistoryResponse> resps = new ArrayList<>();
        for (Map.Entry<String, Message.TChatCounters> counter: counters.entrySet()) {
            resps.add(
                Message.TChatHistoryResponse.newBuilder()
                    .setChatId(counter.getKey())
                    .setCounters(counter.getValue())
                    .build());
        }
        Message.THistoryResponse historyResponse
            = builder.addAllChats(resps).build();

        router.add("/history", new HttpRequestHandler() {
            @Override
            public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context)
                throws HttpException, IOException
            {
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntity entity =
                        ((HttpEntityEnclosingRequest) request).getEntity();
                    InputStream stream =
                        entity.getContent();

                    long skipped =
                        stream.skip(CityHashingArrayOutputStream.THEADER_SIZE);
                    if (skipped != CityHashingArrayOutputStream.THEADER_SIZE) {
                        throw new HttpException("Failed to skip header");
                    }
                    Message.THistoryRequest infoRequest =
                        Message.THistoryRequest.parseFrom(stream);
                    System.out.println("History request "
                        + new JsonFormat().printToString(infoRequest));

                    CityHashingArrayOutputStream out = new CityHashingArrayOutputStream();
                    out.reset();
                    CodedOutputStream googleOut = CodedOutputStream.newInstance(out);
                    historyResponse.writeTo(googleOut);
                    googleOut.flush();

                    final byte[] responseData = out.toByteArrayWithVersion(2);

                    response.setEntity(new ByteArrayEntity(responseData));
                } else {
                    throw new NotImplementedException(
                        "Entity enclosing request expected");
                }
            }
        });
    }

    protected void addIndexingRoutes(
        final StaticServer producer,
        final MaloConfig maloBuilder)
        throws Exception
    {
        Map<String, HttpRequestHandler> producerMap = new LinkedHashMap<>();
        producerMap.put(
            maloBuilder.chatsService(),
            new ProxyMultipartHandler(chatsBackend().indexerPort()));
        producerMap.put(
            maloBuilder.usersService(),
            new ProxyMultipartHandler(chatsBackend().indexerPort()));
        producerMap.put(
            maloBuilder.messagesService(),
            new ProxyMultipartHandler(messagesBackend().indexerPort()));
        producerMap.put(
            maloBuilder.tasksService(),
            new ProxyMultipartHandler(malo().port()));
        producer.add(
            "*",
            new StaticHttpResource(
                new SelectByServiceHandler(producerMap)));
    }

    public StaticServer producer() {
        return producer;
    }
}
