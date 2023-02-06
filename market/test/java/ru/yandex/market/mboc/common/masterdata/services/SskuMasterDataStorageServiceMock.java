package ru.yandex.market.mboc.common.masterdata.services;

import java.util.function.Consumer;

import org.mockito.Mockito;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;

public class SskuMasterDataStorageServiceMock extends SskuMasterDataStorageService {
    public SskuMasterDataStorageServiceMock(MasterDataRepository masterDataRepository) {
        super(masterDataRepository, Mockito.mock(QualityDocumentRepository.class),
            new TransactionHelperMock(), new SupplierConverterServiceMock(), new ComplexMonitoring());
    }

    private static class TransactionHelperMock implements TransactionHelper {

        @Override
        public <T> T doInTransaction(TransactionCallback<T> callback) {
            return callback.doInTransaction(null);
        }

        @Override
        public void doInTransactionVoid(Consumer<TransactionStatus> callback) {
            callback.accept(null);
        }
    }
}
