package ru.yandex.market.delivery.transport_manager.facade.xdoc;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.Supplier;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.enums.XDocMerchantType;
import ru.yandex.market.delivery.transport_manager.domain.enums.XDocToDcMarketSchemeReason;
import ru.yandex.market.delivery.transport_manager.dto.xdoc.XDocMerchantData;
import ru.yandex.market.delivery.transport_manager.dto.xdoc.XDocRequestStatus;
import ru.yandex.market.delivery.transport_manager.facade.xdoc.dc.XDocDcTransportationFacade;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TagMapper;
import ru.yandex.market.delivery.transport_manager.util.XDocTestingConstants;

import static org.mockito.Mockito.when;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"}
)
@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
class XDocDcTransportationFacadeTest extends AbstractContextualTest {

    @Autowired
    TagMapper tagMapper;
    @Autowired
    XDocDcTransportationFacade creator;

    private static final XDocMerchantData MARKET =
        new XDocMerchantData(XDocMerchantType.MARKET);
    private static final XDocMerchantData LMS = new XDocMerchantData(
        XDocMerchantType.LMS,
        XDocToDcMarketSchemeReason.NONE,
        4L,
        5L,
        100
    );

    @BeforeEach
    void setUp() {
        clock.setFixed(
            LocalDateTime.of(2021, 5, 1, 20, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
    }

    @DisplayName("Создание перемещения 3p от мерча в РЦ")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_pallets_to_dc_transportation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_shop_to_rc_transportation_tag_3p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createSupplyToDcIfNecessary3p() {
        creator.createSupplyToDcIfNecessary(
            XDocRequestStatus.ACCEPTED_BY_SERVICE,
            XDocTestingConstants.X_DOC_CREATE_DATA_3P,
            MARKET,
            null
        );
    }

    @DisplayName("Создание перемещения 1p от мерча в РЦ")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_pallets_to_dc_transportation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_shop_to_rc_transportation_tag_1p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createSupplyToDcIfNecessary1p() {
        creator.createSupplyToDcIfNecessary(
            XDocRequestStatus.WAITING_FOR_CONFIRMATION,
            XDocTestingConstants.X_DOC_CREATE_DATA_1P,
            MARKET,
            new Supplier("1", "test_real_supplier")
        );
        tagMapper.getByTransportationIds(List.of(1L));
    }

    @DisplayName("Создание перемещения Break Bulk XDock от мерча в РЦ")
    @DatabaseSetup("/repository/transportation/xdoc_to_ff_transportations_break_bulk_xdock.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/xdoc_to_ff_transportations_break_bulk_xdock.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/task/no_tasks.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createSupplyToDcIfNecessaryBreakBulkXdock() {
        creator.createSupplyToDcIfNecessary(
            XDocRequestStatus.WAITING_FOR_CONFIRMATION,
            XDocTestingConstants.X_DOC_CREATE_DATA_BREAK_BULK_XDOCK.toBuilder()
                .requestId(1L)
                .build(),
            MARKET,
            new Supplier("1", "test_real_supplier")
        );
    }

    @DisplayName("Не создаём перемещения 1p от мерча в РЦ, так как в статусе ACCEPTED_BY_SERVICE уже не надо")
    @ExpectedDatabase(
        value = "/repository/transportation/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createSupplyToDcIfNecessary1pDoNothing() {
        creator.createSupplyToDcIfNecessary(
            XDocRequestStatus.ACCEPTED_BY_SERVICE,
            XDocTestingConstants.X_DOC_CREATE_DATA_1P,
            MARKET,
            null
        );
    }

    @DisplayName(
        "Не создаём перемещения 1p от мерча в РЦ, так как в статусе "
            + "ACCEPTED_BY_SERVICE уже не надо. Проставляем тег confirmed"
    )
    @DatabaseSetup({
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_shop_to_ff_and_dc_transportations.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_shop_to_rc_transportation_tag_1p_accepted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createSupplyToDcIfNecessary1pSetConfirmed() {
        when(propertyService.getBoolean(TmPropertyKey.ENABLE_SENDING_CONFIRMED_INBOUNDS)).
            thenReturn(true);

        creator.createSupplyToDcIfNecessary(
            XDocRequestStatus.ACCEPTED_BY_SERVICE,
            XDocTestingConstants.X_DOC_CREATE_DATA_1P.toBuilder().requestId(1L).build(),
            MARKET,
            null
        );
    }

    @DisplayName("Создание перемещения от мерча в РЦ")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_pallets_to_dc_transportation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_shop_to_rc_transportation_tag_1p_validated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void persistPalletsToDCTransportation() {
        creator.persistPalletsToDCTransportation(
            XDocTestingConstants.X_DOC_CREATE_DATA_1P,
            MARKET,
            XDocRequestStatus.VALIDATED,
            null
        );
    }

    @Test
    void checkValidStatus() {
        creator
            .checkValidStatus(XDocTestingConstants.X_DOC_CREATE_DATA_1P, XDocRequestStatus.WAITING_FOR_CONFIRMATION);
        creator
            .checkValidStatus(XDocTestingConstants.X_DOC_CREATE_DATA_1P, XDocRequestStatus.VALIDATED);
        creator
            .checkValidStatus(XDocTestingConstants.X_DOC_CREATE_DATA_3P, XDocRequestStatus.ACCEPTED_BY_SERVICE);
    }

    @Test
    void is1pStatusAccepted() {
        softly.assertThat(creator
            .is1pStatusAccepted(XDocRequestStatus.ACCEPTED_BY_SERVICE, XDocTestingConstants.X_DOC_CREATE_DATA_1P))
            .isTrue();
    }

    @Test
    void is1pStatusAcceptedFalseByStatus() {
        softly.assertThat(creator
            .is1pStatusAccepted(XDocRequestStatus.VALIDATED, XDocTestingConstants.X_DOC_CREATE_DATA_1P)).isFalse();
    }

    @Test
    void is1pStatusAcceptedFalseBySupplyType() {
        softly.assertThat(creator
            .is1pStatusAccepted(XDocRequestStatus.ACCEPTED_BY_SERVICE, XDocTestingConstants.X_DOC_CREATE_DATA_3P))
            .isFalse();
    }

    @Test
    @DisplayName("Создание перемещения 1p от партнёра в LMS в РЦ")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_pallets_to_dc_lms_transportation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/tag/after/xdoc_shop_to_rc_transportation_tag_1p_lms.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testFirstPartyLmsCreation() {
        creator.createSupplyToDcIfNecessary(
            XDocRequestStatus.WAITING_FOR_CONFIRMATION,
            XDocTestingConstants.X_DOC_CREATE_DATA_1P,
            LMS,
            null
        );
    }
}
