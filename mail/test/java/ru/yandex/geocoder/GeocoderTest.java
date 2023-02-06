package ru.yandex.geocoder;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;

public class GeocoderTest {
    private static final String URI = "/yandsearch?lang=ru_RU&ms=pb";
    private static final String URI_TWO_RESULTS =
        URI + "&results=2&origin=test&text=";
    private static final String GEO_SEARCH = "GeoSearch";
    private static final String RUSSIA = "Россия";
    private static final String MOSCOW = "Москва";
    private static final double EPS = 1e-6;

    private static class ServerWithGeocoder extends StaticServer {
        private final SharedConnectingIOReactor reactor;
        private final GeocoderClient geocoder;

        ServerWithGeocoder(final ImmutableGeocoderConfig geocoderConfig)
            throws Exception
        {
            this(Configs.baseConfig(), geocoderConfig);
        }

        ServerWithGeocoder(
            final ImmutableBaseServerConfig config,
            final ImmutableGeocoderConfig geocoderConfig)
            throws Exception
        {
            super(config);
            reactor =
                new SharedConnectingIOReactor(config, Configs.dnsConfig());
            geocoder = new GeocoderClient(reactor, geocoderConfig);
        }

        public GeocoderClient geocoder() {
            return geocoder;
        }

        @Override
        public void start() throws IOException {
            reactor.start();
            geocoder.start();
            super.start();
        }

        @Override
        public void close() throws IOException {
            super.close();
            geocoder.close();
            reactor.close();
        }
    }

    private static ImmutableGeocoderConfig geocoderConfig(
        final int results,
        final int port)

        throws ConfigException, IOException
    {
        IniConfig config = new IniConfig(new StringReader(
            "results = " + results + "\norigin = test"
                + "\nhost = localhost\nport = " + port
                + "\nconnections = 100"));
        return new GeocoderConfigBuilder(config).build();
    }

    //CSOFF: ParameterNumber
    private void assertEquals(
        final GeocoderResult result,
        final int index,
        final double lowerLongitude,
        final double lowerLatitude,
        final double upperLongitude,
        final double upperLatitude)
    {
        Assert.assertEquals(lowerLongitude, result.lowerLongitude(index), EPS);
        Assert.assertEquals(lowerLatitude, result.lowerLatitude(index), EPS);
        Assert.assertEquals(upperLongitude, result.upperLongitude(index), EPS);
        Assert.assertEquals(upperLatitude, result.upperLatitude(index), EPS);
    }
    //CSON: ParameterNumber

    //CSOFF: MagicNumber
    @Test
    public void testMoscow() throws Exception {
        try (StaticServer search =
                 new StaticServer(Configs.baseConfig(GEO_SEARCH));
             ServerWithGeocoder server =
                 new ServerWithGeocoder(geocoderConfig(2, search.port())))
        {
            String request = "moscow,tverskaya";
            search.add(
                URI_TWO_RESULTS + '\"' + request + '\"',
                new File(
                    GeocoderTest.class.getResource("moscow.response").toURI()),
                ContentType.APPLICATION_OCTET_STREAM);
            search.start();
            server.start();

            GeocoderRequest geocoderRequest = new GeocoderRequest(
                geocoderConfig(2, search.port()));
            geocoderRequest.setText(request);
            GeocoderResult res =
                server.geocoder().execute(geocoderRequest).get();
            Assert.assertEquals(2, res.size());
            assertEquals(res, 0, 37.596489, 55.756832, 37.615183, 55.769631);
            assertEquals(res, 1, 37.609326, 55.761376, 37.610871, 55.762247);
            Assert.assertEquals(213, res.getGeoid());
            Assert.assertEquals(RUSSIA, res.getCountryName());
            Assert.assertEquals(2, res.getCities().size());
            Assert.assertEquals(MOSCOW, res.getCities().get(0));
            Assert.assertEquals(MOSCOW, res.getCities().get(1));
        }
    }

    @Test
    public void testWrangel() throws Exception {
        try (StaticServer search =
                 new StaticServer(Configs.baseConfig(GEO_SEARCH));
            ServerWithGeocoder server =
                new ServerWithGeocoder(geocoderConfig(2, search.port())))
        {
            String request = "остров врангеля";
            search.add(
                URI_TWO_RESULTS + '\"' + request + '\"',
                new File(
                    GeocoderTest.class.getResource("wrangel.response").toURI()),
                ContentType.APPLICATION_OCTET_STREAM);
            search.start();
            server.start();

            GeocoderRequest geocoderRequest = new GeocoderRequest(
                geocoderConfig(2, search.port()));
            geocoderRequest.setText(request);
            GeocoderResult res =
                server.geocoder().execute(geocoderRequest).get();
            Assert.assertEquals(3, res.size());
            assertEquals(res, 0, 178.625814, 70.781841, 180.0, 71.587934);
            assertEquals(res, 1, -180.0, 70.781841, -177.434094, 71.587934);
            assertEquals(res, 2, 55.331775, 74.147736, 55.369297, 74.162086);
            Assert.assertEquals(10000, res.getGeoid());
            Assert.assertEquals("", res.getCountryName());
            Assert.assertEquals(0, res.getCities().size());
        }
    }

    @Test
    public void testRussia() throws Exception {
        try (StaticServer search =
                new StaticServer(Configs.baseConfig(GEO_SEARCH));
            ServerWithGeocoder server =
                new ServerWithGeocoder(geocoderConfig(1, search.port())))
        {
            String request = "russia";
            search.add(
                URI + "&results=1&origin=test&text=\"" + request + '\"',
                new File(
                    GeocoderTest.class.getResource("russia.response").toURI()),
                ContentType.APPLICATION_OCTET_STREAM);
            search.start();
            server.start();

            GeocoderRequest geocoderRequest = new GeocoderRequest(
                geocoderConfig(1, search.port()));
            geocoderRequest.setText(request);
            GeocoderResult res =
                server.geocoder().execute(geocoderRequest).get();
            Assert.assertEquals(2, res.size());
            assertEquals(res, 0, 19.484764, 41.185990, 180.0, 81.886117);
            assertEquals(res, 1, -180.0, 41.185990, -168.871997, 81.886117);
            Assert.assertEquals(225, res.getGeoid());
            Assert.assertEquals(RUSSIA, res.getCountryName());
            Assert.assertEquals(0, res.getCities().size());
        }
    }

    @Test
    public void testOrganization() throws Exception {
        try (StaticServer search =
                 new StaticServer(Configs.baseConfig(GEO_SEARCH));
             ServerWithGeocoder server =
                 new ServerWithGeocoder(geocoderConfig(10, search.port())))
        {
            String request = "yandex";
            search.add(
                URI + "&results=10&origin=test&text=\"" + request + '\"',
                new File(
                    GeocoderTest.class.getResource("yandex.response").toURI()),
                ContentType.APPLICATION_OCTET_STREAM);
            search.start();
            server.start();

            GeocoderRequest geocoderRequest = new GeocoderRequest(
                geocoderConfig(10, search.port()));
            geocoderRequest.setText(request);
            GeocoderResult res =
                server.geocoder().execute(geocoderRequest).get();
            Assert.assertEquals(1, res.size());
            assertEquals(res, 0, 37.584038, 55.731526, 37.592249, 55.736159);
            Assert.assertEquals(120542, res.getGeoid());
            Assert.assertEquals(RUSSIA, res.getCountryName());
            Assert.assertEquals(1, res.getCities().size());
            Assert.assertEquals(MOSCOW, res.getCities().get(0));
        }
    }

    @Test
    public void testReverseGeocoding() throws Exception {
        try (StaticServer search =
                 new StaticServer(Configs.baseConfig(GEO_SEARCH));
             ServerWithGeocoder server =
                 new ServerWithGeocoder(geocoderConfig(1, search.port())))
        {
            String request = "37.422053,55.746341";
            search.add(
                URI + "&results=1&origin=test&ll=" + request,
                new File(
                    GeocoderTest.class
                        .getResource("reverse.response").toURI()),
                ContentType.APPLICATION_OCTET_STREAM);
            search.start();
            server.start();

            GeocoderRequest geocoderRequest = new GeocoderRequest(
                geocoderConfig(1, search.port()));
            geocoderRequest.setLl(request);
            GeocoderResult res =
                server.geocoder().execute(geocoderRequest).get();
            Assert.assertEquals(1, res.size());
            assertEquals(res, 0, 37.417859, 55.744161, 37.42607, 55.748792);
            Assert.assertEquals(116998, res.getGeoid());
            Assert.assertEquals(RUSSIA, res.getCountryName());
            Assert.assertEquals(1, res.getCities().size());
            Assert.assertEquals(MOSCOW, res.getCities().get(0));
        }
    }

    @Test
    public void notFoundTest() throws Exception {
        try (StaticServer search =
                 new StaticServer(Configs.baseConfig(GEO_SEARCH));
             ServerWithGeocoder server =
                 new ServerWithGeocoder(geocoderConfig(2, search.port())))
        {
            String request = "lllllllllllllllllll";
            search.add(
                URI_TWO_RESULTS + '\"' + request + '\"',
                new File(
                    GeocoderTest.class
                        .getResource("not_found.response").toURI()),
                ContentType.APPLICATION_OCTET_STREAM);
            search.start();
            server.start();

            GeocoderRequest geocoderRequest = new GeocoderRequest(
                geocoderConfig(2, search.port()));
            geocoderRequest.setText(request);
            GeocoderResult res =
                server.geocoder().execute(geocoderRequest).get();
            Assert.assertEquals(0, res.size());
            Assert.assertEquals(-1, res.getGeoid());
            Assert.assertTrue(res.getCountryName().isEmpty());
            Assert.assertTrue(res.getCities().isEmpty());
        }
    }
    //CSON: MagicNumber
}

