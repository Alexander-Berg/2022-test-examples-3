package ru.yandex.chemodan.util.test;

import ru.yandex.chemodan.util.jdbc.DataSourceProperties;
import ru.yandex.misc.db.embedded.PreparedDbProvider;
import ru.yandex.misc.ip.IpPort;
import ru.yandex.misc.lang.StringUtils;

/**
 * @author vpronto
 */
public class EmbeddedDBDataSourceProperties extends DataSourceProperties {

    private final PreparedDbProvider.DbInfo dbInfo;

    public EmbeddedDBDataSourceProperties(PreparedDbProvider.DbInfo dbInfo) {
        this.dbInfo = dbInfo;
    }

    @Override
    public String getPassword() {
        return dbInfo.getPassword();
    }

    @Override
    public String getUsername() {return dbInfo.getUser();}

    @Override
    public String getHosts() {
        return dbInfo.getHost();
    }

    @Override
    public String getDbName() {
        return dbInfo.getDbName();
    }

    @Override
    public IpPort getPort() {
        return new IpPort(dbInfo.getPort());
    }

    @Override
    public String getPgaasHost() {
        return dbInfo.getHost();
    }

    @Override
    public IpPort getPgaasPort() {
        return new IpPort(dbInfo.getPort());
    }

    @Override
    public String getUrlSuffix() {
        return StringUtils.replace(super.getUrlSuffix(), "ssl=true", "ssl=false");
    }

    @Override
    public String toString() {
        return "EmbeddedDBDataSourceProperties{" +
                "dbInfo=" + dbInfo +
                '}';
    }
}
