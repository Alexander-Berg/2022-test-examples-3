package ru.yandex.market.antifraud.orders.yt;

import lombok.SneakyThrows;

import ru.yandex.market.antifraud.orders.config.YtClientConfig;
import ru.yandex.market.antifraud.orders.storage.dao.yt.AntifraudYtDao;
import ru.yandex.yt.ytclient.proxy.YtClient;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 07.02.2020
 */
public class AntifraudYtClientTestClass {
    private AntifraudYtDao antifraudYtDao;

    public AntifraudYtClientTestClass(AntifraudYtDao antifraudYtDao) {
        this.antifraudYtDao = antifraudYtDao;
    }

    public static void main(String[] args) {
        YtClient ytAntifraudClient = new YtClientConfig().ytAntifraudClient(
                new String[]{"hahn"},
                "robot-mrkt-antfrd",
                "");
        AntifraudYtClientTestClass ytClientPerformanceTestClass =
                new AntifraudYtClientTestClass(new AntifraudYtDao(ytAntifraudClient));
        ytClientPerformanceTestClass.load();
    }

    @SneakyThrows
    private void load() {
        Long[] ids = new Long[]{815818741L, 689564780L, 690377960L};
        for (var id : ids) {
            var accountState = antifraudYtDao.getPassportFeaturesByUid(id).join();
            accountState.ifPresent(System.out::println);
        }
    }
}
