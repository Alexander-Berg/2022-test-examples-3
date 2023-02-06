package ru.yandex.market.pricelabs.tms.programs;

import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.Shop;
import ru.yandex.market.pricelabs.model.program.AdvProgramActivationRequest;
import ru.yandex.market.pricelabs.model.program.AdvProgramActivationRequestStatus;
import ru.yandex.market.pricelabs.model.program.ProgramService;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.tms.processing.ExecutorSources;

import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offer;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;

//CHECKSTYLE:OFF
@Slf4j
@ExtendWith(MockitoExtension.class)
public class ProgramsActivationTmsTest extends AbstractMonetizationTests {

    @Autowired
    private ProgramService programService;
    @Autowired
    protected ExecutorSources executors;

    @BeforeEach
    private void before() {
        executors.offers().clearTargetTable();
        testControls.resetShops();
    }

    @Test
    public void testOnProgramReset() {
        List<AdvProgramActivationRequest> activationRequests = getActivationRequests();

        advProgramActivationRequestYtScenarioExecutor.insert(activationRequests);

        AdvProgramActivationRequest requestToUpdate1 = activationRequests.get(1);
        AdvProgramActivationRequest requestToUpdate2 = activationRequests.get(2);

        programService.resetProgram();

        assertRequest(requestToUpdate1, AdvProgramActivationRequestStatus.READY_RESET);
        assertRequest(requestToUpdate2, AdvProgramActivationRequestStatus.READY_RESET);
        assertRequest(activationRequests.get(0), AdvProgramActivationRequestStatus.NEW);
        assertRequest(activationRequests.get(3), AdvProgramActivationRequestStatus.NEW);
        assertRequest(activationRequests.get(4), AdvProgramActivationRequestStatus.NEW);
    }

    @Test
    public void testOnProgramActivated() {
        List<AdvProgramActivationRequest> activationRequests = getActivationRequests();

        advProgramActivationRequestYtScenarioExecutor.insert(activationRequests);

        AdvProgramActivationRequest requestToUpdate = activationRequests.get(4);

        testControls.saveShop(shop(50, s -> s.setFeeds(Set.of(1L))));
        executors.offers().insert(List.of(
                offer("111", o -> {
                    o.setShop_id(50);
                    o.setFeed_id(1);
                    o.setStatus(Status.ACTIVE);
                    o.setCategory_id(123);
                    o.setMarket_category_id(123);
                    o.setUpdated_at(getInstant());
                })
        ));

        programService.onShopFinished(newShop(requestToUpdate.getPartner_id()), AutostrategyTarget.blue);

        assertRequest(requestToUpdate, AdvProgramActivationRequestStatus.READY);

        assertRequest(activationRequests.get(0), AdvProgramActivationRequestStatus.NEW);
        assertRequest(activationRequests.get(1), AdvProgramActivationRequestStatus.NEW);
        assertRequest(activationRequests.get(2), AdvProgramActivationRequestStatus.NEW);
        assertRequest(activationRequests.get(3), AdvProgramActivationRequestStatus.NEW);
    }

    private void assertRequest(AdvProgramActivationRequest requestToUpdate1,
                               AdvProgramActivationRequestStatus status) {
        AdvProgramActivationRequest updatedRequest = getRequest(requestToUpdate1);
        Assertions.assertEquals(status, updatedRequest.getStatus());
    }

    private Shop newShop(int shop_id) {
        Shop s = new Shop();
        s.setShop_id(shop_id);
        return s;
    }
}
//CHECKSTYLE:ON
