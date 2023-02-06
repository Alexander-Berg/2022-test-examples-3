package ru.yandex.autotests.direct.cmd.phrases.minusphrases;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public abstract class MinusPhrasesBaseTest {

    public static final String CLIENT = "at-backend-minusphrases";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    public TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    public ArrayList minusPhraseList;

    @Parameterized.Parameter(value = 0)
    public String minusPhraseSrt;

    @Parameterized.Parameters(name = "Сохраняем минус-фразы: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"рано утром рассвете просыпаются мышата утята котята"},
                {"рано утром !на рассвете просыпаются мышата утята"},
                {"[рано] утром !на рассвете просыпаются мышата утята"},
                {"Санкт-Петербург прекрасен !в любое время года"},
                {"!Санкт-Петербург бесспорно прекрасен !в любое время года"},
                {"!Как быстро научиться разговаривать по-русски без акцента"},
                {"!бурый медведь, бело-черный медведь, медведь +гризли"},
                {"!купить коня +в яблоках, [спортивный конь], \"серый в яблоках\""},
        });
    }

    @Before
    public void before() {
        minusPhraseList = new ArrayList<>(Arrays.asList(minusPhraseSrt.split(", ")));
        Collections.sort(minusPhraseList);
    }
}
