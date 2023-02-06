package ru.yandex.direct.grid.processing.service.trackingphone;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterPermission;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.trackingphone.GdCalltrackingOnSite;
import ru.yandex.direct.grid.processing.model.trackingphone.GdCalltrackingOnSiteInputItem;
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect;
import ru.yandex.direct.validation.defect.CommonDefects;

import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CalltrackingOnSiteValidationServiceTest {
    private static final Long COUNTER_ID_WITH_UPDATE_PERMISSION = 123456L;
    private static final Long COUNTER_ID_WITH_READ_PERMISSION = 654321L;

    private static final String CORRECT_URL = "http://example.com/123";
    private static final String INCORRECT_URL = "incorrect_url";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;

    @Autowired
    private CalltrackingOnSiteValidationService validationService;
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    private ClientInfo clientInfo;
    private User user;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        user = userService.getUser(clientInfo.getUid());

        CounterInfoDirect counterWithUpdatePermission = MetrikaClientStub.buildCounter(
                COUNTER_ID_WITH_UPDATE_PERMISSION.intValue(), null, MetrikaCounterPermission.EDIT);

        CounterInfoDirect counterWithReadPermission = MetrikaClientStub.buildCounter(
                COUNTER_ID_WITH_READ_PERMISSION.intValue(), null, MetrikaCounterPermission.VIEW);

        metrikaClientStub.addUserCounters(user.getUid(),
                List.of(counterWithUpdatePermission, counterWithReadPermission));
    }

    @Test
    public void validateGetCalltrackingOnGetByUrlHappyPath() {
        GdCalltrackingOnSite input = new GdCalltrackingOnSite()
                .withItems(List.of(
                        new GdCalltrackingOnSiteInputItem()
                                .withUrl(CORRECT_URL)
                                .withCounterId(COUNTER_ID_WITH_READ_PERMISSION),
                        new GdCalltrackingOnSiteInputItem()
                                .withUrl(CORRECT_URL)
                                .withCounterId(COUNTER_ID_WITH_UPDATE_PERMISSION)));

        validationService.validateGetCalltrackingOnGetByUrl(input);
    }

    @Test
    public void validateGetCalltrackingOnGetByUrlIncorrectUrl() {
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(path(field(GdCalltrackingOnSite.ITEMS), index(0),
                        field(GdCalltrackingOnSiteInputItem.URL)),
                        CommonDefects.invalidValue()))));

        GdCalltrackingOnSite input = new GdCalltrackingOnSite()
                .withItems(List.of(
                        new GdCalltrackingOnSiteInputItem()
                                .withUrl(INCORRECT_URL)
                                .withCounterId(COUNTER_ID_WITH_UPDATE_PERMISSION)));

        validationService.validateGetCalltrackingOnGetByUrl(input);
    }

    @Test
    public void validateCalltrackingOnSiteGetByIdHappyPath() {
        GdCalltrackingOnSite input = new GdCalltrackingOnSite()
                .withItems(List.of(
                        new GdCalltrackingOnSiteInputItem()
                                .withCalltrackingSettingsId(1L),
                        new GdCalltrackingOnSiteInputItem()
                                .withCalltrackingSettingsId(2L)));

        validationService.validateGetCalltrackingOnSiteGetById(input);
    }

    @Test
    public void validateCalltrackingOnSiteGetByIdCalltrackingSettingsIdIsNull() {
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(path(field(GdCalltrackingOnSite.ITEMS), index(0),
                        field(GdCalltrackingOnSiteInputItem.CALLTRACKING_SETTINGS_ID)),
                        CommonDefects.notNull()))));

        GdCalltrackingOnSite input = new GdCalltrackingOnSite()
                .withItems(List.of(
                        new GdCalltrackingOnSiteInputItem(),
                        new GdCalltrackingOnSiteInputItem()
                                .withCalltrackingSettingsId(2L)));

        validationService.validateGetCalltrackingOnSiteGetById(input);
    }
}
