package ru.yandex.autotests.innerpochta.testopithecus.lizaapp.features;

import com.yandex.xplat.testopithecus.YandexLogin;
import com.yandex.xplat.testopithecus.common.UserAccount;
import org.jetbrains.annotations.NotNull;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;

/**
 * @author pavponn
 */
public class LizaYandexLogin implements YandexLogin {

    private InitStepsRule steps;

    public LizaYandexLogin(InitStepsRule steps) {
        this.steps = steps;
    }

    @Override
    public void loginWithYandexAccount(@NotNull UserAccount account) {
        steps.user().loginSteps().forAcc(new Account(account.getLogin(), account.getPassword())).logins();
    }
}
