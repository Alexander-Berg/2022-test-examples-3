package ru.yandex.market.abo.core.problem.approve.checkstage;

import java.util.Set;

import Market.DataCamp.DataCampOfferMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.framework.message.MessageService;
import ru.yandex.market.abo.core.feed.model.DbFeedOfferDetails;
import ru.yandex.market.abo.core.feed.search.task.model.FeedSearchTask;
import ru.yandex.market.abo.core.feed.search.task.model.FeedSearchTaskStatus;
import ru.yandex.market.abo.core.message.Messages;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.ticket.ProblemService;
import ru.yandex.market.common.report.indexer.model.OfferDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 26/06/19.
 */
class EmptyFeedCheckStageTest {
    @InjectMocks
    EmptyFeedCheckStage emptyFeedCheckStage;
    @Mock
    ProblemService problemService;
    @Mock
    MessageService messageService;
    @Mock
    OfferDetails feed;
    @Mock
    Problem problem;
    @Mock
    Offer offer;
    @Mock
    FeedSearchTask task;
    @Mock
    DbFeedOfferDetails dbOffer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(dbOffer.getOfferHasGone()).thenReturn(false);
        when(task.getOffer()).thenReturn(dbOffer);
    }

    @SuppressWarnings("ConstantConditions")
    @ParameterizedTest
    @EnumSource(value = DataCampOfferMeta.DataSource.class, names = {"MARKET_ABO", "PULL_PARTNER_FEED", "MARKET_STOCK"})
    void testDisabled(DataCampOfferMeta.DataSource reason) {
        when(task.getStatus()).thenReturn(FeedSearchTaskStatus.OFFER_FOUND);
        when(dbOffer.getOfferDisabled()).thenReturn(true);
        when(dbOffer.getDisabledReasons()).thenReturn(Set.of(reason));
        ApproveWithTag result = emptyFeedCheckStage.approve(problem, null, task);
        assertEquals(reason == DataCampOfferMeta.DataSource.MARKET_ABO, result.isApprove());
        if (!result.isApprove()) {
            verify(problemService).logApproveProcess(eq(problem), eq(dbOffer), eq(ProblemStatus.DISAPPROVED), any(), any());
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void noFeed() {
        when(task.getStatus()).thenReturn(FeedSearchTaskStatus.OFFER_NOT_FOUND);
        when(task.getOffer()).thenReturn(null);
        ApproveWithTag approve = emptyFeedCheckStage.approve(problem, offer, task);
        assertFalse(approve.isApprove());
        verify(messageService).sendMessage(eq(Messages.AUTOAPPROVAL_OFFER_NOT_FOUND), anyMap());
        verify(problemService).logApproveProcess(eq(problem), eq(null), eq(ProblemStatus.DISAPPROVED), any(), any());
    }
}
