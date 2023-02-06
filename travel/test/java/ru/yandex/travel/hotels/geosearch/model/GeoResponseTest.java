package ru.yandex.travel.hotels.geosearch.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.travel.hotels.geosearch.GeoSearchParser;

import static org.assertj.core.api.Assertions.assertThat;

public class GeoResponseTest {
    private static String responseText;

    @BeforeClass
    public static void loadResponse() throws IOException {
        responseText = Files.readString(Path.of(GeoResponseTest.class.getClassLoader().getResource("geo-response.pb.txt").getPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void testResponseDeserializationTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        GeoSearchParser parser = new GeoSearchParser(mapper);
        GeoSearchRsp gsr = parser.parseTextProtoResponse(responseText);
        assertThat(gsr.getOfferCacheResponseMetadata().getIsFinished()).isTrue();
        assertThat(gsr.getOfferCacheResponseMetadata().getWasFound()).isTrue();
        assertThat(gsr.getHotels()).hasSize(1);
        GeoHotel h = gsr.getHotels().get(0);
        assertThat(h.getOfferCacheResponse().getPricesCount()).isEqualTo(5);
        assertThat(h.getGeoObjectMetadata().getAddress().getFormattedAddress()).isEqualTo("Россия, Москва, площадь Европы, 2");
        assertThat(h.getRating().getScore()).isEqualTo(9.4f);
        assertThat(h.getNumStars()).isEqualTo(4);
        assertThat(h.getPhotos().getPhotoCount()).isEqualTo(11);
        assertThat(h.getLegalInfo().getInn()).isEqualTo("7730001183");
        assertThat(h.getLegalInfo().getName()).isEqualTo("ООО \"СЛАВЯНСКАЯ\"");
        assertThat(h.getLegalInfo().getAddress()).isEqualTo("121059 МОСКВА ГОРОД ПЛОЩАДЬ ЕВРОПЫ 2");

        assertThat(h.getSpravPhotos()).hasSize(56);
        GeoHotelPhoto p = h.getSpravPhotos().get(0);
        assertThat(p.getBase().getId()).isEqualTo("urn:yandex:sprav:photo:885986");
        assertThat(p.getPhoto().getUrlTemplate()).isEqualTo("https://avatars.mds.yandex.net/get-altay/374295/2a0000015b1dade88e53c818ef0951922ba0/%s");

        assertThat(h.getUgcFeatures().getFeatureList()).hasSize(6);
        GeoHotelUgcFeatures.Feature f = h.getUgcFeatures().getFeatureList().get(0);
        assertThat(f.getFeatureId()).isEqualTo("sauna");
        assertThat(f.getFeatureName()).isEqualTo("сауна");
        assertThat(f.getStat().getPositive()).isEqualTo(11);
        assertThat(f.getStat().getNegative()).isEqualTo(10);
        assertThat(f.getStat().getTotal()).isEqualTo(35);

        assertThat(h.getSimilarHotels()).hasSize(10);
        GeoSimilarHotel sh = h.getSimilarHotels().get(0);
        assertThat(sh.getPermalink().toString()).isEqualTo("210656549210");
        assertThat(sh.getPlaceInfo().getName()).isEqualTo("Ibis Москва Киевская");
        GeoSimilarHotel.Extension shExt = sh.getExtension();
        assertThat(shExt.getStars()).isEqualTo(3);
        assertThat(shExt.getReviewCount()).isEqualTo(197);
        assertThat(shExt.getFeatures()).hasSize(6);
        assertThat(shExt.getFeatures().get(0).getId()).isEqualTo("air_conditioning");
        assertThat(shExt.getFeatures().get(0).getBoolValue()).isEqualTo(true);
        assertThat(shExt.getFeatures().get(3).getId()).isEqualTo("room_number");
        assertThat(shExt.getFeatures().get(3).getStringValue()).isEqualTo("350");
        assertThat(shExt.getFeatures().get(5).getId()).isEqualTo("star");
        assertThat(shExt.getFeatures().get(5).getEnumValues()).hasSize(1);
        assertThat(shExt.getFeatures().get(5).getEnumValues().get(0)).isEqualTo("3 звезды");
        assertThat(sh.getOfferCacheResponse().getPrices(0).getOfferId()).isEqualTo("7608e085-2655-4c82-891b-842cdda1f598");

        assertThat(h.getFeatureGroups()).hasSize(9);
        GeoHotelFeatureGroup fg = h.getFeatureGroups().get(4);
        assertThat(fg.getId()).isEqualTo("general_information");
        assertThat(fg.getName()).isEqualTo("Общая информация об отеле");
        assertThat(fg.getFeatureIds()).hasSize(7);
        assertThat(fg.getFeatureIds().get(3)).isEqualTo("year_of_foundation");

        assertThat(h.getCategoryIds()).hasSize(3);
        assertThat(h.getCategoryIds().get(0)).isEqualTo("184106174");
        assertThat(h.getCategoryIds().get(1)).isEqualTo("184105738");
        assertThat(h.getCategoryIds().get(2)).isEqualTo("184105744");
    }
}
