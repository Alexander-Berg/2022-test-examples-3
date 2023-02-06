package ru.yandex.market.bidding.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.bidding.ExchangeProtos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProtocolTest {
    private ExchangeProtos.Parcel parcel;
    private ExchangeProtos.Bid bid;

    @Before
    public void setUp() throws Exception {
        ExchangeProtos.Parcel.Builder parcelBuilder = ExchangeProtos.Parcel.newBuilder();
        ExchangeProtos.Bid.Builder bidBuilder = ExchangeProtos.Bid.newBuilder();
        bidBuilder.setPartnerId(456);
        bidBuilder.setDomainType(ExchangeProtos.Bid.DomainType.FEED_OFFER_ID);
        bidBuilder.setDomainId("987");
        bidBuilder.setFeedId(123);
        bidBuilder.setValueForCard(
                ExchangeProtos.Bid.Value.newBuilder().
                        setPublicationStatus(ExchangeProtos.Bid.PublicationStatus.APPLIED).
                        setValue(12).
                        setModificationTime(1400679913).build());
        bid = bidBuilder.build();
        parcelBuilder.addBids(bid);
        parcel = parcelBuilder.build();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testWriteMagic() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Protocol.writeMagic(baos);
        assertEquals("BIDS", new String(baos.toByteArray(), Charset.forName("ASCII")));
    }

    @Test
    public void testCheckMagic() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream("BIDS9".getBytes(Charset.forName("ASCII")));
        Protocol.checkMagic(bais);
        assertEquals('9', bais.read());
        bais = new ByteArrayInputStream("BID".getBytes(Charset.forName("ASCII")));
        try {
            Protocol.checkMagic(bais);
            fail("Not thrown expected IO exception");
        } catch (IOException ioe) {
            assertEquals(0, bais.available());
        }
        InputStream is = mock(InputStream.class);
        when(is.read(any(byte[].class), anyInt(), anyInt())).thenThrow(IOException.class);
        try {
            Protocol.checkMagic(is);
            fail("Not thrown expected IO exception");
        } catch (IOException ioe) {
            assertTrue(true);
        }
    }

    @Test
    public void testReadWriteParcel() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Protocol.writeParcel(parcel, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertEquals(parcel, Protocol.readParcel(bais));
    }

    @Test
    public void testReverseWrite() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int value = 0x11223344;
        Protocol.reverseWrite(value, baos);
        byte[] bytes = baos.toByteArray();
        assertEquals(0x11, bytes[3]);
        assertEquals(0x22, bytes[2]);
        assertEquals(0x33, bytes[1]);
        assertEquals(0x44, bytes[0]);
    }
}