package ru.yandex.market.wms.shared.libs.printer.config;

import org.springframework.context.annotation.Bean;

import ru.yandex.market.wms.shared.libs.label.printer.service.printer.PrintService;
import ru.yandex.market.wms.shared.libs.label.printer.service.printer.PrintServiceMock;
import ru.yandex.market.wms.trace.Module;

public class PrinterTestConfig {

    @Bean
    PrintService printService() {
        return new PrintServiceMock("localhost", "123");
    }

    @Bean
    Module module() {
        return Module.REPORTER;
    }
}
