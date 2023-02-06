package ru.yandex.autotests.innerpochta.utils;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import java.util.List;
import java.util.stream.Collectors;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

public class AccountRule extends TestWatcher {
    private List<User> senderUsers;
    private List<User> receiverUsers;

    @Override
    protected void starting(Description description) {
        with(description.getTestClass());
    }

    public AccountRule with(Class<?> clazz) {
        List<Account> senderAccounts = mxTestProps().senderAccounts(clazz);
        List<Account> receiverAccounts = mxTestProps().receiverAccounts(clazz);
        senderUsers = this.castAccountListToUserList(senderAccounts);
        receiverUsers = this.castAccountListToUserList(receiverAccounts);
        return this;
    }

    public User getSenderUser() { return senderUsers.get(0); }
    public User getReceiverUser() { return receiverUsers.get(0); }
    public List<User> getSenderUsers() { return senderUsers; }
    public List<User> getReceiverUsers() { return receiverUsers; }

    private List<User> castAccountListToUserList(List<Account> accounts) {
        return accounts.stream().map((account) -> new User(account.getLogin(), account.getPassword(), account.getDomain(), false)).collect(Collectors.toList());
    }
}
