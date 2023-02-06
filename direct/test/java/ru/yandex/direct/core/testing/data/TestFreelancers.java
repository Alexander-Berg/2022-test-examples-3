package ru.yandex.direct.core.testing.data;

import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCard;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificate;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerContacts;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerStatus;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancerCertificateType.DIRECT_PRO;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;

public class TestFreelancers {
    //C 24-го июля 2019-го года тест типа DIRECT пройти можно, но сертификационным тестом он больше не считается
    public static final LocalDateTime DIRECT_TYPE_BEFORE = LocalDateTime.of(2019, 7, 23, 0, 0);
    public static final LocalDateTime DIRECT_TYPE_AFTER = LocalDateTime.of(2019, 7, 25, 0, 0);

    public static final long DEFAULT_AVATAR_ID = 777L;
    public static final long DEFAULT_CERT_ID = 12345L;

    private TestFreelancers() {
    }

    public static Freelancer defaultFreelancer(@Nullable Long clientId) {
        Freelancer freelancer = new Freelancer();
        freelancer.withFreelancerId(clientId)
                .withRating(5.0)
                .withFeedbackCount(1L)
                .withIsSearchable(true)
                .withIsDisabled(false)
                .withFirstName("Эдуард")
                .withSecondName("Сгенеренный")
                .withRegionId(MOSCOW_REGION_ID)
                .withStatus(FreelancerStatus.FREE)
                .withCertificates(singletonList(getDefaultFlCertificate()));
        FreelancerCard freelancerCard = defaultFreelancerCard(clientId);
        freelancer.withCard(freelancerCard);
        return freelancer;
    }

    public static FreelancerCertificate getDefaultFlCertificate() {
        return new FreelancerCertificate()
                .withCertId(DEFAULT_CERT_ID)
                .withType(DIRECT_PRO);
    }

    public static FreelancerCard defaultFreelancerCard(@Nullable Long freelancerId) {
        FreelancerContacts defaultContacts = defaultContacts();
        return new FreelancerCard()
                .withFreelancerId(freelancerId)
                .withBriefInfo("Настройка кампаний в Директе")
                .withAvatarId(DEFAULT_AVATAR_ID)
                .withContacts(defaultContacts)
                .withStatusModerate(FreelancersCardStatusModerate.ACCEPTED)
                .withIsArchived(false)
                .withDeclineReason(emptySet());
    }

    private static FreelancerContacts defaultContacts() {
        return new FreelancerContacts()
                .withEmail("ya@ya.ru")
                .withIcq("123456789")
                .withPhone("+7 (495) 739-37-77")
                .withSiteUrl("http://ya.ru")
                .withTelegram("my_telegram_name_4_business")
                .withTown("Moscow")
                .withWhatsApp("+7 (901) 123-45-67")
                .withSkype("my_skype")
                .withViber("+7 (901) 123-45-67");
    }

    public static FreelancerProject defaultFreelancerProject(@Nullable Long clientId, @Nullable Long freelancerId) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        return new FreelancerProject()
                .withId(RandomUtils.nextLong(1, Integer.MAX_VALUE))
                .withClientId(clientId)
                .withFreelancerId(freelancerId)
                .withCreatedTime(now)
                .withUpdatedTime(now)
                .withStatus(FreelancerProjectStatus.NEW);
    }
}
