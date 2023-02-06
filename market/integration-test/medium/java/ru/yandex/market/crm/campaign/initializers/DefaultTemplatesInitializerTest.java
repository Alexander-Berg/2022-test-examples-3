package ru.yandex.market.crm.campaign.initializers;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.campaign.services.templates.BlockTemplateService;
import ru.yandex.market.crm.campaign.test.AbstractServiceMediumWithoutYtTest;
import ru.yandex.market.crm.core.domain.templates.BlockTemplate;
import ru.yandex.market.crm.core.domain.templates.TemplateType;
import ru.yandex.market.crm.core.services.templates.BlockTemplatesDAO;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/**
 * @author zloddey
 */
@ContextConfiguration(classes = {DefaultTemplatesInitializerTest.Config.class})
public class DefaultTemplatesInitializerTest extends AbstractServiceMediumWithoutYtTest {
    @Configuration
    public static class Config {
        /**
         * Т.к. мы тестируем класс, у которого есть {@literal @PostConstruct}-метод, то должны обеспечить ситуацию,
         * при которой этот метод не вызывается во время создания тестовой фикстуры. Поэтому мы кладём в фикстуру
         * заглушку вместо инстанса тестируемого сервиса, а тестируемый инстанс создаём руками.
         */
        @Bean
        public DefaultTemplatesInitializer mockTemplatesInitializer() {
            return mock(DefaultTemplatesInitializer.class);
        }
    }

    @Inject
    private DefaultTemplatesInitializer fakeInitializer;
    @Inject
    private JsonDeserializer deserializer;
    @Inject
    private BlockTemplateService templateService;
    @Inject
    private BlockTemplatesDAO dao;

    // Этот объект создаём руками, а не через Spring, чтобы не был вызван метод {@literal init}
    private DefaultTemplatesInitializer initializer;

    @BeforeEach
    public void createInitializer() {
        initializer = new DefaultTemplatesInitializer(deserializer, templateService);
    }

    /**
     * Проверяем, что инициализатор из контекста действительно является заглушкой - и потому ничего не делает.
     * Это даст нам гарантию, что следующие тесты не подвержены влиянию окружения.
     */
    @Test
    public void injectedInitializerMustBeFake() {
        Assertions.assertTrue(mockingDetails(fakeInitializer).isMock());
    }

    /**
     * Если исходная база данных пустая, то в неё нужно вставить дефолтную запись
     */
    @Test
    public void insertOneHeaderIntoEmptyDatabase() {
        initializer.init();

        List<BlockTemplate> templates = dao.getFilteredBy(TemplateType.HEAD);
        Assertions.assertEquals(1, templates.size());

        BlockTemplate insertedTemplate = templates.get(0);
        Assertions.assertEquals("Шапка по умолчанию", insertedTemplate.getName());
    }

    /**
     * Повторная инициализация (т.е., перезапуск приложения) не приводит к новым вставкам записей
     */
    @Test
    public void doNotInsertDataTwice() {
        initializer.init();
        initializer.init();

        List<BlockTemplate> templates = dao.getFilteredBy(TemplateType.HEAD);
        Assertions.assertEquals(1, templates.size());
    }
}
