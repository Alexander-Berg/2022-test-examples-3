package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class PersonParsingTest extends ParsingTest<Person> {

    public PersonParsingTest() {
        super(Person.class, "fixture/entities/person.xml");
    }

    @Override
    protected void performAdditionalAssertions(Person person) {
        assertions().assertThat(person.getName())
            .as("Asserting name value")
            .isEqualTo("Bla");
        assertions().assertThat(person.getSurname())
            .as("Asserting surname value")
            .isEqualTo("Blabla");
        assertions().assertThat(person.getPatronymic())
            .as("Asserting patronymic value")
            .isEqualTo("Blablablaa");
    }
}
