package ru.yandex.travel.hotels.common.token;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.hotels.common.encryption.EncryptionService;
import ru.yandex.travel.hotels.proto.EPartnerId;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenCodecTest {
    private TokenCodec codec;

    public TokenCodecTest() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        EncryptionService encryptionService = new EncryptionService("test-secret");
        codec = new TokenCodec(encryptionService);
    }

    @Test
    public void testNewTokenDecrypt() {
        // real-world token of searcher after migration to newer token-building logic
        String newEncodedToken = "9qvmfMwHlzQUNJqtut5-5nLwXdSgvXNrrlhtOcKzhMhm_ejeNoQOvUI5AL0Or2w1GI7FH3cc2HfAAdZkVhXhkxripoXx8BUhfKbovicR4NXl_T-34FjwsQraD371XcTAfMfn0A5gGpMaH-hRkqFQ67w=";
        TravelToken token = codec.decode(newEncodedToken);
        assertThat(token.getTokenId()).isEqualTo("63de5acd76fd2002ba6af47db6c48e6f999990a3");
        assertThat(token.getOfferId()).isEqualTo("f35d65c0-2ee0-49cd-8aa4-87a55f33ff96");
        assertThat(token.getGeneratedAt()).isEqualToIgnoringNanos(LocalDateTime.parse("2019-07-23T10:27:12"));
        assertThat(token.getCheckInDate()).isEqualTo("2019-08-08");
        assertThat(token.getCheckOutDate()).isEqualTo("2019-08-12");
        assertThat(token.getOccupancy().getAdults()).isEqualTo(1);
        assertThat(token.getOccupancy().getChildren()).isEmpty();
        assertThat(token.getPermalink()).isEqualTo(41318495L);
        assertThat(token.getPartnerId()).isEqualTo(EPartnerId.PI_EXPEDIA);
        assertThat(token.getOriginalId()).isEqualTo("63824");
    }

    @Test
    public void testNewTokenEncryptDecrypt() {
        var offerId = ProtoUtils.randomId();
        LocalDateTime now = LocalDateTime.now();
        byte[] tokenIdBytes = {1, 2, 3};
        TravelToken token = TravelToken.builder()
                .setTokenIdBytes(tokenIdBytes)
                .setOfferId(offerId)
                .setGeneratedAt(now)
                .setCheckInDate(LocalDate.of(2019, 9, 13))
                .setCheckOutDate(LocalDate.of(2019, 9, 14))
                .setOccupancy(Occupancy.fromString("3"))
                .setPermalink(1152255963L)
                .setPartnerId(EPartnerId.PI_EXPEDIA)
                .setOriginalId("42")
                .build();
        String tokenString = codec.encode(token);
        assertThat(tokenString).isNotNull();
        TravelToken decodedToken = codec.decode(tokenString);
        assertThat(decodedToken.getTokenId()).isEqualTo("010203");
        assertThat(decodedToken.getOfferId()).isEqualTo(offerId);
        assertThat(decodedToken.getGeneratedAt()).isEqualToIgnoringNanos(now);
        assertThat(decodedToken.getCheckInDate()).isEqualTo("2019-09-13");
        assertThat(decodedToken.getCheckOutDate()).isEqualTo("2019-09-14");
        assertThat(decodedToken.getOccupancy().getAdults()).isEqualTo(3);
        assertThat(decodedToken.getOccupancy().getChildren()).isEmpty();
        assertThat(decodedToken.getPermalink()).isEqualTo(1152255963L);
        assertThat(decodedToken.getPartnerId()).isEqualTo(EPartnerId.PI_EXPEDIA);
        assertThat(decodedToken.getOriginalId()).isEqualTo("42");
    }
}
