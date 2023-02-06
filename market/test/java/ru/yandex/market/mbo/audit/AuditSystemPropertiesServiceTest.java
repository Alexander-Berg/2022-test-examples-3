package ru.yandex.market.mbo.audit;

import org.junit.Test;

import ru.yandex.market.mbo.http.MboAudit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuramalinov
 * @created 21.11.2019
 */
public class AuditSystemPropertiesServiceTest {
    @Test
    public void testMatches() {
        AuditSystemPropertiesService service = new AuditSystemPropertiesService("test-properties.txt");
        assertThat(service.isSystemAction(MboAudit.EntityType.CM_BLUE_OFFER, "acceptance_status_modified")).isTrue();
        assertThat(service.isSystemAction(MboAudit.EntityType.CM_BLUE_OFFER, "some_other_prop")).isFalse();
        assertThat(service.isSystemAction(MboAudit.EntityType.MODEL_SKU, "Дата редактирования")).isTrue();
        assertThat(service.isSystemAction(MboAudit.EntityType.DEPENDENCY_RULE, "Дата редактирования")).isFalse();
    }

    @Test
    public void testDefaultInstantiation() {
        // shouldn't fail
        new AuditSystemPropertiesService();
    }
}
