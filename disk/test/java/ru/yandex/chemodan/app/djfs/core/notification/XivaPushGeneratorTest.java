package ru.yandex.chemodan.app.djfs.core.notification;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.share.Group;
import ru.yandex.chemodan.app.djfs.core.share.GroupLink;
import ru.yandex.chemodan.app.djfs.core.share.ShareInfo;
import ru.yandex.chemodan.app.djfs.core.share.event.UserKickedFromGroupEvent;
import ru.yandex.chemodan.app.djfs.core.share.event.UserLeftGroupEvent;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.web.ConnectionIdHolder;
import ru.yandex.chemodan.queller.celery.job.CeleryJob;
import ru.yandex.commune.json.JsonArray;
import ru.yandex.commune.json.JsonNumber;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.JsonValue;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class XivaPushGeneratorTest extends DjfsTestBase {
    private static final String GID = "GROUP_ID";

    private static final DjfsUid OWNER_UID = DjfsUid.cons(31337);
    private static final DjfsResourcePath OWNER_PATH = DjfsResourcePath.cons(OWNER_UID, "/disk/owner");

    private static final String PARTICIPANT_1_LOGIN = "participant-1";
    private static final String PARTICIPANT_1_PUBLIC_NAME = "participant-1-public";
    private static final DjfsUid PARTICIPANT_1_UID = DjfsUid.cons(666);
    private static final DjfsResourcePath PARTICIPANT_1_PATH =
            DjfsResourcePath.cons(PARTICIPANT_1_UID, "/disk/participant1");

    private static final DjfsUid PARTICIPANT_2_UID = DjfsUid.cons(91);
    private static final DjfsResourcePath PARTICIPANT_2_PATH =
            DjfsResourcePath.cons(PARTICIPANT_2_UID, "/disk/participant2");

    private static final DjfsUid PARTICIPANT_3_UID = DjfsUid.cons(92);
    private static final DjfsResourcePath PARTICIPANT_3_PATH =
            DjfsResourcePath.cons(PARTICIPANT_3_UID, "/disk/participant3");

    private static final Group GROUP = Group.builder()
            .id(GID)
            .owner(OWNER_UID)
            .path(OWNER_PATH)
            .size(3)
            .version(Option.of(5L))
            .build();

    private static final GroupLink PARTICIPANT_1_LINK = GroupLink.builder()
            .id("GL1")
            .groupId(GID)
            .uid(PARTICIPANT_1_UID)
            .path(PARTICIPANT_1_PATH)
            .build();

    private static final GroupLink PARTICIPANT_2_LINK = GroupLink.builder()
            .id("GL2")
            .groupId(GID)
            .uid(PARTICIPANT_2_UID)
            .path(PARTICIPANT_2_PATH)
            .build();

    private static final GroupLink PARTICIPANT_3_LINK = GroupLink.builder()
            .id("GL3")
            .groupId(GID)
            .uid(PARTICIPANT_3_UID)
            .path(PARTICIPANT_3_PATH)
            .build();

    private static final ShareInfo SHARE_INFO = new ShareInfo(GROUP,
            Cf.list(PARTICIPANT_1_LINK, PARTICIPANT_2_LINK, PARTICIPANT_3_LINK));

    @Autowired
    private XivaPushGenerator sut;

    @Test
    public void userKickedFromGroup() {
        blackbox2.add(PARTICIPANT_1_UID, PARTICIPANT_1_LOGIN, PARTICIPANT_1_PUBLIC_NAME);
        ConnectionIdHolder.set("CONN-ID");

        UserKickedFromGroupEvent event = UserKickedFromGroupEvent.builder()
                .uid(PARTICIPANT_1_UID)
                .shareInfo(SHARE_INFO)
                .build();

        sut.handle(event);

        Assert.sizeIs(4, mockCeleryTaskManager.submitted);

        MapF<String, JsonValue> kwargs;
        JsonObject xivaData;
        JsonObject object;
        JsonObject parameters;
        List<JsonValue> values;

        // kicked participant push
        kwargs = getKwargsForUid(mockCeleryTaskManager.submitted, PARTICIPANT_1_UID.asString());
        Assert.equals(1, ((JsonNumber) kwargs.getTs("new_version")).intValue());
        Assert.equals(PARTICIPANT_1_UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals("action", ((JsonString) kwargs.getTs("operation")).getValue());
        Assert.equals("share_user_was_banned", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("CONN-ID", ((JsonString) kwargs.getTs("connection_id")).getValue());

        xivaData = (JsonObject) kwargs.getTs("xiva_data");
        object = (JsonObject) xivaData.get("root");
        Assert.equals("share", ((JsonString) object.get("tag")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals("user_was_banned", ((JsonString) parameters.get("type")).getValue());
        Assert.equals("actor", ((JsonString) parameters.get("for")).getValue());

        values = ((JsonArray) xivaData.get("values")).getArray();
        Assert.sizeIs(1, values);

        object = (JsonObject) values.get(0);
        Assert.equals("folder", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(GID, ((JsonString) parameters.get("gid")).getValue());
        Assert.equals(PARTICIPANT_1_PATH.getPath() + "/", ((JsonString) parameters.get("path")).getValue());

        // owner push
        kwargs = getKwargsForUid(mockCeleryTaskManager.submitted, OWNER_UID.asString());
        Assert.equals(1, ((JsonNumber) kwargs.getTs("new_version")).intValue());
        Assert.equals(OWNER_UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals("action", ((JsonString) kwargs.getTs("operation")).getValue());
        Assert.equals("share_user_was_banned", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("", ((JsonString) kwargs.getTs("connection_id")).getValue());

        xivaData = (JsonObject) kwargs.getTs("xiva_data");
        object = (JsonObject) xivaData.get("root");
        Assert.equals("share", ((JsonString) object.get("tag")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals("user_was_banned", ((JsonString) parameters.get("type")).getValue());
        Assert.equals("owner", ((JsonString) parameters.get("for")).getValue());

        values = ((JsonArray) xivaData.get("values")).getArray();
        Assert.sizeIs(2, values);

        object = (JsonObject) values.get(0);
        Assert.equals("folder", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(GID, ((JsonString) parameters.get("gid")).getValue());
        Assert.equals(OWNER_PATH.getPath() + "/", ((JsonString) parameters.get("path")).getValue());
        Assert.equals(OWNER_PATH.getName(), ((JsonString) parameters.get("name")).getValue());

        object = (JsonObject) values.get(1);
        Assert.equals("user", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(PARTICIPANT_1_UID.asString(), ((JsonString) parameters.get("uid")).getValue());
        Assert.equals(PARTICIPANT_1_PUBLIC_NAME, ((JsonString) parameters.get("name")).getValue());

        // participant 2 push
        kwargs = getKwargsForUid(mockCeleryTaskManager.submitted, PARTICIPANT_2_UID.asString());
        Assert.equals(1, ((JsonNumber) kwargs.getTs("new_version")).intValue());
        Assert.equals(PARTICIPANT_2_UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals("action", ((JsonString) kwargs.getTs("operation")).getValue());
        Assert.equals("share_user_was_banned", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("", ((JsonString) kwargs.getTs("connection_id")).getValue());

        xivaData = (JsonObject) kwargs.getTs("xiva_data");
        object = (JsonObject) xivaData.get("root");
        Assert.equals("share", ((JsonString) object.get("tag")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals("user_was_banned", ((JsonString) parameters.get("type")).getValue());
        Assert.equals("user", ((JsonString) parameters.get("for")).getValue());

        values = ((JsonArray) xivaData.get("values")).getArray();
        Assert.sizeIs(2, values);

        object = (JsonObject) values.get(0);
        Assert.equals("folder", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(GID, ((JsonString) parameters.get("gid")).getValue());
        Assert.equals(PARTICIPANT_2_PATH.getPath() + "/", ((JsonString) parameters.get("path")).getValue());
        Assert.equals(PARTICIPANT_2_PATH.getName(), ((JsonString) parameters.get("name")).getValue());

        object = (JsonObject) values.get(1);
        Assert.equals("user", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(PARTICIPANT_1_UID.asString(), ((JsonString) parameters.get("uid")).getValue());
        Assert.equals(PARTICIPANT_1_PUBLIC_NAME, ((JsonString) parameters.get("name")).getValue());

        // participant 3 push
        kwargs = getKwargsForUid(mockCeleryTaskManager.submitted, PARTICIPANT_3_UID.asString());
        Assert.equals(1, ((JsonNumber) kwargs.getTs("new_version")).intValue());
        Assert.equals(PARTICIPANT_3_UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals("action", ((JsonString) kwargs.getTs("operation")).getValue());
        Assert.equals("share_user_was_banned", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("", ((JsonString) kwargs.getTs("connection_id")).getValue());

        xivaData = (JsonObject) kwargs.getTs("xiva_data");
        object = (JsonObject) xivaData.get("root");
        Assert.equals("share", ((JsonString) object.get("tag")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals("user_was_banned", ((JsonString) parameters.get("type")).getValue());
        Assert.equals("user", ((JsonString) parameters.get("for")).getValue());

        values = ((JsonArray) xivaData.get("values")).getArray();
        Assert.sizeIs(2, values);

        object = (JsonObject) values.get(0);
        Assert.equals("folder", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(GID, ((JsonString) parameters.get("gid")).getValue());
        Assert.equals(PARTICIPANT_3_PATH.getPath() + "/", ((JsonString) parameters.get("path")).getValue());
        Assert.equals(PARTICIPANT_3_PATH.getName(), ((JsonString) parameters.get("name")).getValue());

        object = (JsonObject) values.get(1);
        Assert.equals("user", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(PARTICIPANT_1_UID.asString(), ((JsonString) parameters.get("uid")).getValue());
        Assert.equals(PARTICIPANT_1_PUBLIC_NAME, ((JsonString) parameters.get("name")).getValue());
    }

    @Test
    public void userLeftGroup() {
        blackbox2.add(PARTICIPANT_1_UID, PARTICIPANT_1_LOGIN, PARTICIPANT_1_PUBLIC_NAME);
        ConnectionIdHolder.set("CONN-ID");

        UserLeftGroupEvent event = UserLeftGroupEvent.builder()
                .uid(PARTICIPANT_1_UID)
                .shareInfo(SHARE_INFO)
                .build();

        sut.handle(event);

        Assert.sizeIs(4, mockCeleryTaskManager.submitted);

        MapF<String, JsonValue> kwargs;
        JsonObject xivaData;
        JsonObject object;
        JsonObject parameters;
        List<JsonValue> values;

        // kicked participant push
        kwargs = getKwargsForUid(mockCeleryTaskManager.submitted, PARTICIPANT_1_UID.asString());
        Assert.equals(1, ((JsonNumber) kwargs.getTs("new_version")).intValue());
        Assert.equals(PARTICIPANT_1_UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals("action", ((JsonString) kwargs.getTs("operation")).getValue());
        Assert.equals("share_user_has_left", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("CONN-ID", ((JsonString) kwargs.getTs("connection_id")).getValue());

        xivaData = (JsonObject) kwargs.getTs("xiva_data");
        object = (JsonObject) xivaData.get("root");
        Assert.equals("share", ((JsonString) object.get("tag")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals("user_has_left", ((JsonString) parameters.get("type")).getValue());
        Assert.equals("actor", ((JsonString) parameters.get("for")).getValue());

        values = ((JsonArray) xivaData.get("values")).getArray();
        Assert.sizeIs(1, values);

        object = (JsonObject) values.get(0);
        Assert.equals("folder", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(GID, ((JsonString) parameters.get("gid")).getValue());
        Assert.equals(PARTICIPANT_1_PATH.getPath() + "/", ((JsonString) parameters.get("path")).getValue());

        // owner push
        kwargs = getKwargsForUid(mockCeleryTaskManager.submitted, OWNER_UID.asString());;
        Assert.equals(1, ((JsonNumber) kwargs.getTs("new_version")).intValue());
        Assert.equals(OWNER_UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals("action", ((JsonString) kwargs.getTs("operation")).getValue());
        Assert.equals("share_user_has_left", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("", ((JsonString) kwargs.getTs("connection_id")).getValue());

        xivaData = (JsonObject) kwargs.getTs("xiva_data");
        object = (JsonObject) xivaData.get("root");
        Assert.equals("share", ((JsonString) object.get("tag")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals("user_has_left", ((JsonString) parameters.get("type")).getValue());
        Assert.equals("owner", ((JsonString) parameters.get("for")).getValue());

        values = ((JsonArray) xivaData.get("values")).getArray();
        Assert.sizeIs(2, values);

        object = (JsonObject) values.get(0);
        Assert.equals("folder", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(GID, ((JsonString) parameters.get("gid")).getValue());
        Assert.equals(OWNER_PATH.getPath() + "/", ((JsonString) parameters.get("path")).getValue());
        Assert.equals(OWNER_PATH.getName(), ((JsonString) parameters.get("name")).getValue());

        object = (JsonObject) values.get(1);
        Assert.equals("user", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(PARTICIPANT_1_UID.asString(), ((JsonString) parameters.get("uid")).getValue());
        Assert.equals(PARTICIPANT_1_PUBLIC_NAME, ((JsonString) parameters.get("name")).getValue());

        // participant 2 push
        kwargs = getKwargsForUid(mockCeleryTaskManager.submitted, PARTICIPANT_2_UID.asString());;
        Assert.equals(1, ((JsonNumber) kwargs.getTs("new_version")).intValue());
        Assert.equals(PARTICIPANT_2_UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals("action", ((JsonString) kwargs.getTs("operation")).getValue());
        Assert.equals("share_user_has_left", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("", ((JsonString) kwargs.getTs("connection_id")).getValue());

        xivaData = (JsonObject) kwargs.getTs("xiva_data");
        object = (JsonObject) xivaData.get("root");
        Assert.equals("share", ((JsonString) object.get("tag")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals("user_has_left", ((JsonString) parameters.get("type")).getValue());
        Assert.equals("user", ((JsonString) parameters.get("for")).getValue());

        values = ((JsonArray) xivaData.get("values")).getArray();
        Assert.sizeIs(2, values);

        object = (JsonObject) values.get(0);
        Assert.equals("folder", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(GID, ((JsonString) parameters.get("gid")).getValue());
        Assert.equals(PARTICIPANT_2_PATH.getPath() + "/", ((JsonString) parameters.get("path")).getValue());
        Assert.equals(PARTICIPANT_2_PATH.getName(), ((JsonString) parameters.get("name")).getValue());

        object = (JsonObject) values.get(1);
        Assert.equals("user", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(PARTICIPANT_1_UID.asString(), ((JsonString) parameters.get("uid")).getValue());
        Assert.equals(PARTICIPANT_1_PUBLIC_NAME, ((JsonString) parameters.get("name")).getValue());

        // participant 3 push
        kwargs = getKwargsForUid(mockCeleryTaskManager.submitted, PARTICIPANT_3_UID.asString());
        Assert.equals(1, ((JsonNumber) kwargs.getTs("new_version")).intValue());
        Assert.equals(PARTICIPANT_3_UID.asString(), ((JsonString) kwargs.getTs("uid")).getValue());
        Assert.equals("action", ((JsonString) kwargs.getTs("operation")).getValue());
        Assert.equals("share_user_has_left", ((JsonString) kwargs.getTs("class")).getValue());
        Assert.equals("", ((JsonString) kwargs.getTs("connection_id")).getValue());

        xivaData = (JsonObject) kwargs.getTs("xiva_data");
        object = (JsonObject) xivaData.get("root");
        Assert.equals("share", ((JsonString) object.get("tag")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals("user_has_left", ((JsonString) parameters.get("type")).getValue());
        Assert.equals("user", ((JsonString) parameters.get("for")).getValue());

        values = ((JsonArray) xivaData.get("values")).getArray();
        Assert.sizeIs(2, values);

        object = (JsonObject) values.get(0);
        Assert.equals("folder", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(GID, ((JsonString) parameters.get("gid")).getValue());
        Assert.equals(PARTICIPANT_3_PATH.getPath() + "/", ((JsonString) parameters.get("path")).getValue());
        Assert.equals(PARTICIPANT_3_PATH.getName(), ((JsonString) parameters.get("name")).getValue());

        object = (JsonObject) values.get(1);
        Assert.equals("user", ((JsonString) object.get("tag")).getValue());
        Assert.equals("", ((JsonString) object.get("value")).getValue());
        parameters = (JsonObject) object.get("parameters");
        Assert.equals(PARTICIPANT_1_UID.asString(), ((JsonString) parameters.get("uid")).getValue());
        Assert.equals(PARTICIPANT_1_PUBLIC_NAME, ((JsonString) parameters.get("name")).getValue());
    }

    private MapF<String, JsonValue> getKwargsForUid(ListF<CeleryJob> jobs, String uid) {
        ListF<MapF<String, JsonValue>> kwargs = jobs.map(CeleryJob::getKwargs).filter(kw ->
                kw.getO("uid").filter(JsonString.class::isInstance).map(JsonString.class::cast).map(JsonString::getString)
                        .filter(uidValue -> uid.equals(uidValue)).isPresent());
        Assert.isFalse(kwargs.isEmpty());
        Assert.assertEquals(1, kwargs.size());
        return kwargs.first();
    }
}
