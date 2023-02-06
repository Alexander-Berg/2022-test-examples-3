package ru.yandex.direct.core.entity.freelancer.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificate;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerContacts;
import ru.yandex.direct.dbschema.ppc.enums.FreelancersIsSearchable;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@RunWith(JUnitParamsRunner.class)
public class FreelancerMappingsTest {

    private static final String EMPTY_JSON_LIST = "[]";

    @SuppressWarnings("unused")
    private static Collection<Object[]> allIsSearchableEnumValues() {
        return stream(FreelancersIsSearchable.values()).map(value -> new Object[]{value}).collect(toList());
    }

    @Test
    public void contactsFromJson_null_onNull() {
        assertThat(FreelancerMappings.contactsFromJson(null)).isNull();
    }

    @Test
    public void contactsFromJson_null_onEmpty() {
        String empty = "";
        assertThat(FreelancerMappings.contactsFromJson(empty)).isNull();
    }

    @Test
    public void contactsFromJson_success() {
        FreelancerContacts actual = FreelancerMappings.contactsFromJson(
                "{\"phone\":\"+123456\",\"email\":\"ya@ya.ru\",\"icq\":\"123456\",\"siteUrl\":\"http://ya.ru\",\"telegram\":\"telegram\",\"whatsApp\":\"whatsApp\",\"skype\":\"skype\",\"viber\":\"+123456\",\"town\":\"town\"}");
        FreelancerContacts expected = new FreelancerContacts()
                .withEmail("ya@ya.ru")
                .withIcq("123456")
                .withPhone("+123456")
                .withSiteUrl("http://ya.ru")
                .withTelegram("telegram")
                .withSkype("skype")
                .withViber("+123456")
                .withTown("town")
                .withWhatsApp("whatsApp");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void contactsToJson_null_onNull() {
        assertThat(FreelancerMappings.contactsToJson(null)).isNull();
    }

    @Test
    public void contactsToJson_success() {
        FreelancerContacts contacts = new FreelancerContacts()
                .withEmail("ya@ya.ru")
                .withIcq("123456")
                .withPhone("+123456")
                .withSiteUrl("http://ya.ru")
                .withTelegram("telegram")
                .withSkype("skype")
                .withViber("+123456")
                .withTown("town")
                .withWhatsApp("whatsApp");
        String actual = FreelancerMappings.contactsToJson(contacts);
        String expected =
                "{\"phone\":\"+123456\",\"email\":\"ya@ya.ru\",\"icq\":\"123456\",\"siteUrl\":\"http://ya.ru\",\"telegram\":\"telegram\",\"whatsApp\":\"whatsApp\",\"skype\":\"skype\",\"viber\":\"+123456\",\"town\":\"town\"}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void certificatesFromJson_empty_onNull() {
        assertThat(FreelancerMappings.certificatesFromJson(null)).isEmpty();
    }

    @Test
    public void certificatesFromJson_empty_onEmptyJsonList() {
        assertThat(FreelancerMappings.certificatesFromJson(EMPTY_JSON_LIST)).isEmpty();
    }

    @Test
    public void certificatesFromJson_success_onManyItems() {
        String json = "[" +
                "{\"certId\":1,\"type\":\"DIRECT\"}"
                + "," +
                "{\"certId\":1,\"type\":\"DIRECT\"}"
                + "," +
                "{\"certId\":1,\"type\":\"DIRECT\"}"
                +
                "]";
        List<FreelancerCertificate> actual = FreelancerMappings.certificatesFromJson(json);
        FreelancerCertificate certificate = new FreelancerCertificate()
                .withCertId(1L)
                .withType(FreelancerCertificateType.DIRECT);
        assertThat(actual).isEqualTo(asList(certificate, certificate, certificate));
    }

    @Test
    public void certificatesToJson_Null_onNull() {
        assertThat(FreelancerMappings.certificatesToJson(null)).isEqualTo(null);
    }

    @Test
    public void certificatesToJson_emptyListJson_onEmpty() {
        List<FreelancerCertificate> empty = emptyList();
        assertThat(FreelancerMappings.certificatesToJson(empty)).isEqualTo(EMPTY_JSON_LIST);
    }

    @Test
    public void certificateToJson_success_onSingleItem() {
        FreelancerCertificate certificate = new FreelancerCertificate()
                .withCertId(1L)
                .withType(FreelancerCertificateType.DIRECT);
        String actual = FreelancerMappings.certificatesToJson(singletonList(certificate));
        assertThat(actual).isEqualTo("[{\"certId\":1,\"type\":\"DIRECT\"}]");
    }

    @Test
    public void certificatesToJson_success_onManyItems() {
        FreelancerCertificate certificate = new FreelancerCertificate()
                .withCertId(1L)
                .withType(FreelancerCertificateType.DIRECT);

        String actual = FreelancerMappings.certificatesToJson(asList(certificate, certificate, certificate));

        String expected = "[" +
                "{\"certId\":1,\"type\":\"DIRECT\"}"
                + "," +
                "{\"certId\":1,\"type\":\"DIRECT\"}"
                + "," +
                "{\"certId\":1,\"type\":\"DIRECT\"}"
                +
                "]";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void ratingToDb_null_onNull() {
        assertThat(FreelancerMappings.ratingToDb(null)).isNull();
    }

    @Test
    public void ratingToDb_success() {
        BigDecimal actual = FreelancerMappings.ratingToDb(10.0);
        BigDecimal expected = BigDecimal.TEN;
        assertThat(actual).isEqualByComparingTo(expected);
    }

    @Test
    public void ratingFromDb_null_onNull() {
        assertThat(FreelancerMappings.ratingFromDb(null)).isNull();
    }

    @Test
    public void ratingFromDb_success() {
        Double actual = FreelancerMappings.ratingFromDb(BigDecimal.TEN);
        double expected = 10.0;
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void ratingFromDb_null_onZeroValue() {
        Double actual = FreelancerMappings.ratingFromDb(BigDecimal.valueOf(0.0));
        assertThat(actual).isNull();
    }

    @Test
    public void searchableFromDb_null_onNull() {
        assertThat(FreelancerMappings.searchableFromDb(null)).isNull();
    }

    @Test
    @Parameters(value = {
            "Yes, true",
            "No, false"
    })
    public void searchableFromDb_success(String enumName, String expectedBooleanText) {
        FreelancersIsSearchable enumValue = FreelancersIsSearchable.valueOf(enumName);
        boolean expected = Boolean.parseBoolean(expectedBooleanText);
        Boolean actual = FreelancerMappings.searchableFromDb(enumValue);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @Parameters(method = "allIsSearchableEnumValues")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void searchableFromDb_success_onAllEnumValues(FreelancersIsSearchable enumValue) {
        assertThatCode(() -> FreelancerMappings.searchableFromDb(enumValue))
                .doesNotThrowAnyException();
    }

    @Test
    public void searchableToDb_null_onNull() {
        assertThat(FreelancerMappings.searchableToDb(null)).isNull();
    }

    @Test
    @Parameters(value = {
            "Yes, true",
            "No, false"
    })
    public void searchableToDb_success(String expectedEnum, String searchableString) {
        FreelancersIsSearchable actual =
                FreelancerMappings.searchableToDb(Boolean.parseBoolean(searchableString));

        //noinspection ConstantConditions
        assertThat(actual.getLiteral()).isEqualTo(expectedEnum);
    }
}
