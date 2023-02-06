package ru.yandex.market.replenishment.autoorder.service.user_filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.tender_result.TenderResultField;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.tender_result.TenderResultUserFilter;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.SskuStatus;
import ru.yandex.market.replenishment.autoorder.model.dto.TenderResultDTO;
import ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate;
import ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterableResult;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.EQUAL;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.GREATER;
public class TenderResultUserFiltersServiceTest extends FunctionalTest {

    @Autowired
    TenderResultUserFiltersService tenderResultUserFiltersService;

    TenderResultDTO emptyTenderResult = emptyTender();

    TenderResultDTO t = emptyTender();

    @NotNull
    private TenderResultDTO emptyTender() {
        return new TenderResultDTO();
    }

    @Test
    public void testExtractors() {
        test(TenderResultField.MSKU, EQUAL, r -> r.setMsku(123L), "123");
        test(TenderResultField.SSKU, EQUAL, r -> r.setSsku("123.123"), "123.123");
        test(TenderResultField.COMMENT, EQUAL, r -> r.setComment("comment"), "comment");
        test(TenderResultField.SUPPLIER, EQUAL, r -> r.setSupplier("supplier"), "supplier");
        test(TenderResultField.PRICE, GREATER, r -> r.setPrice(2.0), "1");
        test(TenderResultField.QTY, EQUAL, r -> r.setItems(5L), "5");
        test(TenderResultField.CATEGORY, EQUAL, r -> r.setCategoryName("cat1"), "cat1");
        test(TenderResultField.TITLE, EQUAL, r -> r.setAssortmentTitle("assortment1"), "assortment1");
        test(TenderResultField.STATUS, EQUAL, r -> r.setSskuStatus(SskuStatus.ACTIVE), "ACTIVE");
    }


    private void test(TenderResultField field, UserFilterFieldPredicate predicate,
                      Consumer<TenderResultDTO> fieldSetter, String value) {
        fieldSetter.accept(t);
        UserFilterableResult<TenderResultDTO> result =
                tenderResultUserFiltersService.filter(Arrays.asList(emptyTenderResult, t),
                        Collections.singletonList(new TenderResultUserFilter(field, predicate, value)));
        assertEquals(result.getResults().size(), 1);
        assertEquals(result.getUserFiltersCount()[0], 1);
    }
}

