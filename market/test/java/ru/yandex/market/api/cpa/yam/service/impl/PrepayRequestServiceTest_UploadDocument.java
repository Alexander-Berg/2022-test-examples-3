package ru.yandex.market.api.cpa.yam.service.impl;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.market.api.cpa.CPAPlacementService;
import ru.yandex.market.api.cpa.yam.dao.PrepayRequestDao;
import ru.yandex.market.api.cpa.yam.dao.PrepayRequestDocumentDao;
import ru.yandex.market.api.cpa.yam.entity.PartnerApplicationDocumentType;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.service.PartnerApplicationDocumentsStorageService;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestBalanceHelperService;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestHistoryService;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestValidatorService;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.application.business.repository.BusinessPartnerApplicationDAO;
import ru.yandex.market.core.application.meta.PartnerApplicationBalanceDataDAO;
import ru.yandex.market.core.application.meta.PartnerApplicationContactDAO;
import ru.yandex.market.core.application.meta.PartnerApplicationDAO;
import ru.yandex.market.core.application.meta.PartnerDocument;
import ru.yandex.market.core.application.meta.PartnerDocumentDAO;
import ru.yandex.market.core.application.meta.impl.OrganizationInfoSyncServiceImpl;
import ru.yandex.market.core.application.selfemployed.SelfEmployedApplicationService;
import ru.yandex.market.core.error.EntityNotFoundException;
import ru.yandex.market.core.geocoder.RegionIdFetcher;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.orginfo.OrganizationInfoService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.partner.PartnerLinkService;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.contract.PartnerContractService;
import ru.yandex.market.core.protocol.MockProtocolService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.mbi.lock.LockService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для загрузки файла в {@link PrepayRequestServiceImpl}.
 */
class PrepayRequestServiceTest_UploadDocument {

    private static final long ACTION_ID = 1L;
    private static final long DS_ID_1 = 10;
    private static final long DOC_ID_1 = 1;
    private static final long REQ_ID_1 = 1;
    private static final long REQ_DATASOURCE_ID_1 = 1;
    private static final MultipartFile MOCKED_FILE = new MockMultipartFile(
            "some-filename",
            "original-file-name",
            null,
            "payload".getBytes(StandardCharsets.UTF_8)
    );

    @Mock
    private PrepayRequestDao prepayRequestDao;
    @Mock
    private PrepayRequestDocumentDao prepayRequestDocumentDao;
    @Mock
    private PartnerDocumentDAO partnerDocumentDAO;
    private final ProtocolService protocolService = new MockProtocolService();
    @Mock
    private OrganizationInfoService organizationInfoService;
    @Mock
    private CPAPlacementService cpaPlacementService;
    @Mock
    private ParamService paramService;
    @Mock
    private PrepayRequestBalanceHelperService balanceHelperService;
    @Mock
    private PrepayRequestHistoryService requestHistoryService;
    @Mock
    private HistoryService historyService;
    @Mock
    private PrepayRequestValidatorService validatorService;
    @Mock
    private PartnerContractService supplierContractService;
    @Mock
    private LockService lockService;
    private PrepayRequestServiceImpl service;
    @Mock
    private GeoClient geoClient;
    @Mock
    private BusinessPartnerApplicationDAO businessPartnerApplicationDAO;
    @Mock
    private PartnerApplicationContactDAO partnerApplicationContactDAO;
    @Mock
    private PartnerApplicationDAO partnerApplicationDAO;
    @Mock
    private PartnerApplicationBalanceDataDAO partnerApplicationBalanceDataDAO;
    @Mock
    private PartnerLinkService partnerLinkService;
    @Mock
    private PartnerService partnerService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private PartnerTypeAwareService partnerTypeAwareService;
    @Mock
    private SelfEmployedApplicationService selfEmployedApplicationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        service = new PrepayRequestServiceImpl(
                prepayRequestDao,
                prepayRequestDocumentDao,
                partnerDocumentDAO,
                protocolService,
                new OrganizationInfoSyncServiceImpl(organizationInfoService),
                cpaPlacementService,
                paramService,
                balanceHelperService,
                supplierContractService,
                requestHistoryService,
                historyService,
                validatorService,
                lockService,
                new AssessorFormUpdateService(),
                new RegionIdFetcher(geoClient),
                partnerApplicationDAO,
                businessPartnerApplicationDAO,
                partnerApplicationContactDAO,
                partnerApplicationBalanceDataDAO,
                partnerLinkService,
                partnerTypeAwareService,
                partnerService,
                applicationEventPublisher,
                selfEmployedApplicationService);

    }

    /**
     * Реперные моменты flow-сохраняния документа, для документа НЕ
     * {@link PartnerApplicationDocumentType#SIGNED_APP_PROGRAMS_UPDATE}.
     */
    @Test
    void test_uploadRequestDocument_when_passedResignedDocument() throws IOException {
        PartnerApplicationDocumentsStorageService yamStorage = mock(PartnerApplicationDocumentsStorageService.class);
        URL url = new URL("http://some.url.here/file-uploaded-to-yam-storage");

        when(yamStorage.uploadFile(any(), anyLong(), anyLong())).thenReturn(Optional.of(url));
        when(yamStorage.getDownloadUrl(anyLong(), anyLong())).thenReturn(url);

        service.setPartnerApplicationDocumentsStorageService(yamStorage);

        final PrepayRequest req = new PrepayRequest(REQ_ID_1, PrepayType.YANDEX_MARKET,
                PartnerApplicationStatus.COMPLETED, DS_ID_1);
        final List<PrepayRequest> requests = Collections.singletonList(req);
        final PartnerDocument partnerDocument = new PartnerDocument(DOC_ID_1, REQ_ID_1,
                PartnerApplicationDocumentType.SIGNED_APP_FORM,
                MOCKED_FILE.getName(), MOCKED_FILE.getSize(), url.toString(), Instant.now());
        when(prepayRequestDao.find(any())).thenReturn(requests);
        when(partnerDocumentDAO.create(anyLong(), any(),
                any(), anyLong(), any())).thenReturn(partnerDocument);

        // обычный документ
        service.uploadRequestDocument(REQ_ID_1, REQ_DATASOURCE_ID_1, MOCKED_FILE,
                PartnerApplicationDocumentType.SIGNED_APP_FORM, ACTION_ID);

        // проверка на кол-во документов
        verify(validatorService).checkDocumentsCount(any());

        // вызов в валидиатор для проверки загрузки документа НЕ переподписанного
        verify(validatorService).checkIfWritable(requests, Set.of());
        verify(validatorService, never()).checkIfAllowedResignWithPrograms(any());

        // вызов dao
        ArgumentCaptor<String> nameArgCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> sizeArgCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Function<Long, URL>> urlArgCaptor = ArgumentCaptor.forClass(Function.class);
        verify(partnerDocumentDAO).create(anyLong(), any(), nameArgCaptor.capture(), sizeArgCaptor.capture(),
                urlArgCaptor.capture());
        assertThat(nameArgCaptor.getValue()).isEqualTo(MOCKED_FILE.getOriginalFilename());
        assertThat(sizeArgCaptor.getValue()).isEqualTo(MOCKED_FILE.getSize());
        assertThat(urlArgCaptor.getValue().apply(DOC_ID_1)).hasToString(url.toString());

        // урл отражен в реквесте
        assertThat(req.getDocuments())
                .first()
                .satisfies(e -> assertThat(e.getUrl()).isEqualTo(url.toString()));
    }

    /**
     * Проверяем ключевые моменты сценария сохранения документа,
     * для документа типа {@link PartnerApplicationDocumentType#SIGNED_APP_PROGRAMS_UPDATE}.
     */
    @Test
    void test_uploadRequestDocument_when_passedDocument() throws IOException {
        PartnerApplicationDocumentsStorageService yamStorage = mock(PartnerApplicationDocumentsStorageService.class);
        URL url = new URL("http://some.url.here/file-uploaded-to-yam-storage");

        when(yamStorage.uploadFile(any(), anyLong(), anyLong())).thenReturn(Optional.of(url));
        when(yamStorage.getDownloadUrl(anyLong(), anyLong())).thenReturn(url);

        service.setPartnerApplicationDocumentsStorageService(yamStorage);

        PrepayRequest req = new PrepayRequest(REQ_ID_1, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.COMPLETED,
                DS_ID_1);
        List<PrepayRequest> requests = Collections.singletonList(req);
        PartnerDocument partnerDocument = new PartnerDocument(DOC_ID_1, REQ_ID_1,
                PartnerApplicationDocumentType.SIGNED_APP_PROGRAMS_UPDATE,
                MOCKED_FILE.getName(), MOCKED_FILE.getSize(), url.toString(), Instant.now());
        when(prepayRequestDao.find(any())).thenReturn(requests);
        when(partnerDocumentDAO.create(anyLong(), any(),
                any(), anyLong(), any())).thenReturn(partnerDocument);

        // переподписанное заявление
        service.uploadRequestDocument(REQ_ID_1, REQ_DATASOURCE_ID_1, MOCKED_FILE,
                PartnerApplicationDocumentType.SIGNED_APP_PROGRAMS_UPDATE, ACTION_ID);

        // проверка на кол-во документов
        verify(validatorService).checkDocumentsCount(any());

        // вызов в валидиатор для проверки загрузки документа переподписанного
        verify(validatorService, never()).checkIfWritable(any());
        verify(validatorService).checkIfAllowedResignWithPrograms(requests);

        // вызов dao
        ArgumentCaptor<String> nameArgCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> sizeArgCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Function<Long, URL>> urlArgCaptor = ArgumentCaptor.forClass(Function.class);
        verify(partnerDocumentDAO).create(anyLong(), any(), nameArgCaptor.capture(), sizeArgCaptor.capture(),
                urlArgCaptor.capture());
        assertThat(nameArgCaptor.getValue()).isEqualTo(MOCKED_FILE.getOriginalFilename());
        assertThat(sizeArgCaptor.getValue()).isEqualTo(MOCKED_FILE.getSize());
        assertThat(urlArgCaptor.getValue().apply(DOC_ID_1)).hasToString(url.toString());
        verify(partnerDocumentDAO).create(anyLong(), any(), nameArgCaptor.capture(), sizeArgCaptor.capture(),
                urlArgCaptor.capture());
        assertThat(nameArgCaptor.getValue()).isEqualTo(MOCKED_FILE.getOriginalFilename());
        assertThat(sizeArgCaptor.getValue()).isEqualTo(MOCKED_FILE.getSize());
        assertThat(urlArgCaptor.getValue().apply(DOC_ID_1).toString()).isEqualTo(url.toString());

        // урл отражен в реквесте
        assertThat(req.getDocuments())
                .first()
                .satisfies(e -> assertThat(e.getUrl()).isEqualTo(url.toString()));
    }


    /**
     * Ошибка, если yumStorage отсутствует.
     */
    @Test
    void test_uploadRequestDocument_when_passedDocumentAndNotStorage_then_throw() {
        when(prepayRequestDao.find(any())).thenReturn(Collections.emptyList());

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> service.uploadRequestDocument(
                        REQ_ID_1,
                        REQ_DATASOURCE_ID_1,
                        MOCKED_FILE,
                        PartnerApplicationDocumentType.SIGNED_APP_PROGRAMS_UPDATE,
                        ACTION_ID
                ))
                .withMessage("Will not upload file to storage at all");
    }

    /**
     * Ошибка, если не найдена заявка для указанного req_id.
     */
    @Test
    void test_uploadRequestDocument_when_passedDocumentAndNoRequestsFound_then_throw() throws IOException {
        PartnerApplicationDocumentsStorageService yamStorage = mock(PartnerApplicationDocumentsStorageService.class);
        service.setPartnerApplicationDocumentsStorageService(yamStorage);

        when(prepayRequestDao.find(any())).thenReturn(Collections.emptyList());
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> service.uploadRequestDocument(
                        REQ_ID_1,
                        REQ_DATASOURCE_ID_1,
                        MOCKED_FILE,
                        PartnerApplicationDocumentType.SIGNED_APP_PROGRAMS_UPDATE,
                        ACTION_ID
                ));
    }
}
