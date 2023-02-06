package ru.yandex.market.clab.api.flow;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.yandex.market.clab.api.BaseApiIntegrationTest;
import ru.yandex.market.clab.common.service.ShopSkuKey;
import ru.yandex.market.clab.common.service.good.GoodFilter;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.service.movement.MovementRepository;
import ru.yandex.market.clab.common.service.movement.MovementService;
import ru.yandex.market.clab.common.service.planning.MovementPlanningService;
import ru.yandex.market.clab.common.service.requested.good.RequestedGoodRepository;
import ru.yandex.market.clab.common.service.requested.movement.RequestedMovementRepository;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementDirection;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodMovementState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedMovementState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Movement;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGood;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGoodMovement;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGoodLogistics;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedMovement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.clab.api.flow.ApiTestUtils.getGood;
import static ru.yandex.market.clab.api.flow.ApiTestUtils.getLong;
import static ru.yandex.market.clab.api.flow.ApiTestUtils.getString;


/**
 * @author anmalysh
 * @since 05.06.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class PlannedMovementTest extends BaseApiIntegrationTest {
    private static final ShopSkuKey SNICKERS = new ShopSkuKey(6001, "snickers-supplier-id");
    private static final ShopSkuKey SAMSUNG_TV = new ShopSkuKey(8001, "samsung-tv-supplier-id");
    private static final String PARTNER_ID_KEY = "partner-id";
    private static final String YANDEX_ID_KEY = "yandex-id";

    @Autowired
    private GoodRepository goodRepository;

    @Autowired
    private MovementService movementService;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private RequestedGoodRepository rgRepository;

    @Autowired
    private RequestedMovementRepository rmRepository;

    @Autowired
    private MovementPlanningService planningService;

    @Value("${contentlab.warehouse.id:163}")
    private long contentLabWarehouseId;

    @Test
    public void createInbound3P() throws Exception {

        RequestedMovement requestedMovement =
            createAndSaveRequestedMovement(MovementDirection.INCOMING, RequestedMovementState.IN_PROCESS);

        RequestedGood snickersRG = createAndSaveRequestedGood(
            requestedMovement, SNICKERS, null, RequestedGoodState.PLANNED, RequestedGoodMovementState.PLANNED);
        RequestedGood samsungRG = createAndSaveRequestedGood(
            requestedMovement, SAMSUNG_TV, null, RequestedGoodState.PLANNED, RequestedGoodMovementState.PLANNED);

        /*
         * Create first inbound
         */
        Map<String, Object> responseValues = new HashMap<>();
        send("/requests/simple-flow/createInbound3p-snickers-only.xml").andExpect(
            xml(read("/requests/simple-flow/createInbound-success.xml"), responseValues)
        );

        long movementId = getLong(responseValues, PARTNER_ID_KEY);
        String inboundId = getString(responseValues, YANDEX_ID_KEY);

        Movement movement = movementRepository.getById(movementId);
        List<Good> goods = goodRepository.find(new GoodFilter().setMovementId(movementId));

        assertThat(inboundId).isEqualTo("inboundYandexId-301");

        assertThat(goods).extracting(ShopSkuKey::ofGood).containsExactly(SNICKERS);
        Good snickers = getGood(goods, SNICKERS);
        assertThat(snickers).extracting(Good::getState).isEqualTo(GoodState.NEW);
        assertThat(movement).extracting(Movement::getState).isEqualTo(MovementState.NEW);
        assertThat(movement).extracting(Movement::getRequestedMovementId).isEqualTo(requestedMovement.getId());

        requestedMovement = rmRepository.getById(requestedMovement.getId());
        snickersRG = rgRepository.getById(snickersRG.getId());
        samsungRG = rgRepository.getById(samsungRG.getId());
        assertThat(requestedMovement).isNotNull();
        assertThat(requestedMovement).extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.IN_PROCESS);
        assertThat(snickersRG).isNotNull();
        assertThat(snickersRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.INCOMING);
        assertThat(snickersRG).extracting(RequestedGood::getGoodId)
            .isEqualTo(snickers.getId());
        assertThat(snickersRG).extracting(RequestedGood::getRequestedMovementId)
            .isNotNull();
        assertThat(samsungRG).isNotNull();
        assertThat(samsungRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.PLANNED);
        assertThat(samsungRG).extracting(RequestedGood::getGoodId)
            .isNull();
        assertThat(samsungRG).extracting(RequestedGood::getRequestedMovementId)
            .isNotNull();

        RequestedGoodMovement snickersRGM = rgRepository.getGoodMovement(requestedMovement.getId(), snickersRG.getId());
        RequestedGoodMovement samsungRGM = rgRepository.getGoodMovement(requestedMovement.getId(), samsungRG.getId());
        assertThat(snickersRGM).isNotNull();
        assertThat(snickersRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.IN_PROCESS);
        assertThat(samsungRGM).isNotNull();
        assertThat(samsungRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.PLANNED);

        /*
         * Create second inbound
         */
        responseValues = new HashMap<>();
        send("/requests/simple-flow/createInbound3p-samsung-only.xml").andExpect(
            xml(read("/requests/simple-flow/createInbound-success.xml"), responseValues)
        );

        movementService.acceptGoodManually(snickers.getId(), "some-barcode");

        movementId = getLong(responseValues, PARTNER_ID_KEY);
        inboundId = getString(responseValues, YANDEX_ID_KEY);

        movement = movementRepository.getById(movementId);
        goods = goodRepository.find(new GoodFilter().setMovementId(movementId));

        assertThat(inboundId).isEqualTo("inboundYandexId-302");

        assertThat(goods).extracting(ShopSkuKey::ofGood).containsExactly(SAMSUNG_TV);
        Good samsung = getGood(goods, SAMSUNG_TV);
        assertThat(samsung).extracting(Good::getState).isEqualTo(GoodState.NEW);
        assertThat(movement).extracting(Movement::getState).isEqualTo(MovementState.NEW);
        assertThat(movement).extracting(Movement::getRequestedMovementId).isEqualTo(requestedMovement.getId());

        requestedMovement = rmRepository.getById(requestedMovement.getId());
        snickersRG = rgRepository.getById(snickersRG.getId());
        samsungRG = rgRepository.getById(samsungRG.getId());
        assertThat(requestedMovement).isNotNull();
        assertThat(requestedMovement).extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.IN_PROCESS);
        assertThat(snickersRG).isNotNull();
        assertThat(snickersRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.PROCESSING);
        assertThat(snickersRG).extracting(RequestedGood::getGoodId)
            .isEqualTo(snickers.getId());
        assertThat(snickersRG).extracting(RequestedGood::getRequestedMovementId)
            .isNull();
        assertThat(samsungRG).isNotNull();
        assertThat(samsungRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.INCOMING);
        assertThat(samsungRG).extracting(RequestedGood::getGoodId)
            .isEqualTo(samsung.getId());
        assertThat(samsungRG).extracting(RequestedGood::getRequestedMovementId)
            .isNotNull();

        snickersRGM = rgRepository.getGoodMovement(requestedMovement.getId(), snickersRG.getId());
        samsungRGM = rgRepository.getGoodMovement(requestedMovement.getId(), samsungRG.getId());
        assertThat(snickersRGM).isNotNull();
        assertThat(snickersRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.ACCEPTED);
        assertThat(samsungRGM).isNotNull();
        assertThat(samsungRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.IN_PROCESS);

        movementService.acceptMovement(movement.getId());

        snickers = goodRepository.getById(snickers.getId());
        samsung = goodRepository.getById(samsung.getId());
        snickersRG = rgRepository.getById(snickersRG.getId());
        samsungRG = rgRepository.getById(samsungRG.getId());
        snickersRGM = rgRepository.getGoodMovement(requestedMovement.getId(), snickersRG.getId());
        samsungRGM = rgRepository.getGoodMovement(requestedMovement.getId(), samsungRG.getId());

        assertThat(snickers).extracting(Good::getState).isEqualTo(GoodState.ACCEPTED);
        assertThat(samsung).extracting(Good::getState).isEqualTo(GoodState.NOT_RECEIVED);
        assertThat(snickersRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.PROCESSING);
        assertThat(samsungRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.FAILED);
        assertThat(samsungRG).extracting(RequestedGood::getRequestedMovementId)
            .isNull();
        assertThat(snickersRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.ACCEPTED);
        assertThat(samsungRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.NOT_RECEIVED);

        movementService.cancelAcceptMovement(movement.getId());

        samsung = goodRepository.getById(samsung.getId());
        samsungRG = rgRepository.getById(samsungRG.getId());;
        samsungRGM = rgRepository.getGoodMovement(requestedMovement.getId(), samsungRG.getId());

        assertThat(samsung).extracting(Good::getState).isEqualTo(GoodState.NEW);
        assertThat(samsungRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.INCOMING);
        assertThat(samsungRG).extracting(RequestedGood::getRequestedMovementId)
            .isEqualTo(requestedMovement.getId());
        assertThat(samsungRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.IN_PROCESS);
    }

    @Test
    public void createOutboungNoMovement3P() throws Exception {

        RequestedMovement requestedMovement =
            createAndSaveRequestedMovement(MovementDirection.OUTGOING, RequestedMovementState.IN_PROCESS);

        Movement incomingMovement = createAndSaveMovement(MovementDirection.INCOMING, MovementState.PROCESSED);

        Good snickersG = createAndSaveGood(incomingMovement, null, SNICKERS, GoodState.PREPARED_TO_OUT);
        Good samsungG = createAndSaveGood(incomingMovement, null, SAMSUNG_TV, GoodState.PREPARED_TO_OUT);

        RequestedGood snickersRG = createAndSaveRequestedGood(
            requestedMovement, SNICKERS, snickersG,
            RequestedGoodState.PLANNED_OUTGOING, RequestedGoodMovementState.PLANNED);
        RequestedGood samsungRG = createAndSaveRequestedGood(
            requestedMovement, SAMSUNG_TV, samsungG,
            RequestedGoodState.PLANNED_OUTGOING, RequestedGoodMovementState.PLANNED);

        /*
         * Create first outbound
         */
        Map<String, Object> responseValues = new HashMap<>();
        send("/requests/simple-flow/createOutbound3p-snickers-only.xml").andExpect(
            xml(read("/requests/simple-flow/createOutbound-success.xml"), responseValues)
        );

        long movementId = getLong(responseValues, PARTNER_ID_KEY);
        String outboundId = getString(responseValues, YANDEX_ID_KEY);

        Movement movement = movementRepository.getById(movementId);
        List<Good> goods = goodRepository.find(new GoodFilter().setMovementId(movementId));

        assertThat(outboundId).isEqualTo("outboundYandexId-301");

        assertThat(goods).extracting(ShopSkuKey::ofGood).containsExactly(SNICKERS);
        Good snickers = getGood(goods, SNICKERS);
        assertThat(snickers).extracting(Good::getState).isEqualTo(GoodState.OUT);
        assertThat(movement).extracting(Movement::getState).isEqualTo(MovementState.SENDING);
        assertThat(movement).extracting(Movement::getRequestedMovementId).isEqualTo(requestedMovement.getId());

        requestedMovement = rmRepository.getById(requestedMovement.getId());
        snickersRG = rgRepository.getById(snickersRG.getId());
        samsungRG = rgRepository.getById(samsungRG.getId());
        assertThat(requestedMovement).isNotNull();
        assertThat(requestedMovement).extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.IN_PROCESS);
        assertThat(snickersRG).isNotNull();
        assertThat(snickersRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.OUTGOING);
        assertThat(snickersRG).extracting(RequestedGood::getGoodId)
            .isEqualTo(snickers.getId());
        assertThat(snickersRG).extracting(RequestedGood::getRequestedMovementId)
            .isNotNull();
        assertThat(samsungRG).isNotNull();
        assertThat(samsungRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.PLANNED_OUTGOING);
        assertThat(samsungRG).extracting(RequestedGood::getGoodId)
            .isEqualTo(samsungG.getId());
        assertThat(samsungRG).extracting(RequestedGood::getRequestedMovementId)
            .isNotNull();

        RequestedGoodMovement snickersRGM = rgRepository.getGoodMovement(requestedMovement.getId(), snickersRG.getId());
        RequestedGoodMovement samsungRGM = rgRepository.getGoodMovement(requestedMovement.getId(), samsungRG.getId());
        assertThat(snickersRGM).isNotNull();
        assertThat(snickersRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.IN_PROCESS);
        assertThat(samsungRGM).isNotNull();
        assertThat(samsungRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.PLANNED);

        /*
         * Create second inbound
         */
        responseValues = new HashMap<>();
        send("/requests/simple-flow/createOutbound3p-samsung-only.xml").andExpect(
            xml(read("/requests/simple-flow/createOutbound-success.xml"), responseValues)
        );

        movementService.sendMovement(movement.getId());

        movementId = getLong(responseValues, PARTNER_ID_KEY);
        outboundId = getString(responseValues, YANDEX_ID_KEY);

        movement = movementRepository.getById(movementId);
        goods = goodRepository.find(new GoodFilter().setMovementId(movementId));

        assertThat(outboundId).isEqualTo("outboundYandexId-302");

        assertThat(goods).extracting(ShopSkuKey::ofGood).containsExactly(SAMSUNG_TV);
        Good samsung = getGood(goods, SAMSUNG_TV);
        assertThat(samsung).extracting(Good::getState).isEqualTo(GoodState.OUT);
        assertThat(movement).extracting(Movement::getState).isEqualTo(MovementState.SENDING);
        assertThat(movement).extracting(Movement::getRequestedMovementId).isEqualTo(requestedMovement.getId());

        requestedMovement = rmRepository.getById(requestedMovement.getId());
        snickersRG = rgRepository.getById(snickersRG.getId());
        samsungRG = rgRepository.getById(samsungRG.getId());
        assertThat(requestedMovement).isNotNull();
        assertThat(requestedMovement).extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.IN_PROCESS);
        assertThat(snickersRG).isNotNull();
        assertThat(snickersRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.DONE);
        assertThat(snickersRG).extracting(RequestedGood::getGoodId)
            .isEqualTo(snickers.getId());
        assertThat(snickersRG).extracting(RequestedGood::getRequestedMovementId)
            .isNull();
        assertThat(samsungRG).isNotNull();
        assertThat(samsungRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.OUTGOING);
        assertThat(samsungRG).extracting(RequestedGood::getGoodId)
            .isEqualTo(samsungG.getId());
        assertThat(samsungRG).extracting(RequestedGood::getRequestedMovementId)
            .isNotNull();

        snickersRGM = rgRepository.getGoodMovement(requestedMovement.getId(), snickersRG.getId());
        samsungRGM = rgRepository.getGoodMovement(requestedMovement.getId(), samsungRG.getId());
        assertThat(snickersRGM).isNotNull();
        assertThat(snickersRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.SENT);
        assertThat(samsungRGM).isNotNull();
        assertThat(samsungRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.IN_PROCESS);
    }

    @Test
    public void createOutboundWithMovement3P() throws Exception {

        Movement incomingMovement = createAndSaveMovement(MovementDirection.INCOMING, MovementState.PROCESSED);
        Movement outgoingMovement = createAndSaveMovement(MovementDirection.OUTGOING, MovementState.PREPARING_TO_OUT);

        Good snickersG = createAndSaveGood(incomingMovement, outgoingMovement, SNICKERS, GoodState.OUT);
        Good samsungG = createAndSaveGood(incomingMovement, outgoingMovement, SAMSUNG_TV, GoodState.OUT);

        RequestedGood snickersRG = createAndSaveRequestedGood(SNICKERS, snickersG, RequestedGoodState.PROCESSED);
        RequestedGood samsungRG = createAndSaveRequestedGood(SAMSUNG_TV, samsungG, RequestedGoodState.PROCESSED);

        long warehouseId = 163L;
        rgRepository.upsertStocks(
            ImmutableList.of(
                new RequestedGoodLogistics()
                    .setRequestedGoodId(snickersRG.getId())
                    .setFitCount(1)
                    .setWarehouseId(warehouseId),
                new RequestedGoodLogistics()
                    .setRequestedGoodId(samsungRG.getId())
                    .setFitCount(1)
                    .setWarehouseId(warehouseId)
            )
        );

        planningService.planMovement(outgoingMovement.getId());

        outgoingMovement = movementRepository.getById(outgoingMovement.getId());
        RequestedMovement requestedMovement = rmRepository.getById(outgoingMovement.getRequestedMovementId());

        RequestedGoodMovement snickersRGM = rgRepository.getGoodMovement(requestedMovement.getId(), snickersRG.getId());
        RequestedGoodMovement samsungRGM = rgRepository.getGoodMovement(requestedMovement.getId(), samsungRG.getId());

        assertThat(snickersRGM).isNotNull();
        assertThat(snickersRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.NEW);
        assertThat(samsungRGM).isNotNull();
        assertThat(samsungRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.NEW);

        snickersRGM.setState(RequestedGoodMovementState.REQUESTED);
        samsungRGM.setState(RequestedGoodMovementState.REQUESTED);
        rgRepository.updateGoodMovement(snickersRGM);
        rgRepository.updateGoodMovement(samsungRGM);

        snickersRG = rgRepository.getById(snickersRG.getId());
        samsungRG = rgRepository.getById(samsungRG.getId());
        assertThat(requestedMovement).isNotNull();
        assertThat(requestedMovement).extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.PLANNED);
        assertThat(snickersRG).isNotNull();
        assertThat(snickersRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.PLANNED_OUTGOING);
        assertThat(snickersRG).extracting(RequestedGood::getGoodId)
            .isEqualTo(snickersG.getId());
        assertThat(snickersRG).extracting(RequestedGood::getRequestedMovementId)
            .isNotNull();
        assertThat(samsungRG).isNotNull();
        assertThat(samsungRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.PLANNED_OUTGOING);
        assertThat(samsungRG).extracting(RequestedGood::getGoodId)
            .isEqualTo(samsungG.getId());
        assertThat(samsungRG).extracting(RequestedGood::getRequestedMovementId)
            .isNotNull();
        /*
         * Create partial outbound
         */
        Map<String, Object> responseValues = new HashMap<>();
        send("/requests/simple-flow/createOutbound3p-snickers-only.xml").andExpect(
            xml(read("/requests/simple-flow/createOutbound-success.xml"), responseValues)
        );

        long movementId = getLong(responseValues, PARTNER_ID_KEY);
        String outboundId = getString(responseValues, YANDEX_ID_KEY);

        Movement movement = movementRepository.getById(movementId);
        List<Good> goods = goodRepository.find(new GoodFilter().setMovementId(movementId));

        assertThat(outboundId).isEqualTo("outboundYandexId-301");

        assertThat(goods).extracting(ShopSkuKey::ofGood).containsExactlyInAnyOrder(SNICKERS, SAMSUNG_TV);
        snickersG = getGood(goods, SNICKERS);
        samsungG = getGood(goods, SAMSUNG_TV);
        assertThat(snickersG).extracting(Good::getState).isEqualTo(GoodState.OUT);
        assertThat(samsungG).extracting(Good::getState).isEqualTo(GoodState.NOT_IN_OUTGOING);
        assertThat(movement).extracting(Movement::getState).isEqualTo(MovementState.SENDING);
        assertThat(movement).extracting(Movement::getRequestedMovementId).isNotNull();

        requestedMovement = rmRepository.getById(outgoingMovement.getRequestedMovementId());
        snickersRG = rgRepository.getById(snickersRG.getId());
        samsungRG = rgRepository.getById(samsungRG.getId());
        assertThat(requestedMovement).isNotNull();
        assertThat(requestedMovement).extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.PLANNED);
        assertThat(snickersRG).isNotNull();
        assertThat(snickersRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.OUTGOING);
        assertThat(snickersRG).extracting(RequestedGood::getGoodId)
            .isEqualTo(snickersG.getId());
        assertThat(snickersRG).extracting(RequestedGood::getRequestedMovementId)
            .isNotNull();
        assertThat(samsungRG).isNotNull();
        assertThat(samsungRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.PLANNED_OUTGOING);
        assertThat(samsungRG).extracting(RequestedGood::getGoodId)
            .isEqualTo(samsungG.getId());
        assertThat(samsungRG).extracting(RequestedGood::getRequestedMovementId)
            .isNotNull();

        snickersRGM = rgRepository.getGoodMovement(requestedMovement.getId(), snickersRG.getId());
        samsungRGM = rgRepository.getGoodMovement(requestedMovement.getId(), samsungRG.getId());
        assertThat(snickersRGM).isNotNull();
        assertThat(snickersRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.IN_PROCESS);
        assertThat(samsungRGM).isNotNull();
        assertThat(samsungRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.REQUESTED);

        planningService.removeFromOutgoing(movementId, samsungG.getWhBarcode());

        samsungG = goodRepository.getById(samsungG.getId());
        assertThat(samsungG).extracting(Good::getState).isEqualTo(GoodState.PREPARED_TO_OUT);
        assertThat(samsungG).extracting(Good::getOutgoingMovementId).isNull();

        samsungRG = rgRepository.getById(samsungRG.getId());
        assertThat(samsungRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.PROCESSED);
        assertThat(samsungRG).extracting(RequestedGood::getRequestedMovementId)
            .isNull();

        samsungRGM = rgRepository.getGoodMovement(requestedMovement.getId(), samsungRG.getId());
        assertThat(samsungRGM).isNotNull();
        assertThat(samsungRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.REJECTED);

        movementService.sendMovement(movementId);

        snickersG = goodRepository.getById(snickersG.getId());
        assertThat(snickersG).extracting(Good::getState).isEqualTo(GoodState.SENT);
        assertThat(snickersG).extracting(Good::getOutgoingMovementId).isNotNull();

        snickersRG = rgRepository.getById(snickersRG.getId());
        assertThat(snickersRG).extracting(RequestedGood::getState)
            .isEqualTo(RequestedGoodState.DONE);
        assertThat(snickersRG).extracting(RequestedGood::getRequestedMovementId)
            .isNull();

        snickersRGM = rgRepository.getGoodMovement(requestedMovement.getId(), snickersRG.getId());
        assertThat(snickersRGM).isNotNull();
        assertThat(snickersRGM).extracting(RequestedGoodMovement::getState)
            .isEqualTo(RequestedGoodMovementState.SENT);
    }

    private RequestedMovement createAndSaveRequestedMovement(
        MovementDirection direction, RequestedMovementState state) {

        RequestedMovement movement = RandomTestUtils.randomObject(RequestedMovement.class, "id", "modifiedDate");
        movement.setDirection(direction);
        movement.setState(state);
        movement.setWarehouseToId(direction == MovementDirection.INCOMING ? 163L : 145L);
        movement.setWarehouseFromId(direction == MovementDirection.INCOMING ? 145L : 163L);
        return rmRepository.save(movement);
    }

    private RequestedGood createAndSaveRequestedGood(ShopSkuKey key, Good g, RequestedGoodState state) {
        return createAndSaveRequestedGood(null, key, g, state, null);
    }

    private RequestedGood createAndSaveRequestedGood(
        RequestedMovement movement, ShopSkuKey key, Good g,
        RequestedGoodState state, RequestedGoodMovementState movementState) {

        RequestedGood good = RandomTestUtils.randomObject(RequestedGood.class,
            "id", "modifiedDate", "defectType");
        good.setSupplierId(key.getSupplierId());
        good.setSupplierSkuId(key.getSupplierSkuId());
        good.setState(state);
        if (movement != null) {
            good.setRequestedMovementId(movement.getId());
        } else {
            good.setRequestedMovementId(null);
        }
        good.setGoodId(g == null ? null : g.getId());
        RequestedGood result = rgRepository.save(good);
        if (movement != null) {
            RequestedGoodMovement rgm = new RequestedGoodMovement()
                .setRequestedMovementId(movement.getId())
                .setRequestedGoodId(result.getId())
                .setState(movementState);
            rgRepository.createGoodMovement(rgm);
        }
        return result;
    }

    private Movement createAndSaveMovement(
        MovementDirection direction, MovementState state) {

        Movement movement = RandomTestUtils.randomObject(Movement.class, "id", "modifiedDate");
        movement.setDirection(direction);
        movement.setWarehouseId(145L);
        movement.setState(state);
        movement.setRequestedMovementId(null);
        return movementRepository.save(movement);
    }

    private Good createAndSaveGood(
        Movement incomingMovement, Movement outgoingMovement, ShopSkuKey key, GoodState state) {

        Good good = RandomTestUtils.randomObject(Good.class,
            "id", "modifiedDate", "incomingMovementId", "outgoingMovementId", "defectType", "cartId");
        if (incomingMovement != null) {
            good.setIncomingMovementId(incomingMovement.getId());
        }
        if (outgoingMovement != null) {
            good.setOutgoingMovementId(outgoingMovement.getId());
        }
        good.setSupplierId(key.getSupplierId());
        good.setSupplierSkuId(key.getSupplierSkuId());
        good.setState(state);
        return goodRepository.save(good);
    }
}

