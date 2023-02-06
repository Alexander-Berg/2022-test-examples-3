package ru.yandex.market.antifraud.orders.storage.entity.roles;

import java.io.IOException;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.storage.entity.rules.BaseDetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.rules.DetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.rules.PostPayLimitDetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.rules.UsedCoinsDetectorConfiguration;
import ru.yandex.market.volva.utils.DaoUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class BuyerRoleTest {
    @Test
    public void serialization() throws IOException {
        var confs =
                ImmutableMap.<String, DetectorConfiguration>builder()
                        .put("baseDd", new BaseDetectorConfiguration(true))
                        .put("postPayDd", new PostPayLimitDetectorConfiguration(true, 5, 5))
                        .put("coinsDd", new UsedCoinsDetectorConfiguration(true, 5, 5))
                        .build();
        String roleName = "test_role_getRoleByUid";
        var role = BuyerRole.builder()
                .name(roleName)
                .description("getRoleByUid")
                .detectorConfigurations(confs)
                .build();
        var serialized = DaoUtils.DAO_OBJECT_MAPPER.writeValueAsString(role);
        var deserialized = DaoUtils.DAO_OBJECT_MAPPER.readValue(serialized, BuyerRole.class);
        assertThat(deserialized).isEqualTo(role);
    }
}
