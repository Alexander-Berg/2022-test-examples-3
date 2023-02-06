package ru.yandex.market.fulfillment.stockstorage.service.warehouse.backorder;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;

/**
 * Тест для {@link BackorderService}.
 */
public class BackorderServiceTest extends AbstractContextualTest {

    public static final int BACKORDER_ALLOWED_WAREHOUSE = 1;
    public static final int BACKORDER_DISALLOWED_WAREHOUSE = 2;
    public static final int BACKORDER_INVALID_WAREHOUSE = 3;
    public static final int BACKORDER_NOT_PRESENT_WAREHOUSE = 100;

    @Autowired
    protected BackorderService backorderService;

    @Test
    @DatabaseSetup("classpath:database/states/backorder/warehouses.xml")
    public void isBackorderAllowedTest() throws Exception {
        softly.assertThat(backorderService.isBackorderAllowed(BACKORDER_ALLOWED_WAREHOUSE))
                .as("Is backorder allowed")
                .isTrue();

        softly.assertThat(backorderService.isBackorderAllowed(BACKORDER_DISALLOWED_WAREHOUSE))
                .as("Is backorder disallowed")
                .isFalse();

        softly.assertThat(backorderService.isBackorderAllowed(BACKORDER_INVALID_WAREHOUSE))
                .as("Is backorder disallowed to invalid property")
                .isFalse();

        softly.assertThat(backorderService.isBackorderAllowed(BACKORDER_NOT_PRESENT_WAREHOUSE))
                .as("Is backorder disallowed to not present in properties")
                .isFalse();
    }

    @Test
    @DatabaseSetup("classpath:database/states/backorder/warehouses.xml")
    public void setBackorderAllowedTest() throws Exception {

        backorderService.setBackorderAllowed(BACKORDER_ALLOWED_WAREHOUSE, true);
        softly.assertThat(backorderService.isBackorderAllowed(BACKORDER_ALLOWED_WAREHOUSE))
                .as("Is backorder allowed to disallowed")
                .isTrue();

        backorderService.setBackorderAllowed(BACKORDER_ALLOWED_WAREHOUSE, false);
        softly.assertThat(backorderService.isBackorderAllowed(BACKORDER_ALLOWED_WAREHOUSE))
                .as("Is backorder disallowed to allowed")
                .isFalse();

        backorderService.setBackorderAllowed(BACKORDER_INVALID_WAREHOUSE, true);
        softly.assertThat(backorderService.isBackorderAllowed(BACKORDER_INVALID_WAREHOUSE))
                .as("Is backorder allowed to invalid property")
                .isTrue();

        backorderService.setBackorderAllowed(BACKORDER_NOT_PRESENT_WAREHOUSE, true);
        softly.assertThat(backorderService.isBackorderAllowed(BACKORDER_NOT_PRESENT_WAREHOUSE))
                .as("Is backorder allowed to not present")
                .isTrue();
    }
}
