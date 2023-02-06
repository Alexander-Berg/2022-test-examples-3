package ru.yandex.market.clab.api.flow;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.clab.ControlledClock;
import ru.yandex.market.clab.api.BaseApiIntegrationTest;
import ru.yandex.market.clab.common.service.ShopSkuKey;
import ru.yandex.market.clab.common.service.good.GoodFilter;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.service.movement.MovementRepository;
import ru.yandex.market.clab.common.service.movement.MovementService;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementDirection;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementState;
import ru.yandex.market.clab.db.jooq.generated.enums.SupplierType;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Movement;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static ru.yandex.market.clab.api.flow.ApiTestUtils.getGood;
import static ru.yandex.market.clab.api.flow.ApiTestUtils.getLong;
import static ru.yandex.market.clab.api.flow.ApiTestUtils.getString;


/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 05.12.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SimpleFlowTest extends BaseApiIntegrationTest {
    private static final ShopSkuKey SNICKERS = new ShopSkuKey(6001, "snickers-supplier-id");
    private static final ShopSkuKey SAMSUNG_TV = new ShopSkuKey(8001, "samsung-tv-supplier-id");
    private static final String SNICKERS_ACTUAL_KEY = "snickers-actual";
    private static final String SNICKERS_STOCK_TYPE_KEY = "snickers-stock-";
    private static final String SNICKERS_STOCK_STOCK_UPDATED_DATE = "snickers-stock-updated-";
    private static final String PARTNER_ID_KEY = "partner-id";
    private static final String YANDEX_ID_KEY = "yandex-id";

    @Autowired
    private ControlledClock clock;

    @Autowired
    private GoodRepository goodRepository;

    @Autowired
    private MovementService movementService;

    @Autowired
    private MovementRepository movementRepository;

    @Value("${contentlab.warehouse.id:163}")
    private long contentLabWarehouseId;

    @Before
    public void before() {
        clock.unpause();
    }

    @Test
    @Ignore("To fix a parallel run - https://st.yandex-team.ru/MBO-24781")
    public void createAndCheckStatus() throws Exception {
        clock.pauseAndSet(LocalDateTime.of(2000, Month.APRIL, 10, 0, 0));

        /*
         * Create inbound
         */
        Map<String, Object> responseValues = new HashMap<>();
        send("/requests/simple-flow/createInbound.xml").andExpect(
            xml(read("/requests/simple-flow/createInbound-success.xml"), responseValues)
        );

        long movementId = getLong(responseValues, PARTNER_ID_KEY);
        String inboundId = getString(responseValues, YANDEX_ID_KEY);

        List<Good> goods = goodRepository.find(new GoodFilter().setMovementId(movementId));
        assertThat(goods).extracting(ShopSkuKey::ofGood).containsExactlyInAnyOrder(SNICKERS, SAMSUNG_TV);
        assertThat(inboundId).isEqualTo("inboundYandexId-301");


        send("/requests/simple-flow/getInboundDetails.xml", responseValues).andExpect(
            xml(read("/requests/simple-flow/getInboundDetails-success.xml"), responseValues)
        );
        assertThat(getLong(responseValues, SNICKERS_ACTUAL_KEY)).isEqualTo(0);


        send("/requests/simple-flow/getInboundsStatus.xml", responseValues).andExpect(
            xml(read("/requests/simple-flow/getInboundsStatus-success.xml"))
        );


        send("/requests/simple-flow/getStocks.xml").andExpect(
            xml(read("/requests/simple-flow/getStocks-success.xml"), responseValues)
        );
        assertThat(getLong(responseValues, snickersStockKey(StockType.ACCEPTANCE))).isEqualTo(1);
        assertThat(getLong(responseValues, snickersStockKey(StockType.AVAILABLE))).isEqualTo(0);
        assertThat(getLong(responseValues, snickersStockKey(StockType.FIT))).isEqualTo(0);
        String expectedDate = "2000-04-10T04:00:00+03:00";
        assertThat(getString(responseValues, snickersStockUpdated(StockType.ACCEPTANCE))).isEqualTo(expectedDate);
        assertThat(getString(responseValues, snickersStockUpdated(StockType.AVAILABLE))).isEqualTo(expectedDate);
        assertThat(getString(responseValues, snickersStockUpdated(StockType.FIT))).isEqualTo(expectedDate);

        send("/requests/simple-flow/getReferenceItems.xml").andExpect(
            xml(read("/requests/simple-flow/getReferenceItems-success.xml"), responseValues)
        );

        send("/requests/simple-flow/getExpirationItems.xml").andExpect(
            xml(read("/requests/simple-flow/getExpirationItems-success-empty.xml"), responseValues)
        );
        /*
         * Make snickers accepted
         */
        clock.tick(Duration.ofDays(7).plusMinutes(13));
        Good snickers = getGood(goods, SNICKERS);

        snickers.setState(GoodState.ACCEPTED);
        goodRepository.save(snickers);


        send("/requests/simple-flow/getInboundDetails.xml", responseValues).andExpect(
            xml(read("/requests/simple-flow/getInboundDetails-success.xml"), responseValues)
        );
        assertThat(getLong(responseValues, SNICKERS_ACTUAL_KEY)).isEqualTo(1);


        send("/requests/simple-flow/getStocks.xml").andExpect(
            xml(read("/requests/simple-flow/getStocks-success.xml"), responseValues)
        );
        assertThat(getLong(responseValues, snickersStockKey(StockType.ACCEPTANCE))).isEqualTo(0);
        assertThat(getLong(responseValues, snickersStockKey(StockType.AVAILABLE))).isEqualTo(0);
        assertThat(getLong(responseValues, snickersStockKey(StockType.FIT))).isEqualTo(1);

        expectedDate = "2000-04-17T04:13:00+03:00";
        assertThat(getString(responseValues, snickersStockUpdated(StockType.ACCEPTANCE))).isEqualTo(expectedDate);
        assertThat(getString(responseValues, snickersStockUpdated(StockType.AVAILABLE))).isEqualTo(expectedDate);
        assertThat(getString(responseValues, snickersStockUpdated(StockType.FIT))).isEqualTo(expectedDate);
    }

    private String snickersStockKey(StockType stockType) {
        return SNICKERS_STOCK_TYPE_KEY + stockType.name().toLowerCase();
    }

    private String snickersStockUpdated(StockType stockType) {
        return SNICKERS_STOCK_STOCK_UPDATED_DATE + stockType.name().toLowerCase();
    }

    @Test
    public void createOutbound() throws Exception {
        /*
         * Create inbound
         */
        Map<String, Object> responseValues = new HashMap<>();
        send("/requests/simple-flow/createInbound.xml").andExpect(
            xml(read("/requests/simple-flow/createInbound-success.xml"), responseValues)
        );

        long incomingMovementId = getLong(responseValues, PARTNER_ID_KEY);
        String inboundId = getString(responseValues, YANDEX_ID_KEY);

        List<Good> goods = goodRepository.find(new GoodFilter().setMovementId(incomingMovementId));
        assertThat(goods).extracting(ShopSkuKey::ofGood).containsExactlyInAnyOrder(SNICKERS, SAMSUNG_TV);
        assertThat(inboundId).isEqualTo("inboundYandexId-301");

        /*
         * Create outgoing and match goods
         */
        Good snickers = getGood(goods, SNICKERS);
        Good samsung = getGood(goods, SAMSUNG_TV);

        Movement movement = new Movement();
        movement.setDirection(MovementDirection.OUTGOING);
        movement.setSupplierType(SupplierType.FIRST_PARTY);
        movement.setWarehouseId(contentLabWarehouseId);
        movement = movementService.createMovement(movement);
        movement.setState(MovementState.PREPARING_TO_OUT);

        movementRepository.save(movement);
        samsung.setOutgoingMovementId(movement.getId());
        samsung.setState(GoodState.OUT);
        snickers.setOutgoingMovementId(movement.getId());
        goods = goodRepository.save(Arrays.asList(samsung, snickers));

        send("/requests/simple-flow/createOutbound.xml").andExpect(
            xml(read("/requests/simple-flow/createOutbound-error-no-suitable.xml"))
        );

        /*
         * Make all goods ready for sending
         */
        snickers = getGood(goods, SNICKERS);
        snickers.setState(GoodState.OUT);
        goodRepository.save(Collections.singleton(snickers));

        send("/requests/simple-flow/createOutbound.xml").andDo(print()).andExpect(
            xml(read("/requests/simple-flow/createOutbound-success.xml"), responseValues)
        );

        long partnerId = getLong(responseValues, PARTNER_ID_KEY);
        String outboundId = getString(responseValues, YANDEX_ID_KEY);

        assertThat(partnerId).isEqualTo(movement.getId());
        assertThat(outboundId).isEqualTo("outboundYandexId-301");
    }


    @Test
    @Ignore("To fix a parallel run - https://st.yandex-team.ru/MBO-24781")
    public void getStocks() throws Exception {
        clock.pauseAndSet(LocalDateTime.of(2000, Month.APRIL, 10, 0, 0));

        Map<String, Object> responseValues = new HashMap<>();
        send("/requests/simple-flow/createInbound.xml").andExpect(
            xml(read("/requests/simple-flow/createInbound-success.xml"), responseValues)
        );
        long movementId = getLong(responseValues, PARTNER_ID_KEY);
        List<Good> goods = goodRepository.find(new GoodFilter().setMovementId(movementId));

        Good snickers = getGood(goods, SNICKERS);

        clock.tick(Duration.ofDays(7).plusMinutes(13));

        snickers.setState(GoodState.SENT);
        goodRepository.save(snickers);

        send("/requests/simple-flow/getStocksAll.xml").andExpect(
            xml(read("/requests/simple-flow/getStocksAll-success.xml"), responseValues)
        );
        assertThat(getLong(responseValues, snickersStockKey(StockType.ACCEPTANCE))).isEqualTo(0);
        assertThat(getLong(responseValues, snickersStockKey(StockType.AVAILABLE))).isEqualTo(0);
        assertThat(getLong(responseValues, snickersStockKey(StockType.FIT))).isEqualTo(0);

        String expectedDate = "2000-04-17T04:13:00+03:00";
        assertThat(getString(responseValues, snickersStockUpdated(StockType.ACCEPTANCE))).isEqualTo(expectedDate);
        assertThat(getString(responseValues, snickersStockUpdated(StockType.AVAILABLE))).isEqualTo(expectedDate);
        assertThat(getString(responseValues, snickersStockUpdated(StockType.FIT))).isEqualTo(expectedDate);

        send("/requests/simple-flow/getStocksOutOfRange.xml").andExpect(
            xml(read("/requests/simple-flow/getStocksOutOfRange-response.xml"), responseValues)
        );
    }
}

