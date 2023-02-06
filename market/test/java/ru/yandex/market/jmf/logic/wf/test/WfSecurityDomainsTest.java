package ru.yandex.market.jmf.logic.wf.test;

import java.util.List;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.logic.wf.impl.security.WfSecurityMarker;
import ru.yandex.market.jmf.logic.wf.impl.security.WfSecurityMarkersGroup;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.security.SecurityDomainsService;
import ru.yandex.market.jmf.security.impl.marker.domain.SecurityDomain;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringJUnitConfig(InternalLogicWfTestConfiguration.class)
public class WfSecurityDomainsTest {
    private final SecurityDomainsService securityDomainsService;

    public WfSecurityDomainsTest(SecurityDomainsService securityDomainsService) {
        this.securityDomainsService = securityDomainsService;
    }

    @Test
    public void testTransitionsNotInherited() {
        SecurityDomain rootMC$child3 = securityDomainsService.getDomain(Fqn.of("rootMC$child3"));
        WfSecurityMarkersGroup group = rootMC$child3.getSecurityMarkerGroup(WfSecurityMarkersGroup.class);
        Assertions.assertEquals(List.of(), group.getMarkers("s0", "s2"));
        assertThat(group.getMarkers("s0", "s1"))
                .hasSize(3)
                .extracting(WfSecurityMarker::getGid)
                .containsExactlyInAnyOrder("@wf:*:*", "@wf:*:s1", "manage");
    }
}
