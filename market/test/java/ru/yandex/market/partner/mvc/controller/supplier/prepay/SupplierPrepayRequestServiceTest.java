package ru.yandex.market.partner.mvc.controller.supplier.prepay;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.supplier.prepay.PartnerApplicationKey;
import ru.yandex.market.core.supplier.prepay.SupplierApplicationStatus;
import ru.yandex.market.core.supplier.prepay.SupplierPrepayRequestService;
import ru.yandex.market.partner.test.context.FunctionalTest;

/**
 * Функциональные тесты на {@link SupplierPrepayRequestService}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "SupplierPrepayRequestServiceTest.csv")
class SupplierPrepayRequestServiceTest extends FunctionalTest {

    @Autowired
    private SupplierPrepayRequestService supplierPrepayRequestService;

    @Test
    @DisplayName("Запрос по пустой коллекции")
    void empty() {
        List<SupplierApplicationStatus> applications = supplierPrepayRequestService.getApplicationStatuses(
                Collections.emptyList()
        );
        MatcherAssert.assertThat(applications, Matchers.empty());
    }

    @Test
    @DisplayName("Заявки не найдены")
    void notFound() {
        List<SupplierApplicationStatus> applications = supplierPrepayRequestService.getApplicationStatuses(
                Collections.singletonList(new PartnerApplicationKey(10L, 10L))
        );
        MatcherAssert.assertThat(applications, Matchers.empty());
    }

    @Test
    @DisplayName("Найдена 1 заявка из 2")
    void oneFound() {
        List<SupplierApplicationStatus> applications = supplierPrepayRequestService.getApplicationStatuses(
                Arrays.asList(
                        new PartnerApplicationKey(10L, 10L),
                        new PartnerApplicationKey(1L, 10777L)
                )
        );
        MatcherAssert.assertThat(
                applications,
                Matchers.containsInAnyOrder(SupplierApplicationStatus.of(10777L, PartnerApplicationStatus.FROZEN))
        );
    }

    @Test
    @DisplayName("Найдены 2 заявки из 2")
    void allFound() {
        List<SupplierApplicationStatus> applications = supplierPrepayRequestService.getApplicationStatuses(
                Arrays.asList(
                        new PartnerApplicationKey(2L, 20777L),
                        new PartnerApplicationKey(1L, 10777L)
                )
        );
        MatcherAssert.assertThat(
                applications,
                Matchers.containsInAnyOrder(
                        SupplierApplicationStatus.of(10777L, PartnerApplicationStatus.FROZEN),
                        SupplierApplicationStatus.of(20777L, PartnerApplicationStatus.CLOSED)
                )
        );
    }
}
