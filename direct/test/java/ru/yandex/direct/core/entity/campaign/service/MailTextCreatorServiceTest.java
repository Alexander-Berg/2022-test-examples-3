package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.util.LocaleGuard;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.i18n.Language;

import static org.junit.Assert.assertEquals;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MailTextCreatorServiceTest {

    @Autowired
    private MailTextCreatorService mailTextCreatorService;

    @Test
    public void formatMoney() {
        try (LocaleGuard ignored = LocaleGuard.fromLanguage(Language.RU)) {
            String tenRubles = mailTextCreatorService.formatMoney(new BigDecimal(10.0), CurrencyCode.RUB);
            assertEquals("10.00 руб.", tenRubles);
        }
    }

    @Test
    public void formatMoneyWithThousands() {
        try (LocaleGuard ignored = LocaleGuard.fromLanguage(Language.RU)) {
            String amount = mailTextCreatorService.formatMoney(new BigDecimal(2500001234L), CurrencyCode.RUB);
            assertEquals("2 500 001 234.00 руб.", amount);
        }
    }

    @Test
    public void makeText_Autobudget() {
        DbStrategy newStrategy = new DbStrategy();
        newStrategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);
        newStrategy.setPlatform(CampaignsPlatform.SEARCH);
        StrategyData strategyData = new StrategyData();
        strategyData.setAvgBid(new BigDecimal("10.0"));
        newStrategy.setStrategyData(strategyData);

        DbStrategy oldStrategy = new DbStrategy();
        oldStrategy.setPlatform(CampaignsPlatform.SEARCH);

        String text = makeRussianText(oldStrategy, newStrategy, CurrencyCode.RUB);

        String expected = "Изменены параметры стратегии «Средняя цена клика (за неделю)»\n" +
                "\tУдерживать цену клика 10.00 руб. в среднем за неделю";
        assertEquals(expected, text);
    }

    @Test
    public void makeText_Weekbudget() {
        DbStrategy newStrategy = new DbStrategy();
        newStrategy.setStrategyName(StrategyName.AUTOBUDGET);
        newStrategy.setPlatform(CampaignsPlatform.SEARCH);
        StrategyData strategyData = new StrategyData();
        strategyData.setAvgBid(new BigDecimal("10.0"));
        strategyData.setSum(new BigDecimal("50.0"));
        strategyData.setBid(new BigDecimal("0.16"));
        newStrategy.setStrategyData(strategyData);

        DbStrategy oldStrategy = new DbStrategy();
        oldStrategy.setPlatform(CampaignsPlatform.SEARCH);

        String text = makeRussianText(oldStrategy, newStrategy, CurrencyCode.BYN);

        String expected = "Изменены параметры стратегии «Недельный бюджет»\n" +
                "\tТратить 50.00 BYN в неделю при максимальной ставке 0.16 BYN";
        assertEquals(expected, text);
    }

    @Test
    public void makeText_DifferentPlaces() {
        DbStrategy newStrategy = new DbStrategy();
        newStrategy.setStrategyName(StrategyName.AUTOBUDGET);
        newStrategy.setPlatform(CampaignsPlatform.CONTEXT);
        StrategyData strategyData = new StrategyData();
        strategyData.setAvgBid(new BigDecimal("10.0"));
        strategyData.setSum(new BigDecimal("50.0"));
        strategyData.setBid(new BigDecimal("0.14"));
        newStrategy.setStrategyData(strategyData);
        newStrategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);

        DbStrategy oldStrategy = new DbStrategy();
        oldStrategy.setPlatform(CampaignsPlatform.CONTEXT);

        String text = makeRussianText(oldStrategy, newStrategy, CurrencyCode.BYN);

        String expected = "Изменены параметры стратегии «Независимое управление для разных типов площадок»\n" +
                "\tНа поиске: Показы отключены\n" +
                "\tВ сетях: Недельный бюджет\n" +
                "\t\tТратить 50.00 BYN в неделю при максимальной ставке 0.14 BYN";
        assertEquals(expected, text);
    }

    private String makeRussianText(DbStrategy oldStrategy, DbStrategy newStrategy, CurrencyCode currency) {
        try (LocaleGuard ignored = LocaleGuard.fromLanguage(Language.RU)) {
            return mailTextCreatorService.makeText(oldStrategy, newStrategy, currency);
        }
    }
}
