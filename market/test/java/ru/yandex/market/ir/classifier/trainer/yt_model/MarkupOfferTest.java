package ru.yandex.market.ir.classifier.trainer.yt_model;

import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class MarkupOfferTest {

    @Test
    public void testMarkupOfferComparator() {
        MarkupOffer offer1 = new MarkupOffer();
        offer1.setOfferId("offer1");
        MarkupOffer offer2 = new MarkupOffer();
        offer2.setOfferId("offer2");
        // Проверяем компаратор
        assertEquals(offer1.compareTo(offer2), 1);
        offer1.getIntHash();
        offer2.getIntHash();
        int hash1 = 14529161;
        int hash2 = -291362428;
        // Проверяем хеши
        assertEquals(offer1.getIntHash(),hash1);
        assertEquals(offer2.getIntHash(), hash2);
        // Проверяем что от двух одинаковых айдишников одинаковый хеш
        MarkupOffer offer3 = new MarkupOffer();
        offer3.setOfferId(offer1.getOfferId());
        assertEquals(offer1.getIntHash(), offer3.getIntHash());
    }
}
