package ru.yandex.market.fulfillment.wrap.marschroute.service.geo.reader;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.model.GeoFile;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.model.GeoInformation;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;

class GeoFileReaderTest extends SoftAssertionSupport {

    private final GeoFileReader geoFileReader = new GeoFileReader();

    @Test
    void testReadingFile() throws Exception {
        GeoFile geoFile = geoFileReader.readFile(ClassLoader.getSystemResourceAsStream("geofile.json"));
        Optional<GeoInformation> geoInformationOptional = geoFile.findInformation(2L);

        softly.assertThat(geoInformationOptional).isPresent();

        softly.assertThat(geoInformationOptional).hasValueSatisfying(geoInformation -> {
            softly.assertThat(geoInformation.getGeoId())
                .as("Asserting geoId value")
                .isEqualTo(2);

            softly.assertThat(geoInformation.getFiasId())
                .as("Asserting fiasId value")
                .isEqualTo("c2deb16a-0330-4f05-821f-1d09c93331e6");

            softly.assertThat(geoInformation.getKladrId())
                .as("Asserting kladrId value")
                .isEqualTo("7800000000000");

            softly.assertThat(geoInformation.getKladrIdMarschroute())
                .as("Asserting kladrIdMarschroute value")
                .isEqualTo("7900000000000");

            softly.assertThat(geoInformation.getType())
                .as("Asserting type value")
                .isEqualTo("locality");

        });

        softly.assertThat(geoFile.size())
            .as("Asserting geo file size value")
            .isEqualTo(1);
    }
}
