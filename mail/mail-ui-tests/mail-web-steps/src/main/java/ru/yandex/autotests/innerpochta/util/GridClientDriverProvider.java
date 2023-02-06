package ru.yandex.autotests.innerpochta.util;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.autotests.webcommon.util.prop.WebDriverProperties;
import ru.yandex.qatools.selenium.grid.GridClient;

public class GridClientDriverProvider{

    private DesiredCapabilities capabilities;
    private WebDriverProperties properties;

    public GridClientDriverProvider() {
    }

    public WebDriverProperties getProperties() {
        if (this.properties == null) {
            this.properties = WebDriverProperties.props();
        }

        return this.properties;
    }

    public GridClientDriverProvider usingCapabilities(DesiredCapabilities capabilities) {
        this.capabilities = capabilities;
        return this;
    }

    public DesiredCapabilities getCapabilities() {
        if (this.capabilities == null) {
            this.capabilities = new DesiredCapabilities(
                this.getProperties().driverType(),
                this.getProperties().version(),
                this.getProperties().platform()
            );
        }

        return this.capabilities;
    }

    public WebDriver getDriver() {
        return new GridClient().find(getCapabilities());
    }

}
