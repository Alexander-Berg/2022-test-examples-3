package steps.ordersteps.ordersubsteps;

import ru.yandex.market.delivery.entities.common.Person;

public class PersonSteps {

    private PersonSteps() {
        throw new UnsupportedOperationException();
    }

    public static Person getPerson() {
        Person person = new Person();

        person.setName("Иван");
        person.setSurname("Иванов");
        person.setPatronymic("Иванович");

        return person;
    }
}
