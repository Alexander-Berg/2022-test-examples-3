package ru.yandex.market.deliverycalculator.searchengine.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import ru.yandex.market.deliverycalculator.searchengine.JsonProtoHelper;

public class PrintProtoResultHandler implements ResultHandler {

    private static final Logger log = LoggerFactory.getLogger(PrintProtoResultHandler.class);

    @Override
    public void handle(MvcResult result) {
        byte[] response = result.getResponse().getContentAsByteArray();
        String jsonResponse = JsonProtoHelper.printShopOffersResp(response);

        log.info(jsonResponse);
    }

    public static PrintProtoResultHandler printProto() {
        return new PrintProtoResultHandler();
    }
}
