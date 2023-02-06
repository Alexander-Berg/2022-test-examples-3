package ru.yandex.market.mboc.common.services.migration;

import java.time.Instant;
import java.util.List;

import org.mockito.Mockito;

import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferMetaRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;

public class MigrationServiceTestUtils {
    private MigrationServiceTestUtils() {

    }

    private static MigrationStatusRepository mockMigrationStatusRepository() {
        var migrationStatusRepository = Mockito.mock(MigrationStatusRepository.class);
        Mockito.when(migrationStatusRepository.getLastUpdated()).thenReturn(Instant.MIN);
        Mockito.when(migrationStatusRepository.findAllActive()).thenReturn(List.of());
        return migrationStatusRepository;
    }

    public static MigrationService mockMigrationService(SupplierRepository supplierRepository) {
        var migrationStatusRepository = mockMigrationStatusRepository();
        var migrationOfferRepository = Mockito.mock(MigrationOfferRepository.class);
        var migrationRemovedOfferRepository = Mockito.mock(MigrationRemovedOfferRepository.class);
        var offerUpdateSequenceService = Mockito.mock(OfferUpdateSequenceService.class);
        var offerMetaRepository = Mockito.mock(OfferMetaRepository.class);
        MigrationService migrationService = new MigrationService(migrationStatusRepository, migrationOfferRepository,
            migrationRemovedOfferRepository, supplierRepository, offerUpdateSequenceService, offerMetaRepository);

        Mockito.when(migrationStatusRepository.getLastUpdated()).thenReturn(null);
        migrationService.checkAndUpdateCache();
        return migrationService;
    }
}
