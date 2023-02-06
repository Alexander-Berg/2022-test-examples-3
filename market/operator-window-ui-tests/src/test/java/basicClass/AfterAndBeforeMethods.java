package ui_tests.src.test.java.basicClass;

import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import unit.Config;


public class AfterAndBeforeMethods {

    /**
     * Метод который должен выполняться перед
     *
     * @param className
     * @return
     */
    public WebDriver beforeClases(String className) {
        WebDriver webDriver;
        Config.readFileConfig();
        webDriver = BasicClass.newWebDriver();
        BasicClass.logInToSystem(webDriver, Config.getMainUserLogin(), Config.getMainUserPass());
        Config.deletingFilesFromFolder("target/surefire-reports/" + className);
        //Снять флаг "Запретить переход в Play режим, если есть обращения в процессинге"
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def obj = api.db.get('configuration@1') \n api.bcp.edit(obj, ['disablePlayModeIfProcessingTicketExists' : false,'useRedirectToNewCustomerCard':true])", false);
        Config.getConnectionEmails(webDriver);

        return webDriver;
    }


    public void afterClases(WebDriver webDriver) {
        webDriver.quit();
    }

}
