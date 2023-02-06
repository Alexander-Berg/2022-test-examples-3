package deprecation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NewController {
    @GetMapping("/v1/person/{name}")
    public Person getPerson(String name) {
        return new Person(name);
    }

    @GetMapping("/v0/person/{name}")
    @Deprecated
    public Person getPersonDeprecated(String name) {
        return getPerson(name);
    }

    @GetMapping("/v0/persons/{names}")
    public List<Person> getPersons(@Deprecated String... names) {
        return Arrays.stream(names).map(this::getPerson).collect(Collectors.toList());
    }

    public static class Person {
        private final String fullName;
        @Deprecated
        private final String name;

        public Person(String fullName) {
            this.fullName = fullName;
            this.name = fullName;
        }

        public String getFullName() {
            return fullName;
        }

        @Deprecated
        public String getName() {
            return name;
        }
    }
}
