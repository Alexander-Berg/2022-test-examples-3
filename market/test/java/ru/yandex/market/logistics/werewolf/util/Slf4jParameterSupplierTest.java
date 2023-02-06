package ru.yandex.market.logistics.werewolf.util;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @link https://github.com/sleberknight/slf4j-lazy-params
 */
@DisplayName("Slf4jParameterSupplier")
class Slf4jParameterSupplierTest {
    private FakeClass fakeObject;

    @BeforeEach
    public void setUp() {
        fakeObject = Mockito.spy(FakeClass.class);
    }

    @Test
    @DisplayName("ленивое исполнение, возврат String")
    public void stringSupplier() {
        Object result = Slf4jParameterSupplier.delayed(fakeObject::expensiveToCreateString).get();
        verify(fakeObject).expensiveToCreateString();
        assertThat(result).isEqualTo("42");
    }

    @Test
    @DisplayName("ленивое исполнение, возврат Long")
    public void integerSupplier() {
        Object result = Slf4jParameterSupplier.delayed(fakeObject::expensiveToCreateNumber).get();
        verify(fakeObject).expensiveToCreateNumber();
        assertThat(result).isEqualTo(42L);
    }

    @Test
    @DisplayName("ленивое исполнение, возврат строки null если результат null")
    public void nullSupplier() {
        String result = Slf4jParameterSupplier.lazy(fakeObject::expensiveReturningNull).toString();
        verify(fakeObject).expensiveReturningNull();
        assertThat(result).isEqualTo("null");
    }

    @Test
    @DisplayName("ленивое исполнение, возврат POJO")
    public void pojoSupplier() {
        Object result = Slf4jParameterSupplier.lazy(fakeObject::expensiveToCreateThing).get();
        verify(fakeObject).expensiveToCreateThing();
        assertThat(result).isEqualTo(new Thing(42L, "The Blob", "It's blobby!"));
    }

    static class FakeClass {
        String expensiveToCreateString() {
            return "42";
        }

        Object expensiveReturningNull() {
            return null;
        }

        long expensiveToCreateNumber() {
            return 42;
        }

        Thing expensiveToCreateThing() {
            return new Thing(42L, "The Blob", "It's blobby!");
        }
    }

    @SuppressWarnings("unused")
    @AllArgsConstructor
    @Getter
    @ToString
    @EqualsAndHashCode
    static class Thing {
        private Long id;
        private String name;
        private String description;
    }
}
