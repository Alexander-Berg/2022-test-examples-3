package ru.yandex.market.partner.lead;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.onboarding.lead.PartnerLeadInfo;
import ru.yandex.market.core.partner.onboarding.lead.PartnerLeadService;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Функциональные тесты для {@link PartnerLeadCommentBuilder}
 */
public class PartnerLeadCommonBuilderFunctionalTest extends FunctionalTest {
    @Autowired
    private PartnerLeadCommentBuilder partnerLeadCommentBuilder;

    @Autowired
    private PartnerLeadService partnerLeadService;

    @Test
    void testNoPartnerComment() {
        var partnerLeadInfo = PartnerLeadInfo.builder()
                .setCategory("Toys")
                .setAssortment("1000")
                .setLogin("login")
                .setCreatedAt(Instant.parse("2019-03-10T10:00:00Z"))
                .build();
        assertThat(partnerLeadCommentBuilder.buildComment(partnerLeadInfo))
        .isEqualTo("" +
                "Категория товаров: Toys<br/>" +
                "Ассортимент (кол-во sku): 1000<br/>" +
                "Дата регистрации: 10.03.2019<br/>" +
                "Логин: login");
    }

    @Test
    @DbUnitDataSet(before = "PartnerLeadCommonBuilderFunctionalTest.testNoPartnerCommentDb.before.csv")
    void testNoPartnerCommentDb() {
        var partnerLeadInfo = partnerLeadService.getLeadsWithoutTicket().get(0);
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        assertThat(partnerLeadCommentBuilder.buildComment(partnerLeadInfo))
                .isEqualTo("" + String.format("Категория товаров: Books<br/>" +
                        "Ассортимент (кол-во sku): 500-1000<br/>" +
                        "Дата регистрации: %s<br/>" +
                        "Логин: login", dateFormat.format(
                                Date.from(Objects.requireNonNull(partnerLeadInfo.getCreatedAt())))));
    }

    @Test
    @DbUnitDataSet(before = "PartnerLeadCommonBuilderFunctionalTest.testDbsPartnerComment.before.csv")
    void testDbsPartnerComment() {
        var partnerLeadInfo = partnerLeadService.getLeadsToUpdateTicket().get(0);
        assertThat(partnerLeadCommentBuilder.buildComment(partnerLeadInfo))
                .isEqualTo("" +
                        "Категория товаров: Books<br/>" +
                        "Ассортимент (кол-во sku): 500-1000<br/>" +
                        "Дата регистрации: 10.05.2020<br/>" +
                        "Логин: login<br/>" +
                        "Город: Москва<br/>" +
                        "Сайт: domain.ru<br/>" +
                        "Модель работы: DBS<br/>" +
                        "Название бизнеса: business2<br/>" +
                        "Название магазина: SomeName<br/>" +
                        "ID магазина: 1<br/>" +
                        "ID бизнеса: 2");
    }

    @Test
    @DbUnitDataSet(before = "PartnerLeadCommonBuilderFunctionalTest.testFbsPartnerComment.before.csv")
    void testFbsPartnerComment() {
        var partnerLeadInfo = partnerLeadService.getLeadsToUpdateTicket().get(0);
        assertThat(partnerLeadCommentBuilder.buildComment(partnerLeadInfo))
                .isEqualTo("" +
                        "Категория товаров: Books<br/>" +
                        "Ассортимент (кол-во sku): >1000<br/>" +
                        "Дата регистрации: 10.05.2020<br/>" +
                        "Логин: login<br/>" +
                        "Город: Москва<br/>" +
                        "Сайт: somedomain.com<br/>" +
                        "Модель работы: FBS<br/>" +
                        "Название бизнеса: business2<br/>" +
                        "Название магазина: SomeName<br/>" +
                        "ID магазина: 1<br/>" +
                        "ID бизнеса: 2");
    }

    @Test
    @DbUnitDataSet(before = "PartnerLeadCommonBuilderFunctionalTest.testFbyPlusPartnerComment.before.csv")
    void testFbyPlusPartnerComment() {
        var partnerLeadInfo = partnerLeadService.getLeadsToUpdateTicket().get(0);
        assertThat(partnerLeadCommentBuilder.buildComment(partnerLeadInfo))
                .isEqualTo("" +
                        "Категория товаров: Books<br/>" +
                        "Ассортимент (кол-во sku): >1000<br/>" +
                        "Дата регистрации: 10.05.2020<br/>" +
                        "Логин: login<br/>" +
                        "Город: Москва<br/>" +
                        "Сайт: somedomain.com<br/>" +
                        "Модель работы: FBY+<br/>" +
                        "Название бизнеса: business2<br/>" +
                        "Название магазина: SomeName<br/>" +
                        "ID магазина: 1<br/>" +
                        "ID бизнеса: 2");
    }

    @Test
    @DbUnitDataSet(before = "PartnerLeadCommonBuilderFunctionalTest.testFbyPartnerComment.before.csv")
    void testFbyPartnerComment() {
        var partnerLeadInfo = partnerLeadService.getLeadsToUpdateTicket().get(0);
        assertThat(partnerLeadCommentBuilder.buildComment(partnerLeadInfo))
                .isEqualTo("" +
                        "Категория товаров: Toys<br/>" +
                        "Ассортимент (кол-во sku): <500<br/>" +
                        "Дата регистрации: 10.05.2020<br/>" +
                        "Логин: login<br/>" +
                        "Город: Москва<br/>" +
                        "Сайт: somedomain.com<br/>" +
                        "Модель работы: FBY<br/>" +
                        "Название бизнеса: business2<br/>" +
                        "Название магазина: SomeName<br/>" +
                        "ID магазина: 1<br/>" +
                        "ID бизнеса: 2");
    }
}
