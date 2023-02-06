package ru.yandex.chemodan.app.djfs.core.lastfiles;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.share.ShareInfo;
import ru.yandex.chemodan.app.djfs.core.share.event.UserKickedFromGroupEvent;
import ru.yandex.chemodan.app.djfs.core.share.event.UserLeftGroupEvent;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.util.DjfsAsyncTaskUtils;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.JsonValue;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class LastFilesCacheUpdaterTest extends DjfsTestBase {
    @Autowired
    private LastFilesCacheUpdater sut;

    @Test
    public void handleUserKickedFromGroupEvent() {
        DjfsUid owner = DjfsUid.cons(31337);
        DjfsUid participant = DjfsUid.cons(1337);
        DjfsResourcePath owner_path = DjfsResourcePath.cons(owner, "/disk/owner");
        DjfsResourcePath participant_path = DjfsResourcePath.cons(participant, "/disk/participant");
        ShareInfo shareInfo = util.share.shareInfo(owner_path, participant_path);

        UserKickedFromGroupEvent event = UserKickedFromGroupEvent.builder()
                .instant(Instant.now())
                .uid(participant)
                .shareInfo(shareInfo)
                .build();

        sut.handle(event);

        Assert.sizeIs(1, mockCeleryTaskManager.submitted);
        MapF<String, JsonValue> kwargs = mockCeleryTaskManager.submitted.get(0).getKwargs();
        Assert.equals(shareInfo.getGroupId(), ((JsonString) kwargs.getTs("gid")).getValue());
        Assert.equals(DjfsAsyncTaskUtils.activeUid("update_last_files_cache__" + shareInfo.getGroupId()),
                mockCeleryTaskManager.submitted.get(0).getContext().get().activeUid.get());
    }

    @Test
    public void handleUserLeftGroupEventEvent() {
        DjfsUid owner = DjfsUid.cons(31337);
        DjfsUid participant = DjfsUid.cons(1337);
        DjfsResourcePath owner_path = DjfsResourcePath.cons(owner, "/disk/owner");
        DjfsResourcePath participant_path = DjfsResourcePath.cons(participant, "/disk/participant");
        ShareInfo shareInfo = util.share.shareInfo(owner_path, participant_path);

        UserLeftGroupEvent event = UserLeftGroupEvent.builder()
                .instant(Instant.now())
                .uid(participant)
                .shareInfo(shareInfo)
                .build();

        sut.handle(event);

        Assert.sizeIs(1, mockCeleryTaskManager.submitted);
        MapF<String, JsonValue> kwargs = mockCeleryTaskManager.submitted.get(0).getKwargs();
        Assert.equals(shareInfo.getGroupId(), ((JsonString) kwargs.getTs("gid")).getValue());
        Assert.equals(DjfsAsyncTaskUtils.activeUid("update_last_files_cache__" + shareInfo.getGroupId()),
                mockCeleryTaskManager.submitted.get(0).getContext().get().activeUid.get());
    }
}
