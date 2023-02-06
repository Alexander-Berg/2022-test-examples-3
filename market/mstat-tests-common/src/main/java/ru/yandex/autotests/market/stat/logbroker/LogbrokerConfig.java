package ru.yandex.autotests.market.stat.logbroker;

import com.google.common.base.Strings;
import ru.yandex.autotests.market.common.differ.WithId;
import ru.yandex.autotests.market.stat.beans.cpaclicks.CpaClick;
import ru.yandex.autotests.market.stat.beans.vendorclicks.VendorClicks;
import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;

/**
 * Created by kateleb on 06.11.15.
 */
@Resource.Classpath("logbroker.properties")
public class LogbrokerConfig {

    public final static String CLICKS = "clicks";
    public final static String VENDOR_CLICKS = "vendor-clicks";
    public final static String CPA_CLICKS = "cpa-clicks";

    @Property("logbroker.dc")
    private String dc = "man";

    @Property("logbroker.meta.host")
    public String metaHost = "man.logbroker-prestable.yandex.net";

    @Property("logbroker.port")
    public Integer port = 8999;

    @Property("logbroker.ident")
    public String ident = "marketstat";

    @Property("logbroker.click.logtype")
    public String clickLogtype = "market-clicks-log";

    @Property("logbroker.cpa.logtype")
    public String cpaLogtype = "market-cpa-clicks-log";

    @Property("logbroker.vendor.logtype")
    public String vendorLogtype = "market-vendor-clicks-log";

    @Property("logbroker.logtype")
    public String requestedLogtype;

    @Property("logbroker.chunk.size")
    public Integer chunkSize = 5000;

    @Property("logbroker.client_id")
    public String clientId = "marketstat-dev";


    public LogbrokerConfig() {
        PropertyLoader.populate(this);
    }

    public LogbrokerConfig(String ident) {
        this();
        this.setIdent(ident);
    }

    public String getLogtype(Class<? extends WithId> aClass) {
        return !Strings.isNullOrEmpty(requestedLogtype) ? requestedLogtype :
            aClass.equals(VendorClicks.class) ? getVendorLogtype() :
                aClass.equals(CpaClick.class) ? getCpaLogtype() : getClickLogtype();
    }

    public String getLogtype(String dataType, boolean isRollbackTopic) {
        String logtype = !Strings.isNullOrEmpty(requestedLogtype) ? requestedLogtype :
            dataType.equals(VENDOR_CLICKS) ? getVendorLogtype() :
                dataType.equals(CPA_CLICKS) ? getCpaLogtype() : getClickLogtype();

        if (isRollbackTopic && !Strings.isNullOrEmpty(logtype) && !logtype.contains("rollback")) {
            logtype = logtype.replace("-log", "-rollbacks-log");
        }
        return logtype;
    }

    public String getTopic(String logtype) {
        return String.format("rt3.%s--%s--%s:0", dc, ident, logtype);
    }

    public void setIdent(String ident) {
        this.ident = ident;
    }

    public Integer getPort() {
        return port;
    }

    public String getIdent() {
        return ident;
    }

    public String getClientId() {
        return clientId;
    }


    public String getMetaHost() {
        return metaHost;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public String getDc() {
        return dc;
    }

    public String getClickLogtype() {
        return clickLogtype;
    }

    public String getCpaLogtype() {
        return cpaLogtype;
    }

    public String getVendorLogtype() {
        return vendorLogtype;
    }


}
