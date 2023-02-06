package ru.yandex.autotests.innerpochta.data;

import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.newapi.AccountsObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.newapi.Accounts;
import ru.yandex.autotests.innerpochta.wmi.core.oper.newapi.MessagesByFolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 03.06.15
 * Time: 17:01
 */
public class NewApiData {

    public static List<Object[]> apiHandles() {
        List<Object[]> operList = new ArrayList<>();
        operList.addAll(apiMessagesByFolder());
        operList.addAll(apiAccounts());

        return operList;
    }

    private static Collection<? extends Object[]> apiMessagesByFolder() {
        List<Object[]> operList = new ArrayList<>();
        operList.add(new Object[]{
                api(MessagesByFolder.class),
                new MessagesByFolderObj().setFirst("0").setCount("10").setPage("1"),
                MessagesByFolder.class.getSimpleName()
        });

        operList.add(new Object[]{
                api(MessagesByFolder.class),
                new MessagesByFolderObj().setFirst("0").setCount("10"),
                MessagesByFolder.class.getSimpleName()
        });
        return operList;
    }

    private static Collection<? extends Object[]> apiAccounts() {
        List<Object[]> operList = new ArrayList<>();
        operList.add(new Object[]{
                api(Accounts.class),
                new AccountsObj().silent(),
                Accounts.class.getSimpleName()
        });

        operList.add(new Object[]{
                api(Accounts.class),
                new AccountsObj().silent().emails(),
                Accounts.class.getSimpleName() + " + emails"
        });
        return operList;
    }
}
