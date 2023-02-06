package ru.yandex.direct.oneshot.oneshots;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.oneshot.worker.def.BaseOneshot;

@OneshotTest
@RunWith(SpringRunner.class)
public class OneshotClassFieldsTest {

    @Autowired
    private List<BaseOneshot> oneshots;

    @Test
    public void doesNotHaveMutableFields() {
        SoftAssertions softAssertions = new SoftAssertions();

        for (BaseOneshot oneshot : oneshots) {
            Class<? extends BaseOneshot> oneshotClass = oneshot.getClass();
            for (Field declaredField : oneshotClass.getDeclaredFields()) {
                int modifiers = declaredField.getModifiers();

                softAssertions.assertThat(Modifier.isFinal(modifiers))
                        .describedAs("Field %s of oneshot %s should be final",
                                declaredField.getName(),
                                oneshotClass.getCanonicalName())
                        .isTrue();
            }
        }

        softAssertions.assertAll();
    }
}
