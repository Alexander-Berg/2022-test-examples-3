package ru.yandex.market.api.cpa.yam.service.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.yam.dao.PrepayRequestDao;
import ru.yandex.market.api.cpa.yam.entity.PartnerApplicationDocumentType;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequestDocument;
import ru.yandex.market.api.cpa.yam.entity.RequestInfoData;
import ru.yandex.market.api.cpa.yam.entity.RequestType;
import ru.yandex.market.api.cpa.yam.exception.InvalidPrepayRequestOperationException;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestValidatorService.FullFilled;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestValidatorService.MandatoryFieldValidation;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.application.meta.PartnerApplication;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.application.PartnerApplicationStatus.CANCELLED;
import static ru.yandex.market.core.application.PartnerApplicationStatus.CLOSED;
import static ru.yandex.market.core.application.PartnerApplicationStatus.COMPLETED;
import static ru.yandex.market.core.application.PartnerApplicationStatus.DECLINED;
import static ru.yandex.market.core.application.PartnerApplicationStatus.FROZEN;
import static ru.yandex.market.core.application.PartnerApplicationStatus.INIT;
import static ru.yandex.market.core.application.PartnerApplicationStatus.INTERNAL_CLOSED;
import static ru.yandex.market.core.application.PartnerApplicationStatus.IN_PROGRESS;
import static ru.yandex.market.core.application.PartnerApplicationStatus.NEED_INFO;
import static ru.yandex.market.core.application.PartnerApplicationStatus.NEW;
import static ru.yandex.market.core.application.PartnerApplicationStatus.NEW_PROGRAMS_VERIFICATION_FAILED;
import static ru.yandex.market.core.application.PartnerApplicationStatus.NEW_PROGRAMS_VERIFICATION_REQUIRED;

/**
 * Unit тесты для {@link PrepayRequestValidatorServiceImpl}.
 *
 * @author avetokhin 14/04/17.
 */
public class PrepayRequestValidatorServiceImplTest extends FunctionalTest {

    private static final long DS_ID_1 = 10;
    private static final long DS_ID_2 = 11;
    private static final long DS_ID_3 = 12;
    private static final long REQ_ID_1 = 100;
    private static final long REQ_ID_2 = 101;
    private static final long REQ_ID_3 = 102;
    private static final long REQ_ID_4 = 103;
    private static final int MAX_DOCS = 2;

    // Для простоты можно переходить только из NEW -> INIT -> IN_PROGRESS.
    private static final Map<PartnerApplicationStatus, List<PartnerApplicationStatus>> ALLOWED_MOVES = new HashMap<>();

    static {
        ALLOWED_MOVES.put(NEW, Collections.singletonList(INIT));
        ALLOWED_MOVES.put(INIT, Collections.singletonList(IN_PROGRESS));
    }

    @Mock
    private PrepayRequestDao requestDao;

    @Mock
    private Validator validator;

    @Autowired
    private EnvironmentService environmentService;

    private PrepayRequestValidatorServiceImpl service;

    @SafeVarargs
    private static Map<Long, Collection<RequestInfoData>> existingRequests(
            final long datasourceId, RequestInfoData... requestData
    ) {
        final Map<Long, Collection<RequestInfoData>> existingRequests = new HashMap<>();
        existingRequests.put(datasourceId, Arrays.asList(requestData));
        return existingRequests;
    }

    private static CampaignInfo campaign(final long clientId) {
        final CampaignInfo campaignInfo = new CampaignInfo();
        campaignInfo.setClientId(clientId);
        campaignInfo.setType(CampaignType.SHOP);
        return campaignInfo;
    }

    private static PrepayRequestDocument document() {
        return new PrepayRequestDocument(0, 0L, PartnerApplicationDocumentType.SIGNED_APP_FORM, "", 0, "");
    }

    private static PartnerApplication application(final PartnerApplicationStatus status) {
        return new PartnerApplication(0, status);
    }

    private static PrepayRequest request(final PartnerApplicationStatus status) {
        return new PrepayRequest(1L, PrepayType.YANDEX_MARKET, status, 1L);
    }

    public static void exceptionCheck(final Runnable operation, final boolean expectException) {
        try {
            operation.run();
            if (expectException) {
                fail("Expected exception was not thrown");
            }
        } catch (InvalidPrepayRequestOperationException e) {
            if (!expectException) {
                fail("Unexpected exception was thrown");
            }
        }
    }

    private static Collection<PartnerApplication> applications(final PartnerApplicationStatus... statuses) {
        return Stream.of(statuses).map(PrepayRequestValidatorServiceImplTest::application).collect(Collectors.toList());
    }

    private static Collection<PrepayRequest> requests(final PartnerApplicationStatus... statuses) {
        return Stream.of(statuses).map(PrepayRequestValidatorServiceImplTest::request).collect(Collectors.toList());
    }

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        CampaignService campaignService = mockCampaignService();
        service = new PrepayRequestValidatorServiceImpl(requestDao, campaignService, validator, ALLOWED_MOVES,
                MAX_DOCS, environmentService);
    }

    /**
     * Проверить доступность редактирования заявок.
     */
    @Test
    public void testCheckIfWritable() {
        exceptionCheck(() -> service.checkIfWritable(asList(request(NEW), request(INIT))), true);
        exceptionCheck(() -> service.checkIfWritable(singleton(request(NEW))), false);
        exceptionCheck(() -> service.checkIfWritable(singleton(request(NEED_INFO))), false);
        exceptionCheck(() -> service.checkIfWritable(singleton(request(COMPLETED))), true);
        exceptionCheck(() -> service.checkIfWritable(singleton(request(INIT))), true);
        exceptionCheck(() -> service.checkIfWritable(singleton(request(FROZEN))), true);
        exceptionCheck(() -> service.checkIfWritable(singleton(request(DECLINED))), true);
        exceptionCheck(() -> service.checkIfWritable(singleton(request(CANCELLED))), true);
        exceptionCheck(() -> service.checkIfWritable(singleton(request(CLOSED))), true);
        exceptionCheck(() -> service.checkIfWritable(singleton(request(IN_PROGRESS))), true);
        exceptionCheck(() -> service.checkIfWritable(singleton(request(INTERNAL_CLOSED))), true);
    }

    /**
     * Проверить возможность смены статуса.
     */
    @ParameterizedTest
    @MethodSource
    public void testStatusChangeAllowed(final PartnerApplicationStatus from,
                                        final PartnerApplicationStatus to) {
        final boolean expectingException = !(from == NEW && to == INIT || from == INIT && to == IN_PROGRESS);
        exceptionCheck(() -> service.checkStatusChangeAllowed(from, to), expectingException);
    }

    /**
     * Проверить количество документов.
     */
    @Test
    public void testDocCount() {
        exceptionCheck(() -> service.checkDocumentsCount(emptyList()), false);
        exceptionCheck(() -> service.checkDocumentsCount(singleton(document())), false);
        exceptionCheck(() -> service.checkDocumentsCount(asList(document(), document())), true);
    }

    /**
     * Проверить позитивный кейз проверки заявки на валидность.
     */
    @Test
    public void testRequestIsFullFilledOk() {
        final PrepayRequest request = request(NEW);
        service.checkRequestIsFullFilled(request);

        verify(validator).validate(request, MandatoryFieldValidation.class, FullFilled.class);
    }

    /**
     * Проверить негативный кейз проверки заявки на валидность.
     */
    @Test
    public void testRequestIsFullFilledFail() {
        final PrepayRequest request = request(NEW);
        ConstraintViolation<PrepayRequest> violation =
                ConstraintViolationImpl.forBeanValidation(null, null, null, null, null, null, null, null, null, null,
                        null, null);
        when(validator.validate(eq(request), any())).thenReturn(singleton(violation));

        assertThrows(InvalidPrepayRequestOperationException.class, () -> {
            service.checkRequestIsFullFilled(request);
        });
        verify(validator).validate(request, MandatoryFieldValidation.class, FullFilled.class);
    }

    /**
     * Проверить позитивный кейз проверки заявки на заполненность основных полей.
     */
    @Test
    public void testAllMandatoryFieldsAreFilledOk() {
        final PrepayRequest request = request(NEW);
        assertThat(service.allMandatoryFieldAreFilledCorrectly(request), equalTo(true));

        verify(validator).validate(request, MandatoryFieldValidation.class);
    }

    /**
     * Проверить негативный кейз проверки заявки на заполненность основных полей.
     */
    public void testAllMandatoryFieldsAreFilledFail() {
        final PrepayRequest request = request(NEW);
        ConstraintViolation<PrepayRequest> violation =
                ConstraintViolationImpl.forBeanValidation(null, null, null, null, null, null, null, null, null, null,
                        null, null);
        when(validator.validate(eq(request), any())).thenReturn(singleton(violation));

        assertThat(service.allMandatoryFieldAreFilledCorrectly(request), equalTo(false));

        verify(validator).validate(request, MandatoryFieldValidation.class);
    }

    @Test
    public void testGetClientIdWithCheck() {
        exceptionCheck(() -> service.getClientIdsWithCheck(asList(1L, 2L)), false);
        exceptionCheck(() -> service.getClientIdsWithCheck(asList(1L, 2L, 3L)), false);
        exceptionCheck(() -> service.getClientIdsWithCheck(asList(2L, 3L)), false);
        exceptionCheck(() -> service.getClientIdsWithCheck(asList(1L, 3L)), false);
        exceptionCheck(() -> service.getClientIdsWithCheck(asList(4L)), true);
    }

    /**
     * Проверяем, что сервис выбрасывает исключение при попытке поменять статус на групповой для конкретного магазина.
     */
    @Test
    public void updateRequestStatusForGroupedStatusAndDatasourceId() {
        exceptionCheck(() -> service.checkIfStatusAllowedForDatasources(NEW, singleton(DS_ID_1)), true);
        exceptionCheck(() -> service.checkIfStatusAllowedForDatasources(INIT, singleton(DS_ID_1)), true);
        exceptionCheck(() -> service.checkIfStatusAllowedForDatasources(IN_PROGRESS, singleton(DS_ID_1)), true);
        exceptionCheck(() -> service.checkIfStatusAllowedForDatasources(COMPLETED, singleton(DS_ID_1)), true);
        exceptionCheck(() -> service.checkIfStatusAllowedForDatasources(DECLINED, singleton(DS_ID_1)), true);
        exceptionCheck(() -> service.checkIfStatusAllowedForDatasources(NEED_INFO, singleton(DS_ID_1)), true);
    }

    /**
     * Проверить, что сервис выбрасывает исключение при попытке удаления документа, привязанного к
     * подключенной/проверяемой заявке.
     */
    @Test
    public void checkIfCanDeleteDocTest() {
        exceptionCheck(() -> service.checkIfCanDeleteDoc(applications(NEW, IN_PROGRESS)), true);
        exceptionCheck(() -> service.checkIfCanDeleteDoc(applications(INIT, COMPLETED, CANCELLED)), true);
        exceptionCheck(() -> service.checkIfCanDeleteDoc(applications(NEED_INFO, FROZEN, CANCELLED)), true);
        exceptionCheck(() -> service.checkIfCanDeleteDoc(applications(CANCELLED, CLOSED, NEW, NEED_INFO)), false);
    }

    /**
     * Проверить, что сервис выбрасывает исключение при попытке создания новой заявки, если уже есть другие активные
     * заявки.
     */
    @Test
    public void checkIfAllowedAddingNewRequestTest() {
        when(requestDao.findIdsAndStatuses(any())).thenReturn(
                existingRequests(DS_ID_1, new RequestInfoData(REQ_ID_1, PartnerApplicationStatus.NEW,
                                RequestType.MARKETPLACE),
                        new RequestInfoData(REQ_ID_2, PartnerApplicationStatus.COMPLETED, RequestType.MARKETPLACE))
        );
        exceptionCheck(() -> service.checkIfAllowedAddingNewRequest(singleton(DS_ID_1)), true);

        when(requestDao.findIdsAndStatuses(any())).thenReturn(
                existingRequests(DS_ID_1, new RequestInfoData(REQ_ID_2, PartnerApplicationStatus.DECLINED,
                        RequestType.MARKETPLACE))
        );
        exceptionCheck(() -> service.checkIfAllowedAddingNewRequest(singleton(DS_ID_2)), true);

        when(requestDao.findIdsAndStatuses(any())).thenReturn(
                existingRequests(DS_ID_1, new RequestInfoData(REQ_ID_1, PartnerApplicationStatus.CANCELLED,
                                RequestType.MARKETPLACE),
                        new RequestInfoData(REQ_ID_2, PartnerApplicationStatus.CLOSED, RequestType.MARKETPLACE),
                        new RequestInfoData(REQ_ID_3, PartnerApplicationStatus.FROZEN, RequestType.MARKETPLACE),
                        new RequestInfoData(REQ_ID_4, PartnerApplicationStatus.INTERNAL_CLOSED,
                                RequestType.MARKETPLACE))
        );
        exceptionCheck(() -> service.checkIfAllowedAddingNewRequest(singleton(DS_ID_3)), false);
    }

    /**
     * Проверяем, что выбрасываются исключения при проверке на возможность прикерпления переподписанного заявления.
     */
    @Test
    public void checkIfAllowedResignWithPrograms() {
        exceptionCheck(() -> service.checkIfAllowedResignWithPrograms(requests(COMPLETED)), false);
        exceptionCheck(() -> service.checkIfAllowedResignWithPrograms(requests(NEW_PROGRAMS_VERIFICATION_FAILED)),
                false);
        exceptionCheck(() -> service.checkIfAllowedResignWithPrograms(requests(NEW)), true);
        exceptionCheck(() -> service.checkIfAllowedResignWithPrograms(requests(INIT)), true);
        exceptionCheck(() -> service.checkIfAllowedResignWithPrograms(requests(IN_PROGRESS)), true);
        exceptionCheck(() -> service.checkIfAllowedResignWithPrograms(requests(FROZEN)), true);
        exceptionCheck(() -> service.checkIfAllowedResignWithPrograms(requests(NEW_PROGRAMS_VERIFICATION_REQUIRED)),
                true);
        exceptionCheck(() -> service.checkIfAllowedResignWithPrograms(requests(CANCELLED)), true);
        exceptionCheck(() -> service.checkIfAllowedResignWithPrograms(requests(INTERNAL_CLOSED)), true);
        //если в разных статусах то тоже ошибка
        exceptionCheck(() -> service.checkIfAllowedResignWithPrograms(requests(NEW_PROGRAMS_VERIFICATION_FAILED,
                COMPLETED)), true);
    }

    private CampaignService mockCampaignService() {
        final CampaignService campaignService = mock(CampaignService.class);

        final Map<Long, Long> campaignIds = new HashMap<>();
        campaignIds.put(1L, 1L);
        campaignIds.put(2L, 2L);
        campaignIds.put(3L, 3L);

        final Map<Long, CampaignInfo> campaignInfos = new HashMap<>();
        campaignInfos.put(1L, campaign(10L));
        campaignInfos.put(2L, campaign(10L));
        campaignInfos.put(3L, campaign(20L));

        when(campaignService.getCampaignIds(any())).then(invocation -> {
            final Collection<Long> ids = invocation.getArgument(0);
            return ids.stream()
                    .collect(Collectors.toMap(Function.identity(), id -> campaignIds.getOrDefault(id, -1L)));
        });
        when(campaignService.getMarketCampaigns(any())).then((Answer<Map<Long, CampaignInfo>>) invocation -> {
            final Collection<Long> ids = invocation.getArgument(0);
            return ids.stream()
                    .collect(Collectors.toMap(Function.identity(), campaignInfos::get));
        });
        when(campaignService.getCampaignByDatasource(anyLong())).thenReturn(campaign(10L));
        return campaignService;
    }

    private static Stream<Arguments> testStatusChangeAllowed() {
        return Arrays.stream(PartnerApplicationStatus.values())
                .flatMap(p1 -> Arrays.stream(PartnerApplicationStatus.values()).map(p2 -> Arguments.of(p1, p2)));
    }
}
