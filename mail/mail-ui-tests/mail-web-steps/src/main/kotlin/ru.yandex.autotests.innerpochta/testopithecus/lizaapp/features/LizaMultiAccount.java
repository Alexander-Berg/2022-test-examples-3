package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.MultiAccount;
import org.jetbrains.annotations.NotNull;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author pavponn
 */
public class LizaMultiAccount implements MultiAccount {

    private InitStepsRule steps;

    public LizaMultiAccount(final InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public void switchToAccount(@NotNull String login) {
        // TODO это надо будет убрать когда мы научимся понимать, какой алиас стоит сейчас у юзера
        Pattern p = Pattern.compile("([0-9]{5})(-)([0-9]{5})");
        Matcher m = p.matcher(login);
        if (m.find()) {
            login = m.replaceFirst("$1.$3");
        }
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().mail().home().userMenuDropdown().userList(), login);
    }

    @Override
    public void addNewAccount() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().mail().home().userMenuDropdown().addUserButton());
    }

    @NotNull
    @Override
    public List<String> getLoggedInAccountsList() {
        return steps.pages().mail().home().userMenuDropdown().userList()
            .stream()
            .map(element -> element.getAttribute("data-user-login"))
            .collect(Collectors.toList());
    }

    @Override
    public void logoutFromAccount(@NotNull String login) {

    }

    @NotNull
    @Override
    public String getCurrentAccount() {
        return null;
    }

    @Override
    public int getNumberOfAccounts() {
        return 0;
    }
}
