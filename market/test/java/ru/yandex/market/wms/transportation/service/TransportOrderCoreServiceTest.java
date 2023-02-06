package ru.yandex.market.wms.transportation.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.transportation.controller.visitor.RuleCreatingVisitor;
import ru.yandex.market.wms.transportation.core.domain.TransportUnitQualifier;
import ru.yandex.market.wms.transportation.core.domain.TransportUnitType;
import ru.yandex.market.wms.transportation.core.model.To;
import ru.yandex.market.wms.transportation.model.rule.DestinationRule;
import ru.yandex.market.wms.transportation.model.rule.DirectDestinationRule;

import static ru.yandex.market.wms.transportation.core.domain.TransportUnitType.CONTAINER;


class TransportOrderCoreServiceTest extends IntegrationTest {

    @Autowired
    private TransportOrderCoreService coreService;

    @Test
    @DatabaseSetup("/service/transportordercore/checkcontainertransportordercreate/initial-state.xml")
    @ExpectedDatabase(value = "/service/transportordercore/checkcontainertransportordercreate/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkContainerTransportOrderCreate() {
        TransportUnitQualifier transportUnit = TransportUnitQualifier.builder()
                .id("1")
                .type(CONTAINER)
                .build();
        DirectDestinationRule destinationRule =
                DirectDestinationRule.builder()
                        .cells(Collections.singleton("ST_IN"))
                        .build();
        coreService.createOrder("ST_OUT", destinationRule, transportUnit);
    }

    @Test
    @DatabaseSetup("/service/transportordercore/checkpallettransportordercreate/before.xml")
    @ExpectedDatabase(value = "/service/transportordercore/checkpallettransportordercreate/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkPalletTransportOrderCreate() {
        TransportUnitQualifier transportUnit = TransportUnitQualifier.builder()
                .id("PLT0000001")
                .type(TransportUnitType.PALLET)
                .build();

        Set<String> zones = new HashSet();
        zones.add("PL_STR");

        To to = To.Zones.builder()
                .zones(zones)
                .build();

        RuleCreatingVisitor ruleCreatingVisitor = new RuleCreatingVisitor();
        DestinationRule rule = to.accept(ruleCreatingVisitor);
        coreService.createOrder("STAGE01", rule, transportUnit);
    }
}
