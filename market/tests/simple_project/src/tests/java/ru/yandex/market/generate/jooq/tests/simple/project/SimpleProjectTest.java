package ru.yandex.market.generate.jooq.tests.simple.project;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.generate.jooq.tests.simple.project.generated.School;
import ru.yandex.market.generate.jooq.tests.simple.project.generated.tables.pojos.Student;
import ru.yandex.market.generate.jooq.tests.simple.project.generated.tables.pojos.Tutors;

public class SimpleProjectTest {

    /**
     * Тесты на то, что проект просто собирается.
     */
    @Test
    public void testCompile() {
        Tutors tutors = new Tutors();
        tutors.setFirstName("first name");
        tutors.setLastName("last name");

        Student student = new Student();
        student.setFirstName("first name");
        student.setLastName("last name");

        Assert.assertNotNull(School.SCHOOL);
    }
}
