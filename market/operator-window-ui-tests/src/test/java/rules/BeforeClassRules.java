package ui_tests.src.test.java.rules;

import basicClass.AfterAndBeforeMethods;
import org.junit.rules.ExternalResource;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Field;

public class BeforeClassRules extends ExternalResource {
    private Class myClass;
    private WebDriver webDriver;

    public BeforeClassRules(Class myClass) {
        this.myClass = myClass;
    }


    @Override
    protected void before() {

        webDriver = new AfterAndBeforeMethods().beforeClases(myClass.getName());
        try {
            Field field = myClass.getDeclaredField("webDriver");
            field.setAccessible(true);
            field.set(myClass, webDriver);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void after() {
        try {
            Field field = myClass.getDeclaredField("webDriver");
            field.setAccessible(true);
            webDriver = (WebDriver) field.get(myClass);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        new AfterAndBeforeMethods().afterClases(webDriver);
    }
}
