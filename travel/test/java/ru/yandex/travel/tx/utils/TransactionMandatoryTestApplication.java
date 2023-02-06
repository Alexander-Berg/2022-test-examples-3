package ru.yandex.travel.tx.utils;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(TransactionMandatoryAutoConfiguration.class)
public class TransactionMandatoryTestApplication {
}
