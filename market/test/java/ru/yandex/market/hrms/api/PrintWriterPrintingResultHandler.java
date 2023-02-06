package ru.yandex.market.hrms.api;

import java.io.PrintWriter;

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.result.PrintingResultHandler;
import org.springframework.util.CollectionUtils;

public class PrintWriterPrintingResultHandler extends PrintingResultHandler {

    public PrintWriterPrintingResultHandler(PrintWriter writer) {
        super(new ResultValuePrinter() {
            @Override
            public void printHeading(String heading) {
                writer.println();
                writer.println(String.format("%s:", heading));
            }

            @Override
            public void printValue(String label, @Nullable Object value) {
                if (value != null && value.getClass().isArray()) {
                    value = CollectionUtils.arrayToList(value);
                }
                writer.println(String.format("%17s = %s", label, value));
            }
        });
    }
}
