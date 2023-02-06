package ru.yandex.direct.intapi.entity.connect.controller;

import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.connect.model.ConnectIdmRoles;
import ru.yandex.direct.intapi.entity.connect.model.InfoResponse;
import ru.yandex.direct.intapi.entity.connect.model.InfoResponseField;
import ru.yandex.direct.intapi.entity.connect.model.InfoResponseRoles;
import ru.yandex.direct.intapi.entity.connect.model.InfoResponseRolesGroup;
import ru.yandex.direct.intapi.entity.connect.model.InfoResponseSubRoles;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.direct.utils.JsonUtils.fromJson;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ConnectIdmControllerInfoTest extends ConnectIdmControllerBase {
    private static final String GET_INFO_PATH = "/connect/idm/info";
    private static final String EXPECTED_JSON = "{\n" +
            "       \"roles\": {\n" +
            "         \"slug\": \"direct\",\n" +
            "         \"name\": \"сервис\",\n" +
            "         \"values\": {\n" +
            "           \"organization\": {\n" +
            "             \"name\": \"организация\",\n" +
            "             \"roles\": {\n" +
            "               \"values\": {\n" +
            "                 \"associated\": \"привязанная\"\n" +
            "               },\n" +
            "               \"slug\": \"role\",\n" +
            "               \"name\": \"роль\"\n" +
            "             }\n" +
            "           },\n" +
            "           \"user\": {\n" +
            "             \"name\": \"пользователь\",\n" +
            "             \"roles\": {\n" +
            "               \"values\": {\n" +
            "                 \"chief\": \"основной представитель\",\n" +
            "                 \"employee\": \"представитель\"\n" +
            "               },\n" +
            "               \"slug\": \"role\",\n" +
            "               \"name\": \"роль\"\n" +
            "             }\n" +
            "           }\n" +
            "         }\n" +
            "       },\n" +
            "       \"fields\": [\n" +
            "         {\n" +
            "           \"slug\": \"resource_id\",\n" +
            "           \"name\": \"идентификатор клиента в Директе\",\n" +
            "           \"required\": true,\n" +
            "           \"type\": \"charfield\"\n" +
            "         }\n" +
            "       ],\n" +
            "       \"code\": 0\n" +
            "     }";


    @Before
    public void setUp() {
        super.initTest();
    }

    @Test
    public void getInfo() throws Exception {
        InfoResponseRolesGroup userRoles = new InfoResponseRolesGroup();
        userRoles.setName("пользователь");
        userRoles.setRoles(makeSubRoles(Set.of(ConnectIdmRoles.EMPLOYEE, ConnectIdmRoles.CHIEF)));

        InfoResponseRolesGroup organizationRoles = new InfoResponseRolesGroup();
        organizationRoles.setName("организация");
        organizationRoles.setRoles(makeSubRoles(Set.of(ConnectIdmRoles.ASSOCIATED)));

        InfoResponseRoles expectedRoles = new InfoResponseRoles();
        expectedRoles.setSlug("direct");
        expectedRoles.setName("сервис");
        expectedRoles.setValues(Map.of(
                "user", userRoles,
                "organization", organizationRoles
        ));

        InfoResponse expectedResponse = new InfoResponse()
                .withFields(singletonList(
                        new InfoResponseField()
                                .withRequired(true)
                                .withSlug("resource_id")
                                .withName("идентификатор клиента в Директе")
                                .withType("charfield")
                ))
                .withRoles(expectedRoles);


        //Дергаем ручку
        String jsonResponse = doRequest(get(GET_INFO_PATH), null);
        InfoResponse actualResponse = fromJson(jsonResponse, InfoResponse.class);

        //Проверяем ответ
        //Код ответа в модели всегда "0", поэтому проверим его прямо в сыром ответе
        assertThat(jsonResponse).as("has code: 0").containsPattern("\"code\":\\s*0\\W");
        //Данные
        assertThat(actualResponse).as("data is valid")
                .isEqualToComparingFieldByFieldRecursively(expectedResponse);
    }

    @Test
    public void simpleJsonCheck() throws Exception {
        //Дергаем ручку
        String jsonResponse = doRequest(get(GET_INFO_PATH), null);
        JSONAssert.assertEquals(EXPECTED_JSON, jsonResponse, false);
    }

    private InfoResponseSubRoles makeSubRoles(Set<ConnectIdmRoles> roles) {
        InfoResponseSubRoles item = new InfoResponseSubRoles();
        item.setSlug("role");
        item.setName("роль");
        item.setValues(roles);

        return item;
    }
}
