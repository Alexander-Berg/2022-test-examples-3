package ru.yandex.autotests.innerpochta.hound;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FoldersObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Folders;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.*;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.innerpochta.beans.yplatform.FilterSearchMatchers.withEnvelopes;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okFid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand.filterSearch;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder.messagesByFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Folders.folders;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;


@Aqua.Test
@Title("[HOUND] Работа с hidden_trash")
@Description("Проверяем работу ручек со Скрытой Корзиной")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundHiddenTrash")
public class HiddenTrashTest extends BaseHoundTest {
    private String hiddenTrashFid = "";

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Before
    public void prepare() throws Exception {
        hiddenTrashFid = Mops.createHiddenTrash(authClient)
                .post(shouldBe(okFid()))
                .then().extract().body().path("fid");
    }

    @Test
    @Title("Ручка folders без with_hidden")
    @Description("Проверяем, что ручка не отдает Скрытую Корзину, если не передан параметр with_hidden")
    public void foldersHandlerWithoutWithHiddenParam() {
        Folders folders = folders(FoldersObj.empty().setUid(uid())).get().via(authClient);

        assertThat("Нашли Скрытую Корзину",
                folders.folders(), not(hasKey(equalTo(hiddenTrashFid))));
    }

    @Test
    @Title("Ручка folders c with_hidden")
    @Description("Проверяем, что ручка отдает Скрытую Корзину, если передан параметр with_hidden")
    public void foldersHandlerWithWithHiddenParam() {
        Folders folders = folders(FoldersObj.empty().setUid(uid()).withHidden()).get().via(authClient);

        assertThat("Не нашли Скрытую Корзину",
                folders.folders(), hasEntry(equalTo(hiddenTrashFid),
                hasProperty("symbolicName",
                        hasProperty("title",
                                equalTo(Symbol.HIDDEN_TRASH.toString())))));
    }

    @Test
    @Title("Должны включать в выдачу filter_search результаты из Скрытой Корзины")
    @Description("Два письма. Одно во входящих, другое в скрытой корзине. Ищем только в скрытой корзине, ожидая одно найденное")
    public void includeHiddenTrashWhenFilterFolderBySymbol() throws Exception {
        String hiddenMid = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String inboxMid = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        Mops.complexMove(authClient, hiddenTrashFid, new MidsSource(hiddenMid)).post(shouldBe(okSync()));

        FilterSearchObj params = FilterSearchObj.empty().setUid(uid()).setMids(hiddenMid, inboxMid);

        assertThat("Должны найти оба письма", filterSearch(params)
                        .get().via(authClient).parsed(),
                withEnvelopes((Matcher) hasSize(2)));

        assertThat("Должны найти только письмо в скрытой корзине",
                filterSearch(params.withInclFolders(Symbol.HIDDEN_TRASH))
                        .get().via(authClient)
                        .parsed(),
                withEnvelopes((Matcher) hasSize(1)));
    }

    @Test
    @Title("Должны включать информацию по Скрытой Корзине в выдачу filter_search")
    @Description("При запросе с параметром full_folders_and_labels должны получить информацию по скрытой корзине")
    public void returnHiddenTrashData() throws Exception {
        String hiddenMid = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        Mops.complexMove(authClient, hiddenTrashFid, new MidsSource(hiddenMid)).post(shouldBe(okSync()));

        FilterSearchObj params = FilterSearchObj.empty().setUid(uid()).setMids(hiddenMid).setFullFoldersAndLabels("1");

        assertThat("Должны найти оба письма",
                filterSearch(params).get().via(authClient).parsed().getEnvelopes(),
                hasItem(allOf(
                        hasProperty("mid",
                                equalTo(hiddenMid)),
                        hasProperty("folder",
                                hasProperty("symbolicName",
                                        hasProperty("title",
                                                equalTo(Symbol.HIDDEN_TRASH.toString())))))));
    }

    @Test
    @Title("Для Скрытой Корзины должен работать messages_by_folder")
    public void massagesByFolderForHiddenTrash() throws Exception {
        String hiddenMid = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        Mops.complexMove(authClient, hiddenTrashFid, new MidsSource(hiddenMid)).post(shouldBe(okSync()));

        MessagesByFolderObj params = MessagesByFolderObj.empty()
                .setUid(uid()).setFirst("0").setCount("1").setFid(hiddenTrashFid);

        assertThat("Должны получить письмо в hidden_trash",
                messagesByFolder(params).get().via(authClient).resp().getEnvelopes(),
                hasItem(hasProperty("mid", equalTo(hiddenMid))));
    }
}
