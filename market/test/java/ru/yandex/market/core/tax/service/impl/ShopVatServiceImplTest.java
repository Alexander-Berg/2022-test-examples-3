package ru.yandex.market.core.tax.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.checkout.AsyncCheckouterService;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.tax.dao.ShopVatDao;
import ru.yandex.market.core.tax.model.ShopVat;
import ru.yandex.market.core.tax.model.TaxSystem;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.core.tax.model.VatSource;
import ru.yandex.market.core.tax.service.ShopVatHistoryService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Vadim Lyalin
 */
public class ShopVatServiceImplTest extends FunctionalTest {
    private ShopVatServiceImpl shopVatServiceImpl;

    @Autowired
    private ShopVatDao shopVatDao;
    @Autowired
    private PrepayRequestService prepayRequestService;
    @Autowired
    private AsyncCheckouterService asyncCheckouterService;
    @Autowired
    private ShopVatHistoryService historyService;
    @Autowired
    private ParamService paramService;

    private static Stream<Arguments> testValidateVatWithErrorData() {
        return Stream.of(Arguments.of(
                new ShopVat(1, TaxSystem.OSN, VatRate.NO_VAT, VatSource.WEB, VatRate.NO_VAT),
                new ShopVat(1, TaxSystem.ENVD, VatRate.NO_VAT, VatSource.WEB, VatRate.NO_VAT),
                new ShopVat(1, TaxSystem.ENVD, VatRate.VAT_0, VatSource.WEB, VatRate.NO_VAT),
                new ShopVat(1, TaxSystem.PSN, VatRate.VAT_20, VatSource.WEB, VatRate.NO_VAT)

        ));
    }

    @BeforeEach
    public void setUp() throws Exception {
        shopVatServiceImpl = new ShopVatServiceImpl(shopVatDao, asyncCheckouterService, historyService, paramService);
    }

    @Test
    public void testValidateVatForNonVirtualShops() {
        shopVatServiceImpl.validateVat(Arrays.asList(
                new ShopVat(1, TaxSystem.OSN, VatRate.NO_VAT, VatSource.WEB, VatRate.NO_VAT),
                new ShopVat(2, TaxSystem.ENVD, VatRate.NO_VAT, VatSource.WEB, VatRate.NO_VAT),
                new ShopVat(3, TaxSystem.USN_MINUS_COST, VatRate.NO_VAT, VatSource.WEB, VatRate.NO_VAT),
                new ShopVat(4, TaxSystem.OSN, VatRate.VAT_20, VatSource.WEB, VatRate.NO_VAT)
        ));
    }

    @Test
    @DbUnitDataSet(before = "ShopVatServiceImplTest.testCorrectValidateVat.before.csv")
    public void testCorrectValidateVat() {
        shopVatServiceImpl.validateVat(Arrays.asList(
                new ShopVat(1, TaxSystem.OSN, VatRate.NO_VAT, VatSource.WEB_AND_FEED, VatRate.NO_VAT),
                new ShopVat(2, TaxSystem.ENVD, VatRate.NO_VAT, VatSource.WEB, VatRate.NO_VAT)
        ));
    }


    @ParameterizedTest
    @MethodSource("testValidateVatWithErrorData")
    @DbUnitDataSet(before = "ShopVatServiceImplTest.testCorrectValidateVat.before.csv")
    public void testValidateVatWithError(ShopVat shopVat) {
        assertThatThrownBy(() -> shopVatServiceImpl.validateVat(Collections.singletonList(shopVat)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
