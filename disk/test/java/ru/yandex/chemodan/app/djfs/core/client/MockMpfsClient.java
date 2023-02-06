package ru.yandex.chemodan.app.djfs.core.client;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpResponse;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.bolts.collection.Tuple3;
import ru.yandex.bolts.function.Function;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.mpfs.MpfsCallbackResponse;
import ru.yandex.chemodan.mpfs.MpfsClient;
import ru.yandex.chemodan.mpfs.MpfsCommentsPermissions;
import ru.yandex.chemodan.mpfs.MpfsFileInfo;
import ru.yandex.chemodan.mpfs.MpfsGroupUids;
import ru.yandex.chemodan.mpfs.MpfsHid;
import ru.yandex.chemodan.mpfs.MpfsLentaBlockFileIds;
import ru.yandex.chemodan.mpfs.MpfsListResponse;
import ru.yandex.chemodan.mpfs.MpfsOperation;
import ru.yandex.chemodan.mpfs.MpfsPublicSettings;
import ru.yandex.chemodan.mpfs.MpfsShareFolderInvite;
import ru.yandex.chemodan.mpfs.MpfsStoreOperation;
import ru.yandex.chemodan.mpfs.MpfsStoreOperationContext;
import ru.yandex.chemodan.mpfs.MpfsUid;
import ru.yandex.chemodan.mpfs.MpfsUser;
import ru.yandex.chemodan.mpfs.MpfsUserInfo;
import ru.yandex.chemodan.mpfs.MpfsUserServiceInfo;
import ru.yandex.chemodan.mpfs.lentablock.MpfsLentaBlockFullDescription;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.utils.Language;

/**
 * @author eoshch
 */
public class MockMpfsClient implements MpfsClient {
    private static final MapF<String, ListF> calls = Cf.hashMap();

    public ListF getCallParameters(String methodName) {
        return calls.getO(methodName).getOrElse(Cf.list());
    }

    public void clear() {
        calls.clear();
    }

    @Override
    public boolean needInit(MpfsUser uid) {
        return false;
    }

    @Override
    public void userInit(MpfsUser uid, Language locale, String source, Option<String> b2b) {
    }

    @Override
    public MpfsCallbackResponse markMulcaIdToRemove(MulcaId mulcaId) {
        return null;
    }

    @Override
    public MpfsCallbackResponse getFullRelativeTree(MpfsUser uid, String path) {
        return null;
    }

    @Override
    public MpfsCallbackResponse getFullRelativeTreePublic(String hash, boolean showNda) {
        return null;
    }

    @Override
    public MpfsCallbackResponse getAlbumResources(String publicKey, Option<MpfsUser> uid) {
        return null;
    }

    @Override
    public MpfsCallbackResponse getFilesResources(String mpfsOid, MpfsUser uid) {
        return null;
    }

    @Override
    public MpfsFileInfo getFileInfoByHid(MpfsHid mpfsHid) {
        return null;
    }

    @Override
    public ListF<MpfsFileInfo> bulkInfoByResourceIds(MpfsUser uid, SetF<String> meta, ListF<String> resourceIds,
            ListF<String> enableServiceIds)
    {
        return null;
    }

    @Override
    public ListF<MpfsFileInfo> bulkInfoByPaths(MpfsUser uid, SetF<String> meta, ListF<String> paths) {
        return null;
    }

    @Override
    public void streamingListByUidAndPath(MpfsUser uid, String path, Option<Integer> offset, Option<Integer> amount,
            Option<String> sort, boolean orderDesc, Function1V<Iterator<MpfsFileInfo>> callback)
    {

    }

    @Override
    public MpfsListResponse listByUidAndPath(MpfsUser uid, String path, Option<Integer> offset, Option<Integer> amount,
            Option<String> sort, boolean orderDesc)
    {
        return null;
    }

    @Override
    public MpfsFileInfo getFileInfoByUidAndPath(MpfsUser uid, String path, MapF<String, Object> params, ListF<String> metaFields) {
        return null;
    }

    @Override
    public MpfsFileInfo getFileInfoByUidAndPath(MpfsUser uid, String path, ListF<String> metaFields) {
        return null;
    }

    @Override
    public void mkdir(MpfsUser uid, String path) {

    }

    @Override
    public String mksysdir(MpfsUser uid, String type) {
        return null;
    }

    @Override
    public Option<MpfsShareFolderInvite> getNotApprovedInvite(MpfsUser uid, String hash) {
        return null;
    }

    @Override
    public ListF<MpfsShareFolderInvite> getNotApprovedInvites(MpfsUser uid) {
        return null;
    }

    @Override
    public Option<MpfsOperation> getOperation(MpfsUser uid, String oid) {
        return null;
    }

    @Override
    public ListF<MpfsOperation> activeOperations(MpfsUser uid) {
        return null;
    }

    @Override
    public MpfsStoreOperation astore(MpfsUser uid, String path, String md5, Tuple2List<String, String> headers) {
        return null;
    }

    @Override
    public MpfsStoreOperation dstore(MpfsUser uid, String path, String md5, boolean iSwearIWillNeverPublishThisUrl) {
        return null;
    }

    @Override
    public void setprop(MpfsUser uid, String path, MapF<String, String> properties) {

    }

    @Override
    public void uploadEmptyFile(MpfsUser uid, String path, MapF<String, Object> additionalParams) {

    }

    @Override
    public MpfsStoreOperation store(MpfsStoreOperationContext c, Tuple2List<String, String> headers) {
        return null;
    }

    @Override
    public MpfsCallbackResponse setPrivate(MpfsUser uid, String path) {
        return null;
    }

    @Override
    public MpfsCallbackResponse setPublic(MpfsUser uid, String path) {
        return null;
    }

    @Override
    public MpfsCallbackResponse setPublic(MpfsUser uid, String path, Tuple2List<String, String> headers) {
        return null;
    }

    @Override
    public String getPublicUrl(MpfsCallbackResponse response) {
        return null;
    }

    @Override
    public MpfsPublicSettings getPublicSettings(MpfsUser uid, String path) {
        return null;
    }

    @Override
    public MpfsPublicSettings getPublicSettingsByHash(MpfsUser uid, String hash) {
        return null;
    }

    @Override
    public MpfsCallbackResponse stateRemove(MpfsUser uid, String key) {
        return null;
    }

    @Override
    public MpfsCallbackResponse stateSet(MpfsUser uid, String key, String value) {
        return null;
    }

    @Override
    public MpfsCallbackResponse settingRemove(MpfsUser uid, String namespace, String key) {
        return null;
    }

    @Override
    public MpfsCallbackResponse settingSet(MpfsUser uid, String namespace, String key, String value) {
        return null;
    }

    @Override
    public MpfsCallbackResponse setProp(MpfsUser uid, String path, Tuple2List<String, String> set,
            ListF<String> remove)
    {
        return null;
    }

    @Override
    public void userInstallDevice(MpfsUser uid, String type, String deviceId, MapF<String, Object> params) {

    }

    @Override
    public <T> T diff(MpfsUser uid, String path, Option<Long> version,
            Function<HttpResponse, T> indexResponseProcessor)
    {
        return null;
    }

    @Override
    public MpfsCallbackResponse rm(MpfsUser uid, String path) {
        if (!calls.containsKeyTs("rm")) {
            calls.put("rm", Cf.arrayList());
        }
        calls.getTs("rm").add(Tuple2.tuple(uid, path));
        return new MpfsCallbackResponse(200, "OK", "", Cf.map(), Option.empty());
    }

    @Override
    public MpfsOperation asyncRm(MpfsUser uid, String path, Option<String> ifMatch, String callbackUrl) {
        return null;
    }

    @Override
    public MpfsOperation asyncTrashAppend(MpfsUser uid, String path, Option<String> ifMatch, String callbackUrl) {
        return null;
    }

    @Override
    public MpfsOperation asyncTrashDropAll(MpfsUser uid, String callbackUrl) {
        return null;
    }

    @Override
    public MpfsCallbackResponse copy(MpfsUser uid, String srcPath, String dstPath, boolean checkHidsBlockings) {
        return null;
    }

    @Override
    public MpfsOperation asyncCopy(MpfsUser uid, String srcPath, String dstPath, boolean overwrite,
            String callbackUrl)
    {
        return null;
    }

    @Override
    public MpfsCallbackResponse move(MpfsUser uid, String srcPath, String dstPath, boolean checkHidsBlockings) {
        if (!calls.containsKeyTs("move")) {
            calls.put("move", Cf.arrayList());
        }
        calls.getTs("move").add(Tuple3.tuple(uid, srcPath, dstPath));
        return new MpfsCallbackResponse(200, "OK", "", Cf.map(), Option.empty());
    }

    @Override
    public MpfsOperation asyncMove(MpfsUser uid, String srcPath, String dstPath, boolean overwrite,
            String callbackUrl)
    {
        return null;
    }

    @Override
    public Option<MpfsFileInfo> getFileInfoOByUidAndPath(MpfsUser uid, String path, ListF<String> metaFields) {
        return null;
    }

    @Override
    public Option<MpfsFileInfo> getFileInfoOByFileId(MpfsUser owner, String fileId) {
        return null;
    }

    @Override
    public Option<MpfsFileInfo> getFileInfoOByFileId(MpfsUser uid, String owner, String fileId) {
        return null;
    }

    @Override
    public MpfsFileInfo getFileInfoByFileId(MpfsUser owner, String fileId) {
        return null;
    }

    @Override
    public MpfsFileInfo getFileInfoByFileId(MpfsUser uid, String owner, String fileId) {
        return null;
    }

    @Override
    public MpfsFileInfo getFileInfoByCommentId(MpfsUser uid, String entityType, String entityId) {
        return null;
    }

    @Override
    public MpfsFileInfo getPublicInfo(String hash, Option<MpfsUser> uid) {
        return null;
    }

    @Override
    public Option<MpfsFileInfo> getFileInfoOByPrivateHash(String privateHash) {
        return null;
    }

    @Override
    public Option<MpfsCommentsPermissions> getCommentsPermissions(MpfsUser uid, String entityType, String entityId) {
        return null;
    }

    @Override
    public Option<String> getSharedFolderPathByUidAndGroupId(MpfsUser uid, String groupId) {
        return null;
    }

    @Override
    public String getPublicFileAddress(String privateHash) {
        return null;
    }

    @Override
    public MpfsUserInfo getUserInfoObj(MpfsUser uid) {
        return null;
    }

    @Override
    public ListF<MpfsUserServiceInfo> getUserServiceInfoList(MpfsUser uid) {
        return null;
    }

    @Override
    public MapF<String, String> getDefaultFolders(MpfsUser uid) {
        return null;
    }

    @Override
    public String getUserInfo(MpfsUser uid) {
        return null;
    }

    @Override
    public Option<MapF<String, MapF<String, String>>> getUserSettingsO(MpfsUser uid) {
        return null;
    }

    @Override
    public MpfsCallbackResponse getOfficeStoreInfo(String resourceId, String accessToken, String accessTokenTtl,
            MapF<String, String> headers)
    {
        return null;
    }

    @Override
    public Option<MpfsGroupUids> getShareUidsInGroupO(MpfsUser owner, String fileId) {
        return null;
    }

    @Override
    public MpfsGroupUids getShareUidsInGroup(MpfsUser uid, String owner, String fileId) {
        return null;
    }

    @Override
    public Option<MpfsLentaBlockFileIds> getLentaBlocksFileIds(MpfsUser uid, String resourceId, String mediaType,
            MpfsUid modifier, int mtimeGte, int mtimeLte, int amount)
    {
        return null;
    }

    @Override
    public Option<MpfsLentaBlockFullDescription> getLentaBlockFilesData(MpfsUser uid, String resourceId,
            String mediaType, MpfsUid modifier, int mtimeGte, int mtimeLte, int amount)
    {
        return null;
    }

    @Override
    public Option<MpfsLentaBlockFullDescription> getLentaBlockFilesData(MpfsUser uid, String resourceId,
            String mediaType, MpfsUid modifier, int mtimeGte, int mtimeLte, int amount, String meta)
    {
        return null;
    }

    @Override
    public String getMarkMulcaIdToRemoveUri(MulcaId mulcaId) {
        return null;
    }

    @Override
    public String getFullRelativeTreeUri(String uid, String path) {
        return null;
    }

    @Override
    public String getFullRelativeTreePublicUri(String hash, boolean showNda) {
        return null;
    }

    @Override
    public String getAlbumResourcesListUri(String publicKey, Option<String> uid) {
        return null;
    }

    @Override
    public String getFilesListUri(String mpfsOid, String uid) {
        return null;
    }

    @Override
    public String getOfficeStoreUri(String resourceId, String accessToken, String accessTokenTtl) {
        return null;
    }

    @Override
    public String getKladunDownloadCounterIncUri(String hash, long bytes) {
        return null;
    }

    @Override
    public String getCommentsPermissionsUri(MpfsUser uid, String entityType, String entityId) {
        return null;
    }

    @Override
    public String getShareFolderInfoUri(MpfsUser uid, String groupId) {
        return null;
    }

    @Override
    public String getFileInfoUriByHid(MpfsHid hid) {
        return null;
    }

    @Override
    public UriData getFileInfoUriByUidAndPath(String uid, String path, MapF<String, Object> params, ListF<String> metaFields) {
        return null;
    }

    @Override
    public String getFileInfoUriByFileId(String uid, String owner, String fileId) {
        return null;
    }

    @Override
    public String getFileInfoUriByCommentId(MpfsUser uid, String entityType, String entityId) {
        return null;
    }

    @Override
    public String getResourceInfoUri(String privateHash) {
        return null;
    }

    @Override
    public String getPublicFileAddressUri(String privateHash) {
        return null;
    }

    @Override
    public String getUserInfoUri(String uid) {
        return null;
    }

    @Override
    public String getServiceListUri(String uid) {
        return null;
    }

    @Override
    public String getShareUidsInGroupUri(MpfsUser uid, String owner, String fileId) {
        return null;
    }

    @Override
    public String getLentaBlocksFileIdsUri(MpfsUser uid, String resourceId, String mediaType, MpfsUid modifier,
            int mtimeGte, int mtimeLte, int amount)
    {
        return null;
    }

    @Override
    public String getLentaBlockDataUri(MpfsUser uid, String resourceId, String mediaType, MpfsUid modifier,
            int mtimeGte, int mtimeLte, int amount, String metaFields)
    {
        return null;
    }

    @Override
    public void setMpfsHost(String mpfsHost) {

    }

    @Override
    public boolean isMpfsHost(String host) {
        return false;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public MpfsCallbackResponse shareInviteUser(MpfsUser uid, String realPath, int rightsInt, String login) {
        return null;
    }

    @Override
    public void rejectInvite(MpfsUser uid, String hash) {

    }

    @Override
    public String acceptInvite(MpfsUser uid, String hash) {
        return null;
    }

    @Override
    public int mobileSubscribe(MpfsUser uid, String token, Option<String> allow, String resources) {
        return 0;
    }

    @Override
    public int mobileUnsubscribe(MpfsUser uid, String token) {
        return 0;
    }

    @Override
    public String generateZaberunUrl(String stid, String fileName, String urlType, Option<String> uid,
            Option<String> md5, Option<String> contentType, Option<String> hash, Option<String> parser,
            Option<String> hid, Option<String> mediaType, Option<String> size, Option<Integer> limit,
            Option<Integer> fsize, Option<Integer> expireSeconds, Option<Integer> crop, Option<Boolean> inline,
            Option<Boolean> eternal)
    {
        return null;
    }

    @Override
    public MpfsCallbackResponse initNotes(MpfsUser uid, String src, String dst, MapF<String, Object> metaParams) {
        return null;
    }

    @Override
    public boolean isQuickMoveEnabled(String uid) {
        return false;
    }

    @Override
    public Option<String> getFotkiAlbumItemUrl(PassportUid ownerUid, int albumId, String path) {
        return null;
    }

    @Override
    public Option<String> getFotkiAlbumUrl(PassportUid ownerUid, int albumId) {
        return null;
    }

    @Override
    public boolean arePhotosliceAlbumsEnabledSafe(MpfsUser uid) {
        return false;
    }

    @Override
    public void processInappReceipt(PassportUid uid, String packageName, String storeType, String currency,
            String receipt)
    {
    }

    @Override
    public Map<String, Boolean> getFeatureToggles(MpfsUser uid) {
        return Cf.map();
    }
}
