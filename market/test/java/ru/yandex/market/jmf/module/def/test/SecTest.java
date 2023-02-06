package ru.yandex.market.jmf.module.def.test;

import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.impl.ConfigurationServiceImpl;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.logic.wf.WfSecurityService;
import ru.yandex.market.jmf.logic.wf.impl.security.WfSecurityMarker;
import ru.yandex.market.jmf.logic.wf.impl.security.WfSecurityMarkersGroup;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.security.AttributesSecurityService;
import ru.yandex.market.jmf.security.SecurityDomainsService;
import ru.yandex.market.jmf.security.impl.marker.attribute.AttributesSecurityMarker;
import ru.yandex.market.jmf.security.impl.marker.attribute.view.ViewAttributesSecurityMarkersGroup;
import ru.yandex.market.jmf.security.impl.marker.domain.SecurityDomain;
import ru.yandex.market.jmf.security.marker.SecurityMarker;
import ru.yandex.market.jmf.security.test.SecurityTestConfiguration;
import ru.yandex.market.jmf.security.test.impl.MockAuthRunnerService;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;
import ru.yandex.market.jmf.utils.Maps;

@SpringJUnitConfig(classes = {
        SecTest.Configuration.class
})
@Transactional
public class SecTest {

    private static final Fqn FQN_SIMPLE_1 = Fqn.of("simple1");

    @Inject
    SecurityDomainsService domainsService;
    @Inject
    WfSecurityService wfSecurityService;
    @Inject
    AttributesSecurityService attributesSecurityService;

    @Inject
    BcpService bcpService;
    @Inject
    ConfigurationServiceImpl configurationService;

    @Inject
    MockSecurityDataService securityDataService;
    @Inject
    MockAuthRunnerService authRunnerService;

    /**
     * Проверяем, что в маркер атрибутов по умолчанию попадают только атрибуты, которые не включены ни в один маркер
     * явно.
     */
    @Test
    public void attributeMarkers_defaultAttributeMarker() {
        // действия теста выполнились при инициализации конфигурации приложения
        // проверка утверждений
        ViewAttributesSecurityMarkersGroup group = domainsService.getDomain(FQN_SIMPLE_1).
                getSecurityMarkerGroup(ViewAttributesSecurityMarkersGroup.class);

        Assertions.assertNotNull(group, "Группа для настройки просмотра атрибутов должна присутствовать т.к. она явно" +
                " " +
                "задана для метакласса entity");

        AttributesSecurityMarker defaultMarker = group.getDefaultMarker();
        Assertions.assertNotNull(
                defaultMarker, "Маркер по умолчанию задан явно в метаклассе entity и имеет id viewOtherAttributes");

        Collection<String> attributes = defaultMarker.getAttributes();
        Assertions.assertTrue(
                attributes.contains("attr4"),
                "Аттрибут attr4 не содержится явно ни в одном маркере и должен попасть в маркер по умолчанию");
        Assertions.assertFalse(
                attributes.contains("attr1"), "Аттрибут attr1 входит в маркер marker1 и НЕ должен попасть в маркер по" +
                        " умолчанию");
    }

    /**
     * Проверяем, что атрибут, который явно включен в конфигурации в маркеры, попадает в нужные группы и только в них.
     */
    @Test
    public void attributeMarkers_attributeMarker_attr1() {
        // действия теста выполнились при инициализации конфигурации приложения
        // проверка утверждений
        SecurityDomain domain = domainsService.getDomain(FQN_SIMPLE_1);
        ViewAttributesSecurityMarkersGroup group =
                domain.getSecurityMarkerGroup(ViewAttributesSecurityMarkersGroup.class);

        Assertions.assertNotNull(group, "Группа для настройки просмотра атрибутов должна присутствовать т.к. она явно" +
                " " +
                "задана для метакласса entity");

        // В конфигурации атрибут прописан в маркеры marker1 и marker2
        Set<AttributesSecurityMarker> markers = group.getMarkers("attr1");
        Assertions.assertEquals(2, markers.size());
        Collection<String> gids = Collections2.transform(markers, SecurityMarker::getGid);
        Assertions.assertTrue(gids.contains("marker1"));
        Assertions.assertTrue(gids.contains("marker2"));
    }

    /**
     * Проверяем, что атрибут creationTime правильно проинициализирован из настроек системных доменов (логик).
     */
    @Test
    public void attributeMarkers_attributeMarker_creationTime() {
        // действия теста выполнились при инициализации конфигурации приложения
        // проверка утверждений
        SecurityDomain domain = domainsService.getDomain(FQN_SIMPLE_1);
        ViewAttributesSecurityMarkersGroup group =
                domain.getSecurityMarkerGroup(ViewAttributesSecurityMarkersGroup.class);

        Assertions.assertNotNull(group, "Группа для настройки просмотра атрибутов должна присутствовать т.к. она явно" +
                " " +
                "задана для метакласса entity");

        // В конфигурации атрибут прописан в маркеры technicalView (see logic withCreationTime)
        Set<AttributesSecurityMarker> markers = group.getMarkers("creationTime");
        Assertions.assertEquals(1, markers.size());
        Collection<String> gids = Collections2.transform(markers, SecurityMarker::getGid);
        Assertions.assertTrue(gids.contains("technicalView"));
    }

    /**
     * Проверяем, что у группы маркеров жизненного цикла есть автосгенерированный маркеры для возможных переходов.
     * Вся логика генерации маркеров для перехода по статусам жизненного цикла содержится в
     * {@link ru.yandex.market.jmf.logic.wf.impl.security.WfSecurityMarkersGroupInitializer}.
     *
     * @see ru.yandex.market.jmf.logic.wf.impl.security.WfSecurityMarkersGroupInitializer
     */
    @Test
    public void wf_generatedMarkers() {
        // действия теста выполнились при инициализации конфигурации приложения
        // проверка утверждений
        SecurityDomain domain = domainsService.getDomain(FQN_SIMPLE_1);
        WfSecurityMarkersGroup group = domain.getSecurityMarkerGroup(WfSecurityMarkersGroup.class);

        Assertions.assertNotNull(group, "Группа для настройки жизненного статуса должна присутствовать т.к. метакласс" +
                " simple1" +
                " имеет жизненный цикл");

        WfSecurityMarker allMarker = Iterables.find(group.getSecurityMarkers(), m -> "@wf:*:*".equals(m.getGid()),
                null);
        Assertions.assertNotNull(allMarker, "Должен присутствовать маркер для задания прав на произвольный переход и " +
                "этот маркер " +
                "должен иметь gid '@wf:*:*', который генерируется автоматически");

        WfSecurityMarker toActiveMarker = Iterables.find(group.getSecurityMarkers(),
                m -> "@wf:*:active".equals(m.getGid()), null);
        Assertions.assertNotNull(toActiveMarker, "Должен присутствовать маркер для задания прав на произвольный " +
                "переход в статус active " +
                "и этот маркер должен иметь gid '@wf:*:active', который генерируется автоматически");

    }

    /**
     * Проверяем, что у группы маркеров жизненного цикла правильно конфигурируются маркеры для конкретного перехода,
     * которые задаются в конфигурации.
     *
     * @see ru.yandex.market.jmf.logic.wf.impl.security.WfSecurityMarkersGroupInitializer
     */
    @Test
    public void wf_configuredMarkers() {
        // действия теста выполнились при инициализации конфигурации приложения
        // проверка утверждений
        SecurityDomain domain = domainsService.getDomain(FQN_SIMPLE_1);
        WfSecurityMarkersGroup group = domain.getSecurityMarkerGroup(WfSecurityMarkersGroup.class);

        Assertions.assertNotNull(group, "Группа для настройки жизненного статуса должна присутствовать т.к. метакласс" +
                " simple1" +
                " имеет жизненный цикл");

        WfSecurityMarker marker = Iterables.find(group.getSecurityMarkers(), m -> "activeToArchived".equals(m.getGid()),
                null);
        Assertions.assertNotNull(marker, "Должен присутствовать маркер activeToArchived т.к. он задан в конфигурации");
        Assertions.assertEquals("Тестовый маркер", marker.getTitle(), "Название маркера должно взяться из " +
                "конфигурации");

        Collection<WfSecurityMarker.Transition> transitions = marker.getTransitions();
        Assertions.assertNotNull(transitions);
        Assertions.assertEquals(1, transitions.size(), "В конфигурации маркера задан только один переход");
        WfSecurityMarker.Transition transition = Iterables.getFirst(transitions, null);
        Assertions.assertEquals("active", transition.from());
        Assertions.assertEquals("archived", transition.to());
    }

    @Test
    @Transactional
    public void checkWfPermission_anyTransition() {
        // настройка системы
        initSecurity("profile1");

        // вызов системы
        HasWorkflow entity = createEntity();
        boolean result = wfSecurityService.couldChangeStatus(entity, "archived");

        // проверка утверждений
        Assertions.assertTrue(result, "В матрице прав настроен любой переход для profile1");
    }

    @Test
    @Transactional
    public void checkWfPermission_anyTransitionToStatus() {
        // настройка системы
        initSecurity("profile2");

        // вызов системы
        HasWorkflow entity = createEntity();
        boolean result = wfSecurityService.couldChangeStatus(entity, "archived");

        // проверка утверждений
        Assertions.assertTrue(result, "В матрице прав настроен любой переход в статус archived для profile2");
    }

    @Test
    @Transactional
    public void checkWfPermission_concreteTransition() {
        // настройка системы
        initSecurity("profile3");

        // вызов системы
        HasWorkflow entity = createEntity();
        boolean result = wfSecurityService.couldChangeStatus(entity, "archived");

        // проверка утверждений
        Assertions.assertTrue(result, "В матрице прав настроен переход между статусами active и archived для profile3");
    }

    @Test
    @Transactional
    public void checkWfPermission_hasntPermission() {
        // настройка системы
        initSecurity("profile4");

        // вызов системы
        HasWorkflow entity = createEntity();
        boolean result = wfSecurityService.couldChangeStatus(entity, "archived");

        // проверка утверждений
        Assertions.assertFalse(result, "В матрице прав НЕ выданы права на переходы между статусами для profile4");
    }

    @Test
    @Transactional
    public void viewAttribute_attr1_marker1() {
        // настройка системы
        initSecurity("profile5");

        // вызов системы
        HasWorkflow entity = createEntity();
        boolean result1 = attributesSecurityService.hasViewPermission(entity, "attr1");
        boolean result2 = attributesSecurityService.hasViewPermission(entity, "attr2");

        // проверка утверждений
        Assertions.assertTrue(result1, "Аттрибу attr1 входит в маркер marker1 на который выданы права profile5");
        Assertions.assertFalse(result2, "Аттрибу attr2 входит в маркер marker2, но на него НЕ выданы права profile5");
    }

    @Test
    @Transactional
    public void viewAttribute_attr1_marker2() {
        // настройка системы
        initSecurity("profile6");

        // вызов системы
        HasWorkflow entity = createEntity();
        boolean result1 = attributesSecurityService.hasViewPermission(entity, "attr1");
        boolean result2 = attributesSecurityService.hasViewPermission(entity, "attr2");

        // проверка утверждений
        Assertions.assertTrue(result1, "Аттрибу attr1 входит в маркер marker2 на который выданы права profile6");
        Assertions.assertTrue(result2, "Аттрибу attr2 входит в маркер marker2 на который выданы права profile6");
    }

    private void initSecurity(String profile) {
        authRunnerService.reset();
        securityDataService.reset();

        configurationService.setValue("useNewSecurity", true);
        configurationService.invalidateCache();

        securityDataService.setCurrentUserProfiles(profile);
        authRunnerService.setCurrentUserSuperUser(false);
    }

    private HasWorkflow createEntity() {
        return bcpService.create(FQN_SIMPLE_1, Maps.of("title", Randoms.string()));
    }

    @Import({
            ModuleDefaultTestConfiguration.class,
            SecurityTestConfiguration.class
    })
    public static class Configuration extends AbstractModuleConfiguration {
        public Configuration() {
            super("module/default/test/sec");
        }
    }
}
