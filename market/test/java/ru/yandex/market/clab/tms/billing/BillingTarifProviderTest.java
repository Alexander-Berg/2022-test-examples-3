package ru.yandex.market.clab.tms.billing;

import org.junit.Test;
import ru.yandex.market.clab.db.jooq.generated.enums.PaidAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingTarif;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Category;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 * @since 2/27/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class BillingTarifProviderTest {

    private LocalDateTime billingPeriodStart = LocalDate.now().atStartOfDay();

    @Test
    public void testBillingTarifInCategory() {
        BillingTarif tarif = new BillingTarif()
            .setPaidAction(PaidAction.GOOD_ACCEPT)
            .setStartDate(billingPeriodStart.minusSeconds(1))
            .setCategoryId(4L)
            .setPriceKopeck(100);
        List<BillingTarif> tarifs = Collections.singletonList(
            tarif
        );

        BillingTarifProvider provider = new BillingTarifProvider(billingPeriodStart, createCategoryTree(), tarifs);

        BillingTarif providedTarif = provider.getTarif(PaidAction.GOOD_ACCEPT, 4L);

        assertThat(providedTarif).isEqualTo(tarif);
    }

    @Test
    public void testBillingTarifInParentCategory() {
        BillingTarif tarif = new BillingTarif()
            .setPaidAction(PaidAction.GOOD_ACCEPT)
            .setStartDate(billingPeriodStart.minusSeconds(1))
            .setCategoryId(2L)
            .setPriceKopeck(100);
        List<BillingTarif> tarifs = Collections.singletonList(
            tarif
        );

        BillingTarifProvider provider = new BillingTarifProvider(billingPeriodStart, createCategoryTree(), tarifs);

        BillingTarif providedTarif = provider.getTarif(PaidAction.GOOD_ACCEPT, 4L);

        assertThat(providedTarif).isEqualTo(tarif);
    }

    @Test
    public void testBillingTarifMissing() {
        BillingTarif tarif = new BillingTarif()
            .setPaidAction(PaidAction.GOOD_ACCEPT)
            .setStartDate(billingPeriodStart.minusSeconds(1))
            .setCategoryId(2L)
            .setPriceKopeck(100);
        List<BillingTarif> tarifs = Collections.singletonList(
            tarif
        );

        BillingTarifProvider provider = new BillingTarifProvider(billingPeriodStart, createCategoryTree(), tarifs);

        BillingTarif providedTarif = provider.getTarif(PaidAction.GOOD_ACCEPT, 5L);

        assertThat(providedTarif).isNull();
    }

    @Test
    public void testBillingTarifAfterPeriodStart() {
        BillingTarif tarif = new BillingTarif()
            .setPaidAction(PaidAction.GOOD_ACCEPT)
            .setStartDate(billingPeriodStart.minusSeconds(1))
            .setCategoryId(2L)
            .setPriceKopeck(100);
        BillingTarif tarif2 = new BillingTarif()
            .setPaidAction(PaidAction.GOOD_ACCEPT)
            .setStartDate(billingPeriodStart.plusSeconds(1))
            .setCategoryId(2L)
            .setPriceKopeck(10);
        List<BillingTarif> tarifs = Arrays.asList(
            tarif,
            tarif2
        );

        BillingTarifProvider provider = new BillingTarifProvider(billingPeriodStart, createCategoryTree(), tarifs);

        BillingTarif providedTarif = provider.getTarif(PaidAction.GOOD_ACCEPT, 4L);

        assertThat(providedTarif).isEqualTo(tarif);
    }

    private List<Category> createCategoryTree() {
        return Arrays.asList(
            new Category()
                .setId(1L),
            new Category()
                .setId(2L)
                .setParentHid(1L),
            new Category()
                .setId(3L)
                .setParentHid(2L),
            new Category()
                .setId(4L)
                .setParentHid(2L),
            new Category()
                .setId(5L)
                .setParentHid(1L)
        );
    }
}
