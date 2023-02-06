package ru.yandex.autotests.direct.cmd.steps.wallet;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.wallet.AjaxSaveWalletSettingsRequest;
import ru.yandex.autotests.direct.cmd.data.wallet.ClientWalletResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class WalletSteps extends DirectBackEndSteps {

    @Step("GET cmd = clientWallet (открыть общий счет клиента {0})")
    public ClientWalletResponse getClientWallet(String login) {
        return get(CMD.CLIENT_WALLET, new BasicDirectRequest().withUlogin(login), ClientWalletResponse.class);
    }

    @Step("POST cmd = clientWallet (изменить настройки ОС)")
    public CommonResponse postAjaxSaveWalletSettings(AjaxSaveWalletSettingsRequest request) {
        return post(CMD.AJAX_SAVE_WALLET_SETTINGS, request, CommonResponse.class);
    }

    @Step("POST cmd = enableWallet (включить общий счет клиента {0})")
    public void postEnableWallet(String login) {
        post(CMD.ENABLE_WALLET, new BasicDirectRequest().withUlogin(login), Void.class);
    }
}
