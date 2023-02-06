package ru.yandex.market.b2bcrm.module.pickuppoint;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import jdk.jfr.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.b2bcrm.module.business.process.Bp;
import ru.yandex.market.b2bcrm.module.business.process.BpState;
import ru.yandex.market.b2bcrm.module.business.process.BpStatus;
import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.b2bcrm.module.ticket.test.utils.BpTestUtils;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.ui.impl.FiltrationScriptService;

import static ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert.assertThat;

@B2bPickupPointTests
@ExtendWith(SpringExtension.class)
public class PupBpStatusMappingTest {

    @Inject
    private BpTestUtils bpTestUtils;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private FiltrationScriptService filtrationScriptService;

    @Inject
    private MetadataService metadataService;

    @Inject
    private DbService dbService;

    private Bp bp;

    private BpStatus status1, status2, status3, otherStatus;

    @BeforeEach
    void setUp() {
        status1 = bpTestUtils.createBpStatus("Первый");
        status2 = bpTestUtils.createBpStatus("Второй");
        status3 = bpTestUtils.createBpStatus("Третий");
        otherStatus = bpTestUtils.createBpStatus("От другого БП");
        BpState bpState = bpTestUtils.createBpState(status1, List.of(status2, status3));
        bp = bpTestUtils.createBp("someBp", List.of(bpState));
    }

    @ParameterizedTest
    @CsvSource({
            "prePickupPointTicketBp, prePickupPointBpStatusMapping",
            "pickupPointPotentialTicketBp, preLegalPartnerBpStatusMapping",
    })
    @Description("""
            Префильтрация статусов БП для лидов / лидов партнеров ПВЗ в справочнике маппинга
            https://testpalm.yandex-team.ru/testcase/ocrm-1472
            https://testpalm.yandex-team.ru/testcase/ocrm-1473
            """)
    void testMappingBpStatusFiltration(String property, String mappingFqn) {
        configurationService.setValue(property, bp);
        Fqn fqn = Fqn.of(mappingFqn);
        Attribute attribute = metadataService.getMetaclass(fqn)
                .getAttribute(PupBpStatusMapping.BP_STATUS);
        Filter filter = filtrationScriptService.getFilter(attribute, Map.of(), null)
                .orElse(Filters._true());
        assertThat(dbService.list(Query.of(BpStatus.FQN).withFilters(filter)))
                .hasSize(3)
                .anyHasAttributes(BpStatus.GID, status1.getGid())
                .anyHasAttributes(BpStatus.GID, status2.getGid())
                .anyHasAttributes(BpStatus.GID, status3.getGid())
                .noneHasAttributes(BpStatus.GID, otherStatus.getGid());
    }
}
