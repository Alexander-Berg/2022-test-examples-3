package ru.yandex.market.abo.tms.export.hidden;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.offer.hidden.HiddenOffer;
import ru.yandex.market.abo.api.entity.offer.hidden.HidingReason;
import ru.yandex.market.abo.core.export.hidden.DatacampWhiteParamsConverter;
import ru.yandex.market.abo.core.export.hidden.snapshot.white.HiddenOfferSnapshot;
import ru.yandex.market.abo.core.export.hidden.snapshot.white.HiddenOfferSnapshotService;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.model.ProblemTypeId;
import ru.yandex.market.abo.core.ticket.ProblemManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 12/06/19.
 */
class HiddenOffersWithDetailsProcessorTest {
    private static final Random RND = new Random();

    @InjectMocks
    private HiddenOffersWithDetailsProcessor hiddenProcessor;
    @Mock
    private HiddenOfferSnapshotService snapshotService;
    @Mock
    private ProblemManager problemManager;
    @Mock
    private DatacampWhiteParamsConverter datacampWhiteParamsConverter;

    private List<HiddenOfferSnapshot> currentSnapshot = new ArrayList<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        currentSnapshot.clear();
        when(snapshotService.findAll()).thenReturn(currentSnapshot);
        when(problemManager.loadProblem(anyLong())).thenReturn(someProblem());
    }

    @Test
    void newHidden() {
        List<HiddenOffer> hiddenOffers = List.of(someHiddenOffer());
        Map<Boolean, List<HiddenOfferSnapshot>> updates = hiddenProcessor.getHidingUpdates(hiddenOffers);
        assertNull(updates.get(false));
        assertEquals(from(hiddenOffers), updates.get(true));
    }

    @Test
    void doNotHideAnyMore() {
        List<HiddenOfferSnapshot> snapshot = from(List.of(someHiddenOffer()));
        currentSnapshot.addAll(snapshot);
        Map<Boolean, List<HiddenOfferSnapshot>> updates = hiddenProcessor.getHidingUpdates(Collections.emptyList());
        assertNull(updates.get(true));
        assertEquals(snapshot, updates.get(false));
    }

    @Test
    void diff() {
        HiddenOffer hiddenAndShouldStay = someHiddenOffer();
        HiddenOffer hiddenAndShouldBeShown = someHiddenOffer();
        HiddenOffer notHiddenAndShouldBe = someHiddenOffer();
        currentSnapshot.addAll(from(List.of(hiddenAndShouldStay, hiddenAndShouldBeShown)));
        List<HiddenOffer> hiddenFromLoaders = List.of(hiddenAndShouldStay, notHiddenAndShouldBe);

        Map<Boolean, List<HiddenOfferSnapshot>> updates = hiddenProcessor.getHidingUpdates(hiddenFromLoaders);
        assertEquals(from(List.of(notHiddenAndShouldBe)), updates.get(true));
        assertEquals(from(List.of(hiddenAndShouldBeShown)), updates.get(false));
    }

    @Test
    void statusChangedToApproved() {
        HiddenOffer hiddenOfferNew = someHiddenOffer();
        hiddenOfferNew.setStatusId(ProblemStatus.NEW.getId());
        HiddenOffer hiddenOfferApprovedHold = someHiddenOffer();
        hiddenOfferApprovedHold.setStatusId(ProblemStatus.APPROVED_HOLD.getId());
        HiddenOffer hiddenOfferApproved = someHiddenOffer();
        hiddenOfferApproved.setStatusId(ProblemStatus.APPROVED.getId());
        currentSnapshot.addAll(from(List.of(hiddenOfferApprovedHold, hiddenOfferApproved, hiddenOfferNew)));

        hiddenOfferApprovedHold.setStatusId(ProblemStatus.APPROVED.getId());
        hiddenOfferNew.setStatusId(ProblemStatus.APPROVED.getId());
        List<HiddenOffer> hiddenFromLoaders = List.of(hiddenOfferApproved, hiddenOfferApprovedHold, hiddenOfferNew);

        Map<Boolean, List<HiddenOfferSnapshot>> updates = hiddenProcessor.getHidingUpdates(hiddenFromLoaders);
        assertNull(updates.get(false));
        assertThat(updates.get(true)).containsExactlyInAnyOrderElementsOf(
                from(List.of(hiddenOfferApprovedHold, hiddenOfferNew)));
    }

    @Test
    void statusChangedFromApproved() {
        HiddenOffer hiddenOfferApprovedToResolved = someHiddenOffer();
        hiddenOfferApprovedToResolved.setStatusId(ProblemStatus.APPROVED.getId());
        HiddenOffer hiddenOfferApprovedToRejected = someHiddenOffer();
        hiddenOfferApprovedToRejected.setStatusId(ProblemStatus.APPROVED.getId());
        currentSnapshot.addAll(from(List.of(hiddenOfferApprovedToResolved, hiddenOfferApprovedToRejected)));

        hiddenOfferApprovedToResolved.setStatusId(ProblemStatus.RESOLVED.getId());
        hiddenOfferApprovedToRejected.setStatusId(ProblemStatus.REJECTED.getId());
        List<HiddenOffer> hiddenFromLoaders = List.of(hiddenOfferApprovedToResolved, hiddenOfferApprovedToRejected);

        Map<Boolean, List<HiddenOfferSnapshot>> updates = hiddenProcessor.getHidingUpdates(hiddenFromLoaders);
        assertNull(updates.get(false));
        assertThat(updates.get(true)).containsExactlyInAnyOrderElementsOf(
                from(List.of(hiddenOfferApprovedToResolved, hiddenOfferApprovedToRejected)));
    }

    @Test
    void nothingNew() {
        List<HiddenOffer> hiddenOffers = List.of(someHiddenOffer());
        currentSnapshot.addAll(from(hiddenOffers));

        Map<Boolean, List<HiddenOfferSnapshot>> updates = hiddenProcessor.getHidingUpdates(hiddenOffers);
        assertTrue(updates.isEmpty());
    }

    private static HiddenOffer someHiddenOffer() {
        return HiddenOffer.newBuilder("cmId")
                .feedId(RND.nextLong())
                .offerId(String.valueOf(RND.nextLong()))
                .shopId(RND.nextLong())
                .dbId(RND.nextLong())
                .hidingReason(HidingReason.BAD_QUALITY)
                .rgb(RND.nextInt(3))
                .build();
    }

    private static Problem someProblem() {
        return Problem.newBuilder()
                .problemTypeId(ProblemTypeId.PINGER_PRICE)
                .status(ProblemStatus.APPROVED)
                .build();
    }

    private static List<HiddenOfferSnapshot> from(List<HiddenOffer> hiddenOffers) {
        return hiddenOffers.stream().map(HiddenOfferSnapshot::new).collect(Collectors.toList());
    }
}
