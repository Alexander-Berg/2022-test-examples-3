package ru.yandex.market.ocrm.module.quality.management;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.Channel;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.ocrm.module.quality.management.domain.QualityManagementSurvey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringJUnitConfig(classes = ModuleQualityManagementTestConfiguration.class)
public class QualityManagementSurveyTest {

    @Inject
    BcpService bcpService;

    @Inject
    TicketTestUtils ticketTestUtils;

    private Team team;

    @BeforeEach
    public void setUp() {
        team = ticketTestUtils.createTeam();
    }

    /**
     * Ошибка при создани опроса с очередью, для которой не выбран бренд
     *
     * <ul>
     *     <li>Создаем бренды: brand1, brand2</li>
     *     <li>Создаем очередь: service1</li>
     *     <li>Создаем опрос, указываем бренд brand1, очередь service1</li>
     *     <li>Проверяем, что появилась ошибка: Нельзя указать следующие очереди,
     *     т.к. для них не выбран бренд: ["${service1.title}" (${service1.gid})]</li>
     * </ul>
     */
    @Test
    public void createSurveyWithInvalidService() {
        Brand brand1 = ticketTestUtils.createBrand();
        Brand brand2 = ticketTestUtils.createBrand();
        Service service1 = ticketTestUtils.createService(Map.of(Service.BRAND, brand2));

        ValidationException exception =
                assertThrows(ValidationException.class, () -> createSurvey(Set.of(brand1), Set.of(service1)));
        assertEquals(
                String.format("Нельзя указать следующие очереди, т.к. для них не выбран бренд: [\"%s\" (%s)]",
                        service1.getTitle(), service1.getGid()
                ),
                exception.getMessage()
        );
    }

    /**
     * Ошибка при редактировании у опроса очереди, для которой не выбран бренд
     *
     * <ul>
     *     <li>Создаем бренды: brand1, brand2</li>
     *     <li>Создаем очереди: service1, service2</li>
     *     <li>Создаем опрос survey, указываем бренд brand1, очередь service1</li>
     *     <li>Редактируем survey, указываем очередь service2</li>
     *     <li>Проверяем, что появилась ошибка: Нельзя указать следующие очереди,
     *     т.к. для них не выбран бренд: ["${service2.title}" (${service2.gid})]</li>
     * </ul>
     */
    @Test
    public void editSurveyWithInvalidService() {
        Brand brand1 = ticketTestUtils.createBrand();
        Brand brand2 = ticketTestUtils.createBrand();
        Service service1 = ticketTestUtils.createService(Map.of(Service.BRAND, brand1));
        Service service2 = ticketTestUtils.createService(Map.of(Service.BRAND, brand2));

        QualityManagementSurvey survey = createSurvey(Set.of(brand1), Set.of(service1));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bcpService.edit(survey, Map.of(QualityManagementSurvey.SERVICES, Set.of(service2))));
        assertEquals(
                String.format("Нельзя указать следующие очереди, т.к. для них не выбран бренд: [\"%s\" (%s)]",
                        service2.getTitle(), service2.getGid()
                ),
                exception.getMessage()
        );
    }

    /**
     * Заполнение пустого бренда по очередям при создании опроса
     *
     * <ul>
     *     <li>Создаем бренды: brand1, brand2</li>
     *     <li>Создаем очереди: service1, service2</li>
     *     <li>Создаем опрос, указываем очереди service1, service2, бренды не указываем</li>
     *     <li>Проверяем, что у созданного опроса заполнено поле бренды значением: brand1, brand2</li>
     * </ul>
     */
    @Test
    public void fillBrandsByServicesWhenBrandsIsEmptyForCreate() {
        Brand brand1 = ticketTestUtils.createBrand();
        Brand brand2 = ticketTestUtils.createBrand();
        Service service1 = ticketTestUtils.createService(Map.of(Service.BRAND, brand1));
        Service service2 = ticketTestUtils.createService(Map.of(Service.BRAND, brand2));

        QualityManagementSurvey survey = createSurvey(Set.of(), Set.of(service1, service2));
        assertEquals(Set.of(brand1, brand2), survey.getBrands());
    }

    /**
     * Заполнение пустого бренда по очередям при редактировании опроса
     *
     * <ul>
     *     <li>Создаем бренды: brand1, brand2</li>
     *     <li>Создаем очередь: service1</li>
     *     <li>Создаем опрос survey, указываем бренды brand1, brand2, очередь service1</li>
     *     <li>Редактируем survey: оставляем в поле бренды пустое значение</li>
     *     <li>Проверяем, что у survey в поле бренды значение: brand1</li>
     * </ul>
     */
    @Test
    public void fillBrandsByServicesWhenBrandsIsEmptyForEdit() {
        Brand brand1 = ticketTestUtils.createBrand();
        Brand brand2 = ticketTestUtils.createBrand();
        Service service1 = ticketTestUtils.createService(Map.of(Service.BRAND, brand1));

        QualityManagementSurvey survey = createSurvey(Set.of(brand1, brand2), Set.of(service1));
        bcpService.edit(survey, Map.of(QualityManagementSurvey.BRANDS, Set.of()));
        assertEquals(Set.of(brand1), survey.getBrands());
    }

    /**
     * Ошибка при создании опроса с неуникальной связкой бренд, очередь, канал, линия
     *
     * <ul>
     *     <li>Создаем бренд: brand1</li>
     *     <li>Создаем очередь: service1</li>
     *     <li>Создаем опрос survey, указываем бренд brand1, очередь service1</li>
     *     <li>Создаем еще один опрос с брендом brand1, очередью service1</li>
     *     <li>Проверяем, что появилась ошибка: Один или несколько атрибутов правил выбора анкеты для обращения контроля
     *     качества (Бренд, Очередь, Канал, Линия) уже существует в анкете [\"${survey.title}\" (${survey.gid})]</li>
     * </ul>
     */
    @Test
    public void createWithNonUniqueBrandsServiceChannelsTeams() {
        Brand brand1 = ticketTestUtils.createBrand();
        Service service1 = ticketTestUtils.createService(Map.of(Service.BRAND, brand1));

        QualityManagementSurvey survey = createSurvey(Set.of(brand1), Set.of(service1));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> createSurvey(Set.of(brand1), Set.of(service1)));
        assertEquals(
                String.format("Один или несколько атрибутов правил выбора анкеты для обращения контроля " +
                                "качества (Бренд, Очередь, Канал, Линия) уже существует в анкете [\"%s\" (%s)]",
                        survey.getTitle(), survey.getGid()
                ),
                exception.getMessage()
        );
    }

    /**
     * Ошибка при редактировании опроса с неуникальной связкой бренд, очередь, канал, линия
     *
     * <ul>
     *     <li>Создаем бренды: brand1, brand2</li>
     *     <li>Создаем очереди: service1, service2</li>
     *     <li>Создаем опрос survey1, указываем бренды brand1, brand2, очередь service1</li>
     *     <li>Создаем опрос survey2, указываем бренды brand1, brand2, очередь service2</li>
     *     <li>Редактируем у survey2 атрибут очереди на значение: service1</li>
     *     <li>Проверяем, что появилась ошибка: Один или несколько атрибутов правил выбора анкеты для обращения контроля
     *     качества (Бренд, Очередь, Канал, Линия) уже существует в анкете [\"${survey1.title}\" (${survey1.gid})]</li>
     * </ul>
     */
    @Test
    public void editWithNonUniqueBrandsServiceChannelsTeams() {
        Brand brand1 = ticketTestUtils.createBrand();
        Brand brand2 = ticketTestUtils.createBrand();
        Service service1 = ticketTestUtils.createService(Map.of(Service.BRAND, brand1));
        Service service2 = ticketTestUtils.createService(Map.of(Service.BRAND, brand2));

        QualityManagementSurvey survey1 = createSurvey(Set.of(brand1, brand2), Set.of(service1));
        QualityManagementSurvey survey2 = createSurvey(Set.of(brand1, brand2), Set.of(service2));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> bcpService.edit(survey2, Map.of(QualityManagementSurvey.SERVICES, Set.of(service1))));
        assertEquals(
                String.format("Один или несколько атрибутов правил выбора анкеты для обращения контроля " +
                                "качества (Бренд, Очередь, Канал, Линия) уже существует в анкете [\"%s\" (%s)]",
                        survey1.getTitle(), survey1.getGid()
                ),
                exception.getMessage()
        );
    }

    private QualityManagementSurvey createSurvey(Set<Brand> brands, Set<Service> services) {
        return bcpService.create(QualityManagementSurvey.FQN, Map.of(
                QualityManagementSurvey.TITLE, Randoms.string(),
                QualityManagementSurvey.BRANDS, brands,
                QualityManagementSurvey.CHANNELS, Channel.MAIL,
                QualityManagementSurvey.TEAMS, team,
                QualityManagementSurvey.SERVICES, services
        ));
    }
}
