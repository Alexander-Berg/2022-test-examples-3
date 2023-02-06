package ru.yandex.market.fps.module.payment.netting.test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.fps.module.payment.netting.JournalEntry;
import ru.yandex.market.fps.module.payment.netting.NettingAvailableJournalEntry;
import ru.yandex.market.fps.module.payment.netting.PaymentNetting;
import ru.yandex.market.fps.module.payment.netting.RedundantJournalEntriesException;
import ru.yandex.market.fps.module.payment.netting.test.impl.PaymentNettingTestUtils;
import ru.yandex.market.fps.module.supplier1p.Supplier1p;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.test.assertions.EntityAssert;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.market.jmf.trigger.TriggerServiceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Transactional
@SpringJUnitConfig(InternalModulePaymentNettingTestConfiguration.class)
public class PaymentNettingTest {
    private final SupplierTestUtils supplierTestUtils;
    private final PaymentNettingTestUtils paymentNettingTestUtils;

    public PaymentNettingTest(SupplierTestUtils supplierTestUtils,
                              PaymentNettingTestUtils paymentNettingTestUtils) {
        this.supplierTestUtils = supplierTestUtils;
        this.paymentNettingTestUtils = paymentNettingTestUtils;
    }

    public static Stream<Arguments> redundantJournalEntriesTestData() {
        return Stream.of(
                arguments(
                        List.of(-10, -20),
                        List.of(5, 7, 11, 8, 1),
                        List.of(1)
                ),
                arguments(
                        List.of(-10, -20, -2, -1),
                        List.of(5, 7, 11, 8, 1),
                        List.of(-1)
                ),
                arguments(
                        List.of(-10, -20, -2, -1, -10),
                        List.of(5, 7, 11, 8, 1),
                        List.of(-1, -10)
                ),
                arguments(
                        List.of(-10, -20),
                        List.of(20, 10, 5),
                        List.of(5)
                )
        );
    }

    public static Stream<Arguments> nonRedundantJournalEntriesTestData() {
        return Stream.of(
                arguments(
                        List.of(-10, -20),
                        List.of(5, 7, 11, 8),
                        List.of(-10, -20),
                        List.of(5, 7, 11, 7)
                ),
                arguments(
                        List.of(-10, -20),
                        List.of(5, 7),
                        List.of(-10, -2),
                        List.of(5, 7)
                ),
                arguments(
                        List.of(-10, -20, -2),
                        List.of(5, 7, 11, 8, 1),
                        List.of(-10, -20, -2),
                        List.of(5, 7, 11, 8, 1)
                ),
                arguments(
                        List.of(-10, -20),
                        List.of(20, 10),
                        List.of(-10, -20),
                        List.of(20, 10)
                )
        );
    }

    @SuppressWarnings({"rawtypes", "unused"})
    // rawtypes: using Class instance, unused: parameter needed for type inference
    private static <ELEMENT extends Entity>
    InstanceOfAssertFactory<Iterable, EntityCollectionAssert<ELEMENT>> entities(Class<ELEMENT> elementType) {
        return new InstanceOfAssertFactory<>(Iterable.class, EntityCollectionAssert::<ELEMENT>assertThat);
    }

    @Test
    public void createPaymentNetting() {
        Supplier1p supplier = supplierTestUtils.createSupplier(Map.of(
                Supplier1p.AX_RS_ID, Randoms.string()
        ));
        List<NettingAvailableJournalEntry> yandexJournalEntries =
                Stream.of(
                        new Pair<>(new BigDecimal("-1000.50"), new BigDecimal("-1000.50")),
                        new Pair<>(new BigDecimal("-500.50"), new BigDecimal("-200.50"))
                ).map(x -> paymentNettingTestUtils.createNettingAvailableJournalEntry(Map.of(
                        NettingAvailableJournalEntry.SUPPLIER, supplier,
                        NettingAvailableJournalEntry.FULL_SUM, x.first,
                        NettingAvailableJournalEntry.REMAINING_SUM, x.second
                ))).toList();
        List<NettingAvailableJournalEntry> supplierJournalEntries =
                Stream.of(
                        new Pair<>(new BigDecimal("100"), new BigDecimal("100")),
                        new Pair<>(new BigDecimal("1000"), new BigDecimal("1000")),
                        new Pair<>(new BigDecimal("10"), new BigDecimal("10"))
                ).map(x -> paymentNettingTestUtils.createNettingAvailableJournalEntry(Map.of(
                        NettingAvailableJournalEntry.SUPPLIER, supplier,
                        NettingAvailableJournalEntry.FULL_SUM, x.first,
                        NettingAvailableJournalEntry.REMAINING_SUM, x.second
                ))).toList();

        PaymentNetting paymentNetting = paymentNettingTestUtils.createPaymentNetting(supplier, supplierJournalEntries,
                yandexJournalEntries);

        EntityAssert.assertThat(paymentNetting)
                .hasAttributes(
                        PaymentNetting.DEBT_SUM, new BigDecimal("91.00"),
                        PaymentNetting.FULL_NETTING_SUM, new BigDecimal("1110.00"),
                        PaymentNetting.YANDEX_JOURNAL_ENTRIES_SUM, new BigDecimal("1201.00"),
                        PaymentNetting.SUPPLIER_JOURNAL_ENTRIES_SUM, new BigDecimal("1110.00")
                );
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "В проводках поставщика есть проводки Яндекса,true",
            "В проводках Яндекса есть проводки поставщика,false"
    })
    public void paymentNettingCreationFailedOnWrongJournalEntries(String ignored, boolean negate) {
        Function<BigDecimal, BigDecimal> mapper = d -> negate ? d.negate() : d;
        Supplier1p supplier = supplierTestUtils.createSupplier(Map.of(
                Supplier1p.AX_RS_ID, Randoms.string()
        ));
        List<NettingAvailableJournalEntry> yandexJournalEntries =
                IntStream.range(0, 5).mapToObj(x -> paymentNettingTestUtils.createNettingAvailableJournalEntry(Map.of(
                        NettingAvailableJournalEntry.SUPPLIER, supplier,
                        NettingAvailableJournalEntry.FULL_SUM, mapper.apply(Randoms.moneyAsBigDecimal(50_000, 100_000)),
                        NettingAvailableJournalEntry.REMAINING_SUM, mapper.apply(Randoms.moneyAsBigDecimal(30_000,
                                60_000))
                ))).toList();
        List<NettingAvailableJournalEntry> supplierJournalEntries =
                IntStream.range(0, 5).mapToObj(x -> paymentNettingTestUtils.createNettingAvailableJournalEntry(Map.of(
                        NettingAvailableJournalEntry.SUPPLIER, supplier,
                        NettingAvailableJournalEntry.FULL_SUM, mapper.apply(Randoms.moneyAsBigDecimal(10_000, 70_000)),
                        NettingAvailableJournalEntry.REMAINING_SUM, mapper.apply(Randoms.moneyAsBigDecimal(5_000,
                                30_000))
                ))).toList();

        Assertions.assertThrows(ValidationException.class,
                () -> paymentNettingTestUtils.createPaymentNetting(
                        supplier, supplierJournalEntries, yandexJournalEntries
                )
        );
    }

    @ParameterizedTest
    @MethodSource("redundantJournalEntriesTestData")
    public void testRedundantJournalEntries(List<Integer> yandexSums, List<Integer> supplierSums,
                                            List<Integer> redundantSums) {
        Supplier1p supplier = supplierTestUtils.createSupplier(Map.of(
                Supplier1p.AX_RS_ID, Randoms.string()
        ));


        var planningPayDate = Now.offsetDateTime();
        var offsetCounter = new AtomicLong();

        List<NettingAvailableJournalEntry> yandexJournalEntries =
                generateJournalEntries(yandexSums, supplier, planningPayDate, offsetCounter);
        List<NettingAvailableJournalEntry> supplierJournalEntries =
                generateJournalEntries(supplierSums, supplier, planningPayDate, offsetCounter);

        var thrown = Assertions.assertThrows(TriggerServiceException.class,
                () -> paymentNettingTestUtils.createPaymentNetting(
                        supplier, supplierJournalEntries, yandexJournalEntries
                )
        );

        assertThat(thrown)
                .hasCauseExactlyInstanceOf(RedundantJournalEntriesException.class)
                .getCause()
                .hasMessageContaining("Есть лишние проводки")
                .asInstanceOf(InstanceOfAssertFactories.type(RedundantJournalEntriesException.class))
                .extracting(RedundantJournalEntriesException::getRedundantJournalEntries,
                        entities(JournalEntry.class))
                .extracting(JournalEntry::getRemainingSum)
                .containsExactlyInAnyOrderElementsOf(Lists.transform(redundantSums,
                        x -> new BigDecimal(x).setScale(2, RoundingMode.HALF_EVEN)
                ));
    }

    @Nonnull
    private List<NettingAvailableJournalEntry> generateJournalEntries(List<Integer> yandexSums,
                                                                      Supplier1p supplier,
                                                                      OffsetDateTime planningPayDate,
                                                                      AtomicLong offsetCounter) {
        return yandexSums.stream().map(BigDecimal::new).map(
                x -> paymentNettingTestUtils.createNettingAvailableJournalEntry(Map.of(
                        NettingAvailableJournalEntry.SUPPLIER, supplier,
                        NettingAvailableJournalEntry.FULL_SUM, x,
                        NettingAvailableJournalEntry.REMAINING_SUM, x,
                        NettingAvailableJournalEntry.PLANNING_PAY_DATE,
                        planningPayDate.plusSeconds(offsetCounter.getAndIncrement())
                ))).toList();
    }

    @ParameterizedTest
    @MethodSource("nonRedundantJournalEntriesTestData")
    public void testSimpleNonRedundantJournalEntries(List<Integer> yandexSums, List<Integer> supplierSums,
                                                     List<Integer> yandexNettingSums,
                                                     List<Integer> supplierNettingSums) {
        testNonRedundantJournalEntries(yandexSums, supplierSums, yandexNettingSums, supplierNettingSums);
    }

    @Test
    public void testLargeNonRedundantJournalEntries() {
        List<Integer> yandexLargeCase = IntStream.rangeClosed(-999, -1)
                .boxed()
                .toList();
        List<Integer> supplierLargeCase = IntStream.range(1, 1_000)
                .boxed()
                .toList();

        testNonRedundantJournalEntries(yandexLargeCase, supplierLargeCase, yandexLargeCase, supplierLargeCase);
    }

    private void testNonRedundantJournalEntries(List<Integer> yandexSums, List<Integer> supplierSums,
                                                List<Integer> yandexNettingSums, List<Integer> supplierNettingSums) {
        Supplier1p supplier = supplierTestUtils.createSupplier(Map.of(
                Supplier1p.AX_RS_ID, Randoms.string()
        ));


        var planningPayDate = Now.offsetDateTime();
        var offsetCounter = new AtomicLong();

        List<NettingAvailableJournalEntry> yandexJournalEntries =
                generateJournalEntries(yandexSums, supplier, planningPayDate, offsetCounter);
        List<NettingAvailableJournalEntry> supplierJournalEntries =
                generateJournalEntries(supplierSums, supplier, planningPayDate, offsetCounter);

        PaymentNetting paymentNetting =
                Assertions.assertDoesNotThrow(() -> paymentNettingTestUtils.createPaymentNetting(
                        supplier, supplierJournalEntries, yandexJournalEntries
                ));

        EntityAssert.assertThat(paymentNetting)
                .extracting(PaymentNetting::getSupplierJournalEntries, entities(JournalEntry.class))
                .extracting(JournalEntry::getNettingAmount)
                .containsExactlyInAnyOrderElementsOf(Lists.transform(supplierNettingSums,
                        x -> new BigDecimal(x).setScale(2, RoundingMode.HALF_EVEN)
                ));

        EntityAssert.assertThat(paymentNetting)
                .extracting(PaymentNetting::getYandexJournalEntries, entities(JournalEntry.class))
                .extracting(JournalEntry::getNettingAmount)
                .containsExactlyInAnyOrderElementsOf(Lists.transform(yandexNettingSums,
                        x -> new BigDecimal(x).setScale(2, RoundingMode.HALF_EVEN)
                ));
    }

}
