package ru.yandex.market.adv.shop.integration;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.shop.integration.checkouter.properties.MarketVendorClicksLogTableProperties;
import ru.yandex.market.adv.shop.integration.checkouter.properties.SendToCheckouterProperties;
import ru.yandex.market.adv.shop.integration.properties.YtPricelabsTableProperties;

/**
 * Date: 27.05.2022
 * Project: adv-shop-integration
 *
 * @author alexminakov
 */
public class ShopIntegrationTestRunner {

    @Autowired
    private YtPricelabsTableProperties ytPricelabsTableProperties;
    @Autowired
    private MarketVendorClicksLogTableProperties marketVendorClicksLogTableProperties;
    @Autowired
    private SendToCheckouterProperties sendToCheckouterProperties;

    public void run(String newPrefix, Runnable runnable) {
        String oldPrefix = ytPricelabsTableProperties.getPrefix();
        String oldOrderPrefix = ytPricelabsTableProperties.getOrderPrefix();
        String oldClicksPrefix = marketVendorClicksLogTableProperties.getPrefix();

        try {
            setPrefix("//tmp/" + newPrefix, "//tmp/" + newPrefix, "//tmp/" + newPrefix);
            runnable.run();
            setPrefix(oldPrefix, oldOrderPrefix, oldClicksPrefix);
        } catch (Throwable e) {
            setPrefix(oldPrefix, oldOrderPrefix, oldClicksPrefix);
            throw e;
        }
    }

    public void run(String newPrefix, long clickTableInterval, Runnable runnable) {
        String oldPrefix = ytPricelabsTableProperties.getPrefix();
        String oldOrderPrefix = ytPricelabsTableProperties.getOrderPrefix();
        String oldClicksPrefix = marketVendorClicksLogTableProperties.getPrefix();
        long oldInterval = marketVendorClicksLogTableProperties.getInterval();

        try {
            setPrefix("//tmp/" + newPrefix, "//tmp/" + newPrefix, "//tmp/" + newPrefix);
            marketVendorClicksLogTableProperties.setInterval(clickTableInterval);

            runnable.run();

            setPrefix(oldPrefix, oldOrderPrefix, oldClicksPrefix);
            marketVendorClicksLogTableProperties.setInterval(oldInterval);
        } catch (Throwable e) {
            setPrefix(oldPrefix, oldOrderPrefix, oldClicksPrefix);
            marketVendorClicksLogTableProperties.setInterval(oldInterval);
            throw e;
        }
    }

    private void setPrefix(String prefix, String orderPrefix, String clicksPrefix) {
        ytPricelabsTableProperties.setPrefix(prefix);
        ytPricelabsTableProperties.setOrderPrefix(orderPrefix);
        marketVendorClicksLogTableProperties.setPrefix(clicksPrefix);
    }
}
