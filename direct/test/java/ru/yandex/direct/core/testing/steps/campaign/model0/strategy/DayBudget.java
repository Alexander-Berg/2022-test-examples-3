package ru.yandex.direct.core.testing.steps.campaign.model0.strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Настройки дневного бюджета
 */
public class DayBudget {

    /**
     * Максимальный размер дневного бюджета
     */
    private BigDecimal dayBudget;

    /**
     * Тип распределения дневного бюджета:
     * вести показы непрерывно до тех пор, пока бюджет не кончится,
     * или же при необходимости периодически приостанавливать показы,
     * чтобы распределить их равномерно в течение всего дня
     */
    private DayBudgetShowMode showMode;

    /**
     * Счетчик изменений бюджета за день
     * (дневной бюджет разрешено изменять не более 3х раз в сутки)
     */
    private Long dailyChangeCount;

    /**
     * Время приостановки кампании по дневному ограничению бюджета или 0,
     * если кампания не приостановлена
     */
    private LocalDateTime stopTime;

    /**
     * Признак, показывающий отправляли ли мы письмо об остановке кампании по дневному бюджету.
     * Письма рассылаются скриптом ppcSendPausedByDayBudget.pl
     */
    private Boolean stopNotificationSent;

    public BigDecimal getDayBudget() {
        return dayBudget;
    }

    public void setDayBudget(BigDecimal dayBudget) {
        this.dayBudget = dayBudget;
    }

    public DayBudgetShowMode getShowMode() {
        return showMode;
    }

    public void setShowMode(DayBudgetShowMode showMode) {
        this.showMode = showMode;
    }

    public Long getDailyChangeCount() {
        return dailyChangeCount;
    }

    public void setDailyChangeCount(Long dailyChangeCount) {
        this.dailyChangeCount = dailyChangeCount;
    }

    public LocalDateTime getStopTime() {
        return stopTime;
    }

    public void setStopTime(LocalDateTime stopTime) {
        this.stopTime = stopTime;
    }

    public Boolean getStopNotificationSent() {
        return stopNotificationSent;
    }

    public void setStopNotificationSent(Boolean stopNotificationSent) {
        this.stopNotificationSent = stopNotificationSent;
    }

    public DayBudget withDayBudget(BigDecimal dayBudget) {
        this.dayBudget = dayBudget;
        return this;
    }

    public DayBudget withShowMode(DayBudgetShowMode showMode) {
        this.showMode = showMode;
        return this;
    }

    public DayBudget withDailyChangeCount(Long dailyChangeCount) {
        this.dailyChangeCount = dailyChangeCount;
        return this;
    }

    public DayBudget withStopTime(LocalDateTime stopTime) {
        this.stopTime = stopTime;
        return this;
    }

    public DayBudget withStopNotificationSent(Boolean stopNotificationSent) {
        this.stopNotificationSent = stopNotificationSent;
        return this;
    }
}
