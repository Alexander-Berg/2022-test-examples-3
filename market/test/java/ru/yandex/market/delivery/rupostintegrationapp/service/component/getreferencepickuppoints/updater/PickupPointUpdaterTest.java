package ru.yandex.market.delivery.rupostintegrationapp.service.component.getreferencepickuppoints.updater;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;
import utils.FixtureRepository;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.PickuppointRepository;
import ru.yandex.market.delivery.rupostintegrationapp.log.JobStatusLogger;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.getreferencepickuppoints.updater.consumer.UpdatingConsumer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickupPointUpdaterTest extends BaseTest {

    @Mock
    protected UpdatingConsumer consumer;

    @Mock
    protected PickupPointAddressEnricher enricher;

    @Mock
    protected JobStatusLogger jobStatusLogger;

    @Mock
    protected PickuppointRepository repository;

    @Mock
    protected TransactionTemplate transactionTemplate;

    protected PickupPointUpdaterMock getUpdaterObj() {
        return this.getUpdaterObj(1);
    }

    protected PickupPointUpdaterMock getUpdaterObj(int batchsize) {
        return spy(new PickupPointUpdaterMock(
            consumer, transactionTemplate, repository, enricher, "url", batchsize
        ));
    }

    @Test
    void testUpdateNotSoOld() {
        PickupPointUpdaterMock updater = this.getUpdaterObj();
        when(repository.getLastUpdateDate()).thenReturn(new Date());
        updater.update();
        verify(repository).getLastUpdateDate();
        verify(repository, never()).storeCurrentDateAsLastUpdateDate();
    }

    class PickupPointUpdaterMock extends PickupPointUpdater {
        PickupPointUpdaterMock(
            UpdatingConsumer consumer,
            TransactionTemplate transactionTemplate,
            PickuppointRepository pickuppointRepository,
            PickupPointAddressEnricher addressEnricher,
            String pickupPointUrl,
            int addressEnricherBatchSize) {
            super(
                jobStatusLogger,
                transactionTemplate,
                consumer,
                pickuppointRepository,
                addressEnricher,
                pickupPointUrl,
                addressEnricherBatchSize
            );
        }

        @Override
        public InputStream getPickupPointXmlInputStream() {
            try {
                return new ByteArrayInputStream(FixtureRepository.getSinglePickupPointListXml());
            } catch (Exception e) {
                throw new PickupPointUpdateException("Could not build xml data input stream", e);
            }
        }
    }
}
