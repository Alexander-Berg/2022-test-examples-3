package ru.yandex.market.antifraud.orders.storage.dao;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.storage.entity.DetectorDescription;
import ru.yandex.market.antifraud.orders.storage.entity.DetectorType;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerIdRole;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerRole;
import ru.yandex.market.antifraud.orders.storage.entity.rules.BaseDetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.rules.DetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.rules.PostPayLimitDetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.rules.UsedCoinsDetectorConfiguration;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;
import ru.yandex.market.antifraud.orders.test.config.DaoTestConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DaoTestConfiguration.class, RoleDao.class})
public class RoleDaoTest {

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    @Bean
    public static RoleDao roleDao(NamedParameterJdbcOperations jdbcTemplate) {
        return new RoleDao(jdbcTemplate);
    }

    @Test
    public void saveDetectorDescription() {
        String name = "test_dd";
        DetectorDescription dd = simpleDd(name);
        Optional<DetectorDescription> ddO1 = roleDao.getDescriptionByName(name);
        assertThat(ddO1).isEmpty();
        dd = roleDao.saveDetectorDescriptionIfAbsent(dd);
        dd = roleDao.saveOrUpdateDetectorDescription(dd);
        assertThat(dd.getId()).isNotNull();
        Optional<DetectorDescription> ddO2 = roleDao.getDescriptionByName(name);
        assertThat(ddO2).isPresent();
    }

    @Test
    public void getDescriptionsByNames() {
        roleDao.saveOrUpdateDetectorDescription(simpleDd("name_dd_1"));
        roleDao.saveOrUpdateDetectorDescription(simpleDd("name_dd_2"));
        roleDao.saveOrUpdateDetectorDescription(simpleDd("name_dd_3"));

        List<DetectorDescription> descriptions = roleDao.getDescriptionByNames(Arrays.asList("name_dd_1", "name_dd_2"));
        assertThat(descriptions).hasSize(2);
    }

    @Test
    public void getRoles() {
        BuyerRole role1 = BuyerRole.builder()
                .name("getRoles_role1")
                .description("getRoles")
                .vip(false)
                .detectorConfigurations(Map.of())
                .build();
        BuyerRole role2 = BuyerRole.builder()
                .name("getRoles_role2")
                .description("getRoles")
                .vip(true)
                .detectorConfigurations(Map.of())
                .build();
        role1 = roleDao.saveRole(role1);
        List<BuyerRole> roles = roleDao.getAllRoles();
        assertThat(roles).contains(role1);
        role2 = roleDao.saveRole(role2);
        roles = roleDao.getAllRoles();
        assertThat(roles).contains(role1, role2);
    }


    @Test
    public void getDescriptions() {
        roleDao.saveOrUpdateDetectorDescription(simpleDd("name_dd_11"));
        roleDao.saveOrUpdateDetectorDescription(simpleDd("name_dd_22"));
        roleDao.saveOrUpdateDetectorDescription(simpleDd("name_dd_33"));
        Integer count = jdbcTemplate.query("select count(*) as cnt from detectors",
                (rs, n) -> rs.getInt("cnt")).stream().findAny().orElseThrow(RuntimeException::new);
        List<DetectorDescription> descriptions = roleDao.getDetectorDescriptions();
        assertThat(count).isGreaterThan(0);
        assertThat(descriptions).hasSize(count);
    }

    @Test
    public void getRoleByName() {
        DetectorDescription dd = simpleDd("test_detector_getRoleByName");
        Map<String, DetectorConfiguration> confs =
                ImmutableMap.of(dd.getName(), new BaseDetectorConfiguration(true));
        String roleName = "test_role_getRoleByName";
        BuyerRole role = BuyerRole.builder()
                .name(roleName)
                .description("getRoleByName")
                .detectorConfigurations(confs)
                .build();

        roleDao.saveOrUpdateDetectorDescription(dd);
        roleDao.saveRole(role);

        Optional<BuyerRole> roleO = roleDao.getRoleByName(roleName);
        assertThat(roleO).isPresent();
        assertThat(roleO.get().getDetectorConfigurations()).isNotEmpty();
    }

    @Test
    public void getUsersByRole() {
        BuyerRole role = BuyerRole.builder()
                .name("getUsersByRole_role")
                .description("A test role")
                .detectorConfigurations(Map.of())
                .build();
        role = roleDao.saveRole(role);
        roleDao.addUidToRole("23212312", role);
        List<BuyerIdRole> buyers = roleDao.getUsersByRole(role);
        assertThat(buyers).hasSize(1);
        roleDao.addUidToRole("23212313", role);
        buyers = roleDao.getUsersByRole(role);
        assertThat(buyers).hasSize(2);
    }

    @Test
    public void getRoleByUid() {
        DetectorDescription dd = simpleDd("test_detector_getRoleByUid");
        final String UID = "12345";
        Map<String, DetectorConfiguration> confs =
                ImmutableMap.of(dd.getName(), new BaseDetectorConfiguration(true));
        String roleName = "test_role_getRoleByUid";
        BuyerRole role = BuyerRole.builder()
                .name(roleName)
                .description("getRoleByUid")
                .detectorConfigurations(confs)
                .build();

        roleDao.saveOrUpdateDetectorDescription(dd);
        role = roleDao.saveRole(role);

        roleDao.addUidToRole(UID, role);

        Optional<BuyerRole> roleO = roleDao.getRoleByUid(UID);
        assertThat(roleO).isPresent();
        assertThat(roleO.get().getDetectorConfigurations()).isNotEmpty();
    }

    @Test
    public void getRoleByUidCheckDeserializationInCache() {
        DetectorDescription baseDd = simpleDd("test_detector_getRoleByUid_base");
        DetectorDescription postPayDd = simpleDd("test_detector_getRoleByUid_postPay");
        DetectorDescription coinsDd = simpleDd("test_detector_getRoleByUid_coins");
        final String UID = "12345";
        Map<String, DetectorConfiguration> confs =
                ImmutableMap.<String, DetectorConfiguration>builder()
                        .put(baseDd.getName(), new BaseDetectorConfiguration(true))
                        .put(postPayDd.getName(), new PostPayLimitDetectorConfiguration(true, 5, 5))
                        .put(coinsDd.getName(), new UsedCoinsDetectorConfiguration(true, 5, 5))
                        .build();
        String roleName = "test_role_getRoleByUid";
        BuyerRole role = BuyerRole.builder()
                .name(roleName)
                .description("getRoleByUid")
                .detectorConfigurations(confs)
                .build();

        roleDao.saveOrUpdateDetectorDescription(baseDd);
        roleDao.saveOrUpdateDetectorDescription(postPayDd);
        roleDao.saveOrUpdateDetectorDescription(coinsDd);
        role = roleDao.saveRole(role);

        roleDao.addUidToRole(UID, role);

        Optional<BuyerRole> roleO = roleDao.getRoleByUid(UID);
        assertThat(roleDao.getRoleByUid(UID)).isEqualTo(roleO);
    }

    @Test
    public void rewriteRole() {
        DetectorDescription dd1 = simpleDd("test_detector_rewriteRole_1");
        DetectorDescription dd2 = simpleDd("test_detector_rewriteRole_2");
        Map<String, DetectorConfiguration> confs1 =
                ImmutableMap.of(dd1.getName(), new BaseDetectorConfiguration(true));
        String roleName = "test_role_rewriteRole";
        BuyerRole role = BuyerRole.builder()
                .name(roleName)
                .description("A test role")
                .detectorConfigurations(confs1)
                .build();
        roleDao.saveOrUpdateDetectorDescription(dd1);
        roleDao.saveOrUpdateDetectorDescription(dd2);
        roleDao.saveRole(role);

        Optional<BuyerRole> roleO = roleDao.getRoleByName(roleName);
        assertThat(roleO).isPresent();
        assertThat(roleO.get().getDetectorConfigurations()).hasSize(1);

        Map<String, DetectorConfiguration> confs2 =
                ImmutableMap.of(dd1.getName(), new BaseDetectorConfiguration(true),
                        dd2.getName(), new BaseDetectorConfiguration(true));
        role = role.toBuilder().detectorConfigurations(confs2).build();

        roleDao.saveRole(role);
        Optional<BuyerRole> roleO_2 = roleDao.getRoleByName(roleName);
        assertThat(roleO_2).isPresent();
        assertThat(roleO_2.get().getDetectorConfigurations()).hasSize(2);
    }

    @Test
    public void testInitialMigration() {
        Optional<BuyerRole> roleO = roleDao.getRoleByName("whitelist");
        assertThat(roleO).isPresent();
        BuyerRole role = roleO.get();
        assertThat(role.getDetectorConfigurations().values().stream().noneMatch(DetectorConfiguration::isEnabled)).isTrue();

        assertThat(roleDao.getRoleByUid("2308324861409815965")).isPresent();
        assertThat(roleDao.getRoleByUid("917685447")).isPresent();
        assertThat(roleDao.getRoleByUid("863340080")).isPresent();
    }


    private DetectorDescription simpleDd(String name) {
        return DetectorDescription.builder()
                .name(name)
                .detectorType(DetectorType.ORDER)
                .defaultConfiguration(new BaseDetectorConfiguration(true))
                .build();
    }
}
