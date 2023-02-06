package ru.yandex.market.checkout.pushapi.service.shop.settings;

import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.shop.entity.AllSettings;

public interface SettingsService {

    /**
     * Загружает и возвращает настройки всех магазинов, минуя кэши
     *
     * @return настройки всех магазинов
     */
    AllSettings reloadAllSettings();

    /**
     * Загружает и возвращает настройки одного магазина, минуя кэши
     * @param shopId айди магазина, настройки которого нужно вернуть
     * @return настройки одного магазина
     */
    Settings reloadSettings(long shopId);

    /**
     * Возвращает эакешированные настройки всех магазинов
     *
     * @return настройки всех магазинов
     */
    AllSettings getAllSettings();

    /**
     * Возвращает эакешированные настройки одного магазина
     *
     * @param shopId айди магазина, настройки которого надо вернуть
     * @return настройки одного магазина
     */
    Settings getSettings(long shopId);

    /**
     * Обновляет настройки магазина в промежуточном кэше
     *
     * @param shopId айди магазина, настройки которого надо обновить
     * @param settings новые настройки
     */
    void updateSettings(long shopId, Settings settings);
    
}
