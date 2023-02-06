package ru.yandex.market.partner.mvc.controller.delivery;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональный тест для {@link DeliveryPartnerController}.
 */
public class DeliveryPartnerControllerTest extends FunctionalTest {

    public final static long UUID_1 = 1248L;
    public final static long UUID_2 = 2248L;

    /**
     * Корректность получения списка магазинов при наличии заполненной заявки.
     */
    @Test
    @DbUnitDataSet(before = "csv/delivery-get-partners.csv")
    public void testPartnersInfo() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersInfoUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/delivery-get-partners.json");
    }

    /**
     * Корректность получения списка магазинов при наличии заявки с минимально необходимыми полями.
     */
    @Test
    @DbUnitDataSet(before = "csv/delivery-get-partners-required-only.csv")
    public void testPartnersInfoRequiredOnly() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersInfoUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/delivery-get-partners-required-only.json");
    }

    /**
     * Корректность получения списка магазинов, когда заявка отсутствует.
     */
    @Test
    @DbUnitDataSet(before = "csv/delivery-get-partners-without-app.csv")
    public void testPartnersInfoWithoutApp() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersInfoUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/delivery-get-partners-without-app.json");
    }

    /**
     * Корректность получения пустого списка магазинов.
     */
    @Test
    @DbUnitDataSet(before = "csv/delivery-get-partners-empty-list.csv")
    public void testPartnersInfoEmptyList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersInfoUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/delivery-get-partners-empty-list.json");
    }

    /**
     * Корректность получения списка магазинов при наличии нескольких магазинов.
     */
    @Test
    @DbUnitDataSet(before = "csv/delivery-get-partners-many-shops.csv")
    public void testPartnersInfoManyShops() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersInfoUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/delivery-get-partners-many-shops.json");
    }

    /**
     * Корректность получения списка только своих магазинов конкретным пользователем.
     */
    @Test
    @DbUnitDataSet(before = "csv/delivery-get-partners-other-user.csv")
    public void testPartnersInfoOtherUser() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersInfoUrl(UUID_2));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/delivery-get-partners-other-user.json");
    }

    /**
     * Корректность работы фильтрации при получения списка магазинов.
     */
    @Test
    @DbUnitDataSet(before = "csv/delivery-get-partners-many-shops.csv")
    public void testGetPartnersWithFilter() {
        final ResponseEntity<String> response0 = FunctionalTestHelper.get(partnersInfoUrl(UUID_1, ""));
        JsonTestUtil.assertEquals(response0, this.getClass(), "json/delivery-get-partners-filter0.json");

        final ResponseEntity<String> response1 = FunctionalTestHelper.get(partnersInfoUrl(UUID_1, "10775"));
        JsonTestUtil.assertEquals(response1, this.getClass(), "json/delivery-get-partners-filter1.json");

        final ResponseEntity<String> response2 = FunctionalTestHelper.get(partnersInfoUrl(UUID_1, "бИзНеС 0"));
        JsonTestUtil.assertEquals(response2, this.getClass(), "json/delivery-get-partners-filter2.json");
    }


    /**
     * Корректность работы фильтрации по бизнесу при получении списка магазинов.
     */
    @Test
    @DbUnitDataSet(before = "csv/delivery-get-partners-many-shops.csv")
    public void testGetPartnersByBusinessFilter() {
        //нет такого бизнеса
        ResponseEntity<String> response = FunctionalTestHelper.get(partnersInfoUrl(UUID_1, 1008L, ""));
        JsonTestUtil.assertEquals(response, "{\"partners\":[]}");

        //фильтр по бизнесу
        response = FunctionalTestHelper.get(partnersInfoUrl(UUID_1, 1007L, ""));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/delivery-get-partners-filter2.json");

    }

    private String partnersInfoUrl(final long effectiveUid) {
        return String.format("%s/delivery/partners?euid=%s", baseUrl, effectiveUid);
    }

    private String partnersInfoUrl(final long effectiveUid, @Nonnull final String query) {
        return String.format("%s/delivery/partners?euid=%s&query=%s", baseUrl, effectiveUid, query);
    }

    private String partnersInfoUrl(final long effectiveUid, long businessId, @Nonnull final String query) {
        return String.format("%s/delivery/partners?euid=%s&business_id=%d&query=%s", baseUrl,
                effectiveUid, businessId, query);
    }
}
