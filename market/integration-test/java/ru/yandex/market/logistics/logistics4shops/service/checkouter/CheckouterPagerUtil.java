package ru.yandex.market.logistics.logistics4shops.service.checkouter;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.checkout.common.rest.Pager;

@UtilityClass
public class CheckouterPagerUtil {

    @Nonnull
    public Pager defaultPager() {
        return PagerUtils.getDefaultPager();
    }
}
