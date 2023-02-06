package ru.yandex.market.ff.grid.validation.rule.concrete;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;

/**
 * @author kotovdv 01/08/2017.
 */
public class IsArrayValidationRuleTest extends ValidationRuleTest {

    private static DefaultGridCell createArrayGrid(String array) {
        return new DefaultGridCell(0, 0, array);
    }

    private static IsArrayValidationRule createIsArrayOfIntegersRule() {
        return new IsArrayValidationRule(
                false,
                "",
                ";",
                new IsIntegerValidationRule(true, "")
        );
    }

    @Override
    protected Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(createArrayGrid("1"), true, createIsArrayOfIntegersRule()),
                Arguments.of(createArrayGrid("1;2"), true, createIsArrayOfIntegersRule()),
                Arguments.of(createArrayGrid("1;2;3"), true, createIsArrayOfIntegersRule()),
                Arguments.of(createArrayGrid("1;2;abcd"), false, createIsArrayOfIntegersRule()),
                Arguments.of(createArrayGrid(";1;2"), false, createIsArrayOfIntegersRule()),
                Arguments.of(createArrayGrid(";1;2;"), false, createIsArrayOfIntegersRule()),
                Arguments.of(createArrayGrid(";"), false, createIsArrayOfIntegersRule()),
                Arguments.of(createArrayGrid(" ; "), false, createIsArrayOfIntegersRule()),
                Arguments.of(createArrayGrid(" ; ;"), false, createIsArrayOfIntegersRule()),
                Arguments.of(createArrayGrid(" ; ; "), false, createIsArrayOfIntegersRule())
        );
    }
}
