package ru.yandex.autotests.innerpochta.imap.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 01.07.13
 * Time: 22:03
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Credentials {

    public String loginGroup() default "Default";

    public String login() default "";

    public String pwd() default "";

}
