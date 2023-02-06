package ru.yandex.chemodan.app.docviewer;

import java.net.URL;

import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.app.docviewer.cleanup.CleanupManager;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.chemodan.app.docviewer.copy.DocumentSourceInfo;
import ru.yandex.chemodan.app.docviewer.copy.UriHelper;
import ru.yandex.chemodan.app.docviewer.states.StartManager;
import ru.yandex.chemodan.app.docviewer.states.State;
import ru.yandex.chemodan.app.docviewer.states.StateMachine;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.inside.mulca.MulcaClient;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class TestManager {

    @Autowired
    private StartManager startManager;
    @Autowired
    private StateMachine stateMachine;
    @Autowired
    private UriHelper urihelper;
    @Autowired
    private CleanupManager cleanupManager;
    @Autowired
    private MulcaClient mulcaClient;

    public void cleanupUri(PassportUidOrZero uid, String originalUrl) {
        ActualUri uri = urihelper.rewrite(DocumentSourceInfo.builder().originalUrl(originalUrl).uid(uid).build());
        cleanupManager.cleanupByActualUri(uri);
    }


    public String makeAvailable(PassportUidOrZero uid, URL originalUrl, TargetType targetType) {
        return makeAvailable(uid, UriUtils.toUrlString(originalUrl), targetType);
    }

    public String makeAvailable(PassportUidOrZero uid, String originalUrl, TargetType targetType) {
        return makeAvailable(uid, originalUrl, Option.empty(), targetType);
    }

    public String makeAvailableWithShowNda(PassportUidOrZero uid, String originalUrl, TargetType targetType) {
        return makeAvailable(uid, originalUrl, Option.empty(), targetType, true);
    }

    public String makeAvailable(
            PassportUidOrZero uid, URL originalUrl, Option<String> archivePath, TargetType targetType)
    {
        return makeAvailable(uid, UriUtils.toUrlString(originalUrl), archivePath, targetType);
    }

    public String makeAvailable(
            PassportUidOrZero uid, String originalUrl, Option<String> archivePath, TargetType targetType)
    {
        return makeAvailable(uid, originalUrl, archivePath, targetType, false);
    }


    public String makeAvailable(
            PassportUidOrZero uid, String originalUrl,
            Option<String> archivePath, TargetType targetType, boolean showNda)
    {
        State state = waitUriToComplete(uid, originalUrl, archivePath, targetType, showNda);
        Assert.A.equals(State.AVAILABLE, state);
        ActualUri uri = urihelper.rewrite(DocumentSourceInfo.builder().originalUrl(originalUrl).uid(uid)
                .archivePath(archivePath).showNda(showNda).build());
        return stateMachine.getFileId(uri).get();
    }


    public State waitUriToComplete(PassportUidOrZero uid, String originalUrl, TargetType targetType) {
        return waitUriToComplete(uid, originalUrl, Option.empty(), targetType);
    }

    public State waitUriToComplete(PassportUidOrZero uid, String originalUrl,
            Option<String> archivePath, TargetType targetType)
    {
        return waitUriToComplete(uid, originalUrl, archivePath, targetType, false);
    }


    public State waitUriToComplete(PassportUidOrZero uid, String originalUrl,
            Option<String> archivePath, TargetType targetType, boolean showNda)
    {
        ActualUri uri = urihelper.rewrite(
                DocumentSourceInfo.builder().originalUrl(originalUrl).uid(uid)
                        .archivePath(archivePath).showNda(showNda).build());
        cleanupManager.cleanupByActualUri(uri);
        return waitUriToCompleteNoCleanup(uid, originalUrl, archivePath, targetType, showNda);
    }


    public State waitUriToCompleteNoCleanup(PassportUidOrZero uid, URL originalUrl, TargetType targetType) {
        return waitUriToCompleteNoCleanup(uid, UriUtils.toUrlString(originalUrl), targetType);
    }

    public State waitUriToCompleteNoCleanup(PassportUidOrZero uid, String originalUrl, TargetType targetType) {
        return waitUriToCompleteNoCleanup(uid, originalUrl, Option.empty(), targetType);
    }

    public State waitUriToCompleteNoCleanup(
            PassportUidOrZero uid, String originalUrl, Option<String> archivePath,
            TargetType targetType)
    {
        return waitUriToCompleteNoCleanup(uid, originalUrl, archivePath, targetType, false);
    }

    public State waitUriToCompleteNoCleanup(
            PassportUidOrZero uid, String originalUrl, Option<String> archivePath,
            TargetType targetType, boolean showNda)
    {
        return startManager.startAndWaitUntilComplete(
                DocumentSourceInfo.builder().originalUrl(originalUrl).uid(uid)
                        .archivePath(archivePath).showNda(showNda).build(),
                Option.empty(), targetType, Duration.standardMinutes(3), true);
    }

    public void withUploadedToMulcaFile(URL fileToUpload, boolean asMessage, Function1V<MulcaId> handler) {
        withUploadedToMulcaFile(File2.fromFileUrl(fileToUpload), asMessage, handler);
    }

    public void withUploadedToMulcaFile(InputStreamSource in, boolean asMessage, Function1V<MulcaId> handler) {
        MulcaId mulcaId = mulcaClient.upload(in, "tmp", asMessage);
        try {
            handler.apply(mulcaId);
        } finally {
            mulcaClient.delete(mulcaId);
        }
    }

}
