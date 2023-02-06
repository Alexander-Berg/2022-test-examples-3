package ru.yandex.chemodan.app.djfs.core.test.util;

import lombok.RequiredArgsConstructor;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.Filesystem;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.ResourceExistsException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.share.Group;
import ru.yandex.chemodan.app.djfs.core.share.GroupLink;
import ru.yandex.chemodan.app.djfs.core.share.ShareInfo;
import ru.yandex.chemodan.app.djfs.core.share.ShareManager;
import ru.yandex.chemodan.app.djfs.core.share.SharePermissions;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;

/**
 * @author eoshch
 */
@RequiredArgsConstructor
public class ShareTestUtil {
    private final Filesystem filesystem;
    private final ShareManager shareManager;

    public ShareInfo shareInfo(DjfsResourcePath ownerPath, DjfsResourcePath... participantPaths) {
        return shareInfo(ownerPath, Cf.list(participantPaths));
    }

    public ShareInfo shareInfo(DjfsResourcePath ownerPath, ListF<DjfsResourcePath> participantPaths) {
        Group group = Group.builder()
                .id(UuidUtils.randomToHexString())
                .path(ownerPath)
                .version(Option.empty())
                .size(0)
                .build();

        ListF<GroupLink> groupLinks = participantPaths.map(x -> GroupLink.builder()
                .id(UuidUtils.randomToHexString())
                .groupId(group.getId())
                .uid(x.getUid())
                .path(x)
                .version(1)
                .permissions(SharePermissions.READ_WRITE)
                .build());

        return new ShareInfo(group, groupLinks);
    }

    public Tuple2<Group, GroupLink> create(DjfsUid uid1, String path1, DjfsUid uid2, String path2) {
        return create(DjfsResourcePath.cons(uid1, path1), DjfsResourcePath.cons(uid2, path2),
                SharePermissions.READ_WRITE);
    }

    public Tuple2<Group, GroupLink> create(DjfsResourcePath ownerPath, DjfsResourcePath participantPath,
            SharePermissions permissions)
    {
        ListF<DjfsResourcePath> paths = Cf.arrayList();
        paths.addAll(ownerPath.getAllParents().filter(x -> !x.isRoot() && !x.isAreaRoot()));
        paths.add(ownerPath);
        paths.addAll(participantPath.getAllParents().filter(x -> !x.isRoot() && !x.isAreaRoot()));
        paths.add(participantPath);

        for (DjfsResourcePath path : paths) {
            try {
                filesystem.createFolder(DjfsPrincipal.cons(path.getUid()), path);
            } catch (ResourceExistsException e) {
                // ignore
            }
        }

        Group group = shareManager.createGroup(ownerPath);
        GroupLink groupLink = shareManager.createLink(group.getId(), participantPath, permissions);
        return Tuple2.tuple(group, groupLink);
    }
}
