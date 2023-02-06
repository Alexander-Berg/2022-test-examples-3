package ru.yandex.direct.core.testing.steps.campaign.model0.strategy;

import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;

/**
 * Настройки ручного управления ставками
 */
public class ManualStrategy extends AbstractStrategy implements Strategy {

    /**
     * Раздельное управление ставками на поиске и в рекламных сетях
     */
    private boolean separateBids;

    /**
     * Конкретная ручная стратегия
     */
    private ManualStrategyMode manualStrategyMode;

    /**
     * Дневной бюджет
     */
    private DayBudget dayBudget;

    private CampaignsPlatform platform;


    @Override
    public boolean isAutobudget() {
        return false;
    }


    public boolean isSeparateBids() {
        return separateBids;
    }

    public void setSeparateBids(boolean separateBids) {
        this.separateBids = separateBids;
    }

    public ManualStrategyMode getManualStrategyMode() {
        return manualStrategyMode;
    }

    public void setManualStrategyMode(
            ManualStrategyMode manualStrategyMode) {
        this.manualStrategyMode = manualStrategyMode;
    }

    public DayBudget getDayBudget() {
        return dayBudget;
    }

    public void setDayBudget(DayBudget dayBudget) {
        this.dayBudget = dayBudget;
    }

    public ManualStrategy withSeparateBids(boolean separateBids) {
        this.separateBids = separateBids;
        return this;
    }

    public ManualStrategy withManualStrategyMode(
            ManualStrategyMode manualStrategyMode) {
        this.manualStrategyMode = manualStrategyMode;
        return this;
    }

    public ManualStrategy withDayBudget(DayBudget dayBudget) {
        this.dayBudget = dayBudget;
        return this;
    }

    public CampaignsPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(CampaignsPlatform platform) {
        this.platform = platform;
        if (platform != CampaignsPlatform.BOTH && separateBids) {
            separateBids = false;
        }
    }

    public ManualStrategy withPlatform(CampaignsPlatform platform) {
        setPlatform(platform);
        return this;
    }
}
