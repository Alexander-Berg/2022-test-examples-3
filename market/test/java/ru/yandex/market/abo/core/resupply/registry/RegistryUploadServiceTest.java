package ru.yandex.market.abo.core.resupply.registry;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.resupply.registry.RegistryPosition;
import ru.yandex.market.abo.api.entity.resupply.registry.RegistryType;
import ru.yandex.market.abo.api.entity.resupply.registry.UploadRegistryRequest;
import ru.yandex.market.abo.core.resupply.registry.exception.RegistryUploadConflictException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static ru.yandex.market.abo.core.resupply.registry.RegistryUploadService.API_USER_NAME;

class RegistryUploadServiceTest extends EmptyTest {

    @Autowired
    private RegistryUploadService uploadService;
    @Autowired
    private RegistryRepo registryRepo;
    @Autowired
    private RegistryItemRepo registryItemRepo;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE resupply_registry, resupply_registry_item");
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    void validationExecutedForUnpaidRegistry() {
        uploadService.uploadRegistry(uploadRequest(RegistryType.UNPAID));

        List<RegistryItem> items = registryItemRepo.findAll();
        assertThat(items).hasSize(1);
        RegistryItem item = items.get(0);
        assertThat(item.getReason()).isEqualTo(Reason.UNEXPECTED_ERROR);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    void validationNotExecutedForReturnRegistry() {
        uploadService.uploadRegistry(uploadRequest(RegistryType.REFUND));

        List<RegistryItem> items = registryItemRepo.findAll();
        assertThat(items).hasSize(1);
        RegistryItem item = items.get(0);
        assertThat(item.getReason()).isNull();
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    void testApiUserName() {
        uploadService.uploadRegistry(uploadRequest(RegistryType.REFUND));

        List<Registry> registries = registryRepo.findAll();
        assertThat(registries).hasSize(1);
        Registry registry = registries.get(0);
        assertThat(registry.getUserName()).isEqualTo(API_USER_NAME);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    void shouldntUploadRegistryTwice() {
        uploadService.uploadRegistry(uploadRequest(RegistryType.REFUND));
        try {
            uploadService.uploadRegistry(uploadRequest(RegistryType.REFUND));
            fail("Shouldn't upload registry twice");
        } catch (RegistryUploadConflictException e) {
            // pass
        }
    }

    @Nonnull
    private static UploadRegistryRequest uploadRequest(@Nonnull RegistryType registryType) {
        return UploadRegistryRequest.Builder.newBuilder()
                .setWarehouseId(172L)
                .setName("Sample registry")
                .setDate(LocalDate.now().minusDays(1))
                .setType(registryType)
                .setDeliveryServiceId(1003939L)
                .setRegistryPositions(List.of(
                        new RegistryPosition("45000000", "sample-track-code")
                ))
                .build();
    }
}
