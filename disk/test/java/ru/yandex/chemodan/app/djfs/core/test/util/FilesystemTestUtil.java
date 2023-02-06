package ru.yandex.chemodan.app.djfs.core.test.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.app.djfs.core.db.pg.TransactionUtils;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsResourceDao;
import ru.yandex.chemodan.app.djfs.core.filesystem.Filesystem;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.ResourceExistsException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceArea;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.share.ShareInfo;
import ru.yandex.chemodan.app.djfs.core.share.ShareInfoManager;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
@RequiredArgsConstructor
public class FilesystemTestUtil {
    public final DjfsResourceInitializersTestUtil init = new DjfsResourceInitializersTestUtil();

    private final DjfsResourceDao djfsResourceDao;
    private final ShareInfoManager shareInfoManager;
    private final Filesystem filesystem;
    private final TransactionUtils transactionUtils;

    private static String hexFromPath(DjfsResource.Builder resource) {
        return resource.getPath().getPath().replace("/disk", "").replace("/", "");
    }

    private static String hexFromParentPath(DjfsResource resource) {
        return resource.getPath().getParent().getPath().replace("/disk", "").replace("/", "");
    }

    public FolderDjfsResource insertFolder(DjfsUid uid, String path,
            Function1V<? super FolderDjfsResource.Builder> initialize)
    {
        DjfsResourcePath djfsPath = DjfsResourcePath.cons(uid, path);
        FolderDjfsResource folder = FolderDjfsResource.cons(djfsPath, initialize);
        djfsResourceDao.insert(uid, folder);
        return folder;
    }

    public FileDjfsResource insertFile(DjfsUid uid, String path,
            Function1V<? super FileDjfsResource.Builder> initialize)
    {
        DjfsResourcePath djfsPath = DjfsResourcePath.cons(uid, path);
        FileDjfsResource file = FileDjfsResource.random(djfsPath, initialize);
        djfsResourceDao.insert(uid, file);
        return file;
    }

    public Tuple2<FileDjfsResource, FileDjfsResource> insertLivePhoto(DjfsUid uid, String photoPath) {
        DjfsResourcePath djfsPhotoPath = DjfsResourcePath.cons(uid, photoPath);

        FolderDjfsResource livePhotoParent = (FolderDjfsResource) djfsResourceDao.find(djfsPhotoPath.getParent()).get();
        FolderDjfsResource liveVideoParent = (FolderDjfsResource) djfsResourceDao.find(
                DjfsResourcePath.areaRoot(uid, DjfsResourceArea.ADDITIONAL_DATA)).get();

        FileDjfsResource livePhotoFile = FileDjfsResource.random(
                livePhotoParent, djfsPhotoPath.getName(), x -> x.isLivePhoto(true));
        FileDjfsResource liveVideoFile = FileDjfsResource.random(liveVideoParent, UuidUtils.randomToHexString());

        transactionUtils.executeInNewOrCurrentTransaction(uid, () -> {
            djfsResourceDao.insert2(liveVideoFile);
            djfsResourceDao.insert2AndLinkTo(livePhotoFile, liveVideoFile);
        });

        return Tuple2.tuple(livePhotoFile, liveVideoFile);
    }

    public void createFolderRecursively(DjfsPrincipal principal, DjfsResourcePath path) {
        for (DjfsResourcePath parent : path.getAllParents().filterNot(DjfsResourcePath::isRoot)) {
            try {
                filesystem.createFolder(principal, parent);
            } catch (ResourceExistsException ignore) {
            }
        }
    }

    public void initialize(DjfsResourcePath root, int filesPerFolder, int foldersPerFolder, int depth) {
        initialize(root, filesPerFolder, foldersPerFolder, depth,
                x -> x.fileId(DjfsFileId.cons(StringUtils.leftPad(hexFromPath(x), 64, "0"))),
                x -> x.fileId(DjfsFileId.cons(StringUtils.leftPad(hexFromPath(x), 64, "0"))));
    }

    public void initialize(DjfsResourcePath root, int filesPerFolder, int foldersPerFolder, int depth,
            Function1V<? super FileDjfsResource.Builder> fileInitializer,
            Function1V<? super FolderDjfsResource.Builder> folderInitializer)
    {
        List<FileDjfsResource> files = new ArrayList<>();
        List<FolderDjfsResource> folders = new ArrayList<>();

        List<DjfsResourcePath> currentLevel = new ArrayList<>(1);
        currentLevel.add(root);
        for (int level = 0; level < depth; level++) {
            List<DjfsResourcePath> nextLevel = new ArrayList<>(foldersPerFolder);
            for (DjfsResourcePath path : currentLevel) {
                for (int i = 0; i < filesPerFolder; i++) {
                    files.add(FileDjfsResource.random(path.getChildPath("f" + i), fileInitializer));
                }
                for (int i = 0; i < foldersPerFolder; i++) {
                    folders.add(FolderDjfsResource.cons(path.getChildPath("d" + i), folderInitializer));
                    nextLevel.add(path.getChildPath("d" + i));
                }
            }
            currentLevel = nextLevel;
        }

        djfsResourceDao.insert(root.getUid(), Cf.x(folders).cast());
        djfsResourceDao.insert(root.getUid(), Cf.x(files).cast());
    }

    public void assertParentIdsUuid(ListF<UUID> actualParentIds, DjfsResourcePath resourcePath) {
        assertParentIds(actualParentIds.map(UuidUtils::toHexString), resourcePath, false);
    }

    public void assertParentIds(ListF<String> actualParentIds, DjfsResourcePath resourcePath, boolean skipRootFolder) {
        ListF<DjfsResourcePath> allParents = resourcePath.getAllParents();

        if (skipRootFolder) {
            Assert.equals(allParents.length() - 1, actualParentIds.length());
        } else {
            Assert.equals(allParents.length(), actualParentIds.length());
        }

        for (int i = skipRootFolder ? 1 : 0; i < allParents.length(); ++i) {
            DjfsResourcePath path = allParents.get(i);
            Option<ShareInfo> shareInfoO = shareInfoManager.get(path);

            if (shareInfoO.isPresent()) {
                ShareInfo shareInfo = shareInfoO.get();
                path = path.changeParent(shareInfo.getRootPath(path.getUid()).get(), shareInfo.getGroupPath());
            }

            Option<DjfsResource> folderResourceO = djfsResourceDao.find(path);

            Assert.isTrue(folderResourceO.isPresent(), allParents.get(i).getFullPath());
            DjfsResource folderResource = folderResourceO.get();

            String actualParentId = actualParentIds.get(skipRootFolder ? i - 1 : i);
            Assert.equals(UuidUtils.toHexString(folderResource.getId()), actualParentId);
        }
    }
}
