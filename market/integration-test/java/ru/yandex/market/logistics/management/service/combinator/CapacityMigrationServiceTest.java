package ru.yandex.market.logistics.management.service.combinator;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.repository.PartnerCapacityRepository;
import ru.yandex.market.logistics.management.repository.combinator.ServiceCapacityRepository;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("/data/service/combinator/db/before/service_codes.xml")
@DatabaseSetup(
    value = "/data/service/combinator/db/before/regions.xml",
    type = DatabaseOperation.INSERT
)
class CapacityMigrationServiceTest extends AbstractContextualTest {

    @Autowired
    private CapacityMigrationService capacityMigrationService;

    @Autowired
    private PartnerCapacityRepository partnerCapacityRepository;

    @Autowired
    private ServiceCapacityRepository serviceCapacityRepository;

    @Test
    @DatabaseSetup("/data/service/combinator/db/before/partner_capacity_cascade_delete.xml")
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_cascade_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCapacityCascadeDelete() {
        serviceCapacityRepository.deleteById(5L);
        serviceCapacityRepository.deleteById(4L);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_optimized_deletion.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_optimized_deletion.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testOptimizedDeletion() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_optimized_insertion.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_optimized_insertion.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testOptimizedInsertion() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    /**
     * Ожидаемое дерево капасити для ФФ
     * <pre>
     * 2 root
     *   |
     *   ---2 inbound
     *   |
     *   ---2 shipment
     * </pre>
     * <p>
     * Ожидаемое дерево капасити для кроссдока
     * <pre>
     * 3 root
     *   |
     *   ---3 shipment
     * </pre>
     */
    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_to_migrate.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_migration_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void firstMigration() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    /**
     * Ожидаемое дерево капасити для ФФ (location_to = 10000)
     * <pre>
     * 2 root
     *   |
     *   ---2 inbound
     *   |
     *   ---2 shipment
     * </pre>
     * <p>
     * Ожидаемое дерево капасити для кроссдока
     * <pre>
     * 3 root
     *   |
     *   ---3 shipment
     * </pre>
     */
    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_to_migrate_region_10000.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_migration_expected_region_10000.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void firstMigrationRegion10000() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    /**
     * Ожидаемое дерево капасити для ФФ
     * <pre>
     * 2 root
     *   |
     *   ---2 inbound
     *   |
     *   ---2 shipment
     * </pre>
     */
    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_already_migrated.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_migration_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateSameDataSecondTime() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    /**
     * Ожидаемое дерево капасити для ФФ
     * <pre>
     * 2 root
     *   |
     *   ---2 inbound
     *   |
     *   ---2 shipment
     * </pre>
     */
    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_updated_to_migrate.xml",
        type = DatabaseOperation.INSERT,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_updated_migration_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateNewCapacityAddedSomeDeleted() {
        partnerCapacityRepository.deleteById(6L);
        partnerCapacityRepository.deleteById(13L);
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    /**
     * Проверяем ФФ миграцию для уже полу заполненного дерева капасити. Ожидаем:
     * - service_capacity id=102 is deleted, because it is not referenced from service_capacity_value
     *     - service_capacity id=1 is created to replace it
     * - service_capacity_value id=1 is created, referencing service_capacity id=1
     * - service_capacity_value id=2 and id=3 are created, referencing service_capacity id=103
     */
    @Test
    @DatabaseSetup("/data/service/combinator/db/before/partner_capacity_partial_tree.xml")
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_partial_tree.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateCapacityOverPartialTree() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_supplier_capacity_already_migrated.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_supplier_capacity_migration_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateOnChangeSupplierShipmentType() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_with_tm_movements.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_with_tm_movements_migration_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void ignoreTransportManagerMovements() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_with_two_linehauls.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_linehaul_capacity_propagated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void propagateLinehaulCapacity() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/delivery_capacity_tree_migration.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/delivery_capacity_tree_migration.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateDeliveryCapacityForDeliveryTree() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/delivery_capacity_with_linehauls.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/delivery_capacity_with_linehauls.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateDeliveryCapacityToLinehauls() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_mk.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/delivery_partner_capacity_mk.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/delivery_partner_capacity_mk.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateDeliveryCapacityToMovements() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_mk.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/delivery_partner_capacity_mk.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/set_sc_partner_lavka_subtype.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/delivery_capacity_for_movement_to_lavka.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateDeliveryCapacityToMovementsWorksOnlyForLavka() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_mk.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/set_sc_partner_lavka_subtype.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/movement_capacity_for_movement_to_lavka.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateMovementCapacityToMovementsWorksOnlyForLavka() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/delivery_capacity_with_linehauls.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/delivery_capacity_with_linehauls_with_pickup_dt.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/delivery_capacity_with_linehauls_with_pickup_dt.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateDeliveryCapacityToLinehaulsWithPickupDeliveryType() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_with_two_linehauls_for_pek.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_linehaul_capacity_for_pek_propagated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void propagateLinehaulCapacityForPEK() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
        capacityMigrationService.propagateLinehaulCapacitiesForPEK();
    }

    @Test
    @DatabaseSetup("/data/service/combinator/db/before/partner_capacity_with_unrelated_linehaul.xml")
    @ExpectedDatabase(
        value = "/data/service/combinator/db/before/partner_capacity_with_unrelated_linehaul.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void siblingLinehaulIsNotMistakenForParent() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_with_accuratest_location.xml"
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_with_accuratest_location.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateWithMostAccurateLocationFrom() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_new.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/capacity_from_admin_with_default_service_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newCapacityMigration() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partner-capacities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/service/combinator/controller/create_capacity.json"))
        )
            .andExpect(status().isCreated());
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_new.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/create_new_capacity_migration_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void newCapacityFromAdminMigration() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/admin/lms/partner-capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/service/combinator/controller/create_capacity_from_admin.json"))
        )
            .andExpect(status().isCreated());
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_delete.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/delete_capacity_migration_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void deleteCapacityMigration() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
            .delete("/admin/lms/partner-capacity/{id}", 6)
        )
            .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_reassign.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_reassign.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryCapacityValueReassign() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/sync_partner_relation_with_capacity.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/sync_partner_relation_with_capacity.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("При изменении связки, капасити на старом сервисе остается")
    void capacityDoesntDisappearFromDeactivatedService() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_mk.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_mk_migrated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void assignMkCapacityOnlyForLastMile() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/partner_capacity_few_segments_for_same_partner.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/partner_capacity_few_segments_for_same_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void assignCapacityOnLastServicesForPath() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DisplayName("Миграция капасити на доставку с регионами")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/delivery_capacity_with_regions.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/delivery_capacity_with_regions.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateDeliveryCapacityWithRegions() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DisplayName("Миграция капасити на доставку с регионами, добавляется капасити в регион")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/delivery_capacity_with_regions_update_services.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/delivery_capacity_with_regions_update_services.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateDeliveryCapacityWithAddedRegions() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

    @Test
    @DisplayName("Добавляем общий капасити, когда уже был капасити с курьеркой")
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/delivery_capacity_with_courier.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/delivery_capacity_with_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateDeliveryCapacityAddCapacityWithoutDeliveryType() {
        capacityMigrationService.migratePartnerCapacityToServiceCapacity();
    }

}
