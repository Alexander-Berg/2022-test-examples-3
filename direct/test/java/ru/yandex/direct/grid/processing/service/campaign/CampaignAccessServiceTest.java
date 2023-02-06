package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.direct.core.entity.bs.export.queue.service.BsExportQueueService;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.util.GridCampaignTestUtil;
import ru.yandex.direct.grid.model.campaign.GdCampaignAccess;
import ru.yandex.direct.grid.model.campaign.GdCampaignAction;
import ru.yandex.direct.grid.model.campaign.GdCampaignServicedState;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignActionsHolder;
import ru.yandex.direct.grid.model.campaign.GdiCampaignStrategyName;
import ru.yandex.direct.grid.model.entity.campaign.strategy.GdStrategyExtractorFacade;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.AJAX_SET_AUTO_RESOURCES;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.ARCHIVE_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.COPY_CAMP_CLIENT;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.DELETE_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.DISABLE_WEEKLY_BUDGET;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.EDIT_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.EDIT_METRICA_COUNTERS;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.EDIT_WEEKLY_BUDGET;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.EXPORT_IN_EXCEL;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.LOOKUP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.OFFER_SERVICING;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.PAY;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.REMODERATE_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.RESET_FLIGHT_STATUS_APPROVE;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.RESUME_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.SHOW_CAMP_STAT;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.STOP_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.UNARCHIVE_CAMP;
import static ru.yandex.direct.grid.model.entity.campaign.strategy.GdStrategyExtractorHelper.STRATEGIES_EXTRACTORS_BY_TYPES;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(JUnitParamsRunner.class)
public class CampaignAccessServiceTest {
    private static final long TEST_OPERATOR_UID = 100500;

    @Mock
    private BsExportQueueService bsExportQueueService;

    @Spy
    private CampaignAccessHelper campaignAccessHelper;

    @Mock
    private PricePackageService pricePackageService;

    @Mock
    private FeatureService featureService;

    @InjectMocks
    private CampaignAccessService campaignAccessService;

    public static Collection<Object[]> parametersForTestGetAccess() {
        return Arrays.asList(new Object[][]{
                {
                        operator(RbacRole.CLIENT),
                        defaultCampaign()
                                .withShowing(false)
                                .withActions(new GdiCampaignActionsHolder()
                                .withActions(ImmutableSet.of(RESUME_CAMP, STOP_CAMP, ARCHIVE_CAMP,
                                        UNARCHIVE_CAMP, LOOKUP, EDIT_CAMP, COPY_CAMP_CLIENT, SHOW_CAMP_STAT,
                                        PAY, DELETE_CAMP))
                                .withCanEdit(true)
                                .withHasManager(false)
                                .withHasAgency(false)),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(true)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.RESUME_CAMP,
                                        GdCampaignAction.ARCHIVE_CAMP,
                                        GdCampaignAction.LOOKUP, GdCampaignAction.EDIT_METRICA_COUNTERS,
                                        GdCampaignAction.EDIT_CAMP, GdCampaignAction.COPY_CAMP_CLIENT,
                                        GdCampaignAction.AJAX_SET_AUTO_RESOURCES, GdCampaignAction.SET_AUTO_PRICE,
                                        GdCampaignAction.SHOW_CAMP_STAT, GdCampaignAction.PAY,
                                        GdCampaignAction.PAY_BY_CASH, GdCampaignAction.SET_BIDS))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        operator(RbacRole.CLIENT),
                        defaultCampaign()
                                .withShowing(true)
                                .withActions(new GdiCampaignActionsHolder()
                                        .withActions(ImmutableSet.of(RESUME_CAMP, AJAX_SET_AUTO_RESOURCES, STOP_CAMP,
                                                EDIT_METRICA_COUNTERS, ARCHIVE_CAMP, LOOKUP, COPY_CAMP_CLIENT,
                                                EDIT_CAMP, PAY, DELETE_CAMP))
                                        .withCanEdit(true)
                                        .withHasManager(false)
                                        .withHasAgency(false))
                                .withStatusPostModerate(CampaignStatusPostmoderate.NO)
                                .withSum(BigDecimal.ZERO)
                                .withSumToPay(BigDecimal.ZERO)
                                .withSumLast(BigDecimal.ZERO)
                                .withOrderId(0L),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(true)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.AJAX_SET_AUTO_RESOURCES,
                                        GdCampaignAction.STOP_CAMP, GdCampaignAction.EDIT_METRICA_COUNTERS,
                                        GdCampaignAction.LOOKUP,
                                        GdCampaignAction.COPY_CAMP_CLIENT, GdCampaignAction.EDIT_CAMP,
                                        GdCampaignAction.PAY, GdCampaignAction.DELETE_CAMP, GdCampaignAction.SET_BIDS))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        operator(RbacRole.CLIENT),
                        defaultCampaign()
                                .withShowing(false)
                                .withSumRest(RandomNumberUtils.nextPositiveBigDecimal())
                                .withActions(new GdiCampaignActionsHolder()
                                        .withActions(ImmutableSet.of(AJAX_SET_AUTO_RESOURCES, STOP_CAMP, RESUME_CAMP,
                                                EDIT_METRICA_COUNTERS, ARCHIVE_CAMP, LOOKUP, COPY_CAMP_CLIENT,
                                                EDIT_CAMP, PAY, DELETE_CAMP))
                                        .withCanEdit(true)
                                        .withHasManager(false)
                                        .withHasAgency(false))
                                .withSum(BigDecimal.ZERO)
                                .withSumToPay(BigDecimal.ZERO)
                                .withSumLast(BigDecimal.ZERO)
                                .withOrderId(0L),
                        true,
                        new GdCampaignAccess()
                                .withCanEdit(true)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.AJAX_SET_AUTO_RESOURCES,
                                        GdCampaignAction.RESUME_CAMP, GdCampaignAction.EDIT_METRICA_COUNTERS,
                                        GdCampaignAction.LOOKUP,
                                        GdCampaignAction.COPY_CAMP_CLIENT, GdCampaignAction.EDIT_CAMP,
                                        GdCampaignAction.PAY, GdCampaignAction.PAY_BY_CASH, GdCampaignAction.SET_BIDS))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        operator(RbacRole.CLIENT),
                        defaultCampaign().withActions(
                                new GdiCampaignActionsHolder()
                                        .withActions(ImmutableSet.of(AJAX_SET_AUTO_RESOURCES, STOP_CAMP,
                                                EDIT_METRICA_COUNTERS, ARCHIVE_CAMP, UNARCHIVE_CAMP, LOOKUP,
                                                COPY_CAMP_CLIENT, EDIT_CAMP, PAY, DELETE_CAMP, SHOW_CAMP_STAT))
                                        .withCanEdit(true)
                                        .withHasManager(false)
                                        .withHasAgency(false))
                                .withArchived(true),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.UNARCHIVE_CAMP, GdCampaignAction.LOOKUP,
                                        GdCampaignAction.SET_AUTO_PRICE, GdCampaignAction.SHOW_CAMP_STAT))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        operator(RbacRole.SUPER),
                        defaultCampaign().withActions(
                                new GdiCampaignActionsHolder()
                                        .withActions(ImmutableSet.of(EDIT_CAMP, STOP_CAMP, RESUME_CAMP, OFFER_SERVICING,
                                                SHOW_CAMP_STAT, ARCHIVE_CAMP, UNARCHIVE_CAMP, LOOKUP, COPY_CAMP_CLIENT,
                                                PAY, REMODERATE_CAMP))
                                        .withCanEdit(true)
                                        .withHasManager(false)
                                        .withHasAgency(false)),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(true)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.AJAX_SET_AUTO_RESOURCES,
                                        GdCampaignAction.STOP_CAMP, GdCampaignAction.EDIT_METRICA_COUNTERS,
                                        GdCampaignAction.SET_AUTO_PRICE,
                                        GdCampaignAction.LOOKUP, GdCampaignAction.COPY_CAMP_CLIENT,
                                        GdCampaignAction.REMODERATE_CAMP, GdCampaignAction.EDIT_CAMP,
                                        GdCampaignAction.PAY, GdCampaignAction.PAY_BY_CASH,
                                        GdCampaignAction.SHOW_CAMP_STAT, GdCampaignAction.SET_BIDS))
                                .withPseudoActions(ImmutableSet.of(GdCampaignAction.OFFER_SERVICING))
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        // hasManager = false, except OFFER_SERVICING in pseudoActions
                        operator(RbacRole.MANAGER),
                        defaultCampaign().withActions(
                                new GdiCampaignActionsHolder()
                                        .withActions(Set.of(OFFER_SERVICING))
                                        .withCanEdit(false)
                                        .withHasManager(false)
                                        .withHasAgency(false)),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(true)
                                .withActions(Collections.emptySet())
                                .withPseudoActions(Set.of(GdCampaignAction.OFFER_SERVICING))
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        // hasManager = true, except not OFFER_SERVICING in pseudoActions
                        operator(RbacRole.MANAGER),
                        defaultCampaign().withActions(
                                new GdiCampaignActionsHolder()
                                        .withActions(Set.of(OFFER_SERVICING))
                                        .withCanEdit(false)
                                        .withHasManager(true)
                                        .withHasAgency(false)),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(true)
                                .withActions(Collections.emptySet())
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        operator(RbacRole.CLIENT),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.YES)
                                .withShowing(true),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(true)
                                .withActions(ImmutableSet.of())
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        operator(RbacRole.CLIENT),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.NO)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.YES)
                                .withShowing(true),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.REMODERATE_CAMP))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        operator(RbacRole.CLIENT),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.NO)
                                .withShowing(true),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.REMODERATE_CAMP))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        operator(RbacRole.CLIENT),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.YES)
                                .withShowing(false),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.REMODERATE_CAMP,
                                        GdCampaignAction.ARCHIVE_CAMP))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        operator(RbacRole.CLIENT),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.NO)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.NO)
                                .withShowing(true),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.REMODERATE_CAMP))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        operator(RbacRole.SUPER),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.YES)
                                .withShowing(true),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.STOP_CAMP,
                                        GdCampaignAction.RESET_FLIGHT_STATUS_APPROVE))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                {
                        operator(RbacRole.SUPER),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.YES)
                                .withShowing(false),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.REMODERATE_CAMP,
                                        GdCampaignAction.RESET_FLIGHT_STATUS_APPROVE,
                                        GdCampaignAction.RESUME_CAMP, GdCampaignAction.ARCHIVE_CAMP))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                // Клиент может удалить кампанию с PriceFlightStatusApprove != Yes
                {
                        operator(RbacRole.CLIENT),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.YES)
                                .withShowing(false)
                                .withSum(BigDecimal.ZERO)
                                .withSumLast(BigDecimal.ZERO)
                                .withOrderId(0L),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.REMODERATE_CAMP,
                                        GdCampaignAction.DELETE_CAMP, GdCampaignAction.ARCHIVE_CAMP))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                // Клиент не может удалить кампанию с PriceFlightStatusApprove == Yes
                {
                        operator(RbacRole.CLIENT),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.YES)
                                .withShowing(false)
                                .withSum(BigDecimal.ZERO)
                                .withSumLast(BigDecimal.ZERO)
                                .withOrderId(0L),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.REMODERATE_CAMP,
                                        GdCampaignAction.ARCHIVE_CAMP))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                // Супер не может удалить кампанию с PriceFlightStatusApprove == Yes
                {
                        operator(RbacRole.SUPER),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.YES)
                                .withShowing(false)
                                .withSum(BigDecimal.ZERO)
                                .withSumLast(BigDecimal.ZERO)
                                .withOrderId(0L),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.REMODERATE_CAMP,
                                        GdCampaignAction.RESET_FLIGHT_STATUS_APPROVE,
                                        GdCampaignAction.RESUME_CAMP, GdCampaignAction.ARCHIVE_CAMP))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                // SUPER может сбросить статус апрува у некорректной прайсовой кампании
                {
                        operator(RbacRole.SUPER),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.NO)
                                .withShows(0L),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.RESET_FLIGHT_STATUS_APPROVE,
                                        GdCampaignAction.REMODERATE_CAMP, GdCampaignAction.STOP_CAMP))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                // CLIENT не может сбросить статус апрува у некорректной прайсовой кампании
                {
                        operator(RbacRole.CLIENT),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.NO)
                                .withShows(0L),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.REMODERATE_CAMP))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                // Нельзя сбросить статус апрува, если он уже NEW
                {
                        operator(RbacRole.CLIENT),
                        cpmPriceCampaign()
                                .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                                .withFlightStatusCorrect(PriceFlightStatusCorrect.NO)
                                .withShows(0L),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(ImmutableSet.of(GdCampaignAction.REMODERATE_CAMP))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },

                // Кампанию можно отправить на модерацию
                {
                        operator(RbacRole.CLIENT),
                        getCampaignReadyForModerate(),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(true)
                                .withNoActions(false)
                                .withActions(Set.of(GdCampaignAction.MODERATE_CAMP, GdCampaignAction.EDIT_CAMP,
                                        GdCampaignAction.SET_AUTO_PRICE, GdCampaignAction.SET_BIDS,
                                        GdCampaignAction.EDIT_METRICA_COUNTERS, GdCampaignAction.AJAX_SET_AUTO_RESOURCES
                                ))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                // Кампанию без баннеров нельзя отправить на модерацию
                {
                        operator(RbacRole.CLIENT),
                        getCampaignReadyForModerate()
                                .withHasBanners(false),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(true)
                                .withNoActions(false)
                                .withActions(Set.of(GdCampaignAction.EDIT_CAMP,
                                        GdCampaignAction.SET_AUTO_PRICE, GdCampaignAction.SET_BIDS,
                                        GdCampaignAction.EDIT_METRICA_COUNTERS, GdCampaignAction.AJAX_SET_AUTO_RESOURCES
                                ))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                // Медиапланер не может отправить кампанию на модерацию
                {
                        operator(RbacRole.MEDIA),
                        getCampaignReadyForModerate(),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(true)
                                .withNoActions(false)
                                .withActions(Set.of(GdCampaignAction.EDIT_CAMP,
                                        GdCampaignAction.SET_AUTO_PRICE, GdCampaignAction.SET_BIDS,
                                        GdCampaignAction.EDIT_METRICA_COUNTERS, GdCampaignAction.AJAX_SET_AUTO_RESOURCES
                                ))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                //недоступность мастера ставок для прайсовых (с фиксированным CPM) кампаний
                {
                        operator(RbacRole.CLIENT),
                        getCampaignReadyForModerate()
                                .withType(CampaignType.CPM_PRICE),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(true)
                                .withNoActions(false)
                                .withActions(Set.of(GdCampaignAction.EDIT_CAMP,
                                        GdCampaignAction.SET_AUTO_PRICE,
                                        GdCampaignAction.EDIT_METRICA_COUNTERS,
                                        GdCampaignAction.MODERATE_CAMP
                                ))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                //недоступность мастера ставок для архивных кампаний
                {
                        operator(RbacRole.CLIENT),
                        getCampaignReadyForModerate()
                                .withArchived(true),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(Set.of(GdCampaignAction.SET_AUTO_PRICE))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                //недоступность мастера ставок для внутренних кампаний
                {
                        operator(RbacRole.CLIENT),
                        getCampaignReadyForModerate()
                                .withType(CampaignType.INTERNAL_AUTOBUDGET),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(true)
                                .withNoActions(false)
                                .withActions(Set.of(GdCampaignAction.EDIT_CAMP,
                                        GdCampaignAction.SET_AUTO_PRICE,
                                        GdCampaignAction.EDIT_METRICA_COUNTERS,
                                        GdCampaignAction.MODERATE_CAMP
                                ))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                //недоступность оплаты для внутренних кампаний
                {
                        operator(RbacRole.CLIENT),
                        defaultCampaign()
                                .withType(CampaignType.INTERNAL_AUTOBUDGET),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(true)
                                .withNoActions(false)
                                .withActions(Set.of(GdCampaignAction.EDIT_CAMP,
                                        GdCampaignAction.SET_AUTO_PRICE,
                                        GdCampaignAction.EDIT_METRICA_COUNTERS,
                                        GdCampaignAction.SHOW_CAMP_STAT
                                ))
                                .withPseudoActions(Collections.emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                // Кампанию можно экспортировать в Excel
                {
                        operator(RbacRole.CLIENT),
                        defaultCampaign().withActions(
                                new GdiCampaignActionsHolder()
                                        .withActions(singleton(EXPORT_IN_EXCEL))
                                        .withCanEdit(false)
                                        .withHasManager(false)
                                        .withHasAgency(false)),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(singleton(GdCampaignAction.EXPORT_IN_EXCEL))
                                .withPseudoActions(emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                // Клиент может редактировать недельный бюджет у автобюджетной кампании
                {
                        operator(RbacRole.CLIENT),
                        defaultCampaign()
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_AVG_CLICK)
                                .withStrategyData(
                                        "{\"sum\": 500, \"name\": \"autobudget_avg_click\", \"avg_bid\": 0.09, " +
                                                "\"version\": 1}")
                                .withActions(
                                new GdiCampaignActionsHolder()
                                        .withActions(singleton(EDIT_WEEKLY_BUDGET))
                                        .withCanEdit(false)
                                        .withHasManager(false)
                                        .withHasAgency(false)),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(singleton(GdCampaignAction.EDIT_WEEKLY_BUDGET))
                                .withPseudoActions(emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
                // Клиент может отключить недельный бюджет у автобюджетной кампании
                {
                        operator(RbacRole.CLIENT),
                        defaultCampaign()
                                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_AVG_CLICK)
                                .withStrategyData(
                                        "{\"sum\": 500, \"name\": \"autobudget_avg_click\", \"avg_bid\": 0.09, " +
                                                "\"version\": 1}")
                                .withActions(
                                new GdiCampaignActionsHolder()
                                        .withActions(singleton(DISABLE_WEEKLY_BUDGET))
                                        .withCanEdit(false)
                                        .withHasManager(false)
                                        .withHasAgency(false)),
                        false,
                        new GdCampaignAccess()
                                .withCanEdit(false)
                                .withNoActions(false)
                                .withActions(singleton(GdCampaignAction.DISABLE_WEEKLY_BUDGET))
                                .withPseudoActions(emptySet())
                                .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                },
        });
    }

    private static GdiCampaign getCampaignReadyForModerate() {
        return defaultCampaign().withActions(
                new GdiCampaignActionsHolder()
                        .withActions(Set.of(EDIT_CAMP))
                        .withCanEdit(true)
                        .withHasManager(false)
                        .withHasAgency(false))
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withHasBanners(true)
                .withArchived(false);
    }

    private static User operator(RbacRole role) {
        return new User()
                .withUid(TEST_OPERATOR_UID)
                .withRole(role)
                .withSuperManager(false)
                .withDeveloper(false)
                .withIsReadonlyRep(false);
    }

    public static GdiCampaign defaultCampaign() {
        return GridCampaignTestUtil.defaultCampaign()
                .withSumRest(BigDecimal.ZERO);
    }

    private static GdiCampaign cpmPriceCampaign() {
        return defaultCampaign()
                .withType(CampaignType.CPM_PRICE)
                .withShows(1L)
                .withActions(
                        new GdiCampaignActionsHolder()
                                .withActions(ImmutableSet.of(STOP_CAMP, RESUME_CAMP, ARCHIVE_CAMP,
                                        UNARCHIVE_CAMP, REMODERATE_CAMP, DELETE_CAMP, RESET_FLIGHT_STATUS_APPROVE))
                                .withCanEdit(false)
                                .withHasManager(false)
                                .withHasAgency(false));
    }

    private static PricePackage defaultPricePackageNotAutoApproved() {
        return defaultPricePackage()
                .withCampaignAutoApprove(false);
    }

    @Before
    public void before() {
        campaignAccessHelper = new CampaignAccessHelper(new GdStrategyExtractorFacade(STRATEGIES_EXTRACTORS_BY_TYPES));
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Parameters
    @TestCaseName("{index}")
    public void testGetAccess(User operator, GdiCampaign campaign, Boolean isInQueue, GdCampaignAccess expectedAccess) {
        doReturn(isInQueue ? singleton(campaign.getId()) : Collections.emptySet())
                .when(bsExportQueueService)
                .getCampaignIdsGoingToBeSent(eq(Collections.singletonList(campaign.getId())));
        ClientId clientId = ClientId.fromLong(campaign.getClientId());
        Map<Long, PricePackage> pricePackageByCampaignId = campaign.getType() == CampaignType.CPM_PRICE ?
                Collections.singletonMap(campaign.getId(), defaultPricePackageNotAutoApproved()) :
                Collections.emptyMap();
        doReturn(pricePackageByCampaignId)
                .when(pricePackageService)
                .getPricePackageByCampaignIds(eq(clientId), eq(Collections.singletonList(campaign.getId())));

        Map<Long, GdCampaignAccess> campaignsAccess = campaignAccessService
                .getCampaignsAccess(operator, clientId, Collections.singletonList(campaign), Collections.emptyMap());

        assertThat(campaignsAccess.get(campaign.getId()))
                .is(matchedBy(beanDiffer(expectedAccess)));
    }
}
