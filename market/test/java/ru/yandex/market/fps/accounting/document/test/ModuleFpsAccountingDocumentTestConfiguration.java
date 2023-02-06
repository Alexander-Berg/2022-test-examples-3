package ru.yandex.market.fps.accounting.document.test;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.accounting.document.ModuleAccountingDocumentConfiguration;
import ru.yandex.market.fps.ticket.test.ModuleFpsTicketTestConfiguration;


@Configuration
@Import({
        ModuleAccountingDocumentConfiguration.class,
        ModuleFpsTicketTestConfiguration.class
})
public class ModuleFpsAccountingDocumentTestConfiguration {
}


