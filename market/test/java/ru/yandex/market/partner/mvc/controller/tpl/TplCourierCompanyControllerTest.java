package ru.yandex.market.partner.mvc.controller.tpl;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональный тест для {@link TplCourierCompanyController}.
 */
public class TplCourierCompanyControllerTest extends FunctionalTest {

    public final static long UUID_1 = 1248L;
    public final static long UUID_2 = 2248L;

    /**
     * Корректность получения списка курьерских компаний.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-courier-companies.csv")
    public void testPartnerList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-courier-companies.json");
    }

    /**
     * Корректность получения списка курьерских компаний при наличии нескольких курьерских компаний.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-courier-companies-many.csv")
    public void testPartnerListManyPartners() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-courier-comanies-many.json");
    }

    /**
     * Корректность получения списка только своих курьерских компаний конкретным пользователем.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-courier-companies-other-user.csv")
    public void testParterListOtherUser() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_2));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-courier-comanies-other-user.json");
    }

    /**
     * Корректность получения пустого списка курьерских компаний.
     */
    @Test
    @DbUnitDataSet(before = "csv/tpl-get-courier-companies-empty-list.csv")
    public void testPartnerEmptyList() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(UUID_1));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/tpl-get-courier-comanies-empty-list.json");
    }

    private String partnersUrl(final long effectiveUid) {
        return String.format("%s/tpl/partners?euid=%s", baseUrl, effectiveUid);
    }
}
