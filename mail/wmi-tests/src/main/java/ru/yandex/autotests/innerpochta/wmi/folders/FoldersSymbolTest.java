package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderSymbolObj.Symbols;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.FolderMatchers.hasSpecialValue;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.FolderMatchers.hasSymbol;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderSymbolObj.Symbols.UNKNOWN;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderSymbolObj.symbolOn;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderRemoveSymbol.removeSymbol;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderSetSymbol.settingsFolderSetSymbol;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001;

/**
 * User: vicdev
 * Date: 14.04.14
 * Time: 17:51
 */
@Aqua.Test
@Title("Папки «Архив», «Шаблоны» и «Скидки»")
@Features(MyFeatures.WMI)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "FoldersSymbol")
@Issues({
        @Issue("DARIA-22684"),
        @Issue("DARIA-35593"),
        @Issue("DARIA-36497")
})
@RunWith(Parameterized.class)
public class FoldersSymbolTest extends BaseTest {

    @Rule
    public DeleteFoldersRule folderDelete = DeleteFoldersRule.with(authClient).all();

    @Rule
    public FolderRule folderRule = new FolderRule();

    private class FolderRule extends ExternalResource {

        private SettingsFolderCreate result;

        public String fid() {
            return result.errorcodeShouldBeEmpty().updated();
        }

        @Override
        protected void before() throws IOException {
            result = newFolder(randomAlphabetic(8))
                    .post().via(authClient.authHC());
        }

    }

    @Parameterized.Parameters(name = "Symbol-{0}")
    public static Collection<Symbols> data() throws Exception {
        return asList(
                Symbols.ARCHIVE,
                Symbols.DISCOUNT,
                Symbols.TEMPLATE
        );
    }

    @Parameterized.Parameter
    public Symbols symbols;

    @Test
    public void setSymbolOnNewFolder() throws IOException {
        String fid = folderRule.fid();

        settingsFolderSetSymbol(symbolOn(fid, symbols.value()))
                .post().via(hc).errorcodeShouldBeEmpty().shouldBe().updated(is("ok"));
        assertThat(hc, allOf(
                hasSpecialValue(fid, is(symbols.special())),
                hasSymbol(fid, equalTo(symbols.name().toLowerCase()))
        ));
    }   //TODO Несколько шаблонов или архивов будет низя - [WMI-678]

    @Test
    @Title("Должны вернуть ошибку при попытке поставить символ второй раз")
    public void setSymbolTwiceOnNewFolder() throws IOException {
        String fid = folderRule.fid();

        settingsFolderSetSymbol(symbolOn(fid, symbols.value()))
                .post().via(hc).errorcodeShouldBeEmpty().shouldBe().updated(is("ok"))
                .then().post().via(hc)
                .shouldBe().errorcode(INVALID_ARGUMENT_5001);

        assertThat(hc, hasSpecialValue(fid, is(symbols.special())));
    }

    @Test
    @Title("Должны удалить символ с произвольной папки с символом")
    public void rmSymbol() throws IOException {
        String fid = folderRule.fid();

        settingsFolderSetSymbol(symbolOn(fid, symbols.value())).post().via(hc);
        assumeThat(hc, hasSpecialValue(fid, is(symbols.special())));
        removeSymbol(fid).post().via(hc);
        assertThat(hc, hasSpecialValue(fid, is(UNKNOWN.special())));
    }

    @Test
    @Issue("DARIA-40245")
    @Title("Пробуем поставить символ архив дважды на разные папки")
    public void setSymbolTwice() throws IOException {
        String fid = folderRule.fid();

        settingsFolderSetSymbol(symbolOn(fid, symbols.value()))
                .post().via(hc).errorcodeShouldBeEmpty().shouldBe().updated(is("ok"));
        assertThat(hc, allOf(
                hasSpecialValue(fid, is(symbols.special())),
                hasSymbol(fid, equalTo(symbols.name().toLowerCase()))
        ));

        String fid2 = newFolder(Util.getRandomString()).post().via(hc).updated();

        settingsFolderSetSymbol(symbolOn(fid2, symbols.value()))
                .post().via(hc)
                .shouldBe().errorcode(WmiConsts.WmiErrorCodes.DB_UNKNOWN_ERROR_1000);

        assertThat(hc, IsNot.not(allOf(
                        hasSpecialValue(fid2, is(symbols.special())),
                        hasSymbol(fid2, equalTo(symbols.name().toLowerCase())))
        ));
    }

    @Test
    @Issue("DARIA-22684")
    @Title("Не должны поставить символ на «Отправленные»")
    public void setSymbolOnSystemFldrGetError() throws IOException {
        String fid = folderList.sentFID();
        settingsFolderSetSymbol(symbolOn(fid, symbols.value()))
                .post().via(hc).errorcode(INVALID_ARGUMENT_5001);
    }
}
