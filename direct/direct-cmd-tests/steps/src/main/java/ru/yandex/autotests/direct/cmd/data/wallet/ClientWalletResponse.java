package ru.yandex.autotests.direct.cmd.data.wallet;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.commons.Wallet;

public class ClientWalletResponse {

    @SerializedName("wallet")
    private Wallet wallet;

    @SerializedName("autopay")
    private AutoPay autoPay;

    public Wallet getWallet() {
        return wallet;
    }

    public ClientWalletResponse withWallet(Wallet wallet) {
        this.wallet = wallet;
        return this;
    }

    public AutoPay getAutoPay() {
        return autoPay;
    }

    public ClientWalletResponse withAutoPay(AutoPay autoPay) {
        this.autoPay = autoPay;
        return this;
    }
}
