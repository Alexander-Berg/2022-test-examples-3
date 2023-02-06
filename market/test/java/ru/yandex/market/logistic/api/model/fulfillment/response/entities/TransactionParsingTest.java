package ru.yandex.market.logistic.api.model.fulfillment.response.entities;

import ru.yandex.market.logistic.api.utils.ParsingXmlTest;

public class TransactionParsingTest extends ParsingXmlTest<Transaction> {

    public TransactionParsingTest() {
        super(Transaction.class, "fixture/response/entities/transaction.xml");
    }
}
