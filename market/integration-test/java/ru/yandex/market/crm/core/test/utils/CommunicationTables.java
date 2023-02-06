package ru.yandex.market.crm.core.test.utils;

import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.core.yt.paths.YtFolders;

/**
 * Возможно, мы захотим вынести логику ограничения частоты коммуникаций в самостоятельный сервис.
 * Чтобы это было проще сделать, путь к таблице сразу был изолирован в отдельный класс конфигурации.
 * Если со временем станет понятно, что отдельный сервис создавать нет необходимости, данный класс
 * можно будет слить с каким-нибудь другим конфигом таблиц.
 *
 * @author zloddey
 */
@Component
public class CommunicationTables {
    private final YtFolders ytFolders;

    public CommunicationTables(YtFolders ytFolders) {
        this.ytFolders = ytFolders;
    }

    public YPath pushTable() {
        return ytFolders.getHome().child("throttle").child("push");
    }

    public YPath emailTable() {
        return ytFolders.getHome().child("throttle").child("email");
    }
}
