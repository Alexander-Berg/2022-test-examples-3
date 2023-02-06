package ru.yandex.market.checkout.pushapi.shop;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.pushapi.client.entity.shop.AuthType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.DataType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;

/**
 * See MBI-11441 comments for details on url selection
 *
 * Created by oroboros on 07.10.14.
 */
public class ApiSelectorUtil {
    private static final Logger log = Logger.getLogger(ApiSelectorUtil.class);
    private static final String STUB_URL = "http://%s:%d/%s"; //localhost, 33486, shopId

    private String stubHost;
    private int stubPort;

    public ApiSelection getApiUrl(long shopId, boolean sandbox, Settings settings, String resource, Object request) {
        String urlPrefix = null;
        String args = null;
        DataType dataType = null;
        boolean isShopadmin = false;

        if(resource.contains("cart")) {
            //selection based on settings (and shop api if sandbox)
            if(isStubRequest(settings, sandbox)) {
                urlPrefix = buildStubUrl(shopId);
                isShopadmin = true;
            }
            else {
                urlPrefix = settings.getUrlPrefix();
                args = createArgs(settings);
            }
        }
        else if(resource.contains("order/accept")) {
            //selection based on order accept method
            Order order = (Order) request;

            if(log.isDebugEnabled()) {
                log.debug("Order accept method: " + order.getAcceptMethod());
            }

            if(order.getAcceptMethod() == OrderAcceptMethod.WEB_INTERFACE) {
                urlPrefix = buildStubUrl(shopId);
                isShopadmin = true;
            }
            else {
                urlPrefix = settings.getUrlPrefix();
                args = createArgs(settings);
            }
        }
        else if(resource.contains("order/status")) {
            //selection based on settings, sandbox and order accept method
            Order order = (Order) request;

            if(log.isDebugEnabled()) {
                log.debug("Order accept method: " + order.getAcceptMethod());
            }

            if(order.getAcceptMethod() == OrderAcceptMethod.WEB_INTERFACE) {
                urlPrefix = buildStubUrl(shopId);
                isShopadmin = true;
            }
            else if(isStubRequest(settings, sandbox)) {
                urlPrefix = buildStubUrl(shopId);
                isShopadmin = true;
            }
            else {
                urlPrefix = settings.getUrlPrefix();
                args = createArgs(settings);
            }
        }

        if(urlPrefix == null) {
            throw new IllegalStateException(
                    "Can't select url (shopadmin-stub or shop's api) for push api. This is a programming error."
            );
        }

        if(isShopadmin) {
            dataType = DataType.XML;
            log.debug("Requesting shopadmin-stub");
        } else {
            dataType = settings.getDataType();
            log.debug("Requesting shop API");
        }

        final StringBuilder url = new StringBuilder().append(urlPrefix);
        if(!urlPrefix.endsWith("/")) {
            url.append("/");
        }
        url.append(resource);

        return new ApiSelection(isShopadmin, url.toString(), args, dataType);
    }

    public static class ApiSelection {
        private boolean isShopadmin;
        private String url;
        private String args;
        private DataType dataType;

        public ApiSelection(boolean isShopadmin, String url, String args, DataType dataType) {
            this.isShopadmin = isShopadmin;
            this.url = url;
            this.args = args;
            this.dataType = dataType;
        }

        public boolean isShopadmin() {
            return isShopadmin;
        }

        public String getUrl() {
            return url;
        }

        public String getArgs() {
            return args;
        }

        public DataType getDataType() {
            return dataType;
        }

        public String getUri() {
            return url + (args == null ? "" : ("?" + args));
        }
    }

    private boolean isStubRequest(Settings settings, boolean sandbox) {
        return settings.isPartnerInterface() && !sandbox;
    }

    private String buildStubUrl(long shopId) {
        return String.format(STUB_URL, stubHost, stubPort, shopId);
    }

    private String createArgs(Settings settings) {
        final StringBuilder sb = new StringBuilder();

        //shopadmined always header auth, no extra url param required
        if(settings.getAuthType() == AuthType.URL) {
            sb.append("auth-token=" + settings.getAuthToken());
        }

        return sb.toString();
    }

    public String getStubHost() {
        return stubHost;
    }

    @Required
    public void setStubHost(String stubHost) {
        this.stubHost = stubHost;
    }

    public int getStubPort() {
        return stubPort;
    }

    @Required
    public void setStubPort(int stubPort) {
        this.stubPort = stubPort;
    }
}
