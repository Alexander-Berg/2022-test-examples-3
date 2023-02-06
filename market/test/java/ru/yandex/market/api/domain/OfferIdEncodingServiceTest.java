package ru.yandex.market.api.domain;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.controller.serialization.StringCodecStorage;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.codecs.DefaultCodec;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class OfferIdEncodingServiceTest extends UnitTestBase {

    OfferIdEncodingService service;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        StringCodecStorage mockStorage = mock(StringCodecStorage.class);
        when(mockStorage.get(anyString())).thenReturn(new DefaultCodec());
        service = new OfferIdEncodingService(mockStorage);
    }

    @Test
    public void shouldResolveWareMd5() throws Exception {
        OfferId offerId = service.decode("NF3zzKhgdAw9xTd57_gsk-");

        assertEquals("NF3zzKhgdAw9xTd57_gsk-", offerId.getWareMd5());
        assertEquals(null, offerId.getFeeShow());
    }

    @Test
    public void shouldDecode_PlainOfferId_WareMd5AndFeeShow() throws Exception {
        OfferId offerId = service.decode("{\"wareMd5\":\"NF3zzKhgdAw9xTd57Mgskw\",\"feeShow\":\"8-qH2tqoDtKKQKVxkKIUKHm4_Dsf89YpF1j5BQD9Q-UuL-W4VOFR74IgFXfBngYI5IyuE1Q9srLmJ1sfOxfFwA,,\"}");

        assertEquals("NF3zzKhgdAw9xTd57Mgskw", offerId.getWareMd5());
        assertEquals("8-qH2tqoDtKKQKVxkKIUKHm4_Dsf89YpF1j5BQD9Q-UuL-W4VOFR74IgFXfBngYI5IyuE1Q9srLmJ1sfOxfFwA,,", offerId.getFeeShow());
    }

    @Test
    public void shouldEncodeWareMd5() throws Exception {
        OfferId offerId = new OfferId("NF3zzKhgdAw9xTd57_gsk-", "");
        assertEquals("NF3zzKhgdAw9xTd57_gsk-", service.encode(offerId));
    }

    @Test
    public void shouldEncode_PlainOfferId_WareMd5AndFeeShow() throws Exception {
        OfferId offerId = new OfferId(
            "NF3zzKhgdAw9xTd57Mgskw",
            "8-qH2tqoDtKKQKVxkKIUKHm4_Dsf89YpF1j5BQD9Q-UuL-W4VOFR74IgFXfBngYI5IyuE1Q9srLmJ1sfOxfFwA,,"
        );
        assertEquals(
            "{\"wareMd5\":\"NF3zzKhgdAw9xTd57Mgskw\",\"feeShow\":\"8-qH2tqoDtKKQKVxkKIUKHm4_Dsf89YpF1j5BQD9Q-UuL-W4VOFR74IgFXfBngYI5IyuE1Q9srLmJ1sfOxfFwA,,\"}",
            service.encode(offerId)
        );
    }

    @Test
    public void shouldEncodeOfferIdWithOriginalWareMd5() throws Exception {
        OfferId offerId = new OfferId(
            "NF3zzKhgdAw9xTd57Mgskw",
            "8-qH2tqoDtKKQKVxkKIUKHm4_Dsf89YpF1j5BQD9Q-UuL-W4VOFR74IgFXfBngYI5IyuE1Q9srLmJ1sfOxfFwA,,",
            "0F3zzKhgdAw9xTd57Mgskw"
        );
        assertEquals(
            "{\"wareMd5\":\"NF3zzKhgdAw9xTd57Mgskw\",\"feeShow\":\"8-qH2tqoDtKKQKVxkKIUKHm4_Dsf89YpF1j5BQD9Q-UuL-W4VOFR74IgFXfBngYI5IyuE1Q9srLmJ1sfOxfFwA,,\",\"originalWareMd5\":\"0F3zzKhgdAw9xTd57Mgskw\"}",
            service.encode(offerId)
        );
    }

    @Test
    public void shouldEncodeOfferIdWithOriginalWareMd5AndEmptyFeeShow() throws Exception {
        OfferId offerId = new OfferId(
            "NF3zzKhgdAw9xTd57Mgskw",
            null,
            "0F3zzKhgdAw9xTd57Mgskw"
        );
        assertEquals(
            "{\"wareMd5\":\"NF3zzKhgdAw9xTd57Mgskw\",\"originalWareMd5\":\"0F3zzKhgdAw9xTd57Mgskw\"}",
            service.encode(offerId)
        );
    }

    @Test
    public void shouldDecodeOfferIdWithOriginalWareMd5() throws Exception {
        OfferId offerId = service.decode("{\"wareMd5\":\"NF3zzKhgdAw9xTd57Mgskw\",\"feeShow\":\"8-qH2tqoDtKKQKVxkKIUKHm4_Dsf89YpF1j5BQD9Q-UuL-W4VOFR74IgFXfBngYI5IyuE1Q9srLmJ1sfOxfFwA,,\",\"originalWareMd5\":\"0F3zzKhgdAw9xTd57Mgskw\"}");

        OfferId unionType = (OfferId)offerId;
        assertEquals("NF3zzKhgdAw9xTd57Mgskw", unionType.getWareMd5());
        assertEquals("8-qH2tqoDtKKQKVxkKIUKHm4_Dsf89YpF1j5BQD9Q-UuL-W4VOFR74IgFXfBngYI5IyuE1Q9srLmJ1sfOxfFwA,,", unionType.getFeeShow());
        assertEquals("0F3zzKhgdAw9xTd57Mgskw", unionType.getOriginalWareMd5());
    }

    @Test
    public void shouldDecodeOfferIdWithOriginalWareMd5AndEmptyFeeShow() throws Exception {
        OfferId offerId = service.decode("{\"wareMd5\":\"NF3zzKhgdAw9xTd57Mgskw\",\"feeShow\":null,\"originalWareMd5\":\"0F3zzKhgdAw9xTd57Mgskw\"}");

        OfferId unionType = (OfferId)offerId;
        assertEquals("NF3zzKhgdAw9xTd57Mgskw", unionType.getWareMd5());
        assertEquals(null, unionType.getFeeShow());
        assertEquals("0F3zzKhgdAw9xTd57Mgskw", unionType.getOriginalWareMd5());
    }
}
