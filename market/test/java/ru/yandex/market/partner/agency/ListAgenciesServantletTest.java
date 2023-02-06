package ru.yandex.market.partner.agency;

import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.FullClientInfo;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для {@link ListAgenciesServantlet}.
 */
public class ListAgenciesServantletTest extends FunctionalTest {

    @Autowired
    @Qualifier("patientBalanceService")
    private BalanceService balanceService;

    @DisplayName("Список агентств")
    @Test
    @DbUnitDataSet(before = "ListAgenciesServantletTest.testListAgencies.before.csv")
    void testListAgencies() {
        FullClientInfo fullClientInfo = new FullClientInfo(1L, ClientType.OOO, "AgencyClient1",
                "test@mail.com", "+79801111111", "+79801111111", "url.com",
                true, 1);
        when(balanceService.getFullClients(any())).thenReturn(List.of(fullClientInfo));
        String response =
                FunctionalTestHelper.get(baseUrl + "/listAgencies").getBody();
        assertResponse(response, "ListAgenciesServantletTest.testListAgencies.xml");
    }

    private void assertResponse(String actualContent, String expectedXmlFile) {
        final String expectedContent = FunctionalTestHelper.getResource(getClass(), expectedXmlFile);

        MatcherAssert.assertThat(actualContent, MbiMatchers.xmlEquals(
                expectedContent, new DefaultNodeMatcher(ElementSelectors.Default),
                ImmutableSet.of("servant", "version", "host", "executing-time", "actions")));
    }
}
