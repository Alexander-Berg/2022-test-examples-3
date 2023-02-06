package ru.yandex.autotests.testpers.misc;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;

import java.net.URI;

/**
 * User: lanwen
 * Date: 28.04.15
 * Time: 22:10
 */
@Resource.Classpath("pgsql.db.properties")
public class PgProperties {

    public static final String CONN_STRING = "jdbc:postgresql://%s:%s/%s";

    private static PgProperties instance;

    public static PgProperties pgProps() {
        if (instance == null) {
            instance = new PgProperties();
        }
        return instance;
    }

    public PgProperties() {
        PropertyLoader.populate(this);
    }

    @Property("pgsql.db.uri")
    private URI dburi = URI.create("schema://bases:5432");

    @Property("pgsql.db.user")
    private String dbuser;

    @Property("pgsql.db.pwd")
    private String dbpwd;

    @Property("pgsql.db.db")
    private String dbdb;

    @Property("TRANSFER_PKG")
    private String transferPkg = "yamail-ora2pg";


    public URI getDburi() {
        return dburi;
    }

    public String getDbuser() {
        return dbuser;
    }

    public String getDbpwd() {
        return dbpwd;
    }

    public String getDbdb() {
        return dbdb;
    }

    public String getTransferPkg() {
        return transferPkg;
    }
}
