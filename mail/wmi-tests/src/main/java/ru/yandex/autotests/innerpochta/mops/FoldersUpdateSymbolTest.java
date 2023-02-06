package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereFolder.hasFolderWithSymbol;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.internalError;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okEmptyJson;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 18.11.15
 * Time: 16:47
 */
@Aqua.Test
@Title("[MOPS] Папки «Архив», «Шаблоны» и «Скидки»")
@Features(MyFeatures.MOPS)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "MopsFoldersUpdateSymbolTest")
@Issues({@Issue("DARIA-49310")})
@RunWith(Parameterized.class)
public class FoldersUpdateSymbolTest extends MopsBaseTest {
    @Rule
    public DeleteFoldersRule folderDelete = DeleteFoldersRule.with(authClient).all()
            .symbol(Symbol.ARCHIVE).symbol(Symbol.DISCOUNT).symbol(Symbol.TEMPLATE).after(true);

    @Rule
    public FolderRule folderRule = new FolderRule();
    @Parameterized.Parameter
    public Symbol symbol;

    @Parameterized.Parameters(name = "Symbol-{0}")
    public static Collection<Symbol> data() throws Exception {
        return asList(
                Symbol.ARCHIVE,
                Symbol.DISCOUNT,
                Symbol.TEMPLATE
        );
    }

    @Test
    @Issue("DARIA-53577")
    @Title("Должны поставить и удалить символ с папки")
    public void setAndRemoveSymbolOnNewFolder() {
        val fid = folderRule.fid();
        shouldSetSymbol(fid, symbol);
        shouldRemoveSymbol(fid);
    }

    @Test
    @Title("Должны вернуть ошибку при попытке поставить символ второй раз")
    public void setSymbolTwiceOnNewFolder() throws Exception {
        val fid = folderRule.fid();
        shouldSetSymbol(fid, symbol);
        updateFolderSymbol(fid).withSymbol(symbol.toString()).post(shouldBe(invalidRequest()));

        assertThat(authClient, hasFolderWithSymbol(fid, symbol));
    }

    @Test
    @Issue("DARIA-40245")
    @Title("Пробуем поставить символ архив дважды на разные папки")
    public void setSymbolTwice() throws IOException {
        val fid = folderRule.fid();
        shouldSetSymbol(fid, symbol);

        val fid2 = newFolder(getRandomString());
        updateFolderSymbol(fid2).withSymbol(symbol.toString()).post(shouldBe(internalError()));

        assertThat(authClient, not(hasFolderWithSymbol(fid2, symbol)));
    }

    @Test
    @Issue("DARIA-22684")
    @Title("Не должны поставить символ на «Отправленные»")
    public void setSymbolOnSystemFolderShouldSeeInvalidRequest() throws IOException {
        shouldNotSetSymbolOnSystemFolder(folderList.defaultFID());
        shouldNotSetSymbolOnSystemFolder(folderList.sentFID());
        shouldNotSetSymbolOnSystemFolder(folderList.draftFID());
        shouldNotSetSymbolOnSystemFolder(folderList.deletedFID());
        shouldNotSetSymbolOnSystemFolder(folderList.spamFID());

    }

    @Step("Не должны поставить символ на системную папку")
    private void shouldNotSetSymbolOnSystemFolder(String fid) {
        val expectedError = String.format("can't change symbol name for fid: %s: symbol already exist %s: can not modify folder",
                fid, folderList.symbolByFid(fid));
        updateFolderSymbol(fid).withSymbol(symbol.toString()).post(shouldBe(invalidRequest(containsString(expectedError))));
    }

    @Step("Должны успешно поставить символ на папку")
    private void shouldSetSymbol(String fid, Symbol symbol) {
        updateFolderSymbol(fid).withSymbol(symbol.toString()).post(shouldBe(okEmptyJson()));
        assertThat(updatedFolderList().symbolByFid(fid), equalTo(symbol.name().toLowerCase()));
        assertThat(authClient, hasFolderWithSymbol(fid, symbol));
    }

    @Step("Должны успешно удалить символ с папки")
    private void shouldRemoveSymbol(String fid) {
        updateFolderSymbol(fid).post(shouldBe(okEmptyJson()));
        assertThat(authClient, not(hasFolderWithSymbol(fid, symbol)));
    }

    private class FolderRule extends ExternalResource {
        private String fid;

        public String fid() {
            return fid;
        }

        @Override
        protected void before() throws IOException {
            fid = newFolder(getRandomString());
        }
    }
}