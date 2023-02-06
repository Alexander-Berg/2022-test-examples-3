package ru.yandex.market.checkout.util.personal;

import javax.annotation.Nullable;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersGps;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.allRequests;

@TestComponent
public class PersonalMockConfigurer {

    @Autowired
    private WireMockServer personalMock;

    public void mockV1PhonesRetrieve() {
        personalMock.givenThat(post(urlPathEqualTo("/v1/phones/retrieve"))
                .withRequestBody(matchingJsonPath("$.id", equalTo("c0dec0dedec0dec0dec0dec0dedec0de")))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody("{\"value\":\"+71234567891\"}")
                        .withHeader("Content-Type", "application/json")
                )
        );
        personalMock.givenThat(post(urlPathEqualTo("/v1/phones/retrieve"))
                .withRequestBody(matchingJsonPath("$.id", equalTo("0123456789abcdef0123456789abcdef")))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody("{\"value\":\"+74952234562\"}")
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    public void mockV1PhonesRetrieveNotFound() {
        personalMock.givenThat(post(urlPathEqualTo("/v1/phones/retrieve"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withStatus(404)
                        .withBody("{\"code\":\"404\",\"message\":\"No document with such id\"}")
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    public void mockV1EmailsRetrieve() {
        personalMock.givenThat(post(urlPathEqualTo("/v1/emails/retrieve"))
                .withRequestBody(matchingJsonPath("$.id", equalTo("51e7897da4fa5ec326206b1908fbc43d")))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody("{\"value\":\"asd2@gmail.com\"}")
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    public void mockV1EmailsRetrieveNotFound() {
        personalMock.givenThat(post(urlPathEqualTo("/v1/emails/retrieve"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withStatus(404)
                        .withBody("{\"code\":\"404\",\"message\":\"No document with such id\"}")
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    public void mockV1MultiTypesRetrieveAddressAndGps() {
        PersAddress persAddress = new PersAddress();
        persAddress.setCountry("Russia");
        persAddress.setPostcode("347660");
        persAddress.setCity("Moscow");
        persAddress.setDistrict("left");
        persAddress.setSubway("13");
        persAddress.setStreet("Vavilova");
        persAddress.setKm("35");
        persAddress.setHouse("3/6");
        persAddress.setBuilding("9");
        persAddress.setEstate("3");
        persAddress.setBlock("2");
        persAddress.setEntrance("4");
        persAddress.setEntryPhone("331");
        persAddress.setFloor("8");
        persAddress.setApartment("43");

        PersGps persGps = new PersGps()
                .longitude("67.8759")
                .latitude("123.45");

        mockV1MultiTypesRetrieveAddressAndGps(
                "34251639gwbcesaqq239098jhcdxe453",
                persAddress,
                "k4bv310ccd62ndkgwe3w3c56lgh78923",
                persGps);
    }

    public void mockV1MultiTypesRetrieveAddressAndEmptyGps() {
        PersAddress persAddress = new PersAddress();
        persAddress.setCountry("Russia");
        persAddress.setPostcode("347660");
        persAddress.setCity("Moscow");
        persAddress.setDistrict("left");
        persAddress.setSubway("13");
        persAddress.setStreet("Vavilova");
        persAddress.setKm("35");
        persAddress.setHouse("3/6");
        persAddress.setBuilding("9");
        persAddress.setEstate("3");
        persAddress.setBlock("2");
        persAddress.setEntrance("4");
        persAddress.setEntryPhone("331");
        persAddress.setFloor("8");
        persAddress.setApartment("43");

        mockV1MultiTypesRetrieveAddressAndGps(
                "34251639gwbcesaqq239098jhcdxe453",
                persAddress,
                null,
                null);
    }

    public void mockV1MultiTypesRetrieveAddressAndGps(String addressId,
                                                      PersAddress persAddress,
                                                      String gpsId,
                                                      @Nullable PersGps persGps) {
        var body = "{\"items\":[\n" +
                "{\"id\":\"0123456789abcdef0123456789abcdef\"," +
                "\"type\":\"phone\"," +
                "\"value\":{\"phone\":\"+71234567891\"}},\n" +
                "{\"id\":\"4621897c54fd9ef81e33c0502bd6ab7a\"," +
                "\"type\":\"email\"," +
                "\"value\":{\"email\":\"a@b.com\"}},\n" +
                "{\"id\":\"81e33d098f095f67b1622ccde7a4a5b4\"," +
                "\"type\":\"full_name\"," +
                "\"value\":{\"full_name\":{\"forename\":\"Leo\",\"surname\":\"Tolstoy\"}}},\n" +
                "{\"id\":\"" + addressId + "\"," +
                "\"type\":\"address\"," +
                "\"value\":{\"address\":" +
                "{\"country\":\"" + persAddress.getCountry() + "\"," +
                "\"postcode\":\"" + persAddress.getPostcode() + "\"," +
                "\"city\":\"" + persAddress.getCity() + "\"," +
                "\"district\":\"" + persAddress.getDistrict() + "\"," +
                "\"subway\":\"" + persAddress.getSubway() + "\"," +
                "\"street\":\"" + persAddress.getStreet() + "\"," +
                "\"km\":\"" + persAddress.getKm() + "\"," +
                "\"house\":\"" + persAddress.getHouse() + "\"," +
                "\"building\":\"" + persAddress.getBuilding() + "\"," +
                "\"estate\":\"" + persAddress.getEstate() + "\"," +
                "\"block\":\"" + persAddress.getBlock() + "\"," +
                "\"entrance\":\"" + persAddress.getEntrance() + "\"," +
                "\"entryPhone\":\"" + persAddress.getEntryPhone() + "\"," +
                "\"floor\":\"" + persAddress.getFloor() + "\"," +
                "\"apartment\":\"" + persAddress.getApartment() + "\"}}}" +
                (persGps == null ? "" :
                        ",{\"id\":\"" + gpsId + "\"," +
                                "\"type\":\"gps\"," +
                                "\"value\":{\"gps\":{" +
                                "\"longitude\":\"" + persGps.getLongitude() + "\"," +
                                "\"latitude\":\"" + persGps.getLatitude() + "\"}}}\n") +
                "]}";

        personalMock.givenThat(post(urlPathEqualTo("/v1/multi_types/retrieve"))
                .withRequestBody(containing("k4bv310ccd62ndkgwe3w3c56lgh78923"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody(body)
                        .withHeader("Content-Type", "application/json")
                )
        );

        personalMock.givenThat(post(urlPathEqualTo("/v1/multi_types/store"))
                .withRequestBody(containing("k4bv310ccd62ndkgwe3w3c56lgh78923"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody(body)
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    public void mockV1MultiTypesRetrieve() {
        personalMock.givenThat(post(urlPathEqualTo("/v1/multi_types/retrieve"))
                .withRequestBody(containing("0123456789abcdef0123456789abcdef"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody("{\"items\":[\n" +
                                "{\"id\":\"0123456789abcdef0123456789abcdef\"," +
                                "\"type\":\"phone\"," +
                                "\"value\":{\"phone\":\"+71234567891\"}},\n" +
                                "{\"id\":\"4621897c54fd9ef81e33c0502bd6ab7a\"," +
                                "\"type\":\"email\"," +
                                "\"value\":{\"email\":\"a@b.com\"}},\n" +
                                "{\"id\":\"81e33d098f095f67b1622ccde7a4a5b4\"," +
                                "\"type\":\"full_name\"," +
                                "\"value\":{\"full_name\":{\"forename\":\"Leo\",\"surname\":\"Tolstoy\"}}},\n" +
                                "{\"id\":\"fgdfg43fc343x23w2c5345w3\"," +
                                "\"type\":\"address\"," +
                                "\"value\":{\"address\":{\"country\":\"Russia\",\"postcode\":\"347660\"," +
                                "\"city\":\"Moscow\",\"district\":\"left\",\"subway\":\"13\",\"street\":\"Vavilova\"," +
                                "\"km\":\"35\",\"house\":\"3/6\",\"building\":\"9\",\"estate\":\"3\",\"block\":\"2\"," +
                                "\"entrance\":\"4\",\"entryPhone\":\"331\",\"floor\":\"8\",\"apartment\":\"43\"}}},\n" +
                                "{\"id\":\"mgflk5jng5erfnjerfmk3n4\"," +
                                "\"type\":\"gps\"," +
                                "\"value\":{\"gps\":{\"latitude\":\"123.45\",\"longitude\":\"67.8759\"}}}\n" +
                                "]}")
                        .withHeader("Content-Type", "application/json")
                )
        );

        personalMock.givenThat(post(urlPathEqualTo("/v1/multi_types/retrieve"))
                .withRequestBody(containing("c0dec0dedec0dec0dec0dec0dedec0qd"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody("{\"items\":[\n" +
                                "{\"id\":\"c0dec0dedec0dec0dec0dec0dedec0qd\"," +
                                "\"type\":\"phone\"," +
                                "\"value\":{\"phone\":\"+71234567891\"}},\n" +
                                "{\"id\":\"909701f1b46bdd8a4cafe2ec68255373\"," +
                                "\"type\":\"email\"," +
                                "\"value\":{\"email\":\"a@b.com\"}},\n" +
                                "{\"id\":\"cd368e42341ff6ca7bbd12a05998e705\"," +
                                "\"type\":\"full_name\"," +
                                "\"value\":{\"full_name\":{\"forename\":\"Leo321\",\"surname\":\"Tolstoy123\"}}}\n" +
                                "]}")
                        .withHeader("Content-Type", "application/json")
                )
        );

        personalMock.givenThat(post(urlPathEqualTo("/v1/multi_types/retrieve"))
                .withRequestBody(containing("c0dec0dedec0dec0dec0dec0dedec0de"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody("{\"items\":[\n" +
                                "{\"id\":\"c0dec0dedec0dec0dec0dec0dedec0de\"," +
                                "\"type\":\"phone\"," +
                                "\"value\":{\"phone\":\"+71234567891\"}},\n" +
                                "{\"id\":\"9e92bc743c624f958b8876c7841a653b\"," +
                                "\"type\":\"email\"," +
                                "\"value\":{\"email\":\"a@b.com\"}},\n" +
                                "{\"id\":\"a1c595eb35404207aecfa080f90a8986\"," +
                                "\"type\":\"full_name\"," +
                                "\"value\":{\"full_name\":{\"forename\":\"Leo321\",\"surname\":\"Tolstoy123\"}}}\n" +
                                "]}")
                        .withHeader("Content-Type", "application/json")
                )
        );

        personalMock.givenThat(post(urlPathEqualTo("/v1/multi_types/retrieve"))
                .withRequestBody(containing("jknsvj4-kjefckj3234b-kbb4"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody("{\"items\":[\n" +
                                "{\"id\":\"k6k6j3b3hv2c1ch4j5vj4b5kj\"," +
                                "\"type\":\"phone\"," +
                                "\"value\":{\"phone\":\"+71234567891\"}},\n" +
                                "{\"id\":\"h5b3b3b2v4b6k8l2jb2cg1\"," +
                                "\"type\":\"email\"," +
                                "\"value\":{\"email\":\"a@b.com\"}},\n" +
                                "{\"id\":\"jknsvj4-kjefckj3234b-kbb4\"," +
                                "\"type\":\"full_name\"," +
                                "\"value\":{\"full_name\":" +
                                "{\"forename\":\"fName1\",\"patronymic\":\"sName1\",\"surname\":\"tName1\"}}}\n" +
                                "]}")
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    public void mockV1MultiTypesStore() {
        personalMock.givenThat(post(urlPathEqualTo("/v1/multi_types/store"))
                .withRequestBody(containing("+71234567891"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody("{\"items\":[\n" +
                                "{\"id\":\"0123456789abcdef0123456789abcdef\"," +
                                "\"value\":{\"phone\":\"+71234567891\"}},\n" +
                                "{\"id\":\"4621897c54fd9ef81e33c0502bd6ab7a\"," +
                                "\"value\":{\"email\":\"a@b.com\"}},\n" +
                                "{\"id\":\"81e33d098f095f67b1622ccde7a4a5b4\"," +
                                "\"value\":{\"full_name\":{\"forename\":\"Leo\",\"surname\":\"Tolstoy\"}}},\n" +
                                "{\"id\":\"fgdfg43fc343x23w2c5345w\"," +
                                "\"value\":{\"address\":{\"country\":\"Russia\",\"street\":\"Vavilova\"}}},\n" +
                                "{\"id\":\"mgflk5jng5erfnjerfmk3n\"," +
                                "\"value\":{\"gps\":{\"latitude\":\"123.45\",\"longitude\":\"67.89\"}}}\n" +
                                "]}")
                        .withHeader("Content-Type", "application/json")
                )
        );
        personalMock.givenThat(post(urlPathEqualTo("/v1/multi_types/store"))
                .withRequestBody(containing("Недовольный"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody("{\"items\":[\n" +
                                "{\"id\":\"14e3595b076f62fb1cdc84e23a87a09d\"," +
                                "\"value\":{\"phone\":\"+79998887766\"}},\n" +
                                "{\"id\":\"c6edd583a6774a2b2b1f5f980e09c341\"," +
                                "\"value\":{\"email\":\"user@example.com\"}},\n" +
                                "{\"id\":\"94108c324cfba2f0e676e587a3b5dd91\"," +
                                "\"value\":{\"full_name\":{\"forename\":\"Покупатель\",\"surname\":\"Недовольный\"," +
                                "\"patronymic\":\"Совсем\"}}}\n" +
                                "]}")
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    public void mockV1MultiTypesStoreInvalidNumber() {
        personalMock.givenThat(post(urlPathEqualTo("/v1/multi_types/store"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody("{\"items\":[" +
                                "{\"value\":{\"phone\":\"+71234567891\"},\"error\":\"Invalid phone number\"}" +
                                "]}")
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    public void mockV2PhonesBulkStore() {
        personalMock.givenThat(post(urlPathEqualTo("/v2/phones/bulk_store"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody("{\"items\":[" +
                                "{\"value\":\"+71234567891\",\"id\":\"0123456789abcdef0123456789abcdef\"}" +
                                "]}")
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    public void mockV2PhonesBulkStoreInvalidNumber() {
        personalMock.givenThat(post(urlPathEqualTo("/v2/phones/bulk_store"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody("{\"items\":[" +
                                "{\"value\":\"+71234567891\",\"error\":\"Invalid phone number\"}" +
                                "]}")
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    public void mockUnavailable(int status) {
        personalMock.givenThat(any(anyUrl())
                .willReturn(new ResponseDefinitionBuilder()
                        .withStatus(status)
                )
        );
    }

    public void verifyNoRequests() {
        personalMock.verify(0, allRequests());
    }
}
