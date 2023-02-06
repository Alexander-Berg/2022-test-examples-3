package ru.yandex.autotests.direct.cmd.util;

import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.Strategy;
import ru.yandex.autotests.direct.utils.beans.MongoBeanLoader;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;

import java.math.RoundingMode;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CmdStrategyBeans {
    private static Currency DEFAULT_TEMPLATE_CURRENCY = Currency.RUB;

    private static final String TEMPLATE_COLLECTION_NAME = DirectTestRunProperties.getInstance()
            .getDirectCmdMongoTemplatesCollection();

    private static final String TEMPLATE_PREFIX = "model.strategy.";

    private static String getTemplateName(Strategies strategies) {
        return TEMPLATE_PREFIX + strategies.name();
    }

    public static CampaignStrategy getStrategyBean(Strategies strategies) {
        MongoBeanLoader<CampaignStrategy> loader = new MongoBeanLoader<>(CampaignStrategy.class, TEMPLATE_COLLECTION_NAME);
        CampaignStrategy strategy = loader.getBean(getTemplateName(strategies));
        if(strategy.getIsNetStop() == null || strategy.getIsNetStop().isEmpty()) {
            strategy.setIsNetStop("0");
        }

        if(strategy.getIsSearchStop() == null || strategy.getIsSearchStop().isEmpty()) {
            strategy.setIsSearchStop("0");
        }
        if ("stop".equals(strategy.getSearch().getName())) {
            strategy.withIsSearchStop("1");
        }
        return strategy;
    }

    public static CampaignStrategy getStrategyBean(Strategies strategies, Currency currency) {
        CampaignStrategy strategy = getStrategyBean(strategies);
        convertCampaignStrategy(strategy, currency);
        return strategy;
    }

    public static void convertCampaignStrategy(CampaignStrategy strategy, Currency currency) {
        if(currency == Currency.RUB) {
            return;
        }
        Strategy bean = strategy.getSearch();
        convertString(currency, bean::getAvgBid, bean::setAvgBid);
        convertString(currency, bean::getAvgCpa, bean::setAvgCpa);
        convertDouble(currency, bean::getAvgCpi, bean::setAvgCpi);
        convertString(currency, bean::getSum, bean::setSum);
        convertString(currency, bean::getBid, bean::setBid);
        convertDouble(currency, bean::getFilterAvgBid, bean::setFilterAvgBid);
        convertDouble(currency, bean::getFilterAvgCpa, bean::setFilterAvgCpa);

        Strategy contextBean = strategy.getNet();
        convertString(currency, contextBean::getAvgBid, contextBean::setAvgBid);
        convertString(currency, contextBean::getAvgCpa, contextBean::setAvgCpa);
        convertDouble(currency, contextBean::getAvgCpi, contextBean::setAvgCpi);
        convertString(currency, contextBean::getSum, contextBean::setSum);
        convertString(currency, contextBean::getBid, contextBean::setBid);
        convertDouble(currency, contextBean::getFilterAvgBid, contextBean::setFilterAvgBid);
        convertDouble(currency, contextBean::getFilterAvgCpa, contextBean::setFilterAvgCpa);

    }

    private static void convertDouble(Currency currency, Supplier<Double> getter, Consumer<Double> setter) {
        Double value = getter.get();
        if(value == null) {
            return;
        }
        Double newVal = Money.valueOf(value, DEFAULT_TEMPLATE_CURRENCY)
                .convert(currency).setScale(2, RoundingMode.FLOOR).doubleValue();
        setter.accept(newVal);
    }

    private static void convertString(Currency currency, Supplier<String> getter, Consumer<String> setter) {
        String value = getter.get();
        if(value == null || "".equals(value)) {
            return;
        }
        Double newVal = Money.valueOf(Double.parseDouble(value), DEFAULT_TEMPLATE_CURRENCY)
                .convert(currency).setScale(2, RoundingMode.FLOOR).doubleValue();
        setter.accept(newVal.toString());
    }

    private static void convertFloat(Currency currency, Supplier<Float> getter, Consumer<Float> setter) {
        Float value = getter.get();
        if(value == null) {
            return;
        }
        Double newVal = Money.valueOf(value, DEFAULT_TEMPLATE_CURRENCY)
                .convert(currency).setScale(2, RoundingMode.FLOOR).doubleValue();
        setter.accept(newVal.floatValue());
    }

}
