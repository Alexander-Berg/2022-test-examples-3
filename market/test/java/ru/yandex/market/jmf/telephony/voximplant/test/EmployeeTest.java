package ru.yandex.market.jmf.telephony.voximplant.test;

import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.http.test.ResponseBuilder;
import ru.yandex.market.jmf.http.test.impl.HttpEnvironment;
import ru.yandex.market.jmf.http.test.impl.HttpRequest;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.telephony.voximplant.Employee;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;

@Transactional
@SpringJUnitConfig(classes = VoximplantTestConfiguration.class)
@TestPropertySource(properties = "external.voximplant.enabled=true")
public class EmployeeTest {

    @Inject
    private BcpService bcpService;
    @Inject
    private ObjectSerializeService serializeService;
    @Inject
    private HttpEnvironment environment;
    @Value("${external.voximplant.url}")
    private String baseUrl;
    @Value("${external.voximplant.application.id}")
    private String applicationId;

    @BeforeEach
    public void setUp() {
        environment.setUp();
    }

    @AfterEach
    public void tearDown() {
        environment.tearDown();
    }

    /**
     * Т.к. настройки отдела требуют подключение телефонии, то должны сделать запрос к воксу, создать в нем учетку, и
     * сохранить идентификатор сотрудника на воксе.
     */
    @Test
    public void createEmployee_telephonyRequired_true() {
        var staffLogin = Randoms.string();
        Long resultId = 123L;

        ImmutableMap<String, Object> properties = ImmutableMap.of(
                Employee.TITLE, Randoms.string(),
                Employee.STAFF_LOGIN, staffLogin,
                Employee.OU, createOu(true)
        );

        environment
                .when(HttpRequest.post(baseUrl + "/AddUser")
                        .param("user_name", staffLogin)
                        .param("user_display_name", staffLogin)
                        .param("application_id", applicationId))
                .then(ResponseBuilder.newBuilder()
                        .body(serializeService.serialize(Map.of(
                                "result", 1,
                                "user_id", resultId
                        )))
                        .build());

        Employee employee = bcpService.create(Employee.FQN_DEFAULT, properties);


        // проверка утверждений
        Long value = employee.getVoximplantId();
        Assertions.assertEquals(resultId, value);
    }

    /**
     * Т.к. настройки отдела не требуют подключение телефонии, то не должны сделать запрос к воксу, и
     * сохранить идентификатор сотрудника на воксе.
     */
    @Test
    public void createEmployee_telephonyRequired_false() {
        var staffLogin = Randoms.string();
        Long resultId = 123L;

        ImmutableMap<String, Object> properties = ImmutableMap.of(
                Employee.TITLE, Randoms.string(),
                Employee.STAFF_LOGIN, staffLogin,
                Employee.OU, createOu(false)
        );

        environment
                .when(HttpRequest.post(baseUrl + "/AddUser")
                        .param("user_name", staffLogin)
                        .param("user_display_name", staffLogin)
                        .param("application_id", applicationId))
                .then(ResponseBuilder.newBuilder()
                        .body(serializeService.serialize(Map.of(
                                "result", 1,
                                "user_id", resultId
                        )))
                        .build());

        Employee employee = bcpService.create(Employee.FQN_DEFAULT, properties);


        // проверка утверждений
        Long value = employee.getVoximplantId();
        Assertions.assertNull(value);
    }

    @Test
    public void archiveEmployee() {
        var staffLogin = Randoms.string();
        Long resultId = 123L;

        ImmutableMap<String, Object> properties = ImmutableMap.of(
                Employee.TITLE, Randoms.string(),
                Employee.STAFF_LOGIN, staffLogin,
                Employee.OU, createOu(true),
                Employee.VOX_ENABLED, true
        );

        environment
                .when(HttpRequest.post(baseUrl + "/AddUser")
                        .param("user_name", staffLogin)
                        .param("user_display_name", staffLogin)
                        .param("application_id", applicationId))
                .then(ResponseBuilder.newBuilder()
                        .body(serializeService.serialize(Map.of(
                                "result", 1,
                                "user_id", resultId
                        )))
                        .build());

        Employee employee = bcpService.create(Employee.FQN_DEFAULT, properties);

        environment.when(HttpRequest.post(baseUrl + "/DelUser").param("user_id", resultId.toString()))
                .then(ResponseBuilder.newBuilder()
                        .body(serializeService.serialize(Map.of("result", 1)))
                        .build());

        bcpService.edit(employee, Map.of("status", "archived"));

        // проверка утверждений
        Long value = employee.getVoximplantId();
        Assertions.assertNull(value);
    }

    private Ou createOu(boolean telephonyRequired) {
        ImmutableMap<String, Object> properties = ImmutableMap.of(
                Employee.TITLE, Randoms.string(),
                "telephonyRequired", telephonyRequired
        );
        return bcpService.create(Ou.FQN_DEFAULT, properties);
    }
}
