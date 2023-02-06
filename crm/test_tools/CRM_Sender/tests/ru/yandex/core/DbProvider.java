package ru.yandex.core;
import oracle.jdbc.pool.OracleDataSource;
import org.sql2o.Sql2o;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.codec.binary.Base64;

/**
 * Created by nasyrov on 12.03.2016.
 */
public class DbProvider {


    public static Connection openConnection() throws SQLException {
        OracleDataSource ods = new OracleDataSource();
        ods.setURL(Settings.getOraString());
        Connection conn = ods.getConnection();
        return  conn;
    }

    //public static void main(String args[]) throws IOException {
        //String stringUrl = "https://vault-api.passport.yandex.net/1/secrets/sec-0000000000000000000000ygj0/";
        //URL url = new URL(stringUrl);
        //URLConnection uc = url.openConnection();

        //uc.setRequestProperty("X-Ya-User-Ticket", "Curl");

        //String userpass = "username" + ":" + "password";
        //String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
        //uc.setRequestProperty("Authorization", basicAuth);

        //InputStreamReader inputStreamReader = new InputStreamReader(uc.getInputStream());
        // read this input

    //}

    public static Sql2o db() {
        return new Sql2o(Settings.getOraString(),Settings.get("oracle.user"),Settings.get("oracle.password"));
    }
}
