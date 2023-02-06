package ru.yandex.market.partner.notification.service.mustache.dao;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.service.mustache.model.AliasPlaceCodes;
import ru.yandex.market.partner.notification.service.mustache.model.MustacheTemplate;
import ru.yandex.market.partner.notification.service.mustache.model.MustacheTemplateInfo;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.EMAIL;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.MBI_WEB_UI;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.MOBILE_PUSH;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.TELEGRAM_BOT;

@Disabled // TODO: реализовать изоляцию модифицирующих шаблоны тестов
@Transactional
class MustacheTemplateDaoTest extends AbstractFunctionalTest {
    @Autowired
    MustacheTemplateDao mustacheTemplateDao;

    @Test
    @DbUnitDataSet(before = "MustacheTemplateDaoTest.before.csv")
    void getById() {
        var expected = MustacheTemplate.builder()
                .setInfo(MustacheTemplateInfo.builder()
                        .setId(2)
                        .setName("Тестовый шаблон")
                        .setTransports(List.of(EMAIL, MOBILE_PUSH, MBI_WEB_UI, TELEGRAM_BOT))
                        .setPriorityType(NotificationPriority.HIGH)
                        .setThemeId(1)
                        .setAliases(AliasPlaceCodes.builder()
                                .setAliasFrom(List.of("PublicPartnerAddress"))
                                .setAliasTo(List.of("ShopAdmins"))
                                .setAliasBcc(List.of("YaManagerOnly"))
                                .build())
                        .build())
                .setMustacheSubject("Тема сообщения для магазина {{data.shop-name}}")
                .setMustacheBody("Тестовое сообщение для магазина {{#bold}}{{data.shop-name}}{{/bold}}")
                .setExtraData("{\"service\": \"test\",\"event\": \"test\"}")
                .build();

        var template = mustacheTemplateDao.getById(2).get();

        assertThat(template, equalTo(expected));
    }

    @Test
    @DbUnitDataSet(before = "MustacheTemplateDaoTest.before.csv")
    void getAllTemplates() {
        var infos = getExpectedTemplates().stream()
                .map(MustacheTemplate::getInfo)
                .collect(Collectors.toList());

        var templates = mustacheTemplateDao.getAllTemplateInfo();

        assertThat(templates, equalTo(infos));
    }

    @Test
    @DbUnitDataSet(before = "MustacheTemplateDaoTest.before.csv")
    void update() {
        var template = MustacheTemplate.builder()
                .setInfo(MustacheTemplateInfo.builder()
                        .setId(2)
                        .setName("Новый тестовый шаблон")
                        .setTransports(List.of(MOBILE_PUSH, MBI_WEB_UI))
                        .setPriorityType(NotificationPriority.LOW)
                        .setThemeId(2)
                        .setAliases(AliasPlaceCodes.builder()
                                .setAliasFrom(List.of("YaManagerOnly", "PublicPartnerAddress"))
                                .setAliasTo(List.of("PublicPartnerAddress", "ShopAdmins"))
                                .setAliasCc(List.of("ShopAdmins", "YaManagerOnly"))
                                .setAliasBcc(List.of("PublicPartnerAddress", "YaManagerOnly"))
                                .setAliasReplyTo(List.of("ShopAdmins", "YaManagerOnly", "PublicPartnerAddress"))
                                .build())
                        .build())
                .setMustacheSubject("Новая тема сообщения для магазина {{data.shop-name}}")
                .setMustacheBody("Новое тестовое сообщение для магазина {{#bold}}{{data.shop-name}}{{/bold}}")
                .setExtraData("{\"service\": \"test2\",\"event\": \"test3\"}")
                .build();

        assertTrue(mustacheTemplateDao.update(template));
        assertThat(mustacheTemplateDao.getById(2).get(), equalTo(template));
        assertThat(mustacheTemplateDao.getById(1606103905).get(), equalTo(getExpectedTemplates().get(1)));
    }

    @Test
    @DbUnitDataSet(before = "MustacheTemplateDaoTest.before.csv")
    void updateNotExistent() {
        var template = MustacheTemplate.builder()
                .setInfo(MustacheTemplateInfo.builder()
                        .setId(10000000)
                        .setName("Тестовый шаблон")
                        .setTransports(List.of(EMAIL, MOBILE_PUSH, MBI_WEB_UI, TELEGRAM_BOT))
                        .setPriorityType(NotificationPriority.HIGH)
                        .setThemeId(1)
                        .setAliases(AliasPlaceCodes.builder()
                                .setAliasFrom(List.of("PublicPartnerAddress"))
                                .setAliasTo(List.of("ShopAdmins"))
                                .setAliasBcc(List.of("YaManagerOnly"))
                                .build())
                        .build())
                .setMustacheSubject("Тема сообщения для магазина {{data.shop-name}}")
                .setMustacheBody("Тестовое сообщение для магазина {{#bold}}{{data.shop-name}}{{/bold}}")
                .setExtraData("{\"service\": \"test\",\"event\": \"test\"}")
                .build();

        assertFalse(mustacheTemplateDao.update(template));
    }

    private List<MustacheTemplate> getExpectedTemplates() {
        return List.of(
                MustacheTemplate.builder()
                        .setInfo(MustacheTemplateInfo.builder()
                                .setId(2)
                                .setName("Тестовый шаблон")
                                .setTransports(List.of(EMAIL, MOBILE_PUSH, MBI_WEB_UI, TELEGRAM_BOT))
                                .setPriorityType(NotificationPriority.HIGH)
                                .setThemeId(1)
                                .setAliases(AliasPlaceCodes.builder()
                                        .setAliasFrom(List.of("PublicPartnerAddress"))
                                        .setAliasTo(List.of("ShopAdmins"))
                                        .setAliasBcc(List.of("YaManagerOnly"))
                                        .build())
                                .build())
                        .setMustacheSubject("Тема сообщения для магазина {{data.shop-name}}")
                        .setMustacheBody("Тестовое сообщение для магазина {{#bold}}{{data.shop-name}}{{/bold}}")
                        .setExtraData("{\"service\": \"test\",\"event\": \"test\"}")
                        .build(),
                MustacheTemplate.builder()
                        .setInfo(MustacheTemplateInfo.builder()
                                .setId(1606103905)
                                .setName("DSBS включили после снятия катоффа за пингер")
                                .setTransports(List.of(EMAIL, MBI_WEB_UI, TELEGRAM_BOT))
                                .setPriorityType(NotificationPriority.HIGH)
                                .setThemeId(6)
                                .setHtmlWrapperId(1522741726L)
                                .setAliases(AliasPlaceCodes.builder()
                                        .setAliasFrom(List.of("PublicPartnerAddress"))
                                        .setAliasTo(List.of("ShopAdmins", "ShopOperators", "ShopSupports"))
                                        .build())
                                .build())
                        .setMustacheSubject("Размещение магазина {{data.datasource-info.internal-name}} возобновлено")
                        .setMustacheBody("Товары магазина {{data.datasource-info.internal-name}} вернутся на витрину " +
                                "примерно через 3-6 часов.")
                        .build()
        );
    }
}
