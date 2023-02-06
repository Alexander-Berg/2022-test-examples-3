package ru.yandex.market.deliverycalculator.workflow.util.segmentation2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutletDimensionsTest {

    private double[] dimensions;
    private double dimSum;

    @BeforeEach
    void setUp() {
        // Given
        dimensions = new double[]{1, 2, 3};
        dimSum = 5;
    }

    @Test
    void it_must_be_ok() {
        // When
        OutletDimensions outletDimensions = new OutletDimensions(dimensions, dimSum);

        // Then
        assertThat(outletDimensions.getDimSum()).isEqualTo(dimSum);
        assertThat(outletDimensions.getDimensions()).isEqualTo(dimensions);
    }

    @Test
    void it_must_throw_null_pointer_exception_when_dimensions_is_null() {
        // Given
        dimensions = null;

        final NullPointerException thrown =
                // Then
                assertThrows(NullPointerException.class,
                        // When
                        () -> new OutletDimensions(dimensions, dimSum)
                );

        assertThat(thrown).hasMessage("dimensions");
    }

    @Test
    void it_must_throw_illegal_argument_exception_when_dimensions_length_is_not_equal_to_3() {
        // Given
        dimensions = new double[]{1, 2};

        final IllegalArgumentException thrown =
                // Then
                assertThrows(IllegalArgumentException.class,
                        // When
                        () -> new OutletDimensions(dimensions, dimSum)
                );

        assertThat(thrown).hasMessage("dimensions.length should be equal to 3: 2");
    }

    @Test
    void it_must_throw_illegal_argument_exception_when_dimSum_is_zero() {
        // Given
        dimSum = 0;

        final IllegalArgumentException thrown =
                // Then
                assertThrows(IllegalArgumentException.class,
                        // When
                        () -> new OutletDimensions(dimensions, dimSum)
                );

        assertThat(thrown).hasMessageStartingWith("dimSum should be positive: 0");
    }

    @Test
    void it_must_throw_illegal_argument_exception_when_dimSum_is_negative() {
        // Given
        dimSum = -1;

        final IllegalArgumentException thrown =
                // Then
                assertThrows(IllegalArgumentException.class,
                        // When
                        () -> new OutletDimensions(dimensions, dimSum)
                );

        assertThat(thrown).hasMessageStartingWith("dimSum should be positive: -1");
    }

    @Test
    void it_must_be_sorted() {
        // Given
        dimensions = new double[]{3, 1, 2};

        IllegalArgumentException thrown =
                // Then
                assertThrows(IllegalArgumentException.class,
                        // When
                        () -> new OutletDimensions(this.dimensions, dimSum).getDimensions());

        assertThat(thrown).hasMessageStartingWith("dimensions should be sorted");
    }
}
