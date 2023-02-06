#pragma once

#include <market/library/currency_exchange/currency_exchange.h>
#include <market/library/price_converter/price_converter.h>

static NMarketReport::TCurrencyIdentities GetCurrenciesFake() {
    NMarketReport::TCurrencyIdentities currencies;
    currencies[Market::NCurrency::TCurrency::Rur()] = 0;
    currencies[Market::NCurrency::TCurrency::Byr()] = 1;
    currencies[Market::NCurrency::TCurrency::Kzt()] = 2;
    currencies[Market::NCurrency::TCurrency::Uah()] = 3;
    currencies[Market::NCurrency::TCurrency::Ue()]  = 4;

    return currencies;
}

class TestCurrencyExchange : public Market::NCurrency::ICurrencyExchange {
public:
    TestCurrencyExchange() {}

    double GetRate(const TString& /*bank*/, const Market::NCurrency::TCurrency currencyFrom, const Market::NCurrency::TCurrency currencyTo) const override {
        std::map<TString, double> rates;
        rates["RUR_RUR"] = 1.0;
        rates["RUR_KZT"] = 5.0/1.0;
        rates["RUR_UE"] = 1.0/30.0;

        rates["KZT_KZT"] = 1.0;
        rates["KZT_RUR"] = 1.0/5.0;

        rates["UAH_UAH"] = 1.0;
        rates["UAH_RUR"] = 5.0/1.0;

        auto key = TString(currencyFrom.AlphaCode()) + TString("_") + TString(currencyTo.AlphaCode());
        auto it = rates.find(key);
        if (it == rates.end())
            return 1.0;

        return it->second;
    }

    TString GetBankByCurrency(const Market::NCurrency::TCurrency currency) const override {
        if (currency == Market::NCurrency::TCurrency::Rur())
            return "CBRF";
        if (currency == Market::NCurrency::TCurrency::Uah())
            return "BNU";
        return "XXX";
    }

    std::string bank_by_currency(const std::string&) const override {
        return{};
    };

    TMaybe<Market::NCurrency::TCurrency> GetCurrencyByAlias(const TString&) const override {
        return Nothing();
    }
};
