package ru.yandex.direct.jobs.warnclientdomains;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BidGroup;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.client.model.ClientDomainStripped;
import ru.yandex.direct.core.entity.client.repository.ClientDomainsStrippedRepository;
import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.entity.notification.container.NewDomainMailNotification;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.rbac.ClientPerminfo;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.rbac.RbacService;

import static com.google.common.collect.Sets.newHashSet;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@JobsTest
@ExtendWith(SpringExtension.class)
class JobSendWarnClientDomainsTest {
    private static final int SHARD = 1;

    private static final LocalDateTime DATE = LocalDateTime.now().truncatedTo(SECONDS);
    private static final Long MAX_RECORD_ID = 13L;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private JobSendWarnClientDomains job;

    private BannerRelationsRepository bannerRelationsRepository;
    private DomainRepository domainRepository;
    private BidGroup bidGroup;
    private Map<Long, Collection<ClientDomainStripped>> domainsToClient;
    private User userAgency1;

    @BeforeEach
    void init() {
        ClientDomainStripped elem1 = new ClientDomainStripped()
                .withClientId(1L).withDomainId(1L).withRecordId(11L).withLogtime(DATE);
        ClientDomainStripped elem2 = new ClientDomainStripped()
                .withClientId(1L).withDomainId(2L).withRecordId(12L).withLogtime(DATE);
        ClientDomainStripped elem3 = new ClientDomainStripped()
                .withClientId(3L).withDomainId(3L).withRecordId(MAX_RECORD_ID).withLogtime(DATE);

        domainsToClient = ImmutableMap.of(elem1.getClientId(), newHashSet(elem1, elem2),
                elem3.getClientId(), newHashSet(elem3));

        ClientDomainsStrippedRepository clientDomainsStrippedRepository = mock(ClientDomainsStrippedRepository.class);
        when(clientDomainsStrippedRepository.getRecent(any(), any(), any()))
                .thenReturn(newHashSet(elem1, elem2, elem3));

        userAgency1 = new User().withUid(1L).withFio("Ivanov").withOpts("notify_about_new_domains");
        User userAgency2 = new User().withUid(2L).withFio("Petrov").withOpts("");
        UserService userService = mock(UserService.class);
        when(userService.massGetUser(any())).thenReturn(List.of(userAgency1, userAgency2));
        when(userService.getUser(eq(1L))).thenReturn(userAgency1);

        bidGroup = new BidGroup().withClientUserId(1L)
                .withCampaignId(1L).withFio("Smith")
                .withDomain("ya.ru").withBids(Sets.newSet(1L));
        bannerRelationsRepository = mock(BannerRelationsRepository.class);
        domainRepository = mock(DomainRepository.class);
        when(bannerRelationsRepository.getBidGroups(any(), any(), any())).thenReturn(newHashSet(bidGroup));

        PpcRbac ppcRbac = mock(PpcRbac.class);
        ClientPerminfo clientPerminfo1 = mock(ClientPerminfo.class);
        when(clientPerminfo1.agencyUids())
                .thenReturn(Set.of(1L));

        when(ppcRbac.getClientPerminfo(eq(ClientId.fromLong(1L))))
                .thenReturn(Optional.of(clientPerminfo1));

        job = new JobSendWarnClientDomains(SHARD, ppcPropertiesSupport, clientDomainsStrippedRepository,
                domainRepository, bannerRelationsRepository, mock(RbacService.class), ppcRbac, userService,
                mock(NotificationService.class));
    }

    @Test
    void getRecentDomains() {
        prepareProperties(DATE.minusMinutes(30));
        checkProperResult();
    }

    @Test
    void getRecentDomains_NullLogtime() {
        ppcPropertiesSupport.get(job.getPropertyIdName()).set(0L);
        ppcPropertiesSupport.get(job.getPropertyTimeName()).remove();
        checkProperResult();
    }

    @Test
    void getRecentDomains_EarlyTime() {
        prepareProperties(DATE.minusMinutes(10));
        Assertions.assertThat(job.getRecentData(DATE).getDomainsToClient())
                .as("recent domains")
                .isEmpty();
    }

    private void prepareProperties(LocalDateTime logtime) {
        ppcPropertiesSupport.get(job.getPropertyIdName()).set(0L);
        ppcPropertiesSupport.get(job.getPropertyTimeName()).set(logtime);
    }

    private void checkProperResult() {
        JobSendWarnClientDomains.DomainsBundle domainsBundle = job.getRecentData(DATE);
        job.updateProperties(domainsBundle);
        assertEquals(domainsToClient, domainsBundle.getDomainsToClient());
        assertEquals(MAX_RECORD_ID, ppcPropertiesSupport.get(job.getPropertyIdName()).get());
        assertEquals(DATE, ppcPropertiesSupport.get(job.getPropertyTimeName()).get());
        Assertions.assertThat(job.getRecentData(DATE).getDomainsToClient())
                .as("recent domains")
                .isEmpty();
    }

    @Test
    void prepareNotifications() {
        NewDomainMailNotification notification = new NewDomainMailNotification().withAgencyUserId(userAgency1.getUid())
                .withFio(userAgency1.getFio()).withBidGroups(newHashSet(bidGroup));
        assertEquals(singletonList(notification), job.prepareNotifications(ImmutableMap.of(1L, emptySet())));
    }

    @Test
    @TestCaseName("Не отправляем письмо, когда не нашли домен в объявлениях")
    public void prepareNotifications_mismatchedDomain_noMail() {
        when(bannerRelationsRepository.getBidGroups(any(), any(), any())).thenReturn(emptySet());

        String domain1 = "a.yandex.ru";
        String domain2 = "b.yandex.ru";

        when(domainRepository.getDomainsByIdsFromDict(any())).thenReturn(
                List.of(new Domain().withDomain(domain1)
                                .withReverseDomain(new StringBuffer(domain1).reverse().toString()),
                        new Domain().withDomain(domain2).withReverseDomain(new StringBuffer(domain2).reverse().toString())));

        Assertions.assertThat(job.prepareNotifications(ImmutableMap.of(1L, emptySet())))
                .as("prepared notifications")
                .isNotEmpty();
    }
}
