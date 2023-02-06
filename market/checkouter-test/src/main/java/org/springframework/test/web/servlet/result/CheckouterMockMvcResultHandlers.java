package org.springframework.test.web.servlet.result;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.util.CollectionUtils;

public abstract class CheckouterMockMvcResultHandlers extends MockMvcResultHandlers {


    // главное чтобы было в пакете ru.yandex, так как у него уровень DEBUG
    private static final Log logger = LogFactory.getLog("ru.yandex.log");


    public static ResultHandler log() {
        return new CheckouterMockMvcResultHandlers.LoggingResultHandler();
    }

    private static class PrintWriterPrintingResultHandler extends PrintingResultHandler {

        PrintWriterPrintingResultHandler(final PrintWriter writer) {
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

    private static class LoggingResultHandler implements ResultHandler {

        @Override
        public void handle(MvcResult result) throws Exception {
            StringWriter stringWriter = new StringWriter();
            ResultHandler printingResultHandler =
                    new PrintWriterPrintingResultHandler(new PrintWriter(stringWriter));
            printingResultHandler.handle(result);
            logger.debug("MvcResult details:\n" + stringWriter);
        }
    }
}
