package ru.yandex.hadoop.woodsman.loaders.logbroker.parser;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.JIpReg;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by oroboros on 16.12.15.
 */
public class JIpRegTest {
    private final Random rnd = new Random();
    private JIpReg ipReg;

    @Before
    public void loadIpReg() throws IOException, URISyntaxException {
        Reader jsonReader = Files.newBufferedReader(Paths.get(getClass().getResource("/ipreg_layout.json").toURI()));
        ipReg = new JIpReg(jsonReader);
    }

    @Test
    public void mustBeYandex() throws UnknownHostException {
        for(String yaIp: yaIps) {
            assertThat(ipReg.isYandex(yaIp), is(true));
        }
    }

    @Test
    public void diapasonEndsMustBeYandex() throws UnknownHostException {
        for(String yaIp: yaIpsEnds) {
            assertThat(ipReg.isYandex(yaIp), is(true));
        }
    }

    @Test
    public void mustNotBeYandex() throws UnknownHostException {
        for(String notYaIp: notYaIps) {
            assertThat(ipReg.isYandex(notYaIp), is(false));
        }
    }

    public volatile boolean blackHole;
    private static final int N = 100_000;
    private static final int M = 100;

    /*
    This impl:
        Test # 0: per single call (ns): 368.0
        Test # 1: per single call (ns): 388.8
        Test # 2: per single call (ns): 387.2
        Test # 3: per single call (ns): 377.3
        Test # 4: per single call (ns): 386.7

    Same tests for native libipreg1:
        Test # 0: per single call (ns): 3235.0
        Test # 1: per single call (ns): 3254.2
        Test # 2: per single call (ns): 3153.0
        Test # 3: per single call (ns): 3197.4
        Test # 4: per single call (ns): 3154.5

    Reproducing native libipreg1 test:
        ssh bibigon
        cd /home/oroboros/ipreg1
        javac -cp .:ipreg1-1.0-34.jar A.java && \
         java -cp .:/usr/lib/ipreg1/ipreg1.jar -Djava.library.path=/usr/lib/:/usr/lib/ipreg1/ A
     */
    // @Test
    public void shittyPerformanceTest() throws UnknownHostException {
        // warm up
        for(int i = 0; i < M + rnd.nextInt(2); i++) {
            blackHole = iteration();
        }

        for(int i = 0; i < M + rnd.nextInt(2); i++) {
            blackHole = iteration();
        }

        // test
        for(int test = 0; test < 5; test++) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < M; i++) {
                blackHole = iteration();
            }
            System.out.println("Test # " + test + ": per single call (ns): " + 1000000.0 * (System.currentTimeMillis() - start) / (N * M));
        }
    }

    private boolean iteration() throws UnknownHostException {
        for(int i = 0; i < N; i++) {
            int rndInt = rnd.nextInt(100);
            String ip;
            if(rndInt == 0) {
                ip = randomIp4();
            }
            else if(rndInt <= 75) {
                ip = notYaIps[rnd.nextInt(notYaIps.length)];
            }
            else {
                ip = yaIps[rnd.nextInt(yaIps.length)];
            }

            blackHole = ipReg.isYandex(ip);
        }

        return blackHole;
    }


    private String randomIp4() {
        return rnd.nextInt(256) + "." + rnd.nextInt(256) + "." + rnd.nextInt(256) + "." + rnd.nextInt(256);
    }


    private final String yaIpsEnds[] = new String[] {
            "5.45.222.96", "5.45.222.103", "2a02:6b8:f000:4c4::", "2a02:6b8:f000:4c4:ffff:ffff:ffff:ffff"
    };

    private final String yaIps[] = new String[] {
            "2a02:6b8:0:1495::20", "2a02:6b8:0:1495::3c", "2a02:6b8:0:1495::a7", "2a02:6b8:0:1495::d8", "2a02:6b8:0:2f03:ecd7:c221:f780:18af",
            "2a02:6b8:0:401:3801:f0da:7e8e:c971","2a02:6b8:0:408:309e:e12c:3d1e:dc81", "2a02:6b8:0:40c:3cb2:49fd:14ec:d06b",
            "2a02:6b8:0:40c:c0d1:76dd:bb83:b65c", "2a02:6b8:0:40e:2d0b:7ffe:bf7:d6fa", "2a02:6b8:0:4::7b", "2a02:6b8:0:4::a4",
            "2a02:6b8:0:4::c", "2a02:6b8:0:5::5a", "2a02:6b8:0:5::86", "2a02:6b8:0:6::5", "2a02:6b8:0:6::98", "2a02:6b8:0:6::ef",
            "2a02:6b8:0:81f::15", "2a02:6b8:0:81f::16a", "2a02:6b8:0:81f::1dc", "2a02:6b8:0:81f::1eb", "2a02:6b8:0:c33::11",
            "2a02:6b8:0:c33::158", "2a02:6b8:0:c33::1de", "2a02:6b8:0:c33::1e4", "2a02:6b8:0:c33::e9", "2a02:6b8:0:c33::ee", "2a02:6b8:b010:50c:0:d0c0:3:8", "2a02:6b8:b010:50c:0:d0c0:7:2", "::ffff:130.193.34.123", "::ffff:130.193.34.17",
            "::ffff:130.193.34.198", "::ffff:130.193.34.206", "::ffff:130.193.34.33", "::ffff:130.193.34.37", "::ffff:130.193.34.42",
            "::ffff:130.193.40.188", "::ffff:130.193.40.223", "::ffff:130.193.40.61", "::ffff:130.193.40.74", "::ffff:84.201.150.106",
            "::ffff:84.201.150.144", "::ffff:84.201.150.245", "::ffff:84.201.150.61", "::ffff:84.201.151.125", "::ffff:84.201.151.75",
            "::ffff:84.201.164.161", "::ffff:84.201.165.247", "::ffff:84.201.165.92", "::ffff:84.201.166.145", "::ffff:84.201.166.226",
            "::ffff:84.201.166.31", "::ffff:84.201.166.95", "::ffff:84.201.167.1", "::ffff:84.201.167.182", "::ffff:84.201.167.199",
            "::ffff:95.108.174.199", "2a02:6b8:0:1492::2", "2a02:6b8:0:1495::104", "2a02:6b8:0:1495::2a", "2a02:6b8:0:1495::8e",
            "2a02:6b8:0:1495::9d", "2a02:6b8:0:2309:3555:6a2b:854f:25ba", "2a02:6b8:0:2807:8460:439a:797:40eb", "2a02:6b8:0:40c:95e6:2780:cb6a:95b5",
            "2a02:6b8:0:410:483e:9e2f:e300:1c63", "2a02:6b8:0:4::42", "2a02:6b8:0:4::ce", "2a02:6b8:0:5::1d", "2a02:6b8:0:5::47",
            "2a02:6b8:0:5::8a", "2a02:6b8:0:5::ed", "2a02:6b8:0:6::46", "2a02:6b8:0:6::6d", "2a02:6b8:0:6::73", "2a02:6b8:0:81c:b1c9:54e5:44fd:d426",
            "2a02:6b8:0:81f::10d", "2a02:6b8:0:81f::199", "2a02:6b8:0:81f::41", "2a02:6b8:0:81f::cf", "2a02:6b8:0:82c::7", "2a02:6b8:0:c33::15a",
            "2a02:6b8:0:c33::186", "2a02:6b8:0:c33::1b4", "2a02:6b8:0:c33::1db", "2a02:6b8:0:c33::56", "2a02:6b8:0:c33::7d", "2a02:6b8:0:c33::ab",
            "2a02:6b8:b010:50c:0:d0c0:4:4", "2a02:6b8:b010:50c:0:d0c0:5:3", "::ffff:130.193.34.203", "::ffff:130.193.34.234", "::ffff:130.193.34.43",
            "::ffff:130.193.34.61", "::ffff:130.193.34.67", "::ffff:130.193.34.72", "::ffff:130.193.34.76", "::ffff:130.193.35.136", "::ffff:130.193.35.19",
            "::ffff:130.193.35.237", "::ffff:130.193.35.59", "::ffff:130.193.35.66", "::ffff:130.193.35.71", "::ffff:130.193.40.107",
            "::ffff:130.193.40.110", "::ffff:130.193.40.178", "::ffff:130.193.40.219", "::ffff:130.193.40.33", "::ffff:130.193.61.61",
            "::ffff:141.8.178.146", "::ffff:141.8.178.155", "::ffff:141.8.178.159", "::ffff:84.201.150.17", "::ffff:84.201.150.189",
            "::ffff:84.201.150.40", "::ffff:84.201.151.212", "::ffff:84.201.151.56", "::ffff:84.201.151.76", "::ffff:84.201.151.92",
            "::ffff:84.201.164.122", "::ffff:84.201.164.157", "::ffff:84.201.164.177", "::ffff:84.201.164.212", "::ffff:84.201.164.218",
            "::ffff:84.201.164.56", "::ffff:84.201.164.90", "::ffff:84.201.165.17", "::ffff:84.201.165.35", "::ffff:84.201.165.55", "::ffff:84.201.165.57",
            "::ffff:84.201.165.97", "::ffff:84.201.166.180", "::ffff:84.201.166.207", "::ffff:84.201.167.123", "::ffff:84.201.167.172",
            "::ffff:84.201.167.217"
    };

    private final String notYaIps[] = new String[]{
            "2001:41d0:a:255f::1", "2001:41d0:d:29b3::", "2001:470:1f0b:81:611b:2704:ac2a:5b1", "2001:470:1f15:8cb:e4e9:c0e7:8a26:a11d",
            "2001:470:28:5c2:4db9:5400:f517:50e1", "2001:470:6d:80f:75ed:d1f4:98fc:67b7", "2001:67c:2084:109::e9e8", "2001:67c:2084:160::8208",
            "2001:67c:2084:165::1bb0", "2001:67c:2084:205::4842", "2001:67c:2084:205::7676", "2001:67c:2084:205::88b2", "2001:67c:2084:400::419a",
            "2001:67c:2084:601::6990", "2001:67c:2084:607::6c69", "2001:67c:2084:611::7d4d", "2001:67c:2084:7f4:591f:6085:69a1:cb0b",
            "2001:67c:2084:803::4701", "2001:67c:2084:831::808c", "2001:67c:2084:836::41d7", "2001:67c:2084:875::8bbe", "2001:67c:2084:87a::dc55",
            "2001:67c:2084:88e::6ccd", "2001:67c:2084:8a6::dc98", "2001:67c:2084:a89:319b:c581:e702:ce83", "2001:67c:2084:ad5:c942:e4a7:c56e:9969",
            "2001:67c:21f0:42ff:8431:c54f:4c81:ad7d", "2001:7d0:82d6:3601:1598:1ed0:bd59:6962", "2001:7e8:c03f:3301:98dc:6e28:ae37:4700",
            "2001:980:a622:1:39e3:5b34:c944:be49", "2001:a61:2139:bd01:5c38:7aeb:b90d:d80d", "2a00:11d8:901:0:5dee:b3f0:798f:320b",
            "2a00:1210:1:9d8::1:1dd5", "2a00:1210:2:206::1:143d", "2a00:1210:2:232::1:6b16", "2a00:1210:2:df2::1:30ef", "2a00:1210:2:e11::1:9ed9",
            "2a00:1210:3:19a::1:3af6", "2a00:1210:3:bbe::1:ceae", "2a00:1370:8101:e6f:5c9:3275:332b:9787", "2a00:1370:8101:f715::3",
            "2a00:1370:8109:4a25:50de:baf8:68c8:151c", "2a00:1370:8109:4a25:59f7:b0be:4e4f:ab93", "2a00:1390:6:303f:3d2a:f79d:730f:8554",
            "2a00:1390:7:1f29:ce1:67f5:25b1:c0da", "2a00:1838:aaaa:2007:e1d4:8e7:be96:c81d", "2a00:1838:aaaa:2037:d2c:8a96:d37c:c392",
            "2a00:1838:aaaa:2109:59f2:737c:fe7f:f4a6", "2a00:1838:aaaa:2115:d458:1455:37ce:9799", "2a00:1838:aaaa:2137:fc6e:3c22:d6a6:a020",
            "2a00:1838:bbbb:2600:f87c:dddd:fa50:8915", "2a00:1838:bbbb:3f00:3114:2f63:8456:7590", "2a00:1838:bbbb:9d00:d483:dac:b5f6:38b5",
            "2a00:1838:bbbb:cc00:6560:8ecf:c02a:4622", "2a00:1838:bbbb:e300:f1e5:8a6e:6201:a6e7", "2a00:44a0:1:1fff:910b:e7d7:7db2:6c61",
            "2a00:44a0:1:3000::b3a", "2a00:44a0:1:4000::984", "2a00:44a0:1:4fed:7d8a:eda1:c299:db6", "2a00:44a0:1:4ffc:71d3:4c92:7f93:6d53",
            "2a00:44a0:1:4fff:d199:6b71:2b9a:e5e5", "2a00:44a0:1:7000::1e2", "2a00:44a0:1:7000::bc5", "2a00:44a0:1:7000::c85", "2a00:44a0:1:7000::f",
            "2a00:44a0:1:7fe8:f4a9:2aa7:db3:8eaa", "2a00:44a0:1:7ff4:9cd4:6dc0:aec4:af72", "2a00:44a0:1:8000::80e", "2a00:44a0:1:8000::cd6",
            "2a00:44a0:1:8ff7:18a1:c4ae:a8e:a1b8", "2a00:44a0:1:9000::ea2", "2a00:44a0:1:9fe4:b551:a377:ba00:2a46", "2a00:44a0:1:b000::8dc",
            "2a00:44a0:1:b000::9ee", "2a00:44a0:1:c000::ecb", "2a00:44a0:1:cfe0:e468:c6f2:103e:7cb4", "2a00:44a0:1:cfe8:58dd:445b:7a4d:3be7",
            "2a00:44a0:1:cfee:f1af:40a7:2b18:ee18", "2a00:44a0:1:d000::b74", "2a00:44a0:1:eff3:350b:e5c3:8955:994f", "2a00:44a0:1:efff:202e:cfb:2686:a768",
            "::ffff:1.0.227.31", "::ffff:1.0.248.9", "::ffff:1.0.255.81", "::ffff:1.1.171.247", "::ffff:1.1.254.41", "::ffff:1.170.24.175",
            "::ffff:1.175.242.97", "::ffff:1.179.143.178", "::ffff:1.179.201.18", "::ffff:1.186.65.230", "::ffff:1.192.90.37", "::ffff:1.205.206.170",
            "::ffff:1.234.45.50", "::ffff:1.39.12.129", "::ffff:1.39.33.229", "::ffff:1.46.101.100", "::ffff:1.46.35.10", "::ffff:1.47.129.57",
            "::ffff:1.52.111.234", "::ffff:1.53.55.50", "::ffff:100.38.163.202", "::ffff:101.108.13.92", "::ffff:101.108.154.113", "::ffff:101.109.79.211",
            "::ffff:101.18.161.193", "::ffff:101.230.198.133", "::ffff:101.51.13.60", "::ffff:101.51.208.196", "::ffff:101.62.254.160",
            "::ffff:101.99.7.195", "::ffff:103.10.197.130", "::ffff:103.10.197.154", "::ffff:103.10.197.194", "::ffff:103.10.197.202",
            "::ffff:103.10.197.211", "::ffff:103.10.197.59", "::ffff:103.10.197.62", "::ffff:103.10.199.35", "::ffff:103.10.230.143",
            "::ffff:103.11.116.46", "::ffff:103.14.125.195", "::ffff:103.14.45.134", "::ffff:103.23.103.234", "::ffff:103.23.21.239",
            "::ffff:103.233.116.114", "::ffff:103.253.147.77", "::ffff:103.31.178.114", "::ffff:103.31.45.87", "::ffff:103.36.35.106", "::ffff:103.54.148.2",
            "::ffff:103.54.219.18", "::ffff:103.7.249.84", "::ffff:104.128.202.84", "::ffff:104.131.194.174", "::ffff:104.131.66.8", "::ffff:104.131.75.86",
            "::ffff:104.131.89.67", "::ffff:104.131.98.30", "::ffff:104.139.71.60", "::ffff:104.143.15.10", "::ffff:104.152.25.220",
            "::ffff:104.156.228.113", "::ffff:104.156.228.173", "::ffff:104.156.253.78", "::ffff:104.201.15.220", "::ffff:104.207.159.41"
    };
}
