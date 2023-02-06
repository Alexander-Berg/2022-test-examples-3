package ru.yandex.autotests.innerpochta.matchers;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.AlreadyCreatedFilterBlock;

import java.util.Arrays;
import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.hasSize;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 12.10.12
 * Time: 19:19
 */
public class FilterActionsMatcher extends TypeSafeMatcher<AlreadyCreatedFilterBlock> {

    private List<String> expectedActionsList;

    public boolean matchesSafely(AlreadyCreatedFilterBlock filterBlock) {
        for (MailElement action : filterBlock.conditionActionsList()) {
            if (!expectedActionsList.contains(action.getText())) {
                return false;
            }
        }
        return hasSize(expectedActionsList.size()).matches(filterBlock.conditionActionsList());
    }

    public FilterActionsMatcher(String[] expectedActions) {
        expectedActionsList = Arrays.asList(expectedActions);
    }

    @Factory
    public static FilterActionsMatcher filterActionsContains(String... expectedActions) {
        return new FilterActionsMatcher(expectedActions);
    }

    @Override
    public void describeMismatchSafely(AlreadyCreatedFilterBlock filterBlock, Description description) {
        description.appendValueList("Список действий: ", "\n\t", "", extract(filterBlock.conditionActionsList(),
                on(MailElement.class).getText()));
    }


    @Override
    public void describeTo(Description description) {
        description.appendValueList("Список действий в фильтре должен быть: ", "\n\t", "", expectedActionsList);
    }
}
