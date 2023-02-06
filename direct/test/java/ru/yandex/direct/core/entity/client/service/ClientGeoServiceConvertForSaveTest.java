package ru.yandex.direct.core.entity.client.service;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.adgroup.container.ComplexAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.regions.Region;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(JUnitParamsRunner.class)
public class ClientGeoServiceConvertForSaveTest {
    @Autowired
    private ClientGeoService clientGeoService;
    @Autowired
    private Steps steps;

    private TestContextManager testContextManager = new TestContextManager(ClientGeoServiceConvertForSaveTest.class);

    private ClientId clientId;
    private ComplexAdGroup complexAdGroup;

    private long ru = Region.RUSSIA_REGION_ID;
    private long ua = Region.UKRAINE_REGION_ID;
    private long cr = Region.CRIMEA_REGION_ID;
    private long cis = Region.SNG_REGION_ID;
    private long sf = 146L;
    private long nl = 118L;
    private long w = Region.GLOBAL_REGION_ID;

    private Object[] forSave() {
        return new Object[][]{
                {"Россия для России", RUSSIA_REGION_ID, singletonList(ru), asList(ru, cr)},
                {"Россия для Украины", UKRAINE_REGION_ID, singletonList(ru), singletonList(ru)},
                {"Россия и Крым для России", RUSSIA_REGION_ID, asList(ru, cr), asList(ru, cr)},
                {"Россия и Нидерладны для России", RUSSIA_REGION_ID, asList(ru, nl), asList(ru, nl, cr)},
                {"Россия и Нидерладны для Украины", UKRAINE_REGION_ID, asList(ru, nl), asList(ru, nl)},

                {"Украина для Украины", UKRAINE_REGION_ID, singletonList(ua), asList(ua, cr)},
                {"Украина и Крым для Украины", UKRAINE_REGION_ID, asList(ua, cr), asList(ua, cr)},
                {"Украина для России", RUSSIA_REGION_ID, singletonList(ua), singletonList(ua)},
                {"Украина и Нидерладны для Украины", UKRAINE_REGION_ID, asList(ua, nl), asList(ua, nl, cr)},
                {"Украина, Крым и Нидерладны для Украины", UKRAINE_REGION_ID, asList(ua, cr, nl), asList(ua, cr, nl)},
                {"Украина и Нидерладны для России", RUSSIA_REGION_ID, asList(ua, nl), asList(ua, nl)},

                {"Россия минус Крым для России", RUSSIA_REGION_ID, asList(ru, -cr), singletonList(ru)},
                {"Россия минус Крым для Украины", UKRAINE_REGION_ID, asList(ru, -cr), singletonList(ru)},
                {"Украина минус Крым для России", RUSSIA_REGION_ID, asList(ua, -cr), singletonList(ua)},
                {"Украина минус Крым для Украины", UKRAINE_REGION_ID, asList(ua, -cr), singletonList(ua)},

                {"Россия минус Украина для Украины", UKRAINE_REGION_ID, asList(ru, -ua), asList(ru, -ua)},

                {"Мир минус Крым для Украины", UKRAINE_REGION_ID, asList(w, -cr), asList(w, -cr)},
                {"Мир минус Крым для России", RUSSIA_REGION_ID, asList(w, -cr), asList(w, -cr)},
        };
    }

    private Object[] forWeb() {
        return new Object[][]{
                {"Россия для России", RUSSIA_REGION_ID, singletonList(ru), asList(ru, -cr)},
                {"Россия для Украины", UKRAINE_REGION_ID, singletonList(ru), singletonList(ru)},
                {"Украина и Крым для России", RUSSIA_REGION_ID, asList(ua, cr), asList(ua, cr)},
                {"Россия и Крым для России", RUSSIA_REGION_ID, asList(ru, cr), singletonList(ru)},

                {"Россия и Украина для России", RUSSIA_REGION_ID, asList(ru, ua), asList(ru, -cr, ua)},
                {"Украина и Россия для России", RUSSIA_REGION_ID, asList(ua, ru), asList(ua, ru, -cr)},
                {"Россия и Украина для Украины", UKRAINE_REGION_ID, asList(ru, ua), asList(ru, ua, -cr)},
                {"Украина и Россия для Украины", UKRAINE_REGION_ID, asList(ua, ru), asList(ua, -cr, ru)},

                {"Украина для Украины", UKRAINE_REGION_ID, singletonList(ua), asList(ua, -cr)},
                {"Украина и Крым для Украины", UKRAINE_REGION_ID, asList(ua, cr), singletonList(ua)},
                {"Украина для России", RUSSIA_REGION_ID, singletonList(ua), singletonList(ua)},
                {"СНГ и Крым для Украины", UKRAINE_REGION_ID, asList(cr, cis), singletonList(cis)},
                {"СНГ и Крым для России", RUSSIA_REGION_ID, asList(cr, cis), asList(cr, cis)},
                {"СНГ без Крыма для Украины", UKRAINE_REGION_ID, singletonList(cis), asList(cis, -cr)},
                {"СНГ без Крыма для России", RUSSIA_REGION_ID, singletonList(cis), singletonList(cis)},
                {"Россия и Крым для Украины", UKRAINE_REGION_ID, asList(ru, cr), asList(ru, cr)},

                {"Россия минус Крым для России", RUSSIA_REGION_ID, asList(ru, -cr), asList(ru, -cr)},
                {"Украина минус Крым для Украины", UKRAINE_REGION_ID, asList(ua, -cr), asList(ua, -cr)},

                {"Мир России", RUSSIA_REGION_ID, singletonList(w), singletonList(w)},
                {"Мир Украины", UKRAINE_REGION_ID, singletonList(w), singletonList(w)},

                {"Украина, Крым минус Симферополь для России", RUSSIA_REGION_ID, asList(ua, cr, -sf),
                        asList(ua, cr, -sf)},
                {"Украина, Крым минус Симферополь для Украины", UKRAINE_REGION_ID, asList(ua, cr, -sf),
                        asList(ua, cr, -sf)},
                {"Россия, Крым минус Симферополь для России", RUSSIA_REGION_ID, asList(ru, cr, -sf),
                        asList(ru, cr, -sf)},
                {"Россия, Крым минус Симферополь для Украины", UKRAINE_REGION_ID, asList(ru, cr, -sf),
                        asList(ru, cr, -sf)},

                {"Украина, Крым минус Николаевская обл для Украины", UKRAINE_REGION_ID, asList(ua, cr, -20543L),
                        asList(ua, -20543L)},
                {"Украина, Крым минус Николаевская обл для России", RUSSIA_REGION_ID, asList(ua, -20543L, cr),
                        asList(ua, -20543L, cr)},
                {"Россия, Крым минус Центральный фед окр для Украины", UKRAINE_REGION_ID, asList(ru, -3L, cr),
                        asList(ru, -3L, cr)},
                {"Россия, Крым минус Центральный фед окр для России", RUSSIA_REGION_ID, asList(ru, cr, -3L),
                        asList(ru, -3L)},
        };
    }

    @Before
    public void setUp() throws Exception {
        testContextManager.prepareTestInstance(this);
    }

    @Test
    @Parameters(method = "forSave")
    public void convertForSave(@SuppressWarnings("unused") String testName, long clientCountryId, List<Long> geoIds,
                               List<Long> expectedGeoIds) {
        createAdGroup(clientCountryId, geoIds);

        List<Long> actualGeoIds = clientGeoService.convertForSave(complexAdGroup.getAdGroup().getGeo(),
                clientGeoService.getClientTranslocalGeoTree(clientId));
        assertThat("Check regions", actualGeoIds, beanDiffer(expectedGeoIds));
    }

    @Test
    @Parameters(method = "forWeb")
    public void convertForWeb(@SuppressWarnings("unused") String testName, long clientCountryId, List<Long> geoIds,
                              List<Long> expectedGeoIds) {
        createAdGroup(clientCountryId, geoIds);

        List<Long> actualGeoIds = clientGeoService.convertForWeb(complexAdGroup.getAdGroup().getGeo(),
                clientGeoService.getClientTranslocalGeoTree(clientId));

        assertThat("Check regions", actualGeoIds, beanDiffer(expectedGeoIds));
    }

    private void createAdGroup(long clientCountryId, List<Long> geoIds) {
        clientId = steps.clientSteps().createClient(new ClientInfo()
                .withClient(defaultClient()
                        .withCountryRegionId(clientCountryId))).getClientId();
        AdGroup adGroup = steps.adGroupSteps().createDefaultAdGroup().getAdGroup();
        adGroup.setGeo(geoIds);
        complexAdGroup = new ComplexAdGroup().withAdGroup(adGroup);
    }
}

