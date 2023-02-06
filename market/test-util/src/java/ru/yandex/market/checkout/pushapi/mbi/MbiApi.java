package ru.yandex.market.checkout.pushapi.mbi;

import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.shop.entity.AllSettings;


public interface MbiApi {
    
    AllSettings loadSettings();
    Settings loadSettings(long shopId);
    
}
