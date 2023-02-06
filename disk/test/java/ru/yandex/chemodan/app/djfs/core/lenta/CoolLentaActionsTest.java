package ru.yandex.chemodan.app.djfs.core.lenta;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.client.DataapiMordaBlock;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.user.UserLocale;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.web.servlet.HttpServletRequestX;
import ru.yandex.misc.web.servlet.mock.MockHttpServletRequest;


public class CoolLentaActionsTest extends DjfsSingleUserTestBase {
    @Autowired
    private CoolLentaActions coolLentaActions;
    @Autowired
    private CoolLentaManager coolLentaManager;

    @Test
    public void emptyResultForOneBlock() {
        ListF<DjfsResourceId> resourceIds = createFolderWithFiles("/disk/folder1");

        DataapiMordaBlock block = new DataapiMordaBlock("1", "type1",
                resourceIds.get(0).getValue(), resourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                        Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block);


        LentaResultListPojo previewLinks = coolLentaActions
                .getBlockPreviews(Option.of(UID.asString()), UserLocale.RU.value(), "800x600", createRequestWithValidToken());

        Assert.isTrue(previewLinks.isEmpty);
        Assert.isFalse(previewLinks.items.isPresent());
    }

    @Test
    public void successfulResultForSeveralBlocks() {
        ListF<DjfsResourceId> firstBlockResourceIds = createFolderWithFiles("/disk/folder1");
        ListF<DjfsResourceId> secondBlockResourceIds = createFolderWithFiles("/disk/folder2");

        DataapiMordaBlock block1 = new DataapiMordaBlock("1", "type1",
                firstBlockResourceIds.get(0).getValue(), firstBlockResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now().minus(Duration.standardHours(71)),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block1);
        DataapiMordaBlock block2 = new DataapiMordaBlock("2", "type1",
                secondBlockResourceIds.get(0).getValue(), secondBlockResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now().minus(Duration.standardHours(71)),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block2);

        LentaResultListPojo previewLinks = coolLentaActions.getBlockPreviews(Option.of(UID.asString()), UserLocale.RU.value(), "800x600",
                createRequestWithValidToken());

        Assert.isFalse(previewLinks.isEmpty);
        Assert.isTrue(previewLinks.items.isPresent());
        Assert.sizeIs(2, previewLinks.items.get());
    }

    @Test
    public void emptyResultForEmptyResponseFromDataapi() {
        LentaResultListPojo previewLinks = coolLentaActions.getBlockPreviews(Option.of(UID.asString()), UserLocale.RU.value(), "800x600",
                createRequestWithValidToken());

        Assert.isTrue(previewLinks.isEmpty);
        Assert.isFalse(previewLinks.items.isPresent());
    }

    @Test
    public void ignoreBlockIfOneResourceNotFound() {
        ListF<DjfsResourceId> firstBlockResourceIds = createFolderWithFiles("/disk/folder1");

        ListF<DjfsResourceId> secondBlockResourceIds = createFolderWithNFiles("/disk/folder2", 1);
        secondBlockResourceIds.add(DjfsResourceId.cons(UID, DjfsFileId.random()));

        ListF<DjfsResourceId> thirdBlockResourceIds = createFolderWithFiles("/disk/folder3");

        DataapiMordaBlock block1 = new DataapiMordaBlock("1", "type1",
                firstBlockResourceIds.get(0).getValue(), firstBlockResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block1);
        DataapiMordaBlock block2 = new DataapiMordaBlock("2", "type1",
                secondBlockResourceIds.get(0).getValue(), secondBlockResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block2);
        DataapiMordaBlock block3 = new DataapiMordaBlock("3", "type1",
                thirdBlockResourceIds.get(0).getValue(), thirdBlockResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block3);

        LentaResultListPojo previewLinks = coolLentaActions.getBlockPreviews(Option.of(UID.asString()), UserLocale.RU.value(), "800x600",
                createRequestWithValidToken());

        Assert.isFalse(previewLinks.isEmpty);
        Assert.isTrue(previewLinks.items.isPresent());
        Assert.sizeIs(2, previewLinks.items.get());

        for (LentaResultItemPojo item : previewLinks.items.get()) {
            Assert.in(item.id, Cf.list("1", "3"));
        }
    }

    @Test
    public void wrongUidInResourceIdFromDataApi() {
        DjfsUid wrongUid = DjfsUid.cons(123123123);
        Assert.notEquals(wrongUid, UID);

        ListF<DjfsResourceId> blockResourceIds = Cf.list(
                DjfsResourceId.cons(wrongUid, DjfsFileId.random())
        );

        DataapiMordaBlock block = new DataapiMordaBlock("1", "type1",
                blockResourceIds.get(0).getValue(), blockResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block);
        LentaResultListPojo previewLinks =
                coolLentaActions.getBlockPreviews(Option.of(UID.asString()), UserLocale.RU.value(), "800x600",
                        createRequestWithValidToken());
        Assert.isTrue(previewLinks.isEmpty);
    }

    @Test(expected = LentaBlockPreviewPermissionDenied.class)
    public void invalidTokenReturns403() {
        ListF<DjfsResourceId> resourceIds = createFolderWithFiles("/disk/folder1");

        DataapiMordaBlock block = new DataapiMordaBlock("1", "type1",
                resourceIds.get(0).getValue(), resourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block);
        coolLentaActions.getBlockPreviews(Option.of(UID.asString()), UserLocale.RU.value(), "800x600",
                createRequestWithInvalidToken());
    }

    @Test(expected = LentaBlockPreviewMissingUid.class)
    public void noUidReturns400() {
        ListF<DjfsResourceId> resourceIds = createFolderWithFiles("/disk/folder1");

        DataapiMordaBlock block = new DataapiMordaBlock("1", "type1",
                resourceIds.get(0).getValue(), resourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block);
        coolLentaActions.getBlockPreviews(Option.empty(), UserLocale.RU.value(), "800x600",
                createRequestWithValidToken());
    }

    @Test
    public void getUidFromClientToken() {
        ListF<DjfsResourceId> resourceIds = createFolderWithFiles("/disk/folder1");

        DataapiMordaBlock block = new DataapiMordaBlock("1", "type1",
                resourceIds.get(0).getValue(), resourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block);
        LentaResultListPojo previewLinks = coolLentaActions.getBlockPreviews(Option.empty(), UserLocale.RU.value(), "800x600",
                createRequestWithValidTokenWithUid(UID));
        Assert.isTrue(previewLinks.isEmpty);
        Assert.isFalse(previewLinks.items.isPresent());
    }

    @Test
    public void addTitles() {
        ListF<DjfsResourceId> blockResourceIds = Cf.list(
                DjfsResourceId.cons(UID, DjfsFileId.random())
        );

        DataapiMordaBlock block = new DataapiMordaBlock("1", "type1",
                "best_resource_id",
                blockResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );

        LentaBlockInfoWithTitles blockInfoWithTitles = coolLentaManager.addTitles(UserLocale.RU, block);
        System.out.println(blockInfoWithTitles.title1);
        System.out.println(blockInfoWithTitles.title2);
        System.out.println(blockInfoWithTitles.photosliceLinkText);
        for (Integer i : Cf.range(0, 1000)) {
            LentaBlockInfoWithTitles blockInfoWithTitles2 = coolLentaManager.addTitles(UserLocale.RU, block);

            Assert.equals(blockInfoWithTitles.title1, blockInfoWithTitles2.title1);
            Assert.equals(blockInfoWithTitles.title2, blockInfoWithTitles2.title2);
            Assert.equals(blockInfoWithTitles.photosliceLinkText, blockInfoWithTitles2.photosliceLinkText);
        }
    }

    @Test
    public void successfulIfThereIsSingleResource() {
        ListF<DjfsResourceId> firstResourceIds = createFolderWithNFiles("/disk/folder1", 1);
        ListF<DjfsResourceId> secondResourceIds = createFolderWithNFiles("/disk/folder2", 1);

        DataapiMordaBlock block1 = new DataapiMordaBlock("1", "type1",
                firstResourceIds.get(0).getValue(), firstResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block1);

        DataapiMordaBlock block2 = new DataapiMordaBlock("1", "type1",
                secondResourceIds.get(0).getValue(), secondResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block2);

        LentaResultListPojo previewLinks = coolLentaActions.getBlockPreviews(Option.of(UID.asString()), UserLocale.RU.value(), "800x600",
                createRequestWithValidToken());

        Assert.isFalse(previewLinks.isEmpty);
        Assert.isTrue(previewLinks.items.isPresent());
        Assert.sizeIs(2, previewLinks.items.get());
    }

    @Test
    public void emptyResultIfExceptionIsRaisedFromRoutine() {
        ListF<DjfsResourceId> firstBlockResourceIds = createFolderWithFiles("/disk/folder1");

        DataapiMordaBlock block1 = new DataapiMordaBlock("1", "type1",
                firstBlockResourceIds.get(0).getValue(), firstBlockResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block1);

        LentaResultListPojo previewLinks = coolLentaActions.getBlockPreviews(Option.of("123"), UserLocale.RU.value(), "800x600",
                createRequestWithValidToken());

        Assert.isTrue(previewLinks.isEmpty);
    }

    @Test
    public void emptyResultIfUnknownLocaleIsPassed() {
        ListF<DjfsResourceId> firstBlockResourceIds = createFolderWithFiles("/disk/folder1");

        DataapiMordaBlock block1 = new DataapiMordaBlock("1", "type1",
                firstBlockResourceIds.get(0).getValue(), firstBlockResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block1);

        LentaResultListPojo previewLinks = coolLentaActions.getBlockPreviews(Option.of(UID.asString()), "unknown", "800x600",
                createRequestWithValidToken());

        Assert.isTrue(previewLinks.isEmpty);
    }

    @Test
    public void emptyResultInvalidDimensionsIsPassed() {
        ListF<DjfsResourceId> firstBlockResourceIds = createFolderWithFiles("/disk/folder1");

        DataapiMordaBlock block1 = new DataapiMordaBlock("1", "type1",
                firstBlockResourceIds.get(0).getValue(), firstBlockResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block1);

        LentaResultListPojo previewLinks = coolLentaActions.getBlockPreviews(Option.of(UID.asString()), UserLocale.RU.value(), "AxB",
                createRequestWithValidToken());

        Assert.isTrue(previewLinks.isEmpty);
    }

    @Test
    public void emptyResultMalformedUidIsPassedInQS() {
        ListF<DjfsResourceId> firstBlockResourceIds = createFolderWithFiles("/disk/folder1");

        DataapiMordaBlock block1 = new DataapiMordaBlock("1", "type1",
                firstBlockResourceIds.get(0).getValue(), firstBlockResourceIds.map(DjfsResourceId::getValue),
                DateTime.parse("2007-12-03T00:00:00.00+0300").toInstant(),
                DateTime.parse("2007-12-04T00:00:00.00+0300").toInstant(),
                "Europe/Moscow",
                Instant.now(),
                Option.of(Instant.now()), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
        dataApiHttpClient.addNostalgyDataapiBlock(block1);

        LentaResultListPojo previewLinks = coolLentaActions.getBlockPreviews(Option.of("malformedUid"), UserLocale.RU.value(), "800x600",
                createRequestWithValidToken());

        Assert.isTrue(previewLinks.isEmpty);
    }

    @NotNull
    private ListF<DjfsResourceId> createFolderWithFiles(String folderPath) {
        return createFolderWithNFiles(folderPath, 5);
    }

    @NotNull
    private ListF<DjfsResourceId> createFolderWithNFiles(String folderPath, Integer numOfFiles) {
        filesystem.createFolder(PRINCIPAL, DjfsResourcePath.cons(UID, folderPath));

        ListF<DjfsResourceId> firstBlockResourceIds = Cf.arrayList();
        for (int i = 0; i < numOfFiles; ++i) {
            DjfsFileId fileId = DjfsFileId.random();
            String filename = "file-" + Random2.R.nextAlnum(5) + ".txt";
            filesystem.createFile(PRINCIPAL, DjfsResourcePath.cons(UID, folderPath + "/" + filename),
                    x -> x.fileId(fileId).previewStid(UuidUtils.randomToHexString())
            );
            firstBlockResourceIds.add(DjfsResourceId.cons(UID, fileId));
        }
        return firstBlockResourceIds;
    }

    private HttpServletRequestX createRequestWithValidToken() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "ClientToken token=" + "valid-token");
        return new HttpServletRequestX(req);
    }

    private HttpServletRequestX createRequestWithInvalidToken() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "ClientToken token=" + "invalid-token");
        return new HttpServletRequestX(req);
    }

    private HttpServletRequestX createRequestWithValidTokenWithUid(DjfsUid uid) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "ClientToken token=" + "valid-token" + ";uid=" + uid.asString());
        return new HttpServletRequestX(req);
    }
}
