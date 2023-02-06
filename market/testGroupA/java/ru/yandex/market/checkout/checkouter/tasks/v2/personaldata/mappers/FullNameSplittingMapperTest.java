package ru.yandex.market.checkout.checkouter.tasks.v2.personaldata.mappers;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.service.personal.model.FullName;
import ru.yandex.market.checkouter.jooq.Tables;
import ru.yandex.market.checkouter.jooq.tables.records.ReturnRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FullNameSplittingMapperTest {

    @Test
    public void normalize() {
        var mapper = new FullNameSplittingMapper<>(Tables.RETURN.PERSONAL_FULL_NAME_ID, Tables.RETURN.FULLNAME);
        assertNull(mapper.map(fullNameRecord(null)));
        assertNull(mapper.map(fullNameRecord("")));
        assertNull(mapper.map(fullNameRecord(" ")));
        assertNull(mapper.map(fullNameRecord("Иванов")));
        assertNull(mapper.map(fullNameRecord(" Иванов ")));
        assertFullNameEquals(new FullName().surname("Иванов").forename("Иван"),
                mapper.map(fullNameRecord("Иванов Иван")).getFullName());
        assertFullNameEquals(new FullName().surname("Иванов").forename("Иван"),
                mapper.map(fullNameRecord(" Иванов  Иван ")).getFullName());
        assertFullNameEquals(new FullName().surname("Иванов").forename("Иван").patronymic("Иванович"),
                mapper.map(fullNameRecord("Иванов Иван Иванович")).getFullName());
        assertFullNameEquals(new FullName().surname("Иванов").forename("Иван").patronymic("Иванович"),
                mapper.map(fullNameRecord(" Иванов  Иван   Иванович ")).getFullName());
        assertFullNameEquals(new FullName().surname("Мамедов").forename("Полад").patronymic("Муртуза оглы"),
                mapper.map(fullNameRecord("Мамедов Полад Муртуза оглы")).getFullName());
    }

    private void assertFullNameEquals(FullName expected, FullName actual) {
        assertEquals(expected, actual, () -> String.format("Expected <%s> <%s> <%s>, but was  <%s> <%s> <%s>",
                expected.getSurname(), expected.getForename(), expected.getPatronymic(),
                actual.getSurname(), actual.getForename(), actual.getPatronymic()));
    }

    private ReturnRecord fullNameRecord(String fullName) {
        ReturnRecord record = new ReturnRecord();
        record.setFullname(fullName);
        return record;
    }

}
