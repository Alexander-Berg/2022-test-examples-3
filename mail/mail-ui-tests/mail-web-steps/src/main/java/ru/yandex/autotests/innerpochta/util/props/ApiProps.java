package ru.yandex.autotests.innerpochta.util.props;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;

import java.net.URI;

/**
 * Author @mabelpines
 */
@Resource.Classpath("api.properties")
public class ApiProps {

    private static ApiProps instance;

    public static ApiProps apiProps() {
        if (null == instance) {
            instance = new ApiProps();
        }
        return instance;
    }

    private ApiProps() {
        PropertyLoader.populate(this);
    }

    @Property("api.baseurl")
    private URI baseUrl = URI.create("https://mail.yandex.ru");

    @Property("api.modelsurl")
    private String modelsUrl = "/web-api/models/liza1";

    @Property("api.uploadurl")
    private String uploadUrl = "/web-api/upload-attachment/liza1";

    @Property("api.dosendjsonhandler")
    private String doSendJsonHandlerUrl = "/web-api/do-send/liza1?_send=true";

    @Property("api.newsletterFilters")
    private String newsletterFiltersUrl = "/web-api/get-newsletter-filters/v1";

    @Property("api.createNewsletterFilter")
    private String createNewsletterFilterUrl = "/web-api/create-newsletter-filters/v1";

    @Property("api.deleteNewsletterFilter")
    private String deleteNewsletterFilterUrl = "/web-api/delete-newsletter-filters/v1";

    public URI getbaseUri() {
        return baseUrl;
    }

    public String modelsUrl(){ return modelsUrl; }

    public String doSendJsonHandlerUrl() { return doSendJsonHandlerUrl; }

    public String uploadUrl() { return uploadUrl; }

    public String newsletterFiltersUrl() { return newsletterFiltersUrl; }

    public String createNewsletterFiltersUrl() { return createNewsletterFilterUrl; }

    public String deleteNewsletterFiltersUrl() { return deleteNewsletterFilterUrl; }
}
