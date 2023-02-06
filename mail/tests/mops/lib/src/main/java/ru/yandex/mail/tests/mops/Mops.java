package ru.yandex.mail.tests.mops;

import com.google.common.base.Joiner;
import lombok.val;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.tests.mops.generated.ApiMops;
import ru.yandex.mail.tests.mops.generated.changetab.ApiChangeTab;
import ru.yandex.mail.tests.mops.generated.complexmove.ApiComplexMove;
import ru.yandex.mail.tests.mops.generated.folders.create.ApiFoldersCreate;
import ru.yandex.mail.tests.mops.generated.folders.update.ApiFoldersUpdate;
import ru.yandex.mail.tests.mops.generated.folders.updatepop3.ApiFoldersUpdatePop3;
import ru.yandex.mail.tests.mops.generated.folders.updateposition.ApiFoldersUpdatePosition;
import ru.yandex.mail.tests.mops.generated.folders.updatesymbol.ApiFoldersUpdateSymbol;
import ru.yandex.mail.tests.mops.generated.label.ApiLabel;
import ru.yandex.mail.tests.mops.generated.labels.create.ApiLabelsCreate;
import ru.yandex.mail.tests.mops.generated.labels.delete.ApiLabelsDelete;
import ru.yandex.mail.tests.mops.generated.labels.update.ApiLabelsUpdate;
import ru.yandex.mail.tests.mops.generated.mark.ApiMark;
import ru.yandex.mail.tests.mops.generated.ping.ApiPing;
import ru.yandex.mail.tests.mops.generated.purge.ApiPurge;
import ru.yandex.mail.tests.mops.generated.remove.ApiRemove;
import ru.yandex.mail.tests.mops.generated.spam.ApiSpam;
import ru.yandex.mail.tests.mops.generated.stat.ApiStat;
import ru.yandex.mail.tests.mops.generated.unlabel.ApiUnlabel;
import ru.yandex.mail.tests.mops.generated.unspam.ApiUnspam;
import ru.yandex.mail.tests.mops.generated.unsubscribe.ApiUnsubscribe;
import ru.yandex.mail.tests.mops.source.Source;
import ru.yandex.mail.tests.mops.generated.folders.delete.ApiFoldersDelete;


import java.util.List;

import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.mops.MopsResponses.ok;


public class Mops {
    public static ApiMops apiMops(String ticket) {
        return MopsApi.apiMops(MopsProperties.mopsProperties().mopsUri(), ticket);
    }

    public static ApiComplexMove complexMove(UserCredentials user, String destFid, Source source) throws Exception {
        val api = apiMops(user.account().tvmTicket()).complexMove()
                .withUid(user.account().uid())
                .withDestFid(destFid);
        source.fill(api);
        return api;
    }

    public static ApiFoldersCreate createFolder(UserCredentials user, String name) {
        return apiMops(user.account().tvmTicket()).folders().create()
                .withUid(user.account().uid())
                .withName(name);
    }

    public static String newFolder(UserCredentials user, String name) {
        return createFolder(user, name).post(shouldBe(ok())).then().extract().body().path("fid");
    }

    public static String newFolder(UserCredentials user, String name, String parentFid) {
        return createFolder(user, name).withParentFid(parentFid).post(shouldBe(ok()))
                .then().extract().body().path("fid");
    }

    public static ApiFoldersDelete deleteFolder(UserCredentials user, String fid) {
        return apiMops(user.account().tvmTicket()).folders().delete()
                .withUid(user.account().uid())
                .withFid(fid);
    }

    public static ApiFoldersUpdate updateFolder(UserCredentials user, String fid) {
        return apiMops(user.account().tvmTicket()).folders().update()
                .withUid(user.account().uid())
                .withFid(fid);
    }

    public static ApiFoldersUpdate renameFolder(UserCredentials user, String fid, String name) {
        return updateFolder(user, fid).withName(name);
    }

    public static ApiFoldersUpdatePop3 updatePop3(UserCredentials user, String... fids) {
        return apiMops(user.account().tvmTicket()).folders().updatePop3()
                .withUid(user.account().uid())
                .withFids(Joiner.on(",").join(fids));
    }

    public static ApiFoldersUpdatePosition updateFolderPosition(UserCredentials user, String fid) {
        return apiMops(user.account().tvmTicket()).folders().updatePosition()
                .withUid(user.account().uid())
                .withFid(fid);
    }

    public static ApiFoldersUpdateSymbol updateFolderSymbol(UserCredentials user, String fid) {
        return apiMops(user.account().tvmTicket()).folders().updateSymbol()
                .withUid(user.account().uid())
                .withFid(fid);
    }

    public static ApiLabelsCreate createLabel(UserCredentials user) {
        return apiMops(user.account().tvmTicket()).labels().create()
                .withUid(user.account().uid());
    }

    public static String newLabelBySymbol(UserCredentials user, String symbol) {
        return createLabel(user).withSymbol(symbol).post(shouldBe(ok()))
                .then().extract().body().path("lid");
    }

    public static String newLabelByName(UserCredentials user, String name) {
        return createLabel(user).withName(name).post(shouldBe(ok()))
                .then().extract().body().path("lid");
    }

    public static String newLabelByName(UserCredentials user, String name, String color) {
        return createLabel(user).withName(name).withColor(color).post(shouldBe(ok()))
                .then().extract().body().path("lid");
    }

    public static ApiLabelsDelete deleteLabel(UserCredentials user, String lid) {
        return apiMops(user.account().tvmTicket()).labels().delete()

                .withUid(user.account().uid())
                .withLid(lid);
    }

    public static ApiLabelsUpdate updateLabel(UserCredentials user, String lid) {
        return apiMops(user.account().tvmTicket()).labels().update()
                .withUid(user.account().uid())
                .withLid(lid);
    }

    public static ApiLabelsUpdate changeLabelColor(UserCredentials user, String lid, String color) {
        return updateLabel(user, lid).withColor(color);
    }

    public static ApiLabel label(UserCredentials user, Source source, List<String> lids) throws Exception {
        val api = apiMops(user.account().tvmTicket()).label()
                .withUid(user.account().uid())
                .withLids(Joiner.on(",").join(lids));
        source.fill(api);
        return api;
    }

    public static ApiUnlabel unlabel(UserCredentials user, Source source, List<String> lids) throws Exception {
        val api = apiMops(user.account().tvmTicket()).unlabel()
                .withUid(user.account().uid())
                .withLids(Joiner.on(",").join(lids));
        source.fill(api);
        return api;
    }

    public static ApiMark mark(UserCredentials user, Source source, ApiMark.StatusParam status) throws Exception {
        val api = apiMops(user.account().tvmTicket()).mark()
                .withUid(user.account().uid())
                .withStatus(status);
        source.fill(api);
        return api;
    }

    public static ApiPurge purge(UserCredentials user, Source source) throws Exception {
        val api = apiMops(user.account().tvmTicket()).purge()
                .withUid(user.account().uid());
        source.fill(api);
        return api;
    }

    public static ApiRemove remove(UserCredentials user, Source source) throws Exception {
        val api = apiMops(user.account().tvmTicket()).remove()
                .withUid(user.account().uid());
        source.fill(api);
        return api;
    }

    public static ApiSpam spam(UserCredentials user, Source source) throws Exception {
        val api = apiMops(user.account().tvmTicket()).spam()
                .withUid(user.account().uid());
        source.fill(api);
        return api;
    }

    public static ApiUnspam unspam(UserCredentials user, Source source) throws Exception {
        val api = apiMops(user.account().tvmTicket()).unspam()
                .withUid(user.account().uid());
        source.fill(api);
        return api;
    }

    public static ApiUnsubscribe unsubscribe(UserCredentials user, List<String> mids) throws Exception {
        return apiMops(user.account().tvmTicket()).unsubscribe()
                .withUid(user.account().uid())
                .withMids(Joiner.on(",").join(mids));
    }

    public static ApiChangeTab changeTab(UserCredentials user, Source source, String tab) throws Exception {
        val api = apiMops(user.account().tvmTicket()).changeTab()
                .withUid(user.account().uid())
                .withTab(tab);
        source.fill(api);
        return api;
    }

    public static ApiStat stat(UserCredentials user) {
        return apiMops(user.account().tvmTicket())
                .stat()
                .withUid(user.account().uid());
    }

    public static ApiStat stat(UserCredentials user, String uid) {
        return apiMops(user.account().tvmTicket())
                .stat()
                .withUid(uid);
    }

    public static ApiPing ping() {
        return apiMops("").ping();
    }
}
