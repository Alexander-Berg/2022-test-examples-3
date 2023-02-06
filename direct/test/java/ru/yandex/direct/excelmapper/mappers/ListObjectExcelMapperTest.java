package ru.yandex.direct.excelmapper.mappers;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.excelmapper.ExcelMapper;
import ru.yandex.direct.excelmapper.ReadResult;
import ru.yandex.direct.excelmapper.SheetRange;
import ru.yandex.direct.excelmapper.exceptions.CantReadEmptyException;
import ru.yandex.direct.excelmapper.exceptions.CantReadFormatException;
import ru.yandex.direct.excelmapper.exceptions.CantReadUnexpectedDataException;
import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelProperty;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.excelmapper.ExcelMappers.listMapper;
import static ru.yandex.direct.excelmapper.ExcelMappers.longMapper;
import static ru.yandex.direct.excelmapper.ExcelMappers.maybeLongMapper;
import static ru.yandex.direct.excelmapper.ExcelMappers.maybeStringMapper;
import static ru.yandex.direct.excelmapper.ExcelMappers.modelMapper;
import static ru.yandex.direct.excelmapper.ExcelMappers.stringMapper;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createEmptySheet;
import static ru.yandex.direct.excelmapper.MapperTestUtils.createStringSheetFromLists;
import static ru.yandex.direct.excelmapper.MapperTestUtils.sheetToLists;

class ListObjectExcelMapperTest {
    private static final String TITLE = "Number";
    private static final String TITLE2 = "List of numbers";
    private static final String TITLE3 = "String";

    private ExcelMapper<List<ModelObj>> mapper;

    @BeforeEach
    void setUp() {
        mapper = listMapper(
                modelMapper(ModelObj::new)
                        .field(ModelObj.NUMBER, longMapper(TITLE))
                        .field(ModelObj.LIST_OF_NUMBERS, listMapper(longMapper(TITLE2)))
                        .build()
        );
    }

    @Test
    void writeSingleValueTest() {
        SheetRange sheetRange = createEmptySheet();

        mapper.write(sheetRange, List.of(new ModelObj().setNumber(1L).setListOfNumbers(List.of(10L, 20L))));

        assertThat(sheetToLists(sheetRange, 2), equalTo(List.of(
                List.of("1", "10"),
                List.of("", "20")
        )));
    }

    @Test
    void writeSeveralValuesTest() {
        SheetRange sheetRange = createEmptySheet();

        mapper.write(sheetRange, List.of(
                new ModelObj().setNumber(1L).setListOfNumbers(List.of(10L, 20L)),
                new ModelObj().setNumber(2L).setListOfNumbers(List.of(30L, 40L))
        ));

        assertThat(sheetToLists(sheetRange, 2), equalTo(List.of(
                List.of("1", "10"),
                List.of("", "20"),
                List.of("2", "30"),
                List.of("", "40")
        )));
    }

    @Test
    void readSingleValue1Test() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("1", "10")
        ));

        List<ModelObj> value = mapper.read(sheetRange).getValue();

        assertThat(value, contains(new ModelObj().setNumber(1L).setListOfNumbers(List.of(10L))));
    }

    @Test
    void readSingleValue2Test() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("1", "10"),
                List.of("", "20")
        ));

        List<ModelObj> value = mapper.read(sheetRange).getValue();

        assertThat(value, contains(new ModelObj().setNumber(1L).setListOfNumbers(List.of(10L, 20L))));
    }

    @Test
    void readSeveralValuesTest() {
        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("1", "10"),
                List.of("", "20"),
                List.of("2", "30"),
                List.of("", "40")
        ));

        List<ModelObj> value = mapper.read(sheetRange).getValue();

        assertThat(value, contains(
                new ModelObj().setNumber(1L).setListOfNumbers(List.of(10L, 20L)),
                new ModelObj().setNumber(2L).setListOfNumbers(List.of(30L, 40L))
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
                List.of("1", "10"),
                List.of("", "xx")
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
                List.of("1", "10"),
                List.of("", ""),
                List.of("", "20")
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
    void readInvalidSecondItemOfList() {
        var mapper = listMapper(
                modelMapper(ModelObj::new)
                        .field(ModelObj.NUMBER, longMapper(TITLE))
                        .field(ModelObj.LIST_OF_NUMBERS, listMapper(longMapper(TITLE2)))
                        .field(ModelObj.STRING, stringMapper(TITLE3))
                        .build()
        );

        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("1", "10", "name1"),
                List.of("2", "20", ""),
                List.of("3", "30", "name3")
        ));

        CantReadEmptyException exception = assertThrows(CantReadEmptyException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE3))),
                hasProperty("rowIndex", equalTo(1)),
                hasProperty("columnIndex", equalTo(2))));
    }

    @Test
    void readListObjectsWithSkipEmptyRows() {
        var mapper = listMapper(
                modelMapper(ModelObj::new)
                        .field(ModelObj.NUMBER, longMapper(TITLE))
                        .field(ModelObj.LIST_OF_NUMBERS, listMapper(longMapper(TITLE2)))
                        .field(ModelObj.STRING, stringMapper(TITLE3))
                        .build()
        );

        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("1", "10", "name1"),
                List.of("", "11", ""),
                List.of("", "", ""),
                List.of("", "", ""),
                List.of("2", "20", "name2"),
                List.of("", "", ""),
                List.of("3", "30", "name3"),
                List.of("", "31", "")
        ));

        ReadResult<List<ModelObj>> result = mapper.read(sheetRange);

        assertThat(result.getValue(), contains(
                new ModelObj().setNumber(1L).setString("name1").setListOfNumbers(List.of(10L, 11L)),
                new ModelObj().setNumber(2L).setString("name2").setListOfNumbers(List.of(20L)),
                new ModelObj().setNumber(3L).setString("name3").setListOfNumbers(List.of(30L, 31L))
        ));
    }

    @Test
    void readListObject_WithAllFieldsCanBeEmpty() {
        var mapper = listMapper(
                modelMapper(ModelObj::new)
                        .field(ModelObj.NUMBER, maybeLongMapper(TITLE))
                        .field(ModelObj.STRING, maybeStringMapper(TITLE3))
                        .build()
        );

        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("", "name1"),
                List.of("", "name2"),
                List.of("", "")
        ));

        ReadResult<List<ModelObj>> result = mapper.read(sheetRange);

        assertThat(result.getValue(), contains(
                new ModelObj().setString("name1"),
                new ModelObj().setString("name2")
        ));
    }

    @Test
    void readUnexpectedDataInEndOfList() {
        var mapper = listMapper(
                modelMapper(ModelObj::new)
                        .field(ModelObj.NUMBER, maybeLongMapper(TITLE))
                        .field(ModelObj.STRING, maybeStringMapper(TITLE3))
                        .build()
        );

        SheetRange sheetRange = createStringSheetFromLists(List.of(
                List.of("", "name1"),
                List.of("", ""),
                List.of("", "name3")
        ));

        CantReadUnexpectedDataException exception = assertThrows(CantReadUnexpectedDataException.class, () ->
                mapper.read(sheetRange)
        );
        assertThat(exception, allOf(
                hasProperty("columns", equalTo(List.of(TITLE, TITLE3))),
                hasProperty("rowIndex", equalTo(2)),
                hasProperty("columnIndex", equalTo(0))));
    }

    @SuppressWarnings("WeakerAccess")
    public static class ModelObj implements Model {
        public static final ModelProperty<ModelObj, Long> NUMBER =
                ModelProperty.create(ModelObj.class, "number", ModelObj::getNumber, ModelObj::setNumber);
        public static final ModelProperty<ModelObj, List<Long>> LIST_OF_NUMBERS =
                ModelProperty.create(ModelObj.class, "listOfNumbers", ModelObj::getListOfNumbers,
                        ModelObj::setListOfNumbers);
        public static final ModelProperty<ModelObj, String> STRING =
                ModelProperty.create(ModelObj.class, "string", ModelObj::getString,
                        ModelObj::setString);

        public ModelObj() {
        }

        Long number;
        List<Long> listOfNumbers;
        String string;

        public Long getNumber() {
            return number;
        }

        public ModelObj setNumber(Long number) {
            this.number = number;
            return this;
        }

        public List<Long> getListOfNumbers() {
            return listOfNumbers;
        }

        public ModelObj setListOfNumbers(List<Long> listOfNumbers) {
            this.listOfNumbers = listOfNumbers;
            return this;
        }

        public java.lang.String getString() {
            return string;
        }

        public ModelObj setString(java.lang.String string) {
            this.string = string;
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
            ModelObj modelObj = (ModelObj) o;
            return Objects.equals(number, modelObj.number) &&
                    Objects.equals(listOfNumbers, modelObj.listOfNumbers) &&
                    Objects.equals(string, modelObj.string);
        }

        @Override
        public int hashCode() {
            return Objects.hash(number, listOfNumbers, string);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", ModelObj.class.getSimpleName() + "[", "]")
                    .add("number=" + number)
                    .add("listOfNumbers=" + listOfNumbers)
                    .add("string='" + string + "'")
                    .toString();
        }
    }
}
