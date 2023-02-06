package ru.yandex.direct.excelmapper.mappers;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.excelmapper.ExcelMapper;
import ru.yandex.direct.excelmapper.SheetRange;
import ru.yandex.direct.excelmapper.exceptions.CantReadEmptyException;
import ru.yandex.direct.excelmapper.exceptions.CantReadFormatException;
import ru.yandex.direct.excelmapper.exceptions.CantReadUnexpectedDataException;
import ru.yandex.direct.excelmapper.exceptions.CantWriteEmptyException;
import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelProperty;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.excelmapper.ExcelMappers.listMapper;
import static ru.yandex.direct.excelmapper.ExcelMappers.longMapper;
import static ru.yandex.direct.excelmapper.ExcelMappers.maybeLongMapper;
import static ru.yandex.direct.excelmapper.ExcelMappers.modelMapper;
import static ru.yandex.direct.excelmapper.ExcelMappers.objectMapper;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createEmptySheet;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createStringSheetFromLists;
import static ru.yandex.direct.excelmapper.MapperTestUtils.sheetToLists;

class ObjectExcelMapperTest {
    private static final String TITLE = "Number";
    private static final String TITLE2 = "List of numbers";
    private static final String TITLE3 = "List of numbers 2";

    private static final String TITLE_OPT_GET = "Number (optional in DB)";
    private static final Long DEFAULT_OPT_GET = 9512876982L;
    private static final String TITLE_OPT_SET = "Number (optional in sheet)";
    private static final Long DEFAULT_OPT_SET = 6742938024L;

    private ExcelMapper<Obj> mapper;

    private ExcelMapper<ObjDefault> mapperDefault;

    @BeforeEach
    void setUp() {
        mapper = objectMapper(Obj::new)
                .field(Obj::getNumber, Obj::setNumber, longMapper(TITLE))
                .field(Obj::getListOfNumbers, Obj::setListOfNumbers, listMapper(longMapper(TITLE2)))
                .field(Obj::getListOfNumbers2, Obj::setListOfNumbers2, listMapper(longMapper(TITLE3)))
                .build();

        ModelProperty<ObjDefault, Long> objDefaultNumberProperty = ModelProperty.create(ObjDefault.class, "number",
                ObjDefault::getNumber, ObjDefault::setNumber);
        ModelProperty<ObjDefault, Long> objDefaultNumberNullableProperty = ModelProperty.create(ObjDefault.class,
                "numberNullable",
                ObjDefault::getNumberNullable, ObjDefault::setNumberNullable);
        mapperDefault = modelMapper(ObjDefault::new)
                .fieldWithDefaultValueForSetter(objDefaultNumberProperty,
                        maybeLongMapper(TITLE_OPT_SET), DEFAULT_OPT_SET)
                .fieldWithDefaultValueForGetter(objDefaultNumberNullableProperty,
                        longMapper(TITLE_OPT_GET), DEFAULT_OPT_GET)
                .build();
    }

    @Test
    void writeValueTest() {
        SheetRange sheetRange = createEmptySheet();

        mapper.write(sheetRange, new Obj()
                .setNumber(1L)
                .setListOfNumbers(List.of(10L, 20L))
                .setListOfNumbers2(List.of(100L, 200L, 300L))
        );

        assertThat(sheetToLists(sheetRange, 3), equalTo(List.of(
                List.of("1", "10", "100"),
                List.of("", "20", "200"),
                List.of("", "", "300")
        )));
    }

    @Test
    void writeNullTest() {
        SheetRange sheetRange = createEmptySheet();

        CantWriteEmptyException exception = assertThrows(CantWriteEmptyException.class, () ->
                mapper.write(sheetRange, null)
        );
        assertThat(exception, hasProperty("columns", equalTo(mapper.getMeta().getColumns())));
    }

    @Test
    void readValue1Test() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("1", "10", "100")
        ));

        Obj value = mapper.read(sheetRange).getValue();

        assertThat(value, equalTo(
                new Obj()
                        .setNumber(1L)
                        .setListOfNumbers(List.of(10L))
                        .setListOfNumbers2(List.of(100L))));
    }

    @Test
    void readValue2Test() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("1", "10", "100"),
                List.of("", "20", "200"),
                List.of("", "", "300")
        ));

        Obj value = mapper.read(sheetRange).getValue();

        assertThat(value, equalTo(
                new Obj()
                        .setNumber(1L)
                        .setListOfNumbers(List.of(10L, 20L))
                        .setListOfNumbers2(List.of(100L, 200L, 300L))
        ));
    }

    @Test
    void readInvalidValueTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("xx")
        ));

        CantReadFormatException exception = assertThrows(CantReadFormatException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(0))));
    }

    @Test
    void readInvalidFieldValueTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("1", "10", "100"),
                List.of("", "xx", "")
        ));

        CantReadFormatException exception = assertThrows(CantReadFormatException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE2))),
                hasProperty("rowIndex", equalTo(1)),
                hasProperty("columnIndex", equalTo(1))));
    }

    @Test
    void readUnexpectedFieldValueTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("1", "10", "100"),
                List.of("", "", "200"),
                List.of("", "20", "300")
        ));

        CantReadUnexpectedDataException exception = assertThrows(CantReadUnexpectedDataException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE2))),
                hasProperty("rowIndex", equalTo(2)),
                hasProperty("columnIndex", equalTo(1))));
    }

    @Test
    void readEmptyTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("")
        ));

        CantReadEmptyException exception = assertThrows(CantReadEmptyException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE))),
                hasProperty("rowIndex", equalTo(0)),
                hasProperty("columnIndex", equalTo(0))));
    }

    @Test
    void readNonNullWithDefaultTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("123", "456")
        ));

        ObjDefault value = mapperDefault.read(sheetRange).getValue();

        assertThat(value, equalTo(
                new ObjDefault()
                        .setNumber(123L)
                        .setNumberNullable(456L)
        ));
    }

    @Test
    void readNullWithDefaultTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("", "456")
        ));

        ObjDefault value = mapperDefault.read(sheetRange).getValue();

        assertThat(value, equalTo(
                new ObjDefault()
                        .setNumber(DEFAULT_OPT_SET)
                        .setNumberNullable(456L)
        ));
    }

    @Test
    void writeNonNullWithDefaultTest() {
        SheetRange sheetRange = createEmptySheet();

        mapperDefault.write(sheetRange, new ObjDefault()
                .setNumber(123L)
                .setNumberNullable(456L)
        );

        assertThat(sheetToLists(sheetRange, 2), equalTo(List.of(
                List.of("123", "456")
        )));
    }

    @Test
    void writeNullWithDefaultTest() {
        SheetRange sheetRange = createEmptySheet();

        mapperDefault.write(sheetRange, new ObjDefault()
                .setNumber(123L)
                .setNumberNullable(null)
        );

        assertThat(sheetToLists(sheetRange, 2), equalTo(List.of(
                List.of("123", DEFAULT_OPT_GET.toString())
        )));
    }


    static class Obj {
        Long number;
        List<Long> listOfNumbers;
        List<Long> listOfNumbers2;

        Long getNumber() {
            return number;
        }

        Obj setNumber(Long number) {
            this.number = number;
            return this;
        }

        List<Long> getListOfNumbers() {
            return listOfNumbers;
        }

        Obj setListOfNumbers(List<Long> listOfNumbers) {
            this.listOfNumbers = listOfNumbers;
            return this;
        }

        List<Long> getListOfNumbers2() {
            return listOfNumbers2;
        }

        Obj setListOfNumbers2(List<Long> listOfNumbers2) {
            this.listOfNumbers2 = listOfNumbers2;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Obj obj = (Obj) o;
            return number.equals(obj.number) &&
                    listOfNumbers.equals(obj.listOfNumbers) &&
                    listOfNumbers2.equals(obj.listOfNumbers2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(number, listOfNumbers, listOfNumbers2);
        }

        @Override
        public String toString() {
            return "Obj{" +
                    "number=" + number +
                    ", listOfNumbers=" + listOfNumbers +
                    ", listOfNumbers2=" + listOfNumbers2 +
                    '}';
        }
    }

    static class ObjDefault implements Model {
        Long number;
        Long numberNullable;

        public Long getNumber() {
            return number;
        }

        public ObjDefault setNumber(Long number) {
            this.number = number;
            return this;
        }

        public Long getNumberNullable() {
            return numberNullable;
        }

        public ObjDefault setNumberNullable(Long numberNullable) {
            this.numberNullable = numberNullable;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ObjDefault obj = (ObjDefault) o;
            return number.equals(obj.number) &&
                    Objects.equals(numberNullable, obj.numberNullable);
        }

        @Override
        public int hashCode() {
            return Objects.hash(number, numberNullable);
        }

        @Override
        public String toString() {
            return "ObjOptional{" +
                    "number=" + number +
                    ", numberNullable=" + numberNullable +
                    '}';
        }
    }
}
