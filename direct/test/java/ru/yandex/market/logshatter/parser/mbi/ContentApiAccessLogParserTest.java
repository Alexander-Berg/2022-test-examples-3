package ru.yandex.market.logshatter.parser.mbi;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

/**
 * Created by tesseract on 10.03.15.
 */
public class ContentApiAccessLogParserTest {

    private LogParserChecker checker;

    @Test
    public void simpleLine() throws Exception {
        String line = "tskv\tunixtime=1496123282\tip=2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39\tuser_agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 YaBrowser/16.7.0.3265 (beta) Safari/537.36\tstatus=200\trequest_time=2\tsize=2436\tresource=GET_category\tx_request_id=2ht053052c5eec2\tx_market_req_id=1496123282230/b4ba642095166e4725ce4f81008b3552\tgeo_id=54\tpartner_id=2532\tclid=2236989\tstatistics_requests=0\tstatistics_bytes=0\tmethod=GET\turi=/category.xml?geo_id=54&secret=VFVpgn4km0MNGxD7budHjycARrOS4D&mobile_catalog=1&clid=2236989";
        checker.check(line,
            new Date(1496123282L * 1000),
            "hostname.test",
            "/category.xml?geo_id=54&secret=VFVpgn4km0MNGxD7budHjycARrOS4D&mobile_catalog=1&clid=2236989",
            "2532",
            2,
            200,
            "2ht053052c5eec2",
            "GET_category",
            "2236989",
            "2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39",
            "-",
            "-",
            "-",
            "-",
            "1496123282230/b4ba642095166e4725ce4f81008b3552",
            "-",
            false,
            "-");
    }

    @Test
    public void simpleLine2() throws Exception {
        String line = "tskv\tunixtime=1496121943\tip=93.158.131.150\tuser_agent=Java/1.8.0_112\tstatus=200\trequest_time=16\tsize=1346\tresource=GET_model/{}\tx_request_id=2ht053052c5ecb5\tx_market_req_id=1496121943163/35a46e0dfb621de0e5d041b8648d6b62\tgeo_id=154\tpartner_id=101\tsignature_status=fail\tapp_version=2.6.0\tdevice_type=TABLET\tplatform=IOS\tx_app_version=2.6\tx_device_type=TABLET\tx_platform=IOS\tuuid=20000000200000002000000020000000\tstatistics_requests=3\tstatistics_bytes=15318\tmethod=GET\turi=/v1/model/13058897.xml?count=30&geo_id=154&page=1&secret=5zaAy1Y9J3AV3vyXw9UpyCpSS0IYes&uuid=20000000200000002000000020000000";
        checker.check(line,
            new Date(1496121943L * 1000),
            "hostname.test",
            "/v1/model/13058897.xml?count=30&geo_id=154&page=1&secret=5zaAy1Y9J3AV3vyXw9UpyCpSS0IYes&uuid=20000000200000002000000020000000",
            "101",
            16,
            200,
            "2ht053052c5ecb5",
            "GET_model/id",
            "-",
            "93.158.131.150",
            "20000000200000002000000020000000",
            "IOS",
            "TABLET",
            "2.6.0",
            "1496121943163/35a46e0dfb621de0e5d041b8648d6b62",
            "-",
            false,
            "-");
    }

    @Test
    public void simpleLine3() throws Exception {
        String line = "tskv\tunixtime=1496123679\tip=2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39\tuser_agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 YaBrowser/16.7.0.3265 (beta) Safari/537.36\tstatus=200\trequest_time=82\tsize=738\tresource=GET_popular/{}\tx_request_id=2ht053052c5eedb\tx_market_req_id=1496123679275/32df8eceddeaf067bdc18e10c983f28a\tgeo_id=21274\tpartner_id=101\tsignature_status=fail\tuuid=20000000200000002000000020000000\tstatistics_requests=1\tstatistics_bytes=5\tmethod=GET\turi=/v1/popular/91708.xml?geo_id=21274&ip=:2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39&mobile_catalog=1&lac=151&cellid=19951982&operatorid=2&countrycode=257&signalstrength=0&uuid=20000000200000002000000020000000&secret=5zaAy1Y9J3AV3vyXw9UpyCpSS0IYes";
        checker.check(line,
            new Date(1496123679L * 1000),
            "hostname.test",
            "/v1/popular/91708.xml?geo_id=21274&ip=:2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39&mobile_catalog=1&lac=151&cellid=19951982&operatorid=2&countrycode=257&signalstrength=0&uuid=20000000200000002000000020000000&secret=5zaAy1Y9J3AV3vyXw9UpyCpSS0IYes",
            "101",
            82,
            200,
            "2ht053052c5eedb",
            "GET_popular/id",
            "-",
            "2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39",
            "20000000200000002000000020000000",
            "-",
            "-",
            "-",
            "1496123679275/32df8eceddeaf067bdc18e10c983f28a",
            "-",
            false,
            "-");
    }

    @Test
    public void simpleLine4() throws Exception {
        String line = "tskv\tunixtime=1496096895\tip=141.8.161.108\tuser_agent=Java/1.8.0_112\tstatus=200\trequest_time=2\tsize=2538\tresource=GET_category/{}/children\tx_request_id=2ht053052c5d932\tx_market_req_id=1496096895315/1daa64224ea3d58a9f9264dd1d6be86b\tgeo_id=213\tpartner_id=1\tstatistics_requests=0\tstatistics_bytes=0\tmethod=GET\turi=/v1/category/90401/children.xml?secret=1&geo_id=213&sort=name";
        checker.check(line,
            new Date(1496096895L * 1000),
            "hostname.test",
            "/v1/category/90401/children.xml?secret=1&geo_id=213&sort=name",
            "1",
            2,
            200,
            "2ht053052c5d932",
            "GET_category/id/children",
            "-",
            "141.8.161.108",
            "-",
            "-",
            "-",
            "-",
            "1496096895315/1daa64224ea3d58a9f9264dd1d6be86b",
            "-",
            false,
            "-");
    }

    @Test
    public void simpleLine5() throws Exception {
        String line = "tskv\tunixtime=1496125264\tip=2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39\tuser_agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 YaBrowser/16.7.0.3265 (beta) Safari/537.36\tstatus=200\trequest_time=504\tsize=299161\tresource=GET_V2/models/{}/offers\tx_request_id=2ht053052c5ef28\tx_market_req_id=1496125264392/ec0f9747b51201cb733e9ecefa32b6bd\tgeo_id=54\tpartner_id=1\tstatistics_requests=9\tstatistics_bytes=1145218\tmethod=GET\turi=/v2/models/10495456/offers?secret=1&fields=all&geo_id=54";
        checker.check(line,
            new Date(1496125264L * 1000),
            "hostname.test",
            "/v2/models/10495456/offers?secret=1&fields=all&geo_id=54",
            "1",
            504,
            200,
            "2ht053052c5ef28",
            "GET_V2/models/id/offers",
            "-",
            "2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39",
            "-",
            "-",
            "-",
            "-",
            "1496125264392/ec0f9747b51201cb733e9ecefa32b6bd",
            "-",
            false,
            "-");
    }

    @Test
    public void statusCode422() throws Exception {
        String line = "tskv\tunixtime=1496129319\tip=2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39\tuser_agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 YaBrowser/16.7.0.3265 (beta) Safari/537.36\tstatus=422\trequest_time=3\tsize=720\tresource=GET_V2/models/{}/offers\tx_request_id=2ht053052c5eff9\tx_market_req_id=1496129319352/b2c41ebff46628bd895671802f485679\tgeo_id=54\tpartner_id=1\tstatistics_requests=0\tstatistics_bytes=0\tmethod=GET\turi=/v2/models/10495456/offers?secret=1&fields=asd&geo_id=54";
        checker.check(line,
            new Date(1496129319L * 1000),
            "hostname.test",
            "/v2/models/10495456/offers?secret=1&fields=asd&geo_id=54",
            "1",
            3,
            422,
            "2ht053052c5eff9",
            "GET_V2/models/id/offers",
            "-",
            "2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39",
            "-",
            "-",
            "-",
            "-",
            "1496129319352/b2c41ebff46628bd895671802f485679",
            "-",
            false,
            "-");
    }

    @Test
    public void errorLine() throws Exception {
        String line = "java.lang.RuntimeException: java.util.concurrent.ExecutionException: java.util.concurrent.TimeoutException: Request 9d47 timeout: GET http://marketgurudaemon.yandex.ru:29300/gurudaemon/Filters?use_other_region_if_no_offers=1&currency=RUR&filter-currency=RUR&CAT_ID=969705&region=213&ftype=all";
        checker.checkEmpty(line);
    }

    @Test
    public void pingLine() throws Exception {
        String line = "tskv\tunixtime=1496125391\tip=2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39\tuser_agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 YaBrowser/16.7.0.3265 (beta) Safari/537.36\tstatus=200\trequest_time=0\tsize=5\tx_request_id=2ht053052c5ef30\tx_market_req_id=1496125391799/f69b0c1ab623ed20414641fc661ee226\tgeo_id=225\tstatistics_requests=0\tstatistics_bytes=0\tmethod=GET\turi=/ping";
        checker.check(line,
            new Date(1496125391L * 1000),
            "hostname.test",
            "/ping",
            "-",
            0,
            200,
            "2ht053052c5ef30",
            "-",
            "-",
            "2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39",
            "-",
            "-",
            "-",
            "-",
            "1496125391799/f69b0c1ab623ed20414641fc661ee226",
            "-",
            false,
            "-");
    }

    @Test
    public void lineWithRequestSignature() throws Exception {
        String line = "tskv\tunixtime=1496129031\tip=2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39\tuser_agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 YaBrowser/16.7.0.3265 (beta) Safari/537.36\tstatus=200\trequest_time=343\tsize=299251\tresource=GET_V2/models/{}/offers\tx_request_id=2ht053052c5efe9\tx_market_req_id=1496129031349/76cab067cafde85fb8fa5b1e1a9de250\tgeo_id=54\tpartner_id=1\tstatistics_requests=9\tstatistics_bytes=1145452\tmethod=GET\turi=/v2/models/10495456/offers?secret=1&fields=all&geo_id=54\tsignature=361ec5c88820afebaf259608189b031eqwer\tsignature_status=ok";
        checker.check(line,
            new Date(1496129031L * 1000),
            "hostname.test",
            "/v2/models/10495456/offers?secret=1&fields=all&geo_id=54",
            "1",
            343,
            200,
            "2ht053052c5efe9",
            "GET_V2/models/id/offers",
            "-",
            "2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39",
            "-",
            "-",
            "-",
            "-",
            "1496129031349/76cab067cafde85fb8fa5b1e1a9de250",
            "361ec5c88820afebaf259608189b031eqwer",
            true,
            "-");
    }

    @Test
    public void lineWithException() throws Exception {
        String line = "tskv\tunixtime=1551253756\tip=2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39\tuser_agent=PostmanRuntime/7.1.5\tstatus=500\trequest_time=9952\tsize=324\tx_request_id=u0000l02272dec9cae\tx_market_req_id=1551253756541/98f6c5e4bdae03b4f370087327ddcaad\tgeo_id=225\tstatistics_requests=2\tstatistics_bytes=0\texception=java.lang.RuntimeException\tmethod=GET\turi=/v2/user/cart/items/3303046?uuid=f2c36de8588b880589d9c62978101bc5";
        checker.check(line,
            new Date(1551253756L * 1000),
            "hostname.test",
            "/v2/user/cart/items/3303046?uuid=f2c36de8588b880589d9c62978101bc5",
            "-",
            9952,
            500,
            "u0000l02272dec9cae",
            "-",
            "-",
            "2a02:6b8:0:2807:9d3f:1efb:9fbd:6c39",
            "-",
            "-",
            "-",
            "-",
            "1551253756541/98f6c5e4bdae03b4f370087327ddcaad",
            "-",
            false,
            "java.lang.RuntimeException");
    }

    @Before
    public void setUp() {
        ContentApiAccessLogParser parser = new ContentApiAccessLogParser();
        checker = new LogParserChecker(parser);
    }
}
