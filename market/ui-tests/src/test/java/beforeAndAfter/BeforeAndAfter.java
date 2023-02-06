package beforeAndAfter;

import helpers.Auth;
import helpers.CloseNotification;
import init.Config;
import init.InitDriver;
import org.openqa.selenium.WebDriver;

public class BeforeAndAfter {

    public static WebDriver beforeEach(){
        Config.readFileConfig();
        WebDriver driver = InitDriver.getDriver();
        Auth.auth(Config.getMainUserLogin(), Config.getMainUserPass(), Config.getProjectURL(), driver);
        CloseNotification.clearAllNotification(driver);
        return driver;
    }

    public static void afterEach(WebDriver driver) {
        driver.quit();
    }
}
