package ru.yandex.market.agency.program.purchase;

import java.time.LocalDate;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.agency.program.purchase.ArpAgencyPartnerLinkDao;
import ru.yandex.market.core.agency.program.purchase.model.ArpAgencyPartnerLink;
import ru.yandex.market.core.agency.program.purchase.model.OnBoardingRewardType;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgencyPartnerLinkImportServiceTest extends FunctionalTest {

    private static final LocalDate IMPORT_DATE = LocalDate.of(2021, 2, 9);

    @Autowired
    private Yt hahnYt;

    @Mock
    private Cypress cypress;

    @Autowired
    private ArpAgencyPartnerLinkDao agencyPartnerLinkDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EnvironmentService environmentService;


    @BeforeEach
    void initYt() {
        when(hahnYt.cypress()).thenReturn(cypress);
    }


    @Test
    @DbUnitDataSet(
            before = "PurchaseProgramImportLinks.before.csv",
            after = "PurchaseProgramImportLinks.after.csv"
    )
    void importLinks() {
        AgencyPartnerLinkYtDao ytDaoMock = mock(AgencyPartnerLinkYtDao.class);

        when(ytDaoMock.importAgencyPartnersLinks(any(LocalDate.class)))
                .thenReturn(getLinks());

        AgencyPartnerLinkImportService agencyPartnerLinkImportService = new AgencyPartnerLinkImportService(
                agencyPartnerLinkDao,
                ytDaoMock,
                transactionTemplate
        );


        agencyPartnerLinkImportService.process(IMPORT_DATE);

        verify(ytDaoMock, Mockito.times(1))
                .importAgencyPartnersLinks(eq(IMPORT_DATE.withDayOfMonth(1)));
        verifyNoMoreInteractions(ytDaoMock);
    }

    private static List<ArpAgencyPartnerLink> getLinks() {
        return List.of(
                new ArpAgencyPartnerLink(
                        991438,
                        557208,
                        LocalDate.of(2021, 2, 9),
                        OnBoardingRewardType.PARTIAL,
                        false),
                new ArpAgencyPartnerLink(
                        1192823,
                        977675,
                        LocalDate.of(2021, 2, 2),
                        OnBoardingRewardType.FULL,
                        false),
                new ArpAgencyPartnerLink(
                        1192823,
                        977671,
                        LocalDate.of(2021, 2, 15),
                        OnBoardingRewardType.NONE,
                        false),
                new ArpAgencyPartnerLink(
                        87442204,
                        996156,
                        LocalDate.of(2021, 2, 11),
                        OnBoardingRewardType.FULL,
                        false)
        );
    }
}
