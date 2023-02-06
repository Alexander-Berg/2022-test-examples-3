package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.contact.ContactBlock;
import ru.yandex.autotests.innerpochta.ns.pages.abook.pages.AbookPage;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 12.10.12
 * Time: 19:19
 */
public class ContactExistsMatcher extends TypeSafeMatcher<AbookPage> {
    private String name;
    private Boolean mustExist;

    public boolean matchesSafely(AbookPage abookPage) {
        return mustExist.equals(contactExists(abookPage));
    }

    public ContactExistsMatcher(String name, Boolean mustExists) {
        this.name = name;
        this.mustExist = mustExists;
    }

    private boolean contactExists(AbookPage abookPage) {
        for (ContactBlock contact : abookPage.contacts()) {
            if (contact.name().getText().contains(name)) {
                return true;
            }
        }
        return false;
    }


    @Factory
    public static ContactExistsMatcher contactExists(String name) {
        return new ContactExistsMatcher(name, true);
    }


    @Factory
    public static ContactExistsMatcher contactNotExists(String name) {
        return new ContactExistsMatcher(name, false);
    }

    @Override
    public void describeMismatchSafely(AbookPage abookPage, Description description) {
        description.appendText("Контакт с именем ").appendText(name)
                .appendText(mustExist ? " не существует" : " существует");
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Контакт с именем ").appendText(name)
                .appendText(mustExist ? " существует" : " не существует");
    }
}
