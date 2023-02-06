package ui_tests.src.test.java.interfaces.other;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface InfoTest {
    /**
     * ссылка на тест-кейс автоматизации
     *
     * @return
     */
    String linkFromTestCaseAutoTest() default "Ссылка не указана";

    /**
     * ссылка на тест-кейс из санитарки
     *
     * @return
     */
    String linkFromTestCaseSanityTest() default "Ссылка не указана";

    /**
     * описание что проверяется в тесте
     *
     * @return
     */
    String descriptionTest() default "Описание не указанно";

    /**
     * флаг указывающий на необходимость авторизации в системе
     *
     * @return
     */
    boolean requireYouToLogIn() default true;

    /**
     * Флаг указывающий на необходимость авторизации под отдельным пользователем
     *
     * @return
     */
    boolean requireYouToLogInUnderANewUser() default false;
}
