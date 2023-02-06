package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import lombok.Data;

/**
 * 1. Брак - 50
 * 2. Просроченный - 30
 * 3. Излишек - 70
 * 4. Карантин (LOST) - 40
 * 5. Годный - 10
 * 6. Карантин на основе Честного Знака (CIS_QUAR) - 115
 */
@Data
public class Stock {
    private int fit;
    private int expired;
    private int lost;
    private int damage;
    private int surplus;

    public static final int Fit = 10;
    public static final int Expired = 30;
    public static final int Lost = 40;
    public static final int Damage = 50;
    public static final int Surplus = 70;
    public static final int CisQuar = 115;

    public Stock(int fit, int expired, int lost, int damage, int surplus) {
        this.fit = fit;
        this.expired = expired;
        this.lost = lost;
        this.damage = damage;
        this.surplus = surplus;
    }

    public int getFit() {
        return fit;
    }

    public void setFit(int fit) {
        this.fit = fit;
    }

    public int getExpired() {
        return expired;
    }

    public void setExpired(int expired) {
        this.expired = expired;
    }

    public int getLost() {
        return lost;
    }

    public void setLost(int lost) {
        this.lost = lost;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getSurplus() {
        return surplus;
    }

    public void setSurplus(int surplus) {
        this.surplus = surplus;
    }

}
