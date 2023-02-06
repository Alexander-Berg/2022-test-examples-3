package ru.yandex.direct.grid.processing.service.freelancer;

import java.util.Collection;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerBase;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCard;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificate;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerContacts;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerStatus;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancer;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerCard;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerCertificate;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerCertificateType;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerContacts;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerFull;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancerStatus;
import ru.yandex.direct.grid.processing.model.freelancer.GdFreelancersCardStatusModerate;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdUpdateFreelancer;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdUpdateFreelancerCardItem;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdUpdateFreelancerContactsItem;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;

@RunWith(JUnitParamsRunner.class)
public class FreelancerConverterTest {

    private static final String DEFAULT_AVATAR_SIZE_180 =
            "http://avatars.mdst.yandex.net/get-direct-avatars/70126/default/size180";


    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private FreelancerConverter converter;

    @Before
    public void setUp() {
        converter = new FreelancerConverter();
    }

    @Test
    public void convertToGd_success_convertFreelancer() {
        long freelancerId = 1L;
        Freelancer freelancer = new Freelancer();
        freelancer
                .withFreelancerId(freelancerId)
                .withFirstName("Name")
                .withSecondName("Surname")
                .withRegionId(MOSCOW_REGION_ID)
                .withStatus(FreelancerStatus.FREE)
                .withIsSearchable(true)
                .withRating(5.0)
                .withFeedbackCount(1L);
        freelancer.withCard(new FreelancerCard());
        GdFreelancer actual = converter.convertToGd(freelancer, GdFreelancerFull::new, DEFAULT_AVATAR_SIZE_180);
        softly.assertThat(actual.getFreelancerId()).isEqualTo(freelancerId);
        softly.assertThat(actual.getFio()).isEqualTo("Name Surname");
        softly.assertThat(actual.getRegionId()).isEqualTo(MOSCOW_REGION_ID);
        softly.assertThat(actual.getStatus()).isEqualTo(GdFreelancerStatus.FREE);
        softly.assertThat(actual.getRating()).isEqualTo(5);
        softly.assertThat(actual.getCard()).isNotNull();
        softly.assertThat(actual.getFeedbackCount()).isEqualTo(1L);
    }

    @Test
    public void convertFromGd_success_convertUpdateFreelancer() {
        long freelancerId = 1L;
        GdUpdateFreelancer gdFreelancer = new GdUpdateFreelancer()
                .withRegionId(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID);
        FreelancerBase actual = converter.convertFromGd(freelancerId, gdFreelancer);
        softly.assertThat(actual.getFreelancerId()).isEqualTo(freelancerId);
        softly.assertThat(actual.getRegionId()).isEqualTo(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID);
    }

    @Test
    public void convertToGd_success_convertFreelancerCard() {
        FreelancerCard freelancerCard = new FreelancerCard()
                .withAvatarId(777L)
                .withBriefInfo("Brief")
                .withContacts(new FreelancerContacts())
                .withStatusModerate(FreelancersCardStatusModerate.IN_PROGRESS);
        GdFreelancerCard actual = converter.convertToGd(freelancerCard, DEFAULT_AVATAR_SIZE_180, null);
        softly.assertThat(actual.getAvatarUrl()).isEqualTo(DEFAULT_AVATAR_SIZE_180);
        softly.assertThat(actual.getBriefInfo()).isEqualTo("Brief");
        softly.assertThat(actual.getContacts()).isNotNull();
        softly.assertThat(actual.getStatusModerate()).isEqualTo(GdFreelancersCardStatusModerate.IN_PROGRESS);
    }

    @Test
    public void convertFromGd_success_convertUpdateFreelancerCardItem() {
        long freelancerId = 1L;
        FreelancerCard actual = converter.convertFromGd(freelancerId,
                new GdUpdateFreelancerCardItem()
                        .withAvatarId(777L)
                        .withBriefInfo("Brief")
                        .withContacts(new GdUpdateFreelancerContactsItem()));

        FreelancerCard expected = new FreelancerCard()
                .withId(freelancerId)
                .withAvatarId(777L)
                .withBriefInfo("Brief")
                .withContacts(new FreelancerContacts());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void convertCertificatesToGd_empty_onNullContacts() {
        assertThat(converter.convertCertificatesToGd(null, ""))
                .isEmpty();
    }

    @Test
    public void convertCertificatesToGd_success() {
        FreelancerCertificate certificate = new FreelancerCertificate()
                .withCertId(1L)
                .withType(FreelancerCertificateType.DIRECT);

        List<GdFreelancerCertificate> actual =
                converter.convertCertificatesToGd(singletonList(certificate), "secondName");

        assertThat(actual).hasSize(1);
        GdFreelancerCertificate expected = new GdFreelancerCertificate()
                .withCertId(1L)
                .withLink("https://yandex.ru/adv/expert/certificates?certId=1&lastname=secondName")
                .withType(GdFreelancerCertificateType.DIRECT);
        assertThat(actual).first().isEqualToComparingFieldByField(expected);
    }

    @Test
    public void convertContactsToGd_empty_onNullContacts() {
        assertThat(converter.convertContactsToGd(null, null))
                .isEqualTo(new GdFreelancerContacts()
                        .withEmail("")
                        .withPhone(""));
    }

    @Test
    public void convertContactsToGd_success_convertFreelancerContacts() {
        FreelancerContacts contacts = new FreelancerContacts()
                .withTown("Moscow")
                .withTelegram("t.me/tlgrm")
                .withSiteUrl("http://ya.ru")
                .withPhone("+123456")
                .withIcq("123456")
                .withEmail("ya@ya.ru")
                .withWhatsApp("whatsApp");

        GdFreelancerContacts actual = converter.convertContactsToGd(contacts, null);

        GdFreelancerContacts expected = new GdFreelancerContacts()
                .withTelegram("t.me/tlgrm")
                .withSiteUrl("http://ya.ru")
                .withPhone("+123456")
                .withIcq("123456")
                .withEmail("ya@ya.ru")
                .withWhatsApp("whatsApp");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void convertFromGd_success_convertFreelancerContacts() {
        GdUpdateFreelancerContactsItem contacts = new GdUpdateFreelancerContactsItem()
                .withTown("Moscow")
                .withTelegram("t.me/tlgrm")
                .withSiteUrl("http://ya.ru")
                .withPhone("+123456")
                .withIcq("123456")
                .withEmail("ya@ya.ru")
                .withWhatsApp("whatsApp");
        FreelancerContacts actual = converter.convertFromGd(contacts);

        FreelancerContacts expected = new FreelancerContacts()
                .withTown(null)
                .withTelegram("t.me/tlgrm")
                .withSiteUrl("http://ya.ru")
                .withPhone("+123456")
                .withIcq("123456")
                .withEmail("ya@ya.ru")
                .withWhatsApp("whatsApp");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void convertFromGd_null_onNullConvertGdFreelancerContacts() {
        assertThat(converter.convertFromGd((GdFreelancerContacts) null)).isNull();
    }

    @Test
    public void convertFromGd_success_convertGdFreelancerContacts() {
        GdFreelancerContacts contacts = new GdFreelancerContacts()
                .withTown("Moscow")
                .withTelegram("t.me/tlgrm")
                .withSiteUrl("http://ya.ru")
                .withPhone("+123456")
                .withIcq("123456")
                .withEmail("ya@ya.ru")
                .withWhatsApp("whatsApp");
        FreelancerContacts actual = converter.convertFromGd(contacts);

        FreelancerContacts expected = new FreelancerContacts()
                .withTown(null)
                .withTelegram("t.me/tlgrm")
                .withSiteUrl("http://ya.ru")
                .withPhone("+123456")
                .withIcq("123456")
                .withEmail("ya@ya.ru")
                .withWhatsApp("whatsApp");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @Parameters(method = "statuses")
    public void convertToGd_success_convertFreelancerStatus(FreelancerStatus status) {
        assertThatCode(() -> converter.convertToGd(status))
                .doesNotThrowAnyException();
    }

    @Test
    @Parameters(method = "statuses")
    public void convertToGd_nameMatches_convertFreelancerStatus(FreelancerStatus status) {
        GdFreelancerStatus actual = converter.convertToGd(status);
        assertThat(actual.name()).isEqualToIgnoringCase(status.name());
    }

    public static Collection<Object[]> statuses() {
        return stream(FreelancerStatus.values()).map(status -> new Object[]{status}).collect(toList());
    }

}
