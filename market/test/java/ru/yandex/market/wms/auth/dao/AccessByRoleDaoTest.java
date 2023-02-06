package ru.yandex.market.wms.auth.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.model.dto.Screen;
import ru.yandex.market.wms.auth.model.enums.AccessObject;
import ru.yandex.market.wms.auth.model.enums.AccessType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccessByRoleDaoTest extends AuthIntegrationTest {

    @Autowired
    private AccessByRoleDao dao;

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    @ExpectedDatabase(
            value = "/db/dao/access-by-role/create-access-after-dao.xml",
            connection = "authConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createAccessTest() {
        dao.createAccessByCode(
                AccessObject.SCREEN,
                "TRANSPORT",
                Set.of("ADMINISTRATOR", "ADMINISTRATOR1", "ADMINISTRATOR2"),
                AccessType.FULL);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    public void getCodesByRoleTest() {
        List<String> codes = dao.getCodesByRole("ADMINISTRATOR");
        assertThat(codes).hasSameElementsAs(List.of("INBOUND", "RECEIVING", "ALL"));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    public void getRolesByCodeTest() {
        List<String> codes = dao.getRolesByCode("RECEIVING");
        assertEquals(codes, List.of("ADMINISTRATOR", "ADMINISTRATOR1", "ADMINISTRATOR2", "DROPPING"));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/access-by-role/before.xml", connection = "authConnection")
    public void getMappingCodeRolesTest() {
        List<Screen> screenActual = dao.getMappingCodeRoles();

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
