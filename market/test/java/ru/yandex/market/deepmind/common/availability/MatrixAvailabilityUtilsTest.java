package ru.yandex.market.deepmind.common.availability;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class MatrixAvailabilityUtilsTest {

    @Test
    public void testToErrorInfo() {
        for (MatrixAvailability.Reason reason : MatrixAvailability.Reason.values()) {
            MatrixAvailability matrixAvailability = Mockito.mock(MatrixAvailability.class);
            Mockito.when(matrixAvailability.getReason()).thenReturn(reason);

            ErrorInfo errorInfo = MatrixAvailabilityUtils.toErrorInfo(matrixAvailability);
            Assertions.assertThat(errorInfo).isNotNull();
            Assertions.assertThat(errorInfo.render()).isNotEmpty();
        }
    }

    @Test
    public void testToShortErrorInfo() {
        for (MatrixAvailability.Reason reason : MatrixAvailability.Reason.values()) {
            MatrixAvailability matrixAvailability = Mockito.mock(MatrixAvailability.class);
            Mockito.when(matrixAvailability.getReason()).thenReturn(reason);

            ErrorInfo errorInfo = MatrixAvailabilityUtils.toShortErrorInfo(matrixAvailability);
            Assertions.assertThat(errorInfo).isNotNull();
            Assertions.assertThat(errorInfo.render()).isNotEmpty();
        }
    }

    @Test
    public void testToShortErrorInfoForExplicitlyAllowedSupplier() {
        testErrorInfoForExplicitlyAllowed(
            MatrixAvailability.Reason.MSKU,
            MbocErrors.get().shortExplicitlyAllowedMSKU().toString()
        );
        testErrorInfoForExplicitlyAllowed(
            MatrixAvailability.Reason.SSKU,
            MbocErrors.get().shortExplicitlyAllowedSSKU().toString()
        );
        testErrorInfoForExplicitlyAllowed(
            MatrixAvailability.Reason.MSKU_IN_CATEGORY,
            MbocErrors.get().shortCategoryExplicitlyAllowedCategory().toString()
        );
        testErrorInfoForExplicitlyAllowed(
            MatrixAvailability.Reason.SUPPLIER,
            MbocErrors.get().shortExplicitlyAllowedSupplier().toString()
        );
    }

    private void testErrorInfoForExplicitlyAllowed(MatrixAvailability.Reason reason, String expectedMsg) {
        MatrixAvailability matrixAvailability = Mockito.mock(MatrixAvailability.class);
        Mockito.when(matrixAvailability.getReason()).thenReturn(reason);
        Mockito.when(matrixAvailability.isAvailable()).thenReturn(true);

        ErrorInfo errorInfo = MatrixAvailabilityUtils.toShortErrorInfo(matrixAvailability);
        Assertions.assertThat(errorInfo).isNotNull();
        Assertions.assertThat(errorInfo.render()).isNotEmpty();
        Assertions.assertThat(errorInfo.toString().equals(expectedMsg)).isTrue();
    }

    @Test
    public void testGetForWarehouseTypeWontBeEmpty() {
        for (WarehouseUsingType value : WarehouseUsingType.values()) {
            List<MatrixAvailability.Reason> reasons = MatrixAvailability.Reason.getForWarehouseType(value);
            Assertions.assertThat(reasons).isNotEmpty();
        }
    }
}
