package ru.yandex.market.deliverycalculator.searchengine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.google.protobuf.util.JsonFormat;

import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.PbSnUtils;

public final class JsonProtoHelper {

    private JsonProtoHelper() {
        throw new UnsupportedOperationException();
    }

    private static final JsonFormat.Parser JSON_OFFER_PARSER = JsonFormat.parser()
            .usingTypeRegistry(JsonFormat.TypeRegistry.newBuilder()
                    .add(DeliveryCalcProtos.ShopOffersReq.getDescriptor())
                    .build());

    private static final JsonFormat.Printer JSON_OFFER_PRINTER = JsonFormat.printer()
            .usingTypeRegistry(JsonFormat.TypeRegistry.newBuilder()
                    .add(DeliveryCalcProtos.ShopOffersResp.getDescriptor())
                    .build());

    public static byte[] getShopOffersReq(Class<?> clazz, String jsonPath) {
        var jsonReq = StringTestUtil.getString(clazz, jsonPath);

        try {
            var builder = DeliveryCalcProtos.ShopOffersReq.newBuilder();
            JSON_OFFER_PARSER.merge(jsonReq, builder);

            try (var baos = new ByteArrayOutputStream()) {
                PbSnUtils.writePbSnMessage("DCSR", builder.build(), baos);

                return baos.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String printShopOffersResp(byte[] feedOfferRespProtoSn) {
        try (var bain = new ByteArrayInputStream(feedOfferRespProtoSn)) {
            var parser = DeliveryCalcProtos.ShopOffersResp.parser();
            var resp = PbSnUtils.readPbSnMessage("DCSA", parser, bain);

            return JSON_OFFER_PRINTER.print(resp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
