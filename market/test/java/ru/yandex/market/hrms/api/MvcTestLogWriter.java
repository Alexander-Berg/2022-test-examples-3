package ru.yandex.market.hrms.api;

import java.io.PrintWriter;
import java.io.StringWriter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

@Slf4j
public class MvcTestLogWriter implements ResultHandler {

    @Override
    public void handle(MvcResult result) throws Exception {
        StringWriter stringWriter = new StringWriter();
        ResultHandler printingResultHandler =
                new PrintWriterPrintingResultHandler(new PrintWriter(stringWriter));
        printingResultHandler.handle(result);
        log.info("MvcResult details:\n" + stringWriter);
    }
}

