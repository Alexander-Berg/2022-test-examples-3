package ru.yandex.market.ir.pipeline.dcp;

import ru.yandex.utils.IoUtils;

class DcpTestOfferData {
    static String getOffer1String() {
        return IoUtils.loadResource(DcpTestOfferData.class.getResourceAsStream("/DcpTestOfferData_offer1.json"));
    }

    static String getOffer2String() {
        return IoUtils.loadResource(DcpTestOfferData.class.getResourceAsStream("/DcpTestOfferData_offer2.json"));
    }
}
