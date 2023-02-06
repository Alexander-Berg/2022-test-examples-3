package ru.yandex.autotests.direct.objectfactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.autotests.direct.utils.beans.MongoBeanLoader;
import ru.yandex.autotests.direct.web.data.timetarget.TimeTargetInfoWeb;
import ru.yandex.autotests.direct.web.util.beanutils.MongoPropertyLoader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class MongoPropertyLoaderTest {

    @Before
    public void setUp() {
        MongoPropertyLoader loader = new MongoPropertyLoader<>(TimeTargetInfoWeb.class);
        TimeTargetInfoWeb tt = new TimeTargetInfoWeb();
        tt.setTimeZone("moscow");
        tt.setHolidayShowFrom(7);
        tt.setHolidayShowTo(20);
        loader.save(tt, "Template1");
        tt = new TimeTargetInfoWeb();
        tt.setWorkingHolidays("working");
        loader.save(tt, "Template2");
    }

    @Test
    @Ignore
    public void test() {
        MongoPropertyLoader ml = new MongoPropertyLoader<>(TimeTargetInfoWeb.class);
        TimeTargetInfoWeb tt = (TimeTargetInfoWeb) ml.apply("Template1")
                .apply("Template2").toObject();
        assertThat(tt.getTimeZone(), equalTo("moscow"));
        assertThat(tt.getHolidayShowFrom(), equalTo(7));
        assertThat(tt.getHolidayShowTo(), equalTo(20));
        assertThat(tt.getWorkingHolidays(), equalTo("working"));
    }

    @After
    public void tearDown() {
        MongoBeanLoader<TimeTargetInfoWeb> mongoBeanLoader = new MongoBeanLoader<>(TimeTargetInfoWeb.class,
                "testCollection");
        mongoBeanLoader.removeBean("Template1");
        mongoBeanLoader.removeBean("Template2");
    }
}