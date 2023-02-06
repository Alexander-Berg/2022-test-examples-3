package ru.yandex.autotests.direct.httpclient.util;

import com.google.common.base.CaseFormat;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.Strategy;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.matchers.BeanCompareStrategy;
import ru.yandex.autotests.direct.utils.model.PerlBoolean;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.direct.utils.strategy.data.StrategiesResource;
import ru.yandex.autotests.direct.utils.strategy.data.StrategyType;
import ru.yandex.autotests.direct.utils.strategy.objects.CampaignStrategyInfoWeb;
import ru.yandex.autotests.direct.utils.strategy.objects.StrategyInfoWeb;
import ru.yandex.autotests.directapi.common.api45.CampaignContextStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer.matchvariation.DefaultMatchVariation;
import ru.yandex.autotests.irt.testutils.beandiffer.matchvariation.MatchVariation;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static ru.yandex.autotests.irt.testutils.beans.BeanHelper.getProperty;
import static ru.yandex.autotests.irt.testutils.beans.BeanHelper.setNotNullProperty;
import static ru.yandex.autotests.irt.testutils.matchers.NumberApproximatelyEqual.approxEqualTo;

public class StrategyHelper {

    private static final float MONEY_AVAILABLE_DIFFERENCE = 0.01f;

    public static ru.yandex.autotests.directapi.common.api45.CampaignStrategy toCampaignStrategy(CampaignStrategyInfoWeb bean) {
        ru.yandex.autotests.directapi.common.api45.CampaignStrategy result =
                new ru.yandex.autotests.directapi.common.api45.CampaignStrategy();
        result.setGoalID(bean.getGoalID());
        result.setAverageCPA(bean.getAverageCPA() == null ? null : bean.getAverageCPA().floatValue());
        result.setAveragePrice(bean.getAveragePrice() == null ? null : bean.getAveragePrice().floatValue());
        result.setClicksPerWeek(bean.getClicksPerWeek());
        result.setMaxPrice(bean.getMaxPrice() == null ? null : bean.getMaxPrice().floatValue());
        result.setProfitability(bean.getProfitability() == null ? null : bean.getProfitability().floatValue());
        result.setReserveReturn(bean.getReserveReturn());
        result.setROICoef(bean.getROICoef() == null ? null : bean.getROICoef().floatValue());
        result.setStrategyName(bean.getStrategyName());
        result.setWeeklySumLimit(bean.getWeeklySumLimit() == null ? null : bean.getWeeklySumLimit().floatValue());
        return result;
    }

    public static CampaignContextStrategy toCampaignContextStrategy(StrategyInfoWeb strategyInfoWeb) {
        CampaignStrategyInfoWeb bean = strategyInfoWeb.getCampaignContextStrategy();
        CampaignContextStrategy result = new CampaignContextStrategy();
        result.setGoalID(bean.getGoalID());
        result.setAverageCPA(bean.getAverageCPA() == null ? null : bean.getAverageCPA().floatValue());
        result.setAveragePrice(bean.getAveragePrice() == null ? null : bean.getAveragePrice().floatValue());
        result.setClicksPerWeek(bean.getClicksPerWeek());
        result.setMaxPrice(bean.getMaxPrice() == null ? null : bean.getMaxPrice().floatValue());
        result.setProfitability(bean.getProfitability() == null ? null : bean.getProfitability().floatValue());
        result.setReserveReturn(bean.getReserveReturn());
        result.setROICoef(bean.getROICoef() == null ? null : bean.getROICoef().floatValue());
        result.setStrategyName(bean.getStrategyName());
        result.setWeeklySumLimit(bean.getWeeklySumLimit() == null ? null : bean.getWeeklySumLimit().floatValue());
        result.setContextLimit(bean.getContextLimit());
        result.setContextLimitSum(bean.getContextLimitSum());
        result.setContextPricePercent(bean.getContextPricePercent());
        return result;
    }

    public static CampaignStrategyInfoWeb toCampaignStrategyInfoWeb(CampaignContextStrategy bean) {
        if(bean == null) {
            return null;
        }
        CampaignStrategyInfoWeb result = new CampaignStrategyInfoWeb();
        result.setGoalID(bean.getGoalID());
        result.setAverageCPA(bean.getAverageCPA() == null ? null : bean.getAverageCPA().doubleValue());
        result.setAveragePrice(bean.getAveragePrice() == null ? null : bean.getAveragePrice().doubleValue());
        result.setClicksPerWeek(bean.getClicksPerWeek());
        result.setMaxPrice(bean.getMaxPrice() == null ? null : bean.getMaxPrice().doubleValue());
        result.setProfitability(bean.getProfitability() == null ? null : bean.getProfitability().doubleValue());
        result.setReserveReturn(bean.getReserveReturn());
        result.setROICoef(bean.getROICoef() == null ? null : bean.getROICoef().doubleValue());
        result.setStrategyName(bean.getStrategyName());
        result.setWeeklySumLimit(bean.getWeeklySumLimit() == null ? null : bean.getWeeklySumLimit().doubleValue());
        result.setContextLimit(bean.getContextLimit());
        result.setContextLimitSum(bean.getContextLimitSum());
        result.setContextPricePercent(bean.getContextPricePercent());
        return result;
    }

    public static <T> void convertStrategyField(T bean, String fieldName, Currency currency) {
        if (getProperty(bean, fieldName) != null) {
            setNotNullProperty(bean, fieldName, Money.valueOf(Double.parseDouble(getProperty(bean, fieldName).toString())).
                    convert(currency).setScale(2, RoundingMode.FLOOR).doubleValue());
        }
    }

    public static String getErrorText(String errorType, String errorParam) {
        if (errorType == null)
            return "";
        if (errorParam.equals(""))
            return StrategiesResource.getResourceByName(errorType)
                    .toStringForLocale(DirectTestRunProperties.getInstance().getDirectCmdLocale());
        return String.format(StrategiesResource.getResourceByName(errorType)
                .toStringForLocale(DirectTestRunProperties.getInstance().getDirectCmdLocale()), errorParam);
    }

    public static void convertStrategyCurrency(StrategyInfoWeb strategy, Currency currency) {
        CampaignStrategyInfoWeb bean = strategy.getCampaignStrategy();
        convertStrategyField(bean, "maxPrice", currency);
        convertStrategyField(bean, "averagePrice", currency);
        convertStrategyField(bean, "weeklySumLimit", currency);
        convertStrategyField(bean, "averageCPA", currency);
        convertStrategyField(bean, "averageCPI", currency);

        CampaignStrategyInfoWeb contextBean = strategy.getCampaignContextStrategy();
        convertStrategyField(contextBean, "maxPrice", currency);
        convertStrategyField(contextBean, "averagePrice", currency);
        convertStrategyField(contextBean, "weeklySumLimit", currency);
        convertStrategyField(contextBean, "averageCPA", currency);
        convertStrategyField(contextBean, "averageCPI", currency);

        convertStrategyField(strategy, "dayBudgetSum", currency);
    }

    private static Strategy convertToStrategy(CampaignStrategyInfoWeb src) {
        Strategy strategy = new Strategy();
        String strategyName = src.getStrategyType().getRadioKey();
        if (strategyName == null || strategyName.isEmpty()) {
            strategyName = "default";
        }

        strategyName = strategyName.replace("-", "_");
        strategy.setName(strategyName);
        strategy.setPlace(src.getStrategyType().getSubRadioKey());
        if ("clicks".equals(src.getStrategyType().getSubRadioKey()) ||
                "cpa-conv".equals(src.getStrategyType().getSubRadioKey()) ||
                "cpa".equals(src.getStrategyType().getSubRadioKey())) {
            strategy.setPlace(null);
        }
        if (src.getStrategyType().getSubRadioKey() == null || src.getStrategyType().getSubRadioKey().isEmpty()) {
            strategy.setPlace(null);
        }
        if (src.getMaxPrice() != null) {
            strategy.setBid(String.valueOf(src.getMaxPrice()));
        }
        if (src.getAveragePrice() != null) {
            strategy.setAvgBid(String.valueOf(src.getAveragePrice()));
        }
        if (src.getAverageCPA() != null) {
            strategy.setAvgCpa(String.valueOf(src.getAverageCPA()));
        }
        if (src.getWeeklySumLimit() != null) {
            strategy.setSum(String.valueOf(src.getWeeklySumLimit()));
        }
        if (src.getGoalID() != null) {
            strategy.setGoalId(String.valueOf(src.getGoalID()));
        }
        strategy.setProfitability(src.getProfitability());
        strategy.setReserveReturn(src.getReserveReturn());
        strategy.setRoiCoef(src.getROICoef());
        if (src.getClicksPerWeek() != null) {
            strategy.setLimitClicks(String.valueOf(src.getClicksPerWeek()));
        }
        switch (src.getStrategyType()) {
            case WEEKLY_PACKET_OF_CLICKS:
                if (strategy.getBid() == null) {
                    strategy.setBid("");
                }
                if (strategy.getAvgBid() == null) {
                    strategy.setAvgBid("");
                }
                break;
            case WEEKLY_BUDGET_MAX_CLICKS:
                strategy.setGoalId("");
                break;
        }
        return strategy;
    }

    public static CampaignStrategy convertToFormParameters(StrategyInfoWeb strategyInfo) {
        CampaignStrategy campaignStrategy = new CampaignStrategy();
        campaignStrategy.withIsNetStop(PerlBoolean.ZERO.toString());
        Strategy net = convertToStrategy(strategyInfo.getCampaignContextStrategy());
        net.setAvgCpi(strategyInfo.getCampaignContextStrategy().getAverageCPI());
        Strategy search = convertToStrategy(strategyInfo.getCampaignStrategy());
        search.setAvgCpi(strategyInfo.getCampaignStrategy().getAverageCPI());
        if (strategyInfo.getCampaignStrategy().getIsIndependentControlStrategy()) {
            campaignStrategy.setName("different_places");

        } else {
            campaignStrategy.setName("");
        }

        campaignStrategy.setNet(net);
        campaignStrategy.setSearch(search);
        if (net.getName().equals("stop")) {
            campaignStrategy.withIsNetStop(PerlBoolean.ONE.toString());
        }
        return campaignStrategy;
    }

    private static void convertToResponseParameters(Strategy strategy) {
        if (strategy.getGoalId() != null && strategy.getGoalId().equals("")) {
            strategy.setGoalId(null);
        }
        if (strategy.getBid() != null && strategy.getBid().equals("")) {
            strategy.setBid(null);
        }
        if (strategy.getAvgBid() != null) {
            strategy.setAvgBid(strategy.getAvgBid());
        }
        if (strategy.getBid() != null) {
            strategy.setBid(BigDecimal.valueOf(Float.valueOf(strategy.getBid())).setScale(1, RoundingMode.FLOOR).toString());
        }
        if (strategy.getLimitClicks() != null) {
            strategy.setLimitClicks(strategy.getLimitClicks());
        }
        if (strategy.getGoalId() != null) {
            strategy.setGoalId(strategy.getGoalId().toString());
        }
        if (strategy.getAvgCpa() != null) {
            Float avgCpa = BigDecimal.valueOf(Float.valueOf(strategy.getAvgCpa()) + MONEY_AVAILABLE_DIFFERENCE)
                    .setScale(2, RoundingMode.CEILING).floatValue();
            strategy.setAvgCpa(avgCpa.toString());
        }
    }

    public static CampaignStrategy convertToResponseParameters(StrategyInfoWeb strategyInfo, Currency currency) {
        convertStrategyCurrency(strategyInfo, currency);
        CampaignStrategy campaignStrategy = convertToFormParameters(strategyInfo);
        convertToResponseParameters(campaignStrategy.getSearch());
        convertToResponseParameters(campaignStrategy.getNet());
        return campaignStrategy;

    }

    public static MatchVariation getCampaignStrategyResponseMatchVariation(CampaignStrategy campaignStrategy) {
        MatchVariation matchVariation = new DefaultMatchVariation();
        if (campaignStrategy.getSearch().getSum() != null) {
            matchVariation.forFields("search/sum").useMatcher(approxEqualTo(
                    Float.valueOf(campaignStrategy.getSearch().getSum())).withDifference(MONEY_AVAILABLE_DIFFERENCE));
        }
        if (campaignStrategy.getNet().getSum() != null) {
            matchVariation.forFields("net/sum").useMatcher(approxEqualTo(
                    Float.valueOf(campaignStrategy.getNet().getSum())).withDifference(MONEY_AVAILABLE_DIFFERENCE));
        }
        return matchVariation;

    }

    public static BeanCompareStrategy getCompareStrategy(Object strategy) {
        Double diff = 0.01;
        BeanCompareStrategy compareStrategy = new BeanCompareStrategy();

        putFieldMatcher(compareStrategy, strategy, "maxPrice", diff);
        putFieldMatcher(compareStrategy, strategy, "averagePrice", diff);
        putFieldMatcher(compareStrategy, strategy, "weeklySumLimit", diff);
        putFieldMatcher(compareStrategy, strategy, "averageCPA", diff);
        return compareStrategy;
    }

    public static void putFieldMatcher(BeanCompareStrategy compareStrategy, Object strategy, String fieldName, Double diff) {
        if (getProperty(strategy, fieldName) != null) {
            compareStrategy.putFieldMatcher(fieldName, approxEqualTo(
                    (Double) getProperty(strategy, fieldName)).withDifference(diff));
        }
    }

    public static CampaignStrategyInfoWeb convertStrategyToCampaignStrategyInfoWeb(Strategy strategy) {
        CampaignStrategyInfoWeb strategyInfo = new CampaignStrategyInfoWeb();

        switch (strategy.getName()) {
            case "autobudget":
                if (strategy.getGoalId() != null) {
                    strategyInfo.setStrategyType(StrategyType.WEEKLY_BUDGET_MAX_CONVERSION);
                } else {
                    strategyInfo.setStrategyType(StrategyType.WEEKLY_BUDGET_MAX_CLICKS);
                }
                break;
            case "autobudget_avg_cpa":
                strategyInfo.setStrategyType(StrategyType.AVERAGE_CPA_OPTIMIZATION);
                strategyInfo.setGoalID(0);
                break;
            default:
                strategyInfo.setStrategyType(StrategyType.getStrategyTypeByRadioKey(
                        strategy.getPlace() == null ?
                                strategy.getName().replace('_', '-') :
                                strategy.getName().replace('_', '-') + ":" + strategy.getPlace()));
        }

        strategyInfo.setROICoef(strategy.getRoiCoef());
        strategyInfo.setWeeklySumLimit(strategy.getSum());
        strategyInfo.setProfitability(strategy.getProfitability());
        strategyInfo.setReserveReturn(strategy.getReserveReturn());
        strategyInfo.setAverageCPI(strategy.getAvgCpi());

        if (strategy.getAvgCpa() != null) {
            strategyInfo.setAverageCPA( strategy.getAvgCpa() == null ? null : Double.valueOf(strategy.getAvgCpa()));
        }

        if (strategy.getGoalId() != null) {
            strategyInfo.setGoalID(Integer.valueOf(strategy.getGoalId()));
        }
        if (strategy.getAvgBid() != null) {
            strategyInfo.setAveragePrice(strategy.getAvgBid() == null ? null : Double.valueOf(strategy.getAvgBid()));
        }
        if (strategy.getBid() != null) {
            strategyInfo.setMaxPrice(strategy.getBid());
        }
        if (strategy.getLimitClicks() != null) {
            strategyInfo.setClicksPerWeek(Integer.valueOf(strategy.getLimitClicks()));
        }
        return strategyInfo;
    }

    public static CampaignContextStrategy convertStrategyToCampaignContextStrategy(Strategy strategy) {
        CampaignContextStrategy campaignContextStrategy = new CampaignContextStrategy();

        switch (strategy.getName()) {
            case "autobudget_week_bundle":
                campaignContextStrategy.setStrategyName(StrategyType.WEEKLY_PACKET_OF_CLICKS.getApiKey());
                break;
            case "autobudget_avg_click":
                campaignContextStrategy.setStrategyName(StrategyType.AVERAGE_PRICE.getApiKey());
                break;
            case "autobudget":
                if (strategy.getGoalId() != null) {
                    campaignContextStrategy.setStrategyName(StrategyType.WEEKLY_BUDGET_MAX_CONVERSION.getApiKey());
                } else {
                    campaignContextStrategy.setStrategyName(StrategyType.WEEKLY_BUDGET_MAX_CLICKS.getApiKey());
                }
                break;
            case "autobudget_avg_cpa":
                campaignContextStrategy.setStrategyName(StrategyType.AVERAGE_CPA_OPTIMIZATION.getApiKey());
                campaignContextStrategy.setGoalID(0);
                break;
            case "autobudget_roi":
                campaignContextStrategy.setStrategyName(StrategyType.ROI_OPTIMIZATION.getApiKey());
                break;
            case "autobudget_avg_cpi":
                campaignContextStrategy.setStrategyName("AverageCPIOptimization");
                campaignContextStrategy.setGoalID(0);
                break;
            default:
                campaignContextStrategy.setStrategyName(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, strategy.getName()));
        }

        campaignContextStrategy.setWeeklySumLimit(Float.valueOf(strategy.getSum()));
        campaignContextStrategy.setProfitability(strategy.getProfitability() == null ? null
                : strategy.getProfitability().floatValue());
        campaignContextStrategy.setReserveReturn(strategy.getReserveReturn());
        campaignContextStrategy.setROICoef(strategy.getRoiCoef() == null ? null : strategy.getRoiCoef().floatValue());

        if (strategy.getAvgCpa() != null) {
            campaignContextStrategy.setAverageCPA(Float.valueOf(strategy.getAvgCpa()));
        }

        if (strategy.getGoalId() != null) {
            campaignContextStrategy.setGoalID(Integer.valueOf(strategy.getGoalId()));
        }

        if (strategy.getLimitClicks() != null) {
            campaignContextStrategy.setClicksPerWeek(Integer.valueOf(strategy.getLimitClicks()));
        }
        if (strategy.getAvgBid() != null) {
            campaignContextStrategy.setAveragePrice(Float.valueOf(strategy.getAvgBid()));
        }
        if (strategy.getBid() != null) {
            campaignContextStrategy.setMaxPrice(Float.valueOf(strategy.getBid()));
        }
        return campaignContextStrategy;
    }
}
