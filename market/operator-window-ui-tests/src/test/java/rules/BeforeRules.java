package ui_tests.src.test.java.rules;

import basicClass.BasicClass;
import entity.Entity;
import interfaces.other.InfoTest;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.RemoteWebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import tools.Tools;
import unit.Config;

import java.lang.reflect.Field;
import java.util.Arrays;

public class BeforeRules extends TestWatcher {
    private WebDriver webDriver;

    public BeforeRules(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    private void takeUserOffline(String user){
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def employee = api.db.of('employee')\n" +
                ".withFilters{ eq('staffLogin', '" + user + "') }\n" +
                ".get()\n" +
                "\n" +
                "def employeeDistributionStatus = api.db.of('employeeDistributionStatus')\n" +
                ".withFilters{ eq('employee', employee) }\n" +
                ".get()\n" +
                " api.bcp.edit(employeeDistributionStatus, ['status' : 'offline'])", false);
    }

    @Override
    protected void failed(Throwable e, Description description) {
        Pages.ticketPage(webDriver).toast().hideNotificationError();
        Tools.other().captureScreenshot(webDriver, description.getClassName(), description.getClassName() + "." + description.getMethodName());
        String message = "";
        if (description.getAnnotation(InfoTest.class) != null) {
            message += "\nОписание упавшего теста - " + description.getAnnotation(InfoTest.class).descriptionTest();
        }
        if (description.getAnnotation(InfoTest.class) != null) {
            message += "\nТест-кейс автотеста - " + description.getAnnotation(InfoTest.class).linkFromTestCaseAutoTest();
        }
        if (description.getAnnotation(InfoTest.class) != null) {
            message += "\nСсылка на тест-кейс Санитарки - " + description.getAnnotation(InfoTest.class).linkFromTestCaseSanityTest();
        }
        try {
            message += "\nСсылка на страницу где была ошибка - " + webDriver.getCurrentUrl() + "\n\n";
        } catch (Throwable ignore) {
        }

        message += "Ошибки со страницы - " + Pages.alertDanger(webDriver).getAlertDangerMessages();

        message += ("\nЛоги браузера - http://selenium.yandex-team.ru/logs/" + ((RemoteWebDriver) webDriver).getSessionId());
        message += ("\nВидео прохождения теста - http://selenium.yandex-team.ru/video/" + ((RemoteWebDriver) webDriver).getSessionId());
        System.err.println(message+"\n"+e.getMessage()+"\n"+ Arrays.toString(e.getStackTrace()));

        throw new Error(e);

    }

    @Override
    protected void starting(Description description) {
        try {
            webDriver.getCurrentUrl();
        }
        catch (NoSuchSessionException noSuchSessionException){
            webDriver = BasicClass.newWebDriver();
        }
        catch (WebDriverException webDriverException){
            if (webDriverException.getMessage().contains("Session timed out or not found")){
                webDriver = BasicClass.newWebDriver();
            }
        }

        System.out.println("Старт теста: " + description.getClassName() + "." + description.getMethodName());
        webDriver.get(Config.getProjectURL());
        // Определяем, под кем нужно авторизоваться
        if (description.getAnnotation(InfoTest.class).requireYouToLogIn()) {
            if (description.getAnnotation(InfoTest.class).requireYouToLogInUnderANewUser()) {
                takeUserOffline(Config.getAdditionalUserLogin());
                webDriver.quit();
                //Ожидаем пока освободится дополнительный пользователь
                while (!Config.freeAdditionalUser) {
                    Tools.waitElement(webDriver).waitTime(2000);
                }
                Config.freeAdditionalUser = false;

                webDriver = BasicClass.newWebDriver();
                BasicClass.logInToSystem(webDriver, Config.getAdditionalUserLogin(), Config.getAdditionalUserPass());
                try {
                    Field field = description.getTestClass().getDeclaredField("webDriver");
                    field.setAccessible(true);
                    field.set(description.getTestClass(), webDriver);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } else {
            webDriver.quit();
            webDriver = BasicClass.newWebDriver();
            try {
                Field field = description.getTestClass().getDeclaredField("webDriver");
                field.setAccessible(true);
                field.set(description.getTestClass(), webDriver);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            webDriver.get(Config.getProjectURL());
        }

        Entity.toast(webDriver).hideNotificationError();
    }

    @Override
    protected void finished(Description description) {
        Tools.tabsBrowser(webDriver).resetBrowserTabsToOneAppTab();

        if (!description.getAnnotation(InfoTest.class).requireYouToLogIn()) {
            BasicClass.logInToSystem(webDriver, Config.getMainUserLogin(), Config.getMainUserPass());
        } else {
            if (description.getAnnotation(InfoTest.class).requireYouToLogInUnderANewUser()) {
                takeUserOffline(Config.getAdditionalUserLogin());
                webDriver.quit();
                Config.freeAdditionalUser = true;
                webDriver = BasicClass.newWebDriver();
                try {
                    Field field = description.getTestClass().getDeclaredField("webDriver");
                    field.setAccessible(true);
                    field.set(description.getTestClass(), webDriver);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                BasicClass.logInToSystem(webDriver, Config.getMainUserLogin(), Config.getMainUserPass());
            }
        }

    }
}
