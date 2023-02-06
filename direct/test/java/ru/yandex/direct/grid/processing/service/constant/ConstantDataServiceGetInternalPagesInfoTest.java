package ru.yandex.direct.grid.processing.service.constant;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.agencyofflinereport.service.AgencyOfflineReportParametersService;
import ru.yandex.direct.core.entity.page.service.PageService;
import ru.yandex.direct.core.entity.pages.model.Page;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.grid.processing.model.internalad.GdInternalPageInfo;
import ru.yandex.direct.grid.processing.service.offlinereport.OfflineReportValidationService;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(MockitoJUnitRunner.class)
@ParametersAreNonnullByDefault
public class ConstantDataServiceGetInternalPagesInfoTest {

    @Mock
    private PageService pageService;

    @SuppressWarnings("unused")
    @Mock
    private AgencyOfflineReportParametersService agencyOfflineReportParametersService;

    @SuppressWarnings("unused")
    @Mock
    private OfflineReportValidationService offlineReportValidationService;

    @InjectMocks
    private ConstantDataService constantDataService;

    private List<Page> internalAdPages;
    private List<GdInternalPageInfo> expectedInternalPagesInfo;
    private User operator;

    @Before
    public void before() {
        var firstPage = new Page()
                .withId(1L)
                .withName(RandomStringUtils.randomAlphanumeric(11))
                .withDescription(RandomStringUtils.randomAlphanumeric(11));
        var secondPage = new Page()
                .withId(2L)
                .withName(RandomStringUtils.randomAlphanumeric(11))
                .withDescription(RandomStringUtils.randomAlphanumeric(11));
        internalAdPages = List.of(firstPage, secondPage);
        doReturn(internalAdPages)
                .when(pageService).getAllInternalAdPages();

        expectedInternalPagesInfo = List.of(
                new GdInternalPageInfo()
                        .withPageId(firstPage.getId())
                        .withName(firstPage.getName())
                        .withDescription(firstPage.getDescription()),
                new GdInternalPageInfo()
                        .withPageId(secondPage.getId())
                        .withName(secondPage.getName())
                        .withDescription(secondPage.getDescription())
        );

        operator = new User()
                .withRole(RbacRole.INTERNAL_AD_ADMIN);
    }


    @Test
    public void getInternalPagesInfo() {
        List<GdInternalPageInfo> internalPagesInfo = constantDataService.getInternalPagesInfo(operator);

        verify(pageService).getAllInternalAdPages();
        assertThat(internalPagesInfo)
                .is(matchedBy(beanDiffer(expectedInternalPagesInfo)));
    }

    @Test
    public void getInternalPagesInfo_WhenOperatorIsClient() {
        operator.setRole(RbacRole.CLIENT);
        List<GdInternalPageInfo> internalPagesInfo = constantDataService.getInternalPagesInfo(operator);

        verifyZeroInteractions(pageService);
        assertThat(internalPagesInfo)
                .isEmpty();
    }

    @Test
    public void getInternalPagesInfo_WhenOperatorIsSuperReader() {
        operator.setRole(RbacRole.SUPERREADER);
        List<GdInternalPageInfo> internalPagesInfo = constantDataService.getInternalPagesInfo(operator);

        verify(pageService).getAllInternalAdPages();
        assertThat(internalPagesInfo)
                .hasSameSizeAs(expectedInternalPagesInfo);
    }

    @Test
    public void getInternalPagesInfo_CheckResultOrder() {
        doReturn(Lists.reverse(internalAdPages))
                .when(pageService).getAllInternalAdPages();

        List<GdInternalPageInfo> internalPagesInfo = constantDataService.getInternalPagesInfo(operator);

        assertThat(internalPagesInfo)
                .is(matchedBy(beanDiffer(expectedInternalPagesInfo)));
    }

}
