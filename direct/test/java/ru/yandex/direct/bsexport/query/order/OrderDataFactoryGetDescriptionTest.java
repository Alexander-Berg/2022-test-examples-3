package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.bsexport.query.order.OrderDataFactory.getDescription;

class OrderDataFactoryGetDescriptionTest {

    private CommonCampaign campaign = new TextCampaign();

    @Test
    void typicalCampaignNameTest1() {
        campaign.withId(123L)
                .withName("Новая");

        assertThat(getDescription(campaign))
                .isEqualTo("123: Новая");
    }

    @Test
    void typicalCampaignNameTest2() {
        campaign.withId(50268702L)
                .withName("Бесплатная РК от Яндекса");

        assertThat(getDescription(campaign))
                .isEqualTo("50268702: Бесплатная РК от Яндекса");
    }

    @Test
    void typicalCampaignNameTest3() {
        campaign = new WalletTypedCampaign()
                .withId(8278272L)
                .withName("Общий счет (агентский)");

        assertThat(getDescription(campaign))
                .isEqualTo("8278272: Общий счет (агентский)");
    }

    @Test
    void strippedTab() {
        campaign.withId(1L)
                .withName("abc\tdef");

        assertThat(getDescription(campaign))
                .isEqualTo("1: abcdef");
    }

    @Test
    void strippedVerticalTab() {
        campaign.withId(2L)
                .withName("xxx\u000Byyy");

        assertThat(getDescription(campaign))
                .isEqualTo("2: xxxyyy");
    }

    @Test
    void strippedThinSpace() {
        campaign.withId(3L)
                .withName("qwe\u2009rty");

        assertThat(getDescription(campaign))
                .isEqualTo("3: qwerty");
    }

    /**
     * кейс из TESTIRT-10684: Написать тест на отправку кампании с необычными символами
     */
    @Test
    void typicalCampaignNameTest4() {
        campaign = campaign.withId(4L)
                .withName("Campaign name");

        assertThat(getDescription(campaign))
                .isEqualTo("4: Campaign name");
    }

    /**
     * кейс из TESTIRT-10684: Написать тест на отправку кампании с необычными символами
     */
    @Test
    void allowedSpecialCharactersTest() {
        campaign.withId(5L)
                .withName("1!)%$€;:/&'*=#№«»™®©’°⁰");

        assertThat(getDescription(campaign))
                .isEqualTo("5: 1!)%$€;:/&'*=#№«»™®©’°⁰");
    }

    /**
     * Пример из DIRECT-71125: В названии кампаний сохраняем запрещенные символы - ломаем Баланс.
     */
    @Test
    void strippedX1FCharacter() {
        campaign.withId(30418840L)
                .withName("119\u001F_Орел автосервис");

        assertThat(getDescription(campaign))
                .isEqualTo("30418840: 119_Орел автосервис");
    }

    /**
     * кейс из TESTIRT-10684: Написать тест на отправку кампании с необычными символами
     */
    @Test
    void allowedRubleSignTest() {
        campaign.withId(6L)
                .withName("☎₽סॵ");

        assertThat(getDescription(campaign))
                .isEqualTo("6: ₽");
    }

    @Test
    void allCharactersStripped() {
        campaign.withId(7L)
                .withName("|{}");

        assertThat(getDescription(campaign))
                .isEqualTo("7: ");
    }

    @Test
    void strippedPipe() {
        campaign.withId(48846934L)
                .withName("Регионы | Конкуренты / РСЯ");

        assertThat(getDescription(campaign))
                .isEqualTo("48846934: Регионы  Конкуренты / РСЯ");
    }

    @Test
    void strippedBrace() {
        campaign.withId(47623381L)
                .withName("[Поиск] (Фотон) {Москва}");

        assertThat(getDescription(campaign))
                .isEqualTo("47623381: [Поиск] (Фотон) Москва");
    }
}
