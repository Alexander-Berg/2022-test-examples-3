package ru.yandex.market.wms.common.spring;

import java.util.Set;

import lombok.Data;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ru.yandex.market.wms.common.model.enums.InforRole;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

@Data
@Profile(Profiles.TEST)
@Component
public class TestSecurityDataProvider implements SecurityDataProvider {
    private String user = "TEST";
    private String token = "TEST_TOKEN";
    private Set<String> roles = Set.of(InforRole.PACKING, InforRole.PACKING_WITHOUT_SCAN);
}
