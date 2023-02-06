package ru.yandex.direct.grid.processing.service.trackingphone;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.calltracking.model.CalltrackingSettings;
import ru.yandex.direct.core.entity.calltracking.model.SettingsPhone;
import ru.yandex.direct.core.entity.calltrackingsettings.repository.CalltrackingSettingsRepository;
import ru.yandex.direct.core.entity.calltrackingsettings.validation.CalltrackingSettingDefects;
import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterPermission;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DomainInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.trackingphone.GdSetCalltrackingOnSitePhones;
import ru.yandex.direct.grid.processing.model.trackingphone.GdSetCalltrackingOnSitePhonesItem;
import ru.yandex.direct.grid.processing.model.trackingphone.mutation.GdSetCalltrackingOnSitePhone;
import ru.yandex.direct.grid.processing.model.trackingphone.mutation.GdSetCalltrackingOnSitePhonesPayload;
import ru.yandex.direct.grid.processing.model.trackingphone.mutation.GdSetCalltrackingOnSitePhonesPayloadItem;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.validation.result.DefectId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.calltrackingsettings.validation.CalltrackingSettingDefects.Ids.COUNTER_NOT_AVAILABLE_FOR_CLIENT;
import static ru.yandex.direct.core.entity.calltrackingsettings.validation.CalltrackingSettingDefects.Ids.NO_WRITE_PERMISSIONS_ON_COUNTER;
import static ru.yandex.direct.core.testing.stub.MetrikaClientStub.buildCounter;
import static ru.yandex.direct.grid.processing.service.trackingphone.CalltrackingOnSiteGraphQLService.SET_CALLTRACKING_ON_SITE_PHONES;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX;
import static ru.yandex.direct.validation.result.DefectIds.INVALID_FORMAT;
import static ru.yandex.direct.validation.result.DefectIds.INVALID_VALUE;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CalltrackingOnSiteGraphQLServiceSetCalltrackingOnSitePhonesTest {
    private static final String URL_1 = "http://example1.domain.com";
    private static final String URL_2 = "https://example2.domain.com/campaigns/crusade/9";
    private static final String DOMAIN_1 = "example1.domain.com";
    private static final String DOMAIN_2 = "example2.domain.com";
    private static final String FIRST_FORBIDDEN_DOMAIN = "yandex.ru";
    private static final String SECOND_FORBIDDEN_DOMAIN = "m.yandex.com";
    private static final long COUNTER_ID_1 = 123456L;
    private static final long COUNTER_ID_2 = 654321L;
    private static final long COUNTER_ID_3 = 1197531L;
    private static final String PHONE_1 = "+74950350365";
    private static final String PHONE_2 = "+74990350365";
    private static final String PHONE_3 = "+78660350365";
    private static final String NEW_PHONE = "+74950123456";

    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    items{\n"
            + "      calltrackingSettingsId\n"
            + "      domain\n"
            + "    }\n"
            + "    validationResult{\n"
            + "      errors{\n"
            + "        code,\n"
            + "        params,\n"
            + "        path\n"
            + "      }\n"
            + "      warnings{\n"
            + "        code,\n"
            + "        params,\n"
            + "        path\n"
            + "      }\n"
            + "    }"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.
            TemplateMutation<GdSetCalltrackingOnSitePhones, GdSetCalltrackingOnSitePhonesPayload>
            SET_CALLTRACKING_ON_SITE_PHONES_MUTATION = new GraphQlTestExecutor.TemplateMutation<>(
            SET_CALLTRACKING_ON_SITE_PHONES, MUTATION_TEMPLATE,
            GdSetCalltrackingOnSitePhones.class, GdSetCalltrackingOnSitePhonesPayload.class);

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private MetrikaClient metrikaClient;
    @Autowired
    private MetrikaClientStub metrikaClientStub;
    @Autowired
    private UserService userService;
    @Autowired
    private CalltrackingSettingsRepository calltrackingSettingsRepository;

    private GridGraphQLContext context;
    private ClientInfo clientInfo;
    private ClientId clientId;
    private int shard;
    private User user;
    private Long domainId;
    private long calltrackingSettingId;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        user = userService.getUser(clientInfo.getUid());
        TestAuthHelper.setDirectAuthentication(user);
        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);
        MockitoAnnotations.initMocks(this);
        metrikaClientStub.addUserCounters(clientInfo.getUid(),
                List.of(
                        buildCounter((int) COUNTER_ID_1, "whatever", DOMAIN_1),
                        buildCounter((int) COUNTER_ID_2, "whatever", DOMAIN_1),
                        buildCounter((int) COUNTER_ID_3, null, MetrikaCounterPermission.VIEW)
                )
        );
        domainId = steps.domainSteps().createDomain(new DomainInfo()
                .withShard(shard)
                .withDomain(new Domain()
                        .withDomain(DOMAIN_1)
                )
        ).getDomainId();
        calltrackingSettingId = steps.calltrackingSettingsSteps()
                .add(clientId, domainId, COUNTER_ID_3, List.of(PHONE_1, PHONE_2), false, LocalDateTime.now());
        steps.clientPhoneSteps().addClientManualPhone(clientId, PHONE_1);
        steps.clientPhoneSteps().addClientManualPhone(clientId, PHONE_2);
        steps.clientPhoneSteps().addClientManualPhone(clientId, PHONE_3);
    }

    @After
    public void tearDown() {
        steps.domainSteps().delete(clientInfo.getShard(), List.of(DOMAIN_1, DOMAIN_2));
        steps.calltrackingSettingsSteps().deleteAll(clientInfo.getShard());
        Mockito.reset(metrikaClient);
    }

    @Test
    public void happyPath() {
        var first = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(COUNTER_ID_1)
                .withUrl(URL_1)
                .withCalltrackingPhones(
                        List.of(
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1),
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(NEW_PHONE)
                        )
                );
        var second = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(COUNTER_ID_2)
                .withUrl(URL_2)
                .withCalltrackingPhones(
                        List.of(new GdSetCalltrackingOnSitePhone()
                                .withRedirectPhone(PHONE_2))
                );
        var request = new GdSetCalltrackingOnSitePhones().withSetItems(List.of(first, second));

        GdSetCalltrackingOnSitePhonesPayload result =
                processor.doMutationAndGetPayload(SET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);

        SoftAssertions.assertSoftly(sa -> {
            List<GdSetCalltrackingOnSitePhonesPayloadItem> items = result.getItems();
            assertEquals(2, result.getItems().size());

            var expectedItem1 = new GdSetCalltrackingOnSitePhonesPayloadItem()
                    .withCalltrackingSettingsId(calltrackingSettingId)
                    .withDomain(DOMAIN_1);
            sa.assertThat(expectedItem1).isEqualTo(items.get(0));

            GdSetCalltrackingOnSitePhonesPayloadItem item2 = items.get(1);
            sa.assertThat(DOMAIN_2).isEqualTo(item2.getDomain());
            long newCalltrackingSettingsId = item2.getCalltrackingSettingsId();

            // Check domains table
            var domainIdByDomain = steps.domainSteps().getDomainIdByDomain(shard, List.of(DOMAIN_1, DOMAIN_2));
            sa.assertThat(domainId).isEqualTo((long) domainIdByDomain.get(DOMAIN_1));

            List<CalltrackingSettings> actualSettings = calltrackingSettingsRepository.getByIds(
                    clientId,
                    List.of(calltrackingSettingId, newCalltrackingSettingsId)
            );

            // Check calltracking_settings table
            Map<Long, CalltrackingSettings> actualSettingsById = listToMap(
                    actualSettings,
                    CalltrackingSettings::getCalltrackingSettingsId,
                    Function.identity()
            );

            var firstExpected = new CalltrackingSettings()
                    .withCalltrackingSettingsId(newCalltrackingSettingsId)
                    .withClientId(clientId)
                    .withDomainId(domainIdByDomain.get(DOMAIN_2))
                    .withCounterId(COUNTER_ID_2)
                    .withPhonesToTrack(List.of(new SettingsPhone().withPhone(PHONE_2).withCreateTime(LocalDateTime.now())))
                    .withIsAvailableCounter(true);
            var firstActual = actualSettingsById.get(newCalltrackingSettingsId);
            sa.assertThat(firstActual.getCalltrackingSettingsId()).isEqualTo(firstExpected.getCalltrackingSettingsId());
            sa.assertThat(firstActual.getClientId()).isEqualTo(firstExpected.getClientId());
            sa.assertThat(firstActual.getDomainId()).isEqualTo(firstExpected.getDomainId());
            sa.assertThat(firstActual.getCounterId()).isEqualTo(firstExpected.getCounterId());
            sa.assertThat(firstActual.getIsAvailableCounter()).isEqualTo(firstExpected.getIsAvailableCounter());
            StreamEx.of(firstActual.getPhonesToTrack())
                    .zipWith(firstExpected.getPhonesToTrack().stream())
                    .forKeyValue((k, v) -> {
                        sa.assertThat(k.getCreateTime()).isNotNull();
                        sa.assertThat(k.getCreateTime().isBefore(v.getCreateTime())).isEqualTo(true);
                    });

            var secondExpected = new CalltrackingSettings()
                    .withCalltrackingSettingsId(calltrackingSettingId)
                    .withClientId(clientId)
                    .withDomainId(domainId)
                    .withCounterId(COUNTER_ID_1)
                    .withPhonesToTrack(List.of(new SettingsPhone().withPhone(PHONE_1).withCreateTime(LocalDateTime.now()),
                            new SettingsPhone().withPhone(NEW_PHONE).withCreateTime(LocalDateTime.now())))
                    .withIsAvailableCounter(true);
            var secondActual = actualSettingsById.get(calltrackingSettingId);
            sa.assertThat(secondActual.getCalltrackingSettingsId()).isEqualTo(secondExpected.getCalltrackingSettingsId());
            sa.assertThat(secondActual.getClientId()).isEqualTo(secondExpected.getClientId());
            sa.assertThat(secondActual.getDomainId()).isEqualTo(secondExpected.getDomainId());
            sa.assertThat(secondActual.getCounterId()).isEqualTo(secondExpected.getCounterId());
            sa.assertThat(secondActual.getIsAvailableCounter()).isEqualTo(secondExpected.getIsAvailableCounter());
            StreamEx.of(secondActual.getPhonesToTrack())
                    .zipWith(secondExpected.getPhonesToTrack().stream())
                    .forKeyValue((k, v) -> {
                        sa.assertThat(k.getCreateTime()).isNotNull();
                        sa.assertThat(k.getCreateTime().isBefore(v.getCreateTime())).isEqualTo(true);
                    });

        });
        verify(metrikaClient).turnOnCallTracking(COUNTER_ID_1);
        verify(metrikaClient).turnOnCallTracking(COUNTER_ID_2);
    }

    @Test
    public void tooManyPhonesUpdating() {
        var first = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(COUNTER_ID_1)
                .withUrl(URL_1)
                .withCalltrackingPhones(
                        List.of(
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1),
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_2),
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_3),
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(NEW_PHONE),
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone("+79051245332"),
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone("+79051245333")
                        )
                );
        var request = new GdSetCalltrackingOnSitePhones().withSetItems(List.of(first));

        var payload =
                processor.doMutationAndGetPayload(SET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);
        var expectedPayload =
                new GdSetCalltrackingOnSitePhonesPayload()
                        .withItems(List.of())
                        .withValidationResult(new GdValidationResult()
                                .withErrors(List.of(new GdDefect()
                                        .withCode(SIZE_CANNOT_BE_MORE_THAN_MAX.getCode())
                                        .withParams(Map.of("maxSize", 5))
                                        .withPath("[0].calltrackingPhones")
                                ))
                                .withWarnings(List.of())
                        );
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void wrongPhoneUpdating() {
        var first = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(COUNTER_ID_1)
                .withUrl(URL_1)
                .withCalltrackingPhones(
                        List.of(
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone("+7496035")
                        )
                );
        var request = new GdSetCalltrackingOnSitePhones().withSetItems(List.of(first));

        var payload =
                processor.doMutationAndGetPayload(SET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);
        var expectedPayload =
                new GdSetCalltrackingOnSitePhonesPayload()
                        .withItems(List.of())
                        .withValidationResult(new GdValidationResult()
                                .withErrors(List.of(new GdDefect()
                                        .withCode(INVALID_FORMAT.getCode())
                                        .withParams(null)
                                        .withPath("[0].calltrackingPhones.redirectPhone[0]")
                                ))
                                .withWarnings(List.of())
                        );
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void wrongCounterIdUpdating() {
        var first = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(667L)
                .withUrl(URL_1)
                .withCalltrackingPhones(
                        List.of(
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1)
                        )
                );
        var request = new GdSetCalltrackingOnSitePhones().withSetItems(List.of(first));

        GdSetCalltrackingOnSitePhonesPayload payload =
                processor.doMutationAndGetPayload(SET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);
        var expectedPayload =
                new GdSetCalltrackingOnSitePhonesPayload()
                        .withItems(List.of())
                        .withValidationResult(new GdValidationResult()
                                .withErrors(List.of(new GdDefect()
                                        .withCode(COUNTER_NOT_AVAILABLE_FOR_CLIENT.getCode())
                                        .withParams(null)
                                        .withPath("[0].counterId")
                                ))
                                .withWarnings(List.of())
                        );
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void tooManyPhonesAdding() {
        var first = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(COUNTER_ID_1)
                .withUrl(URL_2)
                .withCalltrackingPhones(
                        List.of(
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1),
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_2),
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_3),
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(NEW_PHONE),
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone("+79051245332"),
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone("+79051245333")
                        )
                );
        var request = new GdSetCalltrackingOnSitePhones().withSetItems(List.of(first));

        var payload =
                processor.doMutationAndGetPayload(SET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);
        var expectedPayload =
                new GdSetCalltrackingOnSitePhonesPayload()
                        .withItems(List.of())
                        .withValidationResult(new GdValidationResult()
                                .withErrors(List.of(new GdDefect()
                                        .withCode(SIZE_CANNOT_BE_MORE_THAN_MAX.getCode())
                                        .withParams(Map.of("maxSize", 5))
                                        .withPath("[0].calltrackingPhones")
                                ))
                                .withWarnings(List.of())
                        );
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void wrongPhoneAdding() {
        var first = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(COUNTER_ID_1)
                .withUrl(URL_2)
                .withCalltrackingPhones(
                        List.of(
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone("+7496035")
                        )
                );
        var request = new GdSetCalltrackingOnSitePhones().withSetItems(List.of(first));

        var payload =
                processor.doMutationAndGetPayload(SET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);
        var expectedPayload =
                new GdSetCalltrackingOnSitePhonesPayload()
                        .withItems(List.of())
                        .withValidationResult(new GdValidationResult()
                                .withErrors(List.of(new GdDefect()
                                        .withCode(INVALID_FORMAT.getCode())
                                        .withParams(null)
                                        .withPath("[0].calltrackingPhones.redirectPhone[0]")
                                ))
                                .withWarnings(List.of())
                        );
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void forbiddenDomainAdding() {
        var first = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(COUNTER_ID_1)
                .withUrl("https://" + FIRST_FORBIDDEN_DOMAIN + "/page")
                .withCalltrackingPhones(
                        List.of(
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1)
                        )
                );
        var second = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(COUNTER_ID_2)
                .withUrl("https://" + SECOND_FORBIDDEN_DOMAIN + "/page")
                .withCalltrackingPhones(
                        List.of(
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_2)
                        )
                );
        var request = new GdSetCalltrackingOnSitePhones().withSetItems(List.of(first, second));

        var payload =
                processor.doMutationAndGetPayload(SET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);
        var expectedPayload =
                new GdSetCalltrackingOnSitePhonesPayload()
                        .withItems(List.of())
                        .withValidationResult(new GdValidationResult()
                                .withErrors(List.of(
                                        new GdDefect()
                                                .withCode(CalltrackingSettingDefects.Ids.FORBIDDEN_DOMAIN.getCode())
                                                .withParams(null)
                                                .withPath("[0].url"),
                                        new GdDefect()
                                                .withCode(CalltrackingSettingDefects.Ids.FORBIDDEN_DOMAIN.getCode())
                                                .withParams(null)
                                                .withPath("[1].url")
                                ))
                                .withWarnings(List.of())
                        );
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void existsCounterId() {
        makeWrongCounterIdRequest(666L, COUNTER_NOT_AVAILABLE_FOR_CLIENT);
    }

    @Test
    public void onlyViewPermissionOnCounter() {
        makeWrongCounterIdRequest(COUNTER_ID_3, NO_WRITE_PERMISSIONS_ON_COUNTER);
    }

    private void makeWrongCounterIdRequest(long counterId, DefectId<Void> defectId) {
        var first = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(counterId)
                .withUrl(URL_2)
                .withCalltrackingPhones(
                        List.of(
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1)
                        )
                );
        var request = new GdSetCalltrackingOnSitePhones().withSetItems(List.of(first));

        GdSetCalltrackingOnSitePhonesPayload payload =
                processor.doMutationAndGetPayload(SET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);
        var expectedPayload =
                new GdSetCalltrackingOnSitePhonesPayload()
                        .withItems(List.of())
                        .withValidationResult(new GdValidationResult()
                                .withErrors(List.of(new GdDefect()
                                        .withCode(defectId.getCode())
                                        .withParams(null)
                                        .withPath("[0].counterId")
                                ))
                                .withWarnings(List.of())
                        );
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void wrongUrl() {
        var first = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(COUNTER_ID_1)
                .withUrl("https;\\%@#&%#")
                .withCalltrackingPhones(
                        List.of(
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1)
                        )
                );
        var request = new GdSetCalltrackingOnSitePhones().withSetItems(List.of(first));

        var payload =
                processor.doMutationAndGetPayload(SET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);
        var expectedPayload =
                new GdSetCalltrackingOnSitePhonesPayload()
                        .withItems(List.of())
                        .withValidationResult(new GdValidationResult()
                                .withErrors(List.of(new GdDefect()
                                        .withCode(INVALID_VALUE.getCode())
                                        .withParams(null)
                                        .withPath("[0].url")
                                ))
                                .withWarnings(List.of())
                        );
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void partlySuccessPartlyFail() {
        var first = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(COUNTER_ID_1)
                .withUrl(URL_1)
                .withCalltrackingPhones(
                        List.of(
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1),
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(NEW_PHONE)
                        )
                );
        var second = new GdSetCalltrackingOnSitePhonesItem()
                .withCounterId(COUNTER_ID_1)
                .withUrl("https;\\%@#&%#")
                .withCalltrackingPhones(
                        List.of(
                                new GdSetCalltrackingOnSitePhone().withRedirectPhone(PHONE_2)
                        )
                );
        var request = new GdSetCalltrackingOnSitePhones().withSetItems(List.of(first, second));

        var payload =
                processor.doMutationAndGetPayload(SET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);
        var expectedPayload =
                new GdSetCalltrackingOnSitePhonesPayload()
                        .withItems(List.of(new GdSetCalltrackingOnSitePhonesPayloadItem()
                                .withCalltrackingSettingsId(calltrackingSettingId)
                                .withDomain(DOMAIN_1)
                        ))
                        .withValidationResult(new GdValidationResult()
                                .withErrors(List.of(
                                        new GdDefect()
                                                .withCode(INVALID_VALUE.getCode())
                                                .withParams(null)
                                                .withPath("[1].url")
                                ))
                                .withWarnings(List.of())
                        );
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
        verify(metrikaClient).turnOnCallTracking(COUNTER_ID_1);
        verify(metrikaClient, never()).turnOnCallTracking(COUNTER_ID_2);
    }

}
