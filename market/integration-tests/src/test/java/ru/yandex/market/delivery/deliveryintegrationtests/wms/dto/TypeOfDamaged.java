package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.returns.SkuCheckPage;

/**
 * Типы повреждений в ЦТЭ
 */
public enum TypeOfDamaged {
    DEFORMATION("ТРЕБУЕТ УТИЛИЗАЦИИ") {
        @Override
        public void selectDamage(SkuCheckPage skuCheckPage) {
            skuCheckPage.clickDeformation();
        }
    },
    SEVERE_DAMAGE("СИЛЬНЫЕ ПОВРЕЖДЕНИЯ") {
        @Override
        public void selectDamage(SkuCheckPage skuCheckPage) {
            skuCheckPage.clickSevereDamage();
        }
    };

    private final String state;

    TypeOfDamaged(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public abstract void selectDamage(SkuCheckPage skuCheckPage);
}
