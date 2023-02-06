package ru.yandex.autotests.innerpochta.data;

import ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.yandex.autotests.innerpochta.wmi.core.Common.toParameterized;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.CommonUtils.getMidsOfAllMsgsInFolder;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 30.05.13
 * Time: 22:11
 */
public class B2BMessages {
    public static List<String> getMids(String loginGroup) throws Exception {
        HttpClientManagerRule manager4 = auth().with(loginGroup).login();
        List<String> folderIds = jsx(FolderList.class).post().via(manager4.authHC()).getAllFolderIds();

        folderIds.remove("2360000090042631685"); //Exclude

        List<String> mids = new ArrayList<>();
        for (String fid : folderIds) {
            mids.addAll(getMidsOfAllMsgsInFolder(fid, manager4.authHC()));
        }
        return mids;
    }

    public static List<Object[]> messagesId(String loginGroup) throws Exception {
        return new ArrayList<>(toParameterized(getMids(loginGroup)));
    }

    public static DocumentCompareMatcher excludeNodes(DocumentCompareMatcher matcher) {
        return matcher.exclude("//timer_mulca")
                .exclude("//timer_db")
                .exclude("//timer_logic")
                .exclude("//timestamp")
                .exclude("//actual-version")
                .exclude("//reply_to_all")
                .exclude("//facts")
                .excludeAll(props().getB2bUselessNodes().split(","));
    }
}
