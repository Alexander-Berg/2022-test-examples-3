package ru.yandex.market.hrms.tms.manager.timex;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.AccessLevel;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Company;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Post;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.WorkingArea;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.service.timex.TimexApiFacadeNew;
import ru.yandex.market.hrms.core.service.timex.client.TimexClient;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "TimexDictionaryManagerTest.before.csv")
public class TimexDictionaryManagerTest extends AbstractTmsTest {

    @Autowired
    @MockBean
    @SpyBean
    private TimexApiFacadeNew timexApiFacadeNew;

    @Autowired
    @MockBean
    private TimexClient timexClient;

    @Autowired
    private TimexDictionarySyncManager timexDictionarySyncManager;

    @Test
    @DbUnitDataSet( after = "TimexDictionaryManagerTest.after.csv")
    public void dictionarieShouldSync() throws IOException {
        mockClock(Instant.parse("2022-06-21T15:00:00Z"));
        when(timexApiFacadeNew.getSession()).thenReturn("");
        when(timexClient.getOrCreateSession()).thenReturn("");
        var companyMock = new Company();
        companyMock.setOid("company1");
        companyMock.setName("ФФЦ Софьино СТАФФ");
        when(timexApiFacadeNew.findCompanyByName("ФФЦ Софьино СТАФФ")).thenReturn(Optional.of(companyMock));

        var accessLevelMock = new AccessLevel();
        accessLevelMock.setOid("accessLevel1");
        accessLevelMock.setName("ФФЦ Софьино");
        when(timexApiFacadeNew.findAccessLevelByName("ФФЦ Софьино")).thenReturn(Optional.of(accessLevelMock));

        var workingAreaMock = new WorkingArea();
        workingAreaMock.setOid("workingArea1");
        workingAreaMock.setName("ФФЦ Софьино. Операционный зал");
        when(timexApiFacadeNew.findWorkingAreaByName("ФФЦ Софьино")).thenReturn(Optional.of(workingAreaMock));

        var outstaffCompanyMock = new Company();
        outstaffCompanyMock.setOid("outstaffCompany1");
        outstaffCompanyMock.setName("ЛайтЛог");
        when(timexApiFacadeNew.findCompanyByName("ЛайтЛог")).thenReturn(Optional.of(outstaffCompanyMock));

        var positionMock = new Post();
        positionMock.setOid("position1");
        positionMock.setName("кладовщик");
        when(timexApiFacadeNew.findPostByName("кладовщик")).thenReturn(Optional.of(positionMock));

        timexDictionarySyncManager.sync();
    }
}
