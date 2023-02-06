package ru.yandex.market.wms.auth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.model.dto.Screen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccessByRoleServiceTest extends AuthIntegrationTest {

    @Autowired
    private AccessByRoleService service;

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    public void getCodesByRoleTest() {
        List<String> codes = service.getCodesByRole("ADMINISTRATOR");
        assertThat(codes).hasSameElementsAs(List.of("INBOUND", "RECEIVING", "ALL"));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    public void getRolesByCodeTest() {
        List<String> codes = service.getRolesByCode("RECEIVING");
        assertEquals(codes, List.of("ADMINISTRATOR", "ADMINISTRATOR1", "ADMINISTRATOR2", "DROPPING"));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    public void getMappingCodeRolesTest() {
        List<Screen> screenActual = service.getMappingCodeRoles();

        List<Screen> expected = new ArrayList<>();
        expected.add(Screen.builder()
                .code("INBOUND")
                .roles(Set.of("ADMINISTRATOR"))
                .build());
        expected.add(Screen.builder()
                .code("RECEIVING")
                .roles(Set.of("ADMINISTRATOR", "ADMINISTRATOR1", "ADMINISTRATOR2", "DROPPING"))
                .build());

        assertEquals(screenActual, expected);
    }
}
