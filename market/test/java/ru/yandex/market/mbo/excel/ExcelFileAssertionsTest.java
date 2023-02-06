package ru.yandex.market.mbo.excel;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ExcelFileAssertionsTest {

    @Test
    public void doesntContainHeadersContaining() {
        ExcelFile file = ExcelFile.Builder.withHeaders("frodo", "Bilbo", "bilbo").build();
        ExcelFileAssertions assertions = ExcelFileAssertions.assertThat(file);

        // wont fail
        assertions
            .doesntContainHeadersContaining("a")
            .doesntContainHeadersContaining("foo")
            .doesntContainHeadersContaining("bar")
            .doesntContainHeadersContaining("Frodo")
            .doesntContainHeadersContaining("frodo2")
            .doesntContainHeadersContaining("bIlBo");

        // fail
        Assertions.assertThatThrownBy(() -> {
            assertions.doesntContainHeadersContaining("o");
        }).isInstanceOf(AssertionError.class);

        Assertions.assertThatThrownBy(() -> {
            assertions.doesntContainHeadersContaining("rodo");
        }).isInstanceOf(AssertionError.class);

        Assertions.assertThatThrownBy(() -> {
            assertions.doesntContainHeadersContaining("Bilbo");
        }).isInstanceOf(AssertionError.class);

        Assertions.assertThatThrownBy(() -> {
            assertions.doesntContainHeadersContaining("bilbo");
        }).isInstanceOf(AssertionError.class);

        Assertions.assertThatThrownBy(() -> {
            assertions.doesntContainHeadersContaining("bo");
        }).isInstanceOf(AssertionError.class);
    }
}
