package ru.yandex.chemodan.app.psbilling.core.converter;

import java.util.UUID;

import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoPayloadDao;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoPayloadEntity;
import ru.yandex.chemodan.app.psbilling.core.promos.PromoPayloadParser;
import ru.yandex.misc.test.Assert;

public class ToPromoPayloadConverterTest {

    private ToPromoPayloadConverter converter;
    private PromoPayloadDao promoPayloadDao;
    private PromoPayloadParser promoPayloadParser;

    @Before
    public void setUp() throws Exception {
        this.promoPayloadDao = Mockito.mock(PromoPayloadDao.class);
        this.promoPayloadParser = Mockito.mock(PromoPayloadParser.class);
        this.converter = new ToPromoPayloadConverter(this.promoPayloadDao, this.promoPayloadParser);
    }

    @Test
    public void testApply() {
        val expected = "Ты лучший!";

        val promoId = UUID.randomUUID();
        val payloadType = Option.of("test");
        val payloadVersion = Option.of(1);
        val language = "RU";

        PromoPayloadEntity payload = Mockito.mock(PromoPayloadEntity.class);

        val payloadContent = "Твой код прекрасный!";
        Mockito.when(payload.getContent()).thenReturn(payloadContent);
        Mockito.when(promoPayloadParser.processPayload(payloadContent, language, false)).thenReturn(expected);

        Mockito.when(promoPayloadDao.get(promoId, payloadType.get(), payloadVersion)).thenReturn(Option.of(payload));

        Option<String> actual = converter.apply(
                ToPromoPayloadConverter.ConvertData.cons(
                        promoId,
                        payloadType,
                        payloadVersion,
                        language
                )
        );

        Assert.some(expected, actual);
    }

    @Test
    public void testApplyEmpty() {
        val promoId = UUID.randomUUID();
        val payloadType = Option.of("test");
        val payloadVersion = Option.of(1);
        val language = "RU";

        Mockito.when(promoPayloadDao.get(promoId, payloadType.get(), payloadVersion)).thenReturn(Option.empty());

        Option<String> actual = converter.apply(
                ToPromoPayloadConverter.ConvertData.cons(
                        promoId,
                        payloadType,
                        payloadVersion,
                        language
                )
        );

        Assert.none(actual);
    }

    @Test
    public void testApplyEmptyFull() {
        Option<String> actual = converter.apply(
                ToPromoPayloadConverter.ConvertData.cons(
                        UUID.randomUUID(),
                        Option.empty(),
                        Option.of(1),
                        "RU"
                )
        );

        Assert.none(actual);
    }
}
