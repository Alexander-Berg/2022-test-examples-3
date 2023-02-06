package ru.yandex.direct.queryrec;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.queryrec.model.Language;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryrecJniTest {
    @Test
    void test_Recognize_IsSuccessful() {
        QueryrecJni queryrecJni = new QueryrecJni(true);

        Map<Language, Double> langsToProb = queryrecJni.recognize("test");
        assertThat(langsToProb.keySet(), Matchers.contains(Language.ENGLISH));

        queryrecJni.destroy();
    }

    @Test
    void test_Destroy_SkipOnSecondCall() {
        QueryrecJni queryrecJni = new QueryrecJni(true);
        queryrecJni.destroy();

        queryrecJni.destroy();
    }

    @Test
    void test_Recognize_FailedAfterDestroy() {
        QueryrecJni queryrecJni = new QueryrecJni(true);
        queryrecJni.recognize("test");
        queryrecJni.destroy();

        assertThrows(IllegalStateException.class,
                () -> queryrecJni.recognize("test"));
    }

}
