package ru.yandex.market.delivery.transport_manager.service.register.splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.register.RegisterAxaptaRequest;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.register.RegisterAxaptaResponse;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.register.RegisterAxaptaResponseUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.PartialId;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.IdType;
import ru.yandex.market.delivery.transport_manager.dto.Stock;
import ru.yandex.market.delivery.transport_manager.service.register.splitter.dto.RegisterUnitQuantityChanges;

class RegisterUnitSplitterTest {
    @Autowired
    private RegisterUnitSplitter registerUnitSplitter;
    private List<RegisterAxaptaRequest> requests;

    @BeforeEach
    void setUp() {
        registerUnitSplitter = new RegisterUnitSplitter();
        requests = new ArrayList<>(16);
        // Товар aaa1 доступен полностью
        // Используем пустой realMerchantId для того, чтобы проверить, что он равноценен null-у
        requests.add(axaptaRequest("aaa1", "", "1F", 10));
        requests.add(axaptaRequest("aaa1", null, "1D", 10));

        // Товар bbb1 не доступен совсем

        // Товар ccc1 доступен только на одном из стоков
        requests.add(axaptaRequest("ccc1", null, "1D", 5));

        // Товар ddd1 доступен в меньших кол-вах
        requests.add(axaptaRequest("ddd1", null, "1F", 2));
        requests.add(axaptaRequest("ddd1", null, "1D", 3));

        // Товар eee1 доступен, но с другим real_vendor_id
        requests.add(axaptaRequest("eee1", null, "1F", 10));
        requests.add(axaptaRequest("eee1", null, "1D", 10));

        // Товара fff1 по одному стоку не хватает в Меркурии, по другому - и в АХ, и в Меркурии
        requests.add(axaptaRequest("fff1", "1F", null, 10, true, 3));
        requests.add(axaptaRequest("fff1", "1D", null, 3, true, 1));

        // Отрицательное кол-во маппится в ноль
        requests.add(axaptaRequest("hhh1", null, "1D", -1));

    }

    @Test
    void testSplit() {

        final RegisterUnitQuantityChanges actual = registerUnitSplitter.split(
            getOriginal(),
            new AvailabilityRegisterCountSplitter(requests, Set.of())
        );

        final RegisterUnitQuantityChanges expected = new RegisterUnitQuantityChanges();
        // aaa1 без изменений
        expected.getNotChanged()
            .add(unit(null, "aaa1", count(CountType.FIT, 10), count(CountType.DEFECT, 5)).setId(1L));
        // bbb1 полностью удалён
        expected.getRemoved().add(unit(null, "bbb1").setId(3L));

        // остальные - пересчитали кол-во
        expected.getChanged()
            .add(unit(null, "ccc1", count(CountType.DEFECT, 5)).setId(5L));
        expected.getChanged()
            .add(unit(null, "ddd1", count(CountType.FIT, 2), count(CountType.DEFECT, 3)).setId(7L));

        // Новый красный реестр на [bbb-ddd]1
        expected.getNewRed()
            .add(unitRed(null, "bbb1", "FIT: Физически отсутствует в АХ:10 шт.\n" +
                "DEFECT: Физически отсутствует в АХ:5 шт.", count(CountType.FIT, 10), count(CountType.DEFECT, 5)));
        expected.getNewRed()
            .add(unitRed(null, "ccc1", "FIT: Физически отсутствует в АХ:10 шт.", count(CountType.FIT, 10)));
        expected.getNewRed()
            .add(unitRed(null, "ddd1", "FIT: Физически отсутствует в АХ:8 шт.\n" +
                "DEFECT: Физически отсутствует в АХ:2 шт.", count(CountType.FIT, 8), count(CountType.DEFECT, 2)));
        // eee1 полностью удален
        expected.getRemoved().add(unit("000222", "eee1").setId(10L));
        expected.getNewRed()
            .add(unitRed("000222", "eee1", "FIT: Физически отсутствует в АХ:10 шт.\n" +
                "DEFECT: Физически отсутствует в АХ:5 шт.", count(CountType.FIT, 10), count(CountType.DEFECT, 5)));
        // fff1 берется минимальное доступное кол-во  среди АХ и Меркурия, остальное - в красный
        expected.getChanged()
            .add(unit(null, "fff1", count(CountType.FIT, 3), count(CountType.DEFECT, 1)).setId(11L));
        expected.getNewRed()
            .add(unitRed(null, "fff1", "FIT: Запрещено к перевозке в 'Меркурии':2 шт.\n" +
                    "DEFECT: Запрещено к перевозке в 'Меркурии':4 шт.; Физически отсутствует в АХ:2 шт.",
                count(CountType.FIT, 2), count(CountType.DEFECT, 4))
            );

        // hhh1 полностью удалён
        expected.getRemoved().add(unit(null, "hhh1").setId(12L));
        expected.getNewRed()
            .add(unitRed(null, "hhh1", "DEFECT: Физически отсутствует в АХ:5 шт.", count(CountType.DEFECT, 5)));

        assertChangesEquals(expected, actual, RegisterUnitQuantityChanges::getChanged);
        assertChangesEquals(expected, actual, RegisterUnitQuantityChanges::getNotChanged);
        assertChangesEquals(expected, actual, RegisterUnitQuantityChanges::getRemoved);
        assertChangesEquals(expected, actual, RegisterUnitQuantityChanges::getNewRed);
    }

    private List<RegisterUnit> getOriginal() {
        return List.of(
            unit(null, "aaa1", count(CountType.FIT, 10), count(CountType.DEFECT, 5)).setId(1L),
            unit(null, "bbb1", count(CountType.FIT, 10), count(CountType.DEFECT, 5)).setId(3L),
            unit(null, "ccc1", count(CountType.FIT, 10), count(CountType.DEFECT, 5)).setId(5L),
            unit(null, "ddd1", count(CountType.FIT, 10), count(CountType.DEFECT, 5)).setId(7L),
            unit("000222", "eee1", count(CountType.FIT, 10), count(CountType.DEFECT, 5)).setId(10L),
            unit(null, "fff1", count(CountType.FIT, 5), count(CountType.DEFECT, 5)).setId(11L),
            unit(null, "hhh1", count(CountType.DEFECT, 5)).setId(12L)
        );
    }

    @Test
    void testSplitSkippingDefect() {

        final RegisterUnitQuantityChanges actual = registerUnitSplitter.split(
            getOriginal(),
            new AvailabilityRegisterCountSplitter(requests, Set.of(CountType.DEFECT))
        );

        final RegisterUnitQuantityChanges expected = new RegisterUnitQuantityChanges();
        // aaa1 без изменений
        expected.getNotChanged()
            .add(unit(null, "aaa1", count(CountType.FIT, 10), count(CountType.DEFECT, 5)).setId(1L));
        // bbb1 - удалён весь FIT
        expected.getChanged().add(unit(null, "bbb1", count(CountType.DEFECT, 5)).setId(3L));

        // остальные - пересчитали кол-во
        expected.getChanged()
            .add(unit(null, "ccc1", count(CountType.DEFECT, 5)).setId(5L));
        expected.getChanged()
            .add(unit(null, "ddd1", count(CountType.FIT, 2), count(CountType.DEFECT, 5)).setId(7L));

        // Новый красный реестр на [bbb-ddd]1
        expected.getNewRed()
            .add(unitRed(null, "bbb1", "FIT: Физически отсутствует в АХ:10 шт.", count(CountType.FIT, 10)));
        expected.getNewRed()
            .add(unitRed(null, "ccc1", "FIT: Физически отсутствует в АХ:10 шт.", count(CountType.FIT, 10)));
        expected.getNewRed()
            .add(unitRed(null, "ddd1", "FIT: Физически отсутствует в АХ:8 шт.", count(CountType.FIT, 8)));
        // eee1 - удалён весь FIT
        expected.getChanged().add(unit("000222", "eee1", count(CountType.DEFECT, 5)).setId(10L));
        expected.getNewRed()
            .add(unitRed("000222", "eee1", "FIT: Физически отсутствует в АХ:10 шт.", count(CountType.FIT, 10)));
        // fff1 берется минимальное доступное кол-во  среди АХ и Меркурия, остальное - в красный,
        // но для DEFECT и здесь ничего не меняется
        expected.getChanged()
            .add(unit(null, "fff1", count(CountType.FIT, 3), count(CountType.DEFECT, 5)).setId(11L));
        expected.getNewRed()
            .add(unitRed(null, "fff1", "FIT: Запрещено к перевозке в 'Меркурии':2 шт.",
                count(CountType.FIT, 2))
            );

        // hhh1 полностью удалён
        expected.getNotChanged().add(unit(null, "hhh1", count(CountType.DEFECT, 5)).setId(1L).setId(12L));

        assertChangesEquals(expected, actual, RegisterUnitQuantityChanges::getChanged);
        assertChangesEquals(expected, actual, RegisterUnitQuantityChanges::getNotChanged);
        assertChangesEquals(expected, actual, RegisterUnitQuantityChanges::getRemoved);
        assertChangesEquals(expected, actual, RegisterUnitQuantityChanges::getNewRed);
    }

    private void assertChangesEquals(
        RegisterUnitQuantityChanges expected, RegisterUnitQuantityChanges actual,
        Function<RegisterUnitQuantityChanges, List<RegisterUnit>> getter
    ) {
        Assertions.assertEquals(getter.apply(expected), getter.apply(actual));
    }

    private RegisterUnit unitRed(
        String realVendorId,
        String article,
        String denyReason,
        UnitCount... counts
    ) {
        final RegisterUnit unit = unit(realVendorId, article, counts);
        unit.setDenyReason(denyReason);
        return unit;
    }

    private RegisterUnit unit(String realVendorId, String article, UnitCount... counts) {
        return new RegisterUnit()
            .setPartialIds(List.of(
                partialId(IdType.VENDOR_ID, "1"),
                partialId(IdType.REAL_VENDOR_ID, realVendorId),
                partialId(IdType.ARTICLE, article)
            ))
            .setCounts(new ArrayList<>(List.of(counts)));
    }

    private PartialId partialId(IdType vendorId, String s) {
        return new PartialId().setIdType(vendorId).setValue(s);
    }

    private UnitCount count(CountType fit, int i) {
        return new UnitCount().setCountType(fit).setQuantity(i);
    }

    private RegisterAxaptaRequest axaptaRequest(
        String ssku,
        String realMerchantId,
        String stockType,
        int availQty
    ) {
        return axaptaRequest(ssku, stockType, realMerchantId, availQty, null, null);
    }

    private RegisterAxaptaRequest axaptaRequest(
        String ssku,
        String stockType,
        String realMerchantId,
        int availQty,
        Boolean isMercury,
        Integer availMercuryQty
    ) {
        return new RegisterAxaptaRequest()
            .setResponse(new RegisterAxaptaResponse()
                .setUnits(List.of(new RegisterAxaptaResponseUnit()
                    .setSsku(ssku)
                    .setMerchantId(1)
                    .setRealMerchantId(realMerchantId)
                    .setStock(new Stock(stockType))
                    .setAvailPhysicalQty(availQty)
                    .setIsMercury(isMercury)
                    .setAvailMercuryQty(availMercuryQty)
                ))
            );
    }
}
