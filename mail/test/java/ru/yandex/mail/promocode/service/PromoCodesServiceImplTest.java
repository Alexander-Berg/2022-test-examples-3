package ru.yandex.mail.promocode.service;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.mail.promocode.PromoCodeService;
import ru.yandex.mail.promocode.mocks.TestConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Sergey Galyamichev
 */
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class})
public class PromoCodesServiceImplTest {
    private static final String TAG_1 = "PROMO_2019";
    private static final String TAG_2 = "PROMO_2020";
    private static final Long UID = 13L;
    private static final String DEVICE_ID = "TEST";
    private static final String DEVICE_ID_2 = "TEST2";

    private static final List<String> PROMO_CODES_2019 = List.of("promo1", "promo2");
    private static final List<String> PROMO_CODES_2020 = List.of("promo1", "promo2");

    @Inject
    private EmbeddedPostgres ps;
    @Inject
    private PromoCodeService promoCodeService;

    @Before
    @SuppressWarnings("SqlWithoutWhere")
    public void setUp() throws Exception {
        ps.getPostgresDatabase().getConnection().createStatement()
                .execute("delete from promocodes;");
        promoCodeService.upload(TAG_1, PROMO_CODES_2019);
    }

    @Test
    public void assign() {
        String code = promoCodeService.assign(TAG_1, UID, DEVICE_ID);
        assertNotNull(code);
        promoCodeService.validate(UID, DEVICE_ID, code);
    }

    @Test(expected = BadRequestException.class)
    public void doublePromo() {
        promoCodeService.upload(TAG_1, PROMO_CODES_2019);
    }

    @Test
    public void doubleAssign() {
        String code = promoCodeService.assign(TAG_1, UID, DEVICE_ID);
        assertNotNull(code);
        String code2 = promoCodeService.assign(TAG_1, UID, DEVICE_ID);
        assertEquals(code, code2);
    }

    @Test
    public void doubleAssignUidDevice() {
        String code = promoCodeService.assign(TAG_1, UID, DEVICE_ID);
        String code2 = promoCodeService.assign(TAG_1, UID, DEVICE_ID + "1");
        assertEquals(code, code2);
        String code3 = promoCodeService.assign(TAG_1, UID + 1, DEVICE_ID);
        assertEquals(code, code3);
    }

    @Test
    public void assignTwo() {
        String code = promoCodeService.assign(TAG_1, UID, DEVICE_ID);
        assertNotNull(code);
        promoCodeService.validate(UID, DEVICE_ID, code);

        String code2 = promoCodeService.assign(TAG_1, UID + 1, DEVICE_ID + "1");
        assertNotNull(code2);
        promoCodeService.validate(UID + 1, DEVICE_ID + "1", code2);
        assertNotEquals(code, code2);
    }

    @Test(expected = NotFoundException.class)
    public void invalid() {
        promoCodeService.validate(UID, DEVICE_ID, UUID.randomUUID().toString());
    }

    @Test
    public void assignTwoPromo() {
        promoCodeService.upload(TAG_2, PROMO_CODES_2020);
        String code = promoCodeService.assign(TAG_1, UID, DEVICE_ID);
        assertNotNull(code);
        promoCodeService.validate(UID, DEVICE_ID, code);
        String code2 = promoCodeService.assign(TAG_2, UID, DEVICE_ID);
        assertNotNull(code2);
        promoCodeService.validate(UID, DEVICE_ID, code2);
    }

    @Test
    public void assignPromoTwiceWhenOutOfNew() {
        promoCodeService.upload(TAG_2, PROMO_CODES_2020);
        String code11 = promoCodeService.assign(TAG_1, UID, DEVICE_ID);
        assertNotNull(code11);
        String code21 = promoCodeService.assign(TAG_1, UID, DEVICE_ID_2);
        assertNotNull(code21);
        String code22 = promoCodeService.assign(TAG_1, UID, DEVICE_ID_2);
        assertNotNull(code22);
    }

    @Test
    public void assignNoDevice() {
        String code = promoCodeService.assign(TAG_1, UID, null);
        assertNotNull(code);
        promoCodeService.validate(UID, null, code);
    }
}
