package ru.yandex.market.adv.b2bmonetization.bonus.yt.repository;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.bonus.database.entity.BonusEntity;
import ru.yandex.market.adv.b2bmonetization.bonus.yt.entity.AdvProgramMultiBonus;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.yt.client.YtClientProxy;

@DisplayName("Тесты репозитория AdvProgramMultiBonusRepositoryImpl")
class AdvProgramMultiBonusRepositoryTest extends AbstractMonetizationTest {

    @Autowired
    private AdvProgramMultiBonusRepository repository;
    @Autowired
    private YtClientProxy ytClient;

    @DisplayName("Успешно сохранили информацию о новых бонусах в Yt-таблицу")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AdvProgramMultiBonus.class,
                    path = "//tmp/advProgramMultiBonusSuccess_adv_program_multi_bonus"
            ),
            after = "AdvProgramMultiBonusRepositoryTest/json/advProgramMultiBonus_emptyTable_success.after.json"
    )
    @Test
    void advProgramMultiBonus_emptyTable_success() {
        run("advProgramMultiBonusSuccess_",
                () -> ytClient.execInTransaction(tx -> {
                    repository.insert(
                            tx,
                            new BonusEntity(
                                    1L,
                                    1L,
                                    "INVOLVE_ALL",
                                    0L,
                                    1000,
                                    Instant.parse("2022-04-25T21:00:00Z"),
                                    Instant.parse("2022-04-30T21:00:00Z"),
                                    true
                            )
                    );
                    repository.insert(
                            tx,
                            new BonusEntity(
                                    2L,
                                    2L,
                                    "INVOLVE_ALL",
                                    0L,
                                    2000,
                                    Instant.parse("2022-04-26T12:00:00Z"),
                                    Instant.parse("2022-05-26T12:00:00Z"),
                                    true
                            )
                    );
                })
        );
    }

    @DisplayName("Успешно обновили информацию о существующем бонусе в Yt-таблице")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AdvProgramMultiBonus.class,
                    path = "//tmp/advProgramMultiBonusUpdated_adv_program_multi_bonus"
            ),
            before = "AdvProgramMultiBonusRepositoryTest/json/advProgramMultiBonus_existBonus_success.before.json",
            after = "AdvProgramMultiBonusRepositoryTest/json/advProgramMultiBonus_existBonus_success.after.json"
    )
    @Test
    void advProgramMultiBonus_existBonus_success() {
        run("advProgramMultiBonusUpdated_",
                () -> ytClient.execInTransaction(
                        tx -> repository.insert(
                                tx,
                                new BonusEntity(
                                        1L,
                                        1L,
                                        "INVOLVE_ALL",
                                        0L,
                                        2000,
                                        Instant.parse("2022-04-25T15:00:00Z"),
                                        Instant.parse("2022-04-30T15:00:00Z"),
                                        false
                                )
                        )
                )
        );
    }
}
