package ru.yandex.market.delivery.transport_manager.db;

import org.springframework.transaction.annotation.Transactional;

public class SampleTransactionalService {

    @Transactional
    public void writingTransaction() {
    }

    @Transactional(readOnly = true)
    public void readingTransaction() {
    }
}
