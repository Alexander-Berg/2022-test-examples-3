package ru.yandex.market.partner.mvc.controller.tpl;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональный тест для {@link TplPartnerController}.
 */
public class TplPartnerControllerTest extends FunctionalTest {

    public final static long UUID_1 = 1248L;
    public final static long UUID_2 = 2248L;

    /**
     * Корректность получения списка партнеров 3PL.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-partners.csv")
    public void testTplPartnerList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-partners.json");
    }

    /**
     * Корректность получения списка партнеров 3PL при наличии нескольких партнеров.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-partners-many.csv")
    public void testTplPartnerListWithMultipleElements() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-partners-many.json");
    }

    /**
     * Корректность получения списка только своих партнеров 3PL конкретным пользователем.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-partners-other-user.csv")
    public void testTplPartnerListForOtherUser() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_2));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-partners-other-user.json");
    }

    /**
     * Корректность получения пустого списка партнеров 3PL.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-partners-empty-list.csv")
    public void testTplPartnerEmptyList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-partners-empty-list.json");
    }

    /**
     * Корректность фильтрации магазинов другого типа.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-partners-filtering.csv")
    public void testTplPartnerListFiltering() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-partners-empty-list.json");
    }

    private String partnersUrl(final long effectiveUid) {
        return String.format("%s/tpl/tpl-partners?euid=%s", baseUrl, effectiveUid);
    }
}
