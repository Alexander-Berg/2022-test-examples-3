package ru.yandex.market.crm.core.test.utils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.yt.paths.CrmYtTables;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.User;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.PuidToEmail;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;

import static com.google.common.base.Strings.nullToEmpty;

/**
 * @author apershukov
 */
@Component
public class UserTestHelper implements StatefulHelper {

    @YTreeObject
    public static class UserIdsRow {
        @YTreeField(key = "ids")
        @JsonProperty("ids")
        private List<String> ids;

        @YTreeField(key = "id_type")
        @JsonProperty("id_type")
        private String idType;

        @YTreeField(key = "id_value")
        @JsonProperty("id_value")
        private String idValue;

        public List<String> getIds() {
            return ids;
        }

        public void setIds(List<String> ids) {
            this.ids = ids;
        }

        public String getIdType() {
            return idType;
        }

        public void setIdType(String idType) {
            this.idType = idType;
        }

        public String getIdValue() {
            return idValue;
        }

        public void setIdValue(String idValue) {
            this.idValue = idValue;
        }
    }

    public static class IdRelation {

        final String source;
        final String target;

        public IdRelation(String source, String target) {
            this.source = source;
            this.target = target;
        }
    }

    private static class IdLink {
        final Uid id1;
        final Uid id2;

        IdLink(Uid id1, Uid id2) {
            this.id1 = id1;
            this.id2 = id2;
        }
    }

    public static final String UUID = "uuid";
    public static final String EMAIL_MD5 = "email_md5";
    public static final String PUID = "puid";
    public static final String YUID = "yandexuid";
    public static final String CRYPTA_ID = "crypta_id";

    private final YtClient ytClient;
    private final CrmYtTables ytTables;
    private final YtTestTables ytTestTables;
    private final YtSchemaTestHelper ytSchemaTestHelper;

    private final List<IdLink> directLinks = new ArrayList<>();
    private final List<IdLink> allLinks = new ArrayList<>();
    private final List<User> users = new ArrayList<>();

    public UserTestHelper(YtClient ytClient,
                          CrmYtTables ytTables,
                          YtTestTables ytTestTables,
                          YtSchemaTestHelper ytSchemaTestHelper) {
        this.ytClient = ytClient;
        this.ytTables = ytTables;
        this.ytTestTables = ytTestTables;
        this.ytSchemaTestHelper = ytSchemaTestHelper;
    }

    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
        directLinks.clear();
        allLinks.clear();
        users.clear();
    }

    public static YTreeMapNode passportProfile(long puid,
                                               @Nullable String gender,
                                               @Nullable String displayName,
                                               @Nullable String firstname,
                                               @Nullable String lastname,
                                               @Nullable String userDefinedLogin,
                                               String login,
                                               @Nullable String avatar) {
        return YTree.mapBuilder()
                .key("uid").value(String.valueOf(puid))
                .key("login").value(login)
                .key("user_defined_login").value(nullToEmpty(userDefinedLogin))
                .key("display_name").value(nullToEmpty(displayName))
                .key("has_display_name").value(null != displayName)
                .key("firstname").value(nullToEmpty(firstname))
                .key("has_firstname").value(null != firstname)
                .key("lastname").value(nullToEmpty(lastname))
                .key("has_lastname").value(null != lastname)
                .key("gender").value(nullToEmpty(gender))
                .key("default_avatar").value(nullToEmpty(avatar))
                .key("has_default_avatar").value(null != avatar)
                .buildMap();
    }

    public static YTreeMapNode passportProfile(long puid,
                                               @Nullable String gender,
                                               @Nullable String firstname,
                                               @Nullable String lastname,
                                               @Nullable String userDefinedLogin,
                                               String login,
                                               @Nullable String avatar) {
        return passportProfile(puid, gender, null, firstname, lastname, userDefinedLogin, login, avatar);
    }

    public static YTreeMapNode passportProfile(long puid,
                                               @Nullable String gender,
                                               @Nullable String firstname,
                                               @Nullable String lastname,
                                               @Nullable String userDefinedLogin,
                                               String login) {
        return passportProfile(puid, gender, firstname, lastname, userDefinedLogin, login, null);
    }

    public static YTreeMapNode passportProfile(long puid, String gender, String login) {
        return passportProfile(puid, gender, "John", "Walker", login, login);
    }

    public static YTreeMapNode passportProfile(long puid,
                                               @Nullable String gender,
                                               String firstname,
                                               String lastname) {
        return passportProfile(puid, gender, firstname, lastname, "default_login", "login");
    }

    public static YTreeMapNode passportProfile(long puid, @Nullable String gender) {
        return passportProfile(puid, gender, "default_login");
    }

    public static YTreeMapNode plusData(long puid) {
        return YTree.mapBuilder()
                .key("puid").value(String.valueOf(puid))
                .key("plus").value(true)
                .key("end_date").value(LocalDate.now().toString())
                .key("child").value(false)
                .key("end").value(1)
                .key("fielddate").value("")
                .key("grace").value(false)
                .key("intro").value(false)
                .key("order_type").value("")
                .key("parent").value(false)
                .key("region").value(312)
                .key("start").value(1)
                .key("trial").value(false)
                .buildMap();
    }

    public static YTreeMapNode accessEntry(String yuid, Long puid) {
        return accessEntry(yuid, puid, DeviceType.DESKTOP);
    }

    public static YTreeMapNode accessEntry(String yuid, Long puid, DeviceType deviceType) {
        return YTree.mapBuilder()
                .key("color").value("GREEN")
                .key("yuid").value(yuid == null ? new YTreeEntityNodeImpl(Cf.map()) : yuid)
                .key("puid").value(puid == null ? new YTreeEntityNodeImpl(Cf.map()) : puid)
                .key("device_type").value(deviceType.name())
                .buildMap();
    }

    public void addUsers(User... users) {
        this.users.addAll(Arrays.asList(users));
    }

    public void addPassportProfiles(YTreeMapNode... profiles) {
        ytClient.write(ytTestTables.getPassportProfiles(), Arrays.asList(profiles));
    }

    /**
     * Завершить подготовку данных пользователей.
     * 1. Замораживает таблицы users и user_ids
     * <p>
     * Это провоцирует слив всей записанной в ней информации в чанки, что бывает полезно
     * если нужны гарантии на то что в тесте map-reduce операции увидят актуальные данные в
     * этих таблицах.
     * <p>
     * 2. Записывает информацию о связях в специальные таблицы, необходимые для работы сегментатора
     */
    public void finishUsersPreparation() {
        ytClient.write(ytTables.getUsers(), User.class, users.stream()
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toList()));

        SortedSet<UserIdsRow> userIdsRowSet = new TreeSet<>(Comparator
                .comparing(UserIdsRow::getIdValue)
                .thenComparing(UserIdsRow::getIdType)
        );

        for (var user : users) {

            for (var id : user.getIdsGraph().getNodes()) {
                var userIds = new UserIdsRow();
                userIds.setIds(List.of(user.getId()));
                userIds.setIdType(id.getType().name());
                userIds.setIdValue(id.getValue());
                userIdsRowSet.add(userIds);
            }

            saveAllLinks(user);
            saveDirectLinks(user);
        }

        ytClient.write(ytTables.getUserIds(), UserIdsRow.class, userIdsRowSet);
        writeLinks(LinkingMode.ALL, allLinks);
        writeLinks(LinkingMode.DIRECT_ONLY, directLinks);
    }

    public void addPlusData(YTreeMapNode... row) {
        ytClient.write(ytTestTables.getPlusDataTablePath(), Arrays.asList(row));
    }

    public void addFapiAccessLogEntries(YTreeMapNode... rows) {
        ytClient.write(ytTestTables.getFapiAccessDataTablePath(), Arrays.asList(rows));
    }

    public void addCapiAccessLogEntries(YTreeMapNode... rows) {
        ytClient.write(ytTestTables.getCapiAccessDataTablePath(), Arrays.asList(rows));
    }

    public void saveLinks(String id1, String id1Type, String id2, String id2Type) {
        saveLinks(id1Type, id2Type, new IdRelation(id1, id2));
    }

    public void saveLinks(String id1Type, String id2Type, IdRelation... relations) {
        YPath tablePath = ytTestTables.getCryptaMatchingDir().child(id1Type).child(id2Type);
        saveLinks(tablePath, id1Type, id2Type, relations);
    }

    public void saveLinks(YPath tablePath, String id1Type, String id2Type, IdRelation... relations) {
        List<YTreeMapNode> rows = Stream.of(relations)
                .map(relation -> YTree.mapBuilder()
                        .key("id").value(relation.source)
                        .key("id_type").value(id1Type)
                        .key("target_id").value(relation.target)
                        .key("target_id_type").value(id2Type)
                        .buildMap()
                )
                .sorted(
                        Comparator.<YTreeMapNode, String>comparing(row -> row.getString("id"))
                                .thenComparing(row -> row.getString("id_type"))
                )
                .collect(Collectors.toList());

        ytClient.write(tablePath.append(true), rows);
    }

    public void preparePlatformPuidToEmails(PuidToEmail... facts) {
        var path = ytTestTables.getPlatformPuidToEmail();
        ytSchemaTestHelper.prepareFactsTable(path, "minimal_key_fact.yson", PuidToEmail.getDescriptor());

        List<YTreeMapNode> rows = Stream.of(facts)
                .sorted(Comparator.comparing(x -> x.getUid().getStringValue()))
                .map(fact -> YTree.mapBuilder()
                        .key("id").value(String.valueOf(fact.getUid().getIntValue()))
                        .key("id_type").value("puid")
                        .key("fact").value(new YTreeStringNodeImpl(fact.toByteArray(), null))
                        .buildMap()
                )
                .collect(Collectors.toList());

        ytClient.write(path, rows);
    }

    public static PuidToEmail puidToEmailFact(long puid, String email) {
        return PuidToEmail.newBuilder()
                .setUid(Uids.create(UidType.PUID, puid))
                .setEmail(email)
                .build();
    }

    private void saveAllLinks(User user) {
        var nodes = user.getIdsGraph().getNodes();
        for (int i = 0; i < nodes.size() - 1; ++i) {
            for (int j = i + 1; j < nodes.size(); ++j) {
                var node1 = nodes.get(i);
                var node2 = nodes.get(j);
                allLinks.add(new IdLink(node1, node2));
                allLinks.add(new IdLink(node2, node1));
            }
        }
    }

    private void saveDirectLinks(User user) {
        var graph = user.getIdsGraph();
        var nodes = graph.getNodes();

        for (var edge : graph.getEdges()) {
            var node1 = nodes.get(edge.getNode1());
            var node2 = nodes.get(edge.getNode2());
            directLinks.add(new IdLink(node1, node2));
            directLinks.add(new IdLink(node2, node1));
        }
    }

    private void writeLinks(LinkingMode mode, List<IdLink> links) {
        var groups = links.stream()
                .collect(Collectors.groupingBy(link -> link.id1.getType()))
                .entrySet().stream().collect(Collectors.toMap(
                        Entry::getKey,
                        e -> e.getValue().stream()
                                .collect(Collectors.groupingBy(link -> link.id2.getType()))
                ));

        for (var source : groups.entrySet()) {
            var sourceIdType = source.getKey();

            for (var target : source.getValue().entrySet()) {
                var targetIdType = target.getKey();
                var tablePath = ytTables.getLinksTable(mode, sourceIdType, targetIdType);

                var rows = target.getValue().stream()
                        .sorted(Comparator.comparing(link -> link.id1.getValue()))
                        .map(UserTestHelper::toRow)
                        .collect(Collectors.toList());

                ytClient.write(tablePath, YTableEntryTypes.YSON, rows);
            }
        }
    }

    private static YTreeMapNode toRow(IdLink link) {
        var id1 = link.id1;
        var id2 = link.id2;
        return YTree.mapBuilder()
                .key("id_value_1").value(id1.getValue())
                .key("id_type_1").value(id1.getType().name())
                .key("id_value_2").value(id2.getValue())
                .key("id_type_2").value(id2.getType().name())
                .buildMap();
    }
}
