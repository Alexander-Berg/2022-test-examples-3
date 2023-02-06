package ru.yandex.market.core.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.order.model.CheckouterParcelBoxBuilder;
import ru.yandex.market.core.order.model.Parcel;
import ru.yandex.market.core.order.model.ParcelBox;
import ru.yandex.market.core.order.model.WeightAndSize;

/**
 * Тесты для {@link DefaultParcelDao}.
 *
 * @author ivmelnik
 * @since 14.08.18
 */
@DbUnitDataSet(before = {"db/datasource.csv", "db/DbOrderServiceOrderItemsTest_multipleItems.before.csv"})
class DefaultParcelDaoTest extends FunctionalTest {
    private static final long ORDER_ID = 12L;

    private static final WeightAndSize WEIGHT_AND_SIZE1 = WeightAndSize.builder()
            .withWeight(1000L)
            .withHeight(120L)
            .withWidth(130L)
            .withDepth(140L)
            .build();

    private static final WeightAndSize WEIGHT_AND_SIZE2 = WeightAndSize.builder()
            .withWeight(2000L)
            .withHeight(220L)
            .withWidth(230L)
            .withDepth(240L)
            .build();

    private static final ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox CHECKOUTER_PARCEL_BOX =
            new CheckouterParcelBoxBuilder(1L, WEIGHT_AND_SIZE1).build();

    @Autowired
    private ParcelDao parcelDao;

    @Test
    @DbUnitDataSet(after = "ParcelBoxTest.empty.insert.after.csv")
    void storeEmptyParcelBoxes() {
        parcelDao.storeParcelBoxes(ORDER_ID, Collections.emptyList());
    }

    @Test
    @DbUnitDataSet(after = "ParcelBoxTest.null.insert.after.csv")
    void storeNullParcelBox() {
        parcelDao.storeParcelBoxes(ORDER_ID, ImmutableList.of(
                ParcelBox.builder(1L).build()));
    }

    @Test
    @DbUnitDataSet(after = "ParcelBoxTest.single.insert.after.csv")
    void storeSingleParcelBox() {
        parcelDao.storeParcelBoxes(ORDER_ID, ImmutableList.of(
                ParcelBox.transform(CHECKOUTER_PARCEL_BOX, null)));
    }

    @Test
    @DbUnitDataSet(after = "ParcelBoxTest.multiple.insert.after.csv")
    void storeParcelBoxes() {
        parcelDao.storeParcelBoxes(ORDER_ID, ImmutableList.of(
                ParcelBox.transform(CHECKOUTER_PARCEL_BOX, null),
                ParcelBox.builder(2L).withWeightAndSize(WEIGHT_AND_SIZE2).build()));
    }

    @Test
    @DbUnitDataSet(before = "ParcelBoxTest.before.csv", after = "ParcelBoxTest.delete.after.csv")
    void deleteParcelBoxes() {
        parcelDao.deleteOrderBoxes(ORDER_ID);
    }

    @Test
    @DbUnitDataSet(
            before = "DefaultDaoParcelTest.storeParcel.before.csv",
            after = "DefaultDaoParcelTest.storeParcel.after.csv")
    void storeParcels() {
        parcelDao.storeParcels(
                101L,
                List.of(
                        Parcel.builder()
                                .withParcelId(1L)
                                .withShipmentDate(LocalDate.parse("2020-10-12"))
                                .withShipmentDateTimeBySupplier(LocalDateTime.parse("2021-05-12T10:00:00"))
                                .build(),
                        Parcel.builder()
                                .withParcelId(2L)
                                .withShipmentDate(LocalDate.parse("2020-12-20"))
                                .build(),
                        Parcel.builder()
                                .withParcelId(3L)
                                .withShipmentDateTimeBySupplier(LocalDateTime.parse("2021-10-20T10:00:00"))
                                .build()
                ));
    }
}
