package ru.yandex.market.jmf.module.ticket.test;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.AttributeTypeService;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.metadata.AttributeFqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.module.ticket.DistributionService;
import ru.yandex.market.jmf.module.ticket.EmployeeDistributionStatus;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.utils.Maps;

import static ru.yandex.market.jmf.module.ticket.Ou.CAPACITY;
import static ru.yandex.market.jmf.module.ticket.Ou.CAPACITY_OVERRIDE;

@SpringJUnitConfig(classes = ModuleTicketTestConfiguration.class)
public class OuCapacityTest {

    @Inject
    private BcpService bcpService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private DistributionService distributionService;
    @Inject
    private OuTestUtils ouTestUtils;
    @Inject
    private AttributeTypeService attributeTypeService;
    @Inject
    private MetadataService metadataService;
    @Inject
    private EntityService entityService;

    /**
     * Проверяем, что при установке значения {@code capacityOverride} значение {@code capacity} у всех дочерних
     * отделов и сотрудников тоже меняется
     */
    @Test
    @Transactional
    public void capacityOverrideChangesCapacityForNestedOuAndEmployees() {
        final var root = ouTestUtils.createOu();

        final var a = ouTestUtils.createOu(root);
        final var aa = ouTestUtils.createOu(a);
        final var ab = ouTestUtils.createOu(a);

        final var employeeAa = ticketTestUtils.createEmployee(aa);
        final var employeeAb = ticketTestUtils.createEmployee(aa);

        final var b = ouTestUtils.createOu(root);
        final var ba = ouTestUtils.createOu(b);
        final var bb = ouTestUtils.createOu(b);

        final var employeeBa = ticketTestUtils.createEmployee(ba);
        final var employeeBb = ticketTestUtils.createEmployee(bb);

        // вызов системы
        bcpService.edit(a, Map.of(CAPACITY_OVERRIDE, 5));

        // проверка утверждений
        Assertions.assertEquals(5, (long) a.<Long>getAttribute(CAPACITY));
        Assertions.assertEquals(5, (long) aa.<Long>getAttribute(CAPACITY));
        Assertions.assertEquals(5, (long) ab.<Long>getAttribute(CAPACITY));

        final var employeeAaStatus = distributionService.setEmployeeStatus(employeeAa,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        final var employeeAbStatus = distributionService.setEmployeeStatus(employeeAb,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        Assertions.assertEquals(5, (long) employeeAaStatus.getCapacity());
        Assertions.assertEquals(5, (long) employeeAbStatus.getCapacity());

        final var ouCapacityAttribute = metadataService.getAttributeOrError(AttributeFqn.of("ou@capacity"));
        final var ouDefaultCapacity = attributeTypeService.wrap(ouCapacityAttribute,
                entityService.getDefaultValue(ouCapacityAttribute));
        Assertions.assertEquals(ouDefaultCapacity, b.getAttribute(CAPACITY));
        Assertions.assertEquals(ouDefaultCapacity, ba.getAttribute(CAPACITY));
        Assertions.assertEquals(ouDefaultCapacity, bb.getAttribute(CAPACITY));

        final var employeeBaStatus = distributionService.setEmployeeStatus(employeeBa,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        final var employeeBbStatus = distributionService.setEmployeeStatus(employeeBb,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        Assertions.assertEquals(ouDefaultCapacity, employeeBaStatus.getCapacity());
        Assertions.assertEquals(ouDefaultCapacity, employeeBbStatus.getCapacity());
    }

    /**
     * Проверяем, что при установке значения {@code capacityOverride} значение {@code capacity} у всех дочерних
     * отделов и сотрудников тоже меняется
     */
    @Test
    @Transactional
    public void capacityOverrideChangesCapacityForNestedOuAndEmployeesButSkipsOuAndEmployeeWithOverriddenCapacity() {
        final var root = ouTestUtils.createOu();

        final var a = ouTestUtils.createOu(root);
        final var aa = ouTestUtils.createOu(a);
        final var ab = ouTestUtils.createOu(a);

        final var employeeAa1 = ticketTestUtils.createEmployee(aa);
        final var employeeAa2 = ticketTestUtils.createEmployee(aa);
        final var employeeAb1 = ticketTestUtils.createEmployee(ab);
        final var employeeAb2 = ticketTestUtils.createEmployee(ab);

        final var b = ouTestUtils.createOu(root);
        final var ba = ouTestUtils.createOu(b);
        final var bb = ouTestUtils.createOu(b);

        final var employeeBa = ticketTestUtils.createEmployee(ba);
        final var employeeBb = ticketTestUtils.createEmployee(bb);

        bcpService.edit(aa, Map.of(CAPACITY_OVERRIDE, 2));
        bcpService.edit(employeeAb1, Map.of(CAPACITY_OVERRIDE, 3));

        // вызов системы
        bcpService.edit(a, Map.of(CAPACITY_OVERRIDE, 5));

        // проверка утверждений
        Assertions.assertEquals(5, (long) a.<Long>getAttribute(CAPACITY));
        Assertions.assertEquals(2, (long) aa.<Long>getAttribute(CAPACITY));
        Assertions.assertEquals(5, (long) ab.<Long>getAttribute(CAPACITY));

        final var employeeAa1Status = distributionService.setEmployeeStatus(employeeAa1,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        final var employeeAa2Status = distributionService.setEmployeeStatus(employeeAa2,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        final var employeeAb1Status = distributionService.setEmployeeStatus(employeeAb1,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        final var employeeAb2Status = distributionService.setEmployeeStatus(employeeAb2,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        Assertions.assertEquals(2, (long) employeeAa1Status.getCapacity());
        Assertions.assertEquals(2, (long) employeeAa2Status.getCapacity());
        Assertions.assertEquals(3, (long) employeeAb1Status.getCapacity());
        Assertions.assertEquals(5, (long) employeeAb2Status.getCapacity());

        final var ouCapacityAttribute = metadataService.getAttributeOrError(AttributeFqn.of("ou@capacity"));
        final var ouDefaultCapacity = attributeTypeService.wrap(ouCapacityAttribute,
                entityService.getDefaultValue(ouCapacityAttribute));
        Assertions.assertEquals(ouDefaultCapacity, b.getAttribute(CAPACITY));
        Assertions.assertEquals(ouDefaultCapacity, ba.getAttribute(CAPACITY));
        Assertions.assertEquals(ouDefaultCapacity, bb.getAttribute(CAPACITY));

        final var employeeBaStatus = distributionService.setEmployeeStatus(employeeBa,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        final var employeeBbStatus = distributionService.setEmployeeStatus(employeeBb,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        Assertions.assertEquals(ouDefaultCapacity, employeeBaStatus.getCapacity());
        Assertions.assertEquals(ouDefaultCapacity, employeeBbStatus.getCapacity());
    }

    /**
     * Проверяем, что при установке значения {@code capacityOverride} значение {@code capacity} у всех дочерних
     * отделов и сотрудников тоже меняется
     */
    @Test
    @Transactional
    public void capacityOverrideClearingReturnsCapacityToOriginalValue() {
        final var root = ouTestUtils.createOu();

        final var a = ouTestUtils.createOu(root);
        final var aa = ouTestUtils.createOu(a);
        final var ab = ouTestUtils.createOu(a);

        final var employeeAa = ticketTestUtils.createEmployee(aa);
        final var employeeAb = ticketTestUtils.createEmployee(ab);

        final var b = ouTestUtils.createOu(root);
        final var ba = ouTestUtils.createOu(b);
        final var bb = ouTestUtils.createOu(b);

        final var employeeBa = ticketTestUtils.createEmployee(ba);
        final var employeeBb = ticketTestUtils.createEmployee(bb);

        bcpService.edit(aa, Map.of(CAPACITY_OVERRIDE, 2));
        bcpService.edit(a, Map.of(CAPACITY_OVERRIDE, 5));

        // вызов системы
        bcpService.edit(aa, Maps.of(CAPACITY_OVERRIDE, null));

        // проверка утверждений
        Assertions.assertEquals(5, (long) a.<Long>getAttribute(CAPACITY));
        Assertions.assertEquals(5, (long) aa.<Long>getAttribute(CAPACITY));
        Assertions.assertEquals(5, (long) ab.<Long>getAttribute(CAPACITY));

        final var employeeAaStatus = distributionService.setEmployeeStatus(employeeAa,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        final var employeeAbStatus = distributionService.setEmployeeStatus(employeeAb,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        Assertions.assertEquals(5, (long) employeeAaStatus.getCapacity());
        Assertions.assertEquals(5, (long) employeeAbStatus.getCapacity());

        final var ouCapacityAttribute = metadataService.getAttributeOrError(AttributeFqn.of("ou@capacity"));
        final var ouDefaultCapacity = attributeTypeService.wrap(ouCapacityAttribute,
                entityService.getDefaultValue(ouCapacityAttribute));
        Assertions.assertEquals(ouDefaultCapacity, b.getAttribute(CAPACITY));
        Assertions.assertEquals(ouDefaultCapacity, ba.getAttribute(CAPACITY));
        Assertions.assertEquals(ouDefaultCapacity, bb.getAttribute(CAPACITY));

        final var employeeBaStatus = distributionService.setEmployeeStatus(employeeBa,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        final var employeeBbStatus = distributionService.setEmployeeStatus(employeeBb,
                EmployeeDistributionStatus.STATUS_WAIT_TICKET);
        Assertions.assertEquals(ouDefaultCapacity, employeeBaStatus.getCapacity());
        Assertions.assertEquals(ouDefaultCapacity, employeeBbStatus.getCapacity());
    }
}
