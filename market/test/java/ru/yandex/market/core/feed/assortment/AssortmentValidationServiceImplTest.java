package ru.yandex.market.core.feed.assortment;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.API.UpdateTask;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.excel.ColumnSpec;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.feed.assortment.model.AssortmentFeedValidationRequest;
import ru.yandex.market.core.feed.assortment.model.AssortmentValidationInfo;
import ru.yandex.market.core.feed.assortment.model.AssortmentValidationType;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.feed.validation.model.FeedValidationLogbrokerEvent;
import ru.yandex.market.core.logbroker.samovar.SamovarEvent;
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.core.supplier.SupplierState;
import ru.yandex.market.core.supplier.model.SupplierOffer;
import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.core.tax.model.TaxSystem;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.common.test.util.ProtoTestUtil.getProtoMessageByJson;
import static ru.yandex.market.core.feed.validation.FeedValidationTestUtils.createUnitedValidationInfoWrapper;
import static ru.yandex.market.core.samovar.SamovarTestUtils.assertSamovarEvent;

/**
 * @author belmatter
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "environment.csv")
class AssortmentValidationServiceImplTest extends FunctionalTest {

    private static final Collection<ColumnSpec<SupplierOffer>> columnSpecs =
            new ArrayList<>(SupplierXlsHelper.GENERAL_COLUMN_SPECS);

    @Autowired
    private AssortmentValidationService assortmentValidationService;
    @Autowired
    private SupplierService defaultSupplierService;
    @Autowired
    @Qualifier("samovarLogbrokerService")
    private LogbrokerService samovarLogBrokerService;
    @Autowired
    @Qualifier("qParserLogBrokerService")
    private LogbrokerService qParserLogBrokerService;

    @BeforeAll
    static void beforeAll() {
        columnSpecs.addAll(SupplierXlsHelper.OFFER_COLUMN_SPECS);
        columnSpecs.addAll(SupplierXlsHelper.SUGGESTED_SKU_COLUMN_SPECS);
    }

    @BeforeEach
    void init() {
        RequestContextHolder.createNewContext();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    @DbUnitDataSet(
            before = "FeedValidationServiceTest.before.csv",
            after = "FeedValidationServiceTestPrices.after.csv"
    )
    void testPrepareFeedValidationPrices() {
        long supplierId = 775L;
        long uploadId = 11L;
        SupplierType supplierType = defaultSupplierService.getStateBySupplierId(supplierId)
                .map(SupplierState::getSupplierType)
                .get();
        AssortmentFeedValidationRequest validationRequest = new AssortmentFeedValidationRequest.Builder()
                .setPartnerId(supplierId)
                .setResource(RemoteResource.of("mds.url"))
                .setType(AssortmentValidationType.PRICES)
                .setTaxSystem(TaxSystem.OSN)
                .setUploadId(uploadId)
                .setSupplierType(supplierType)
                .build();

        assortmentValidationService.prepareValidation(validationRequest);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    @DbUnitDataSet(
            before = "FeedValidationInfoTest.before.csv"
    )
    void testGetValidationInfoRequest() {
        long supplierId = 775L;
        long validationId = 22L;
        long uploadId = 13L;
        SupplierType supplierType = defaultSupplierService.getStateBySupplierId(supplierId)
                .map(SupplierState::getSupplierType)
                .get();
        AssortmentFeedValidationRequest expectedValidationRequest = new AssortmentFeedValidationRequest.Builder()
                .setPartnerId(supplierId)
                .setResource(RemoteResource.of("mds.url"))
                .setType(AssortmentValidationType.PRICES)
                .setTaxSystem(TaxSystem.OSN)
                .setUploadId(uploadId)
                .setSupplierType(supplierType)
                .build();

        AssortmentValidationInfo feedValidationInfo = assortmentValidationService.getValidationInfo(validationId)
                .orElseThrow(() -> new AssertionError("Couldn't get feed validation info"));
        AssortmentFeedValidationRequest actualRequest = feedValidationInfo.request();

        Assertions.assertThat(actualRequest.partnerId())
                .isEqualTo(expectedValidationRequest.partnerId());
        Assertions.assertThat(actualRequest.type())
                .isEqualTo(expectedValidationRequest.type());
        Assertions.assertThat(actualRequest.supplierType())
                .isEqualTo(expectedValidationRequest.supplierType());
    }

    /**
     * Обновляем уже существующую запись
     */
    @Test
    @DbUnitDataSet(
            before = "SupplierValidationResult.before.csv",
            after = "SupplierValidationResultWhenNoteExist.after.csv"
    )
    void testSaveErrorValidationResultWhenNoteExist() {
        assortmentValidationService.saveErrorValidationResult(21, null);
    }

    /**
     * Обновляем еще не существующую запись
     */
    @Test
    @DbUnitDataSet(
            before = "SupplierValidationResult.before.csv",
            after = "SupplierValidationResultWhenNoteNotExist.after.csv"
    )
    void testSaveErrorValidationResultWhenNoteNotExist() {
        assortmentValidationService.saveErrorValidationResult(23, null);
    }

    @DisplayName("Проверка отправки запроса на валидацию фида через самовар")
    @ParameterizedTest(name = "{2}")
    @CsvSource({
            "43,1001,SHOP",
            "45,774,SUPPLIER",
    })
    @DbUnitDataSet(
            before = "AssortmentValidationService/before.csv"
    )
    void runValidation_samovar_correct(long id, long partnerId, CampaignType type)
            throws IOException, URISyntaxException {
        assortmentValidationService.runValidation(id);

        ArgumentCaptor<SamovarEvent> eventCaptor = ArgumentCaptor.forClass(SamovarEvent.class);
        Mockito.verify(samovarLogBrokerService, Mockito.times(1))
                .publishEvent(eventCaptor.capture());
        Mockito.verify(qParserLogBrokerService, Mockito.never())
                .publishEvent(any());

        assertSamovarEvent(eventCaptor.getValue(),
                createUnitedValidationInfoWrapper(id, partnerId, RemoteResource.of("http://ya"), type));
    }

    @DisplayName("Проверка отправки запроса на валидацию фида через самовар для синих с выключенной настройкой")
    @Test
    @DbUnitDataSet(
            before = "AssortmentValidationService/before.csv"
    )
    void runValidation_samovar_correct() throws IOException, URISyntaxException {
        runValidation_samovar_correct(45L, 774L, CampaignType.SUPPLIER);
    }

    @DisplayName("Проверка отправки запроса на валидацию фида через QParser")
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "42,shop",
            "46,supplier",
            "47,supplier",
    })
    @DbUnitDataSet(
            before = "AssortmentValidationService/before.csv"
    )
    void runValidation_qParser_correct(long id, String name) {
        assortmentValidationService.runValidation(id);

        Mockito.verify(samovarLogBrokerService, Mockito.never())
                .publishEvent(any());
        ArgumentCaptor<FeedValidationLogbrokerEvent> ec = ArgumentCaptor.forClass(FeedValidationLogbrokerEvent.class);
        Mockito.verify(qParserLogBrokerService, Mockito.times(1))
                .publishEvent(ec.capture());

        var feedParsingTask = getProtoMessageByJson(UpdateTask.FeedParsingTask.class,
                "AssortmentValidationService/proto/" + name + "." + id + ".json", getClass());

        ProtoTestUtil.assertThat(ec.getValue().getPayload().getFeedParsingTask())
                .ignoringFields("timestamp_")
                .isEqualTo(feedParsingTask);
    }
}
