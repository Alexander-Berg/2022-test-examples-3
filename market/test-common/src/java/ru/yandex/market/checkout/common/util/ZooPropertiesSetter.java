package ru.yandex.market.checkout.common.util;

import org.springframework.beans.factory.InitializingBean;

public class ZooPropertiesSetter implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        System.setProperty("zookeeper.forceSync", "no");
    }
}
