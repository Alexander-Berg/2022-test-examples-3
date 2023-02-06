package ru.yandex.autotests.innerpochta.util.props;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;

/**
 * Created by mabelpines
 */
@Resource.Classpath("webdriver.properties")
public class UrlProps {

    private static UrlProps instance;
    @Property("webdriver.base.url")
    private String baseUri = "https://mail.yandex.ru";
    @Property("webdriver.prod.url")
    private String prodUri = "https://mail.yandex.ru";
    @Property("webdriver.corp.url")
    private String corpUri = "https://mail.yandex-team.ru";
    @Property("webdriver.driver")
    private String driver = "chrome";
    @Property("experiment")
    private String experiments = null;
    @Property("experimentJson")
    private String experimentsJson = null;
    @Property("hazelcast.semaphore.permits")
    private String semaphorePermits = null;
    @Property("deviceType")
    private String deviceType = null;
    @Property("ignoreElement")
    private String ignoreElement = null;
    @Property("theme")
    private String theme = null;
    @Property("project")
    private String project = null;
    @Property("runCondition")
    private String runCondition = null;
    @Property("video")
    private String video = null;

    private UrlProps() {
        PropertyLoader.populate(this);
    }

    public static UrlProps getInstance() {
        return instance;
    }

    public static void setInstance(UrlProps instance) {
        UrlProps.instance = instance;
    }

    public static UrlProps urlProps() {
        if (null == instance) {
            instance = new UrlProps();
        }
        return instance;
    }

    public String getCorpUri() {
        return corpUri;
    }

    public void setCorpUri(String corpUri) {
        this.corpUri = corpUri;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getIgnoreElement() {
        return ignoreElement;
    }

    public void setIgnoreElement(String ignoreElement) {
        this.ignoreElement = ignoreElement;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getRunCondition() {
        return runCondition;
    }

    public void setRunCondition(String runCondition) {
        this.runCondition = runCondition;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public String getBaseUri() {
        return baseUri.replaceFirst("([a-z])(/+)", "$1");
    }

    public String getProdUri() {
        return prodUri;
    }

    public String setBaseUri(String url) {
        return baseUri = URI.create(url).toString();
    }

    public String setProdUri(String url) {
        return prodUri = URI.create(url).toString();
    }

    public Map<String, String> getExperiments() {
        if ((experiments != null) & (experimentsJson != null)) {
            return of(experiments, experimentsJson);
        }
        return new HashMap<>();
    }

    public void setExperiments(Map<String, String> expPairs) {
        for (Map.Entry<String, String> expPair : expPairs.entrySet()) {
            if (experiments == null) {
                experiments = expPair.getKey();
                experimentsJson = expPair.getValue();
            } else {
                experiments += "%3B" + expPair.getKey();
                experimentsJson += "," + expPair.getValue();
            }
        }
    }

    public String getSemaphorePermits() {
        return semaphorePermits;
    }

    public void setSemaphorePermits(String semaphorePermits) {
        this.semaphorePermits = semaphorePermits;
    }
}
