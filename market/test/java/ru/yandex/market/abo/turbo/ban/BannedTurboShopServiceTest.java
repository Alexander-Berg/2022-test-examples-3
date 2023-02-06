package ru.yandex.market.abo.turbo.ban;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.turbo.ban.BannedTurboShopService;
import ru.yandex.market.abo.turbo.ban.model.TurboShopBanReason;
import ru.yandex.market.abo.turbo.ban.model.BannedTurboShop;
import ru.yandex.market.abo.util.entity.DeletableEntityService;
import ru.yandex.market.abo.util.entity.DeletableEntityServiceTest;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 17.09.2020
 */
public class BannedTurboShopServiceTest extends DeletableEntityServiceTest<BannedTurboShop, String> {

    private static final String HOST = "test-turbo.ru";
    private static final String COMMENT = "Турбо-плохиш";

    @Autowired
    private BannedTurboShopService bannedTurboShopService;

    @Override
    protected DeletableEntityService<BannedTurboShop, String> service() {
        return bannedTurboShopService;
    }

    @Override
    protected String extractId(BannedTurboShop entity) {
        return entity.getHost();
    }

    @Override
    protected BannedTurboShop newEntity() {
        return new BannedTurboShop(HOST, TurboShopBanReason.PINGER_PRICE, COMMENT);
    }

    @Override
    protected BannedTurboShop example() {
        return new BannedTurboShop();
    }
}
