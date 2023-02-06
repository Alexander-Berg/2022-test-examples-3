package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.MobileContentInfo;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.grid.core.util.GridCampaignTestUtil;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignAction;
import ru.yandex.direct.grid.processing.model.campaign.GdWallet;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignAccessHelper.DURATION_AFTER_LAST_SHOW_AND_STOP_TIME_FOR_ARCHIVE_CAMPAIGN;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignAccessHelperMethodTest {

    public static Collection<Object[]> parametersForCheckAllowArchive() {
        return Arrays.asList(new Object[][]{
                {
                        "allow archive campaign",
                        (Consumer<GdiCampaign>) (campaign) -> { //do nothing
                        },
                        true,
                },
                {
                        "archived campaign",
                        (Consumer<GdiCampaign>) (campaign) -> campaign.withArchived(true),
                        false,
                },
                {
                        "not stopped campaign",
                        (Consumer<GdiCampaign>) (campaign) -> campaign.withShowing(true),
                        false,
                },

                {
                        "campaign with lastShowTime=now()",
                        (Consumer<GdiCampaign>) (campaign) -> campaign.withLastShowTime(LocalDateTime.now()),
                        false,
                },
                {
                        "campaign with lastShowTime=null",
                        (Consumer<GdiCampaign>) (campaign) -> campaign.withLastShowTime(null),
                        true,
                },
                {
                        "campaign with lastShowTime=now()" +
                                " - DURATION_AFTER_LAST_SHOW_AND_STOP_TIME_FOR_ARCHIVE_CAMPAIGN - 1min",
                        (Consumer<GdiCampaign>) (campaign) -> campaign
                                .withLastShowTime(LocalDateTime.now()
                                        .minus(DURATION_AFTER_LAST_SHOW_AND_STOP_TIME_FOR_ARCHIVE_CAMPAIGN).minusMinutes(1)),
                        true,
                },
                {
                        "campaign with lastShowTime=now()" +
                                " - DURATION_AFTER_LAST_SHOW_AND_STOP_TIME_FOR_ARCHIVE_CAMPAIGN + 1min",
                        (Consumer<GdiCampaign>) (campaign) -> campaign
                                .withLastShowTime(LocalDateTime.now()
                                        .minus(DURATION_AFTER_LAST_SHOW_AND_STOP_TIME_FOR_ARCHIVE_CAMPAIGN).plusMinutes(1)),
                        false,
                },

                {
                        "campaign with stopTime=now()",
                        (Consumer<GdiCampaign>) (campaign) -> campaign.withStopTime(LocalDateTime.now()),
                        false,
                },
                {
                        "campaign with stopTime=now() and orderId=null",
                        (Consumer<GdiCampaign>) (campaign) -> campaign
                                .withStopTime(LocalDateTime.now())
                                .withOrderId(null),
                        true,
                },
                {
                        "campaign with stopTime=null",
                        (Consumer<GdiCampaign>) (campaign) -> campaign.withStopTime(null),
                        true,
                },
                {
                        "campaign with stopTime=now()" +
                                " - DURATION_AFTER_LAST_SHOW_AND_STOP_TIME_FOR_ARCHIVE_CAMPAIGN - 1min",
                        (Consumer<GdiCampaign>) (campaign) -> campaign
                                .withStopTime(LocalDateTime.now()
                                        .minus(DURATION_AFTER_LAST_SHOW_AND_STOP_TIME_FOR_ARCHIVE_CAMPAIGN).minusMinutes(1)),
                        true,
                },
                {
                        "campaign with stopTime=now()" +
                                " - DURATION_AFTER_LAST_SHOW_AND_STOP_TIME_FOR_ARCHIVE_CAMPAIGN + 1min",
                        (Consumer<GdiCampaign>) (campaign) -> campaign
                                .withStopTime(LocalDateTime.now()
                                        .minus(DURATION_AFTER_LAST_SHOW_AND_STOP_TIME_FOR_ARCHIVE_CAMPAIGN).plusMinutes(1)),
                        false,
                },

                {
                        "campaign has sumRest",
                        (Consumer<GdiCampaign>) (campaign) -> campaign.withSumRest(RandomNumberUtils.nextPositiveBigDecimal()),
                        false,
                }
        });
    }

    @SuppressWarnings("unused")
    private Object[] parametersForShowBsLink() {
        return new Object[][]{
                {"CLIENT, got OrderID", new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                        new User().withRole(RbacRole.CLIENT), new GdiCampaign().withOrderId(1L),
                        null, false, false), false
                },
                {"AGENCY, got OrderID", new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                        new User().withRole(RbacRole.AGENCY), new GdiCampaign().withOrderId(1L),
                        null, false, false), false
                },
                {"MANAGER, got OrderID", new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                        new User().withRole(RbacRole.MANAGER), new GdiCampaign().withOrderId(1L),
                        null, false, false), false
                },
                {"MEDIA, got OrderID", new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                        new User().withRole(RbacRole.MEDIA), new GdiCampaign().withOrderId(1L),
                        null, false, false), false
                },

                {"INTERNAL_AD_ADMIN, got OrderID",
                        new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                                new User().withRole(RbacRole.INTERNAL_AD_ADMIN), new GdiCampaign().withOrderId(1L),
                                null, false, false),
                        true
                },
                {"INTERNAL_AD_MANAGER, got OrderID",
                        new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                                new User().withRole(RbacRole.INTERNAL_AD_MANAGER), new GdiCampaign().withOrderId(1L),
                                null, false, false),
                        true
                },
                {"INTERNAL_AD_SUPERREADER, got OrderID",
                        new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                                new User().withRole(RbacRole.INTERNAL_AD_SUPERREADER),
                                new GdiCampaign().withOrderId(1L),
                                null, false, false),
                        true
                },
                {"PLACER, got OrderID", new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                        new User().withRole(RbacRole.PLACER), new GdiCampaign().withOrderId(1L),
                        null, false, false), true
                },
                {"SUPPORT, got OrderID", new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                        new User().withRole(RbacRole.SUPPORT), new GdiCampaign().withOrderId(1L),
                        null, false, false), true
                },
                {"SUPERREADER, got OrderID", new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                        new User().withRole(RbacRole.SUPERREADER), new GdiCampaign().withOrderId(1L),
                        null, false, false), true
                },
                {"SUPER, got OrderID", new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                        new User().withRole(RbacRole.SUPER), new GdiCampaign().withOrderId(1L),
                        null, false, false), true
                },
                {"SUPER, no OrderID", new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                        new User().withRole(RbacRole.SUPER), new GdiCampaign().withOrderId(0L), null,
                        false, false), false
                },
                {"LIMITED_SUPPORT, got OrderID",
                        new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                                new User().withRole(RbacRole.LIMITED_SUPPORT), new GdiCampaign().withOrderId(1L), null,
                                false, false), true
                },
                {"LIMITED_SUPPORT, no OrderID", new CampaignAccessHelper.ActionCheckNode(GdiCampaignAction.SHOW_BS_LINK,
                        new User().withRole(RbacRole.LIMITED_SUPPORT), new GdiCampaign().withOrderId(0L), null,
                        false, false), false
                },
        };
    }

    @SuppressWarnings("unused")
    private Object[] parametersForCanBeAutodeleted() {
        return new Object[][]{
                {"New text campaign, created 1 day ago", createActionCheckNode(new GdiCampaign()
                        .withType(CampaignType.TEXT)
                        .withOrderId(0L)
                        .withSum(BigDecimal.ZERO)
                        .withSumLast(BigDecimal.ZERO)
                        .withSumSpent(BigDecimal.ZERO)
                        .withSumToPay(BigDecimal.ZERO)
                        .withCreateTime(LocalDateTime.now().minusDays(1))
                ), false},
                {"New text campaign, created 15 days ago", createActionCheckNode(new GdiCampaign()
                        .withType(CampaignType.TEXT)
                        .withOrderId(0L)
                        .withSum(BigDecimal.ZERO)
                        .withSumLast(BigDecimal.ZERO)
                        .withSumSpent(BigDecimal.ZERO)
                        .withSumToPay(BigDecimal.ZERO)
                        .withCreateTime(LocalDateTime.now().minusDays(15))
                ), true},
                {"New text campaign, without create time", createActionCheckNode(new GdiCampaign()
                        .withType(CampaignType.TEXT)
                        .withOrderId(0L)
                        .withSum(BigDecimal.ZERO)
                        .withSumLast(BigDecimal.ZERO)
                        .withSumSpent(BigDecimal.ZERO)
                        .withSumToPay(BigDecimal.ZERO)
                        .withCreateTime(null)
                ), false},
                {"With OrderId, created 15 days ago", createActionCheckNode(new GdiCampaign()
                        .withType(CampaignType.TEXT)
                        .withOrderId(1L)
                        .withSum(BigDecimal.ZERO)
                        .withSumLast(BigDecimal.ZERO)
                        .withSumSpent(BigDecimal.ZERO)
                        .withSumToPay(BigDecimal.ZERO)
                        .withCreateTime(LocalDateTime.now().minusDays(15))
                ), false},
                {"With OrderId, created one day ago", createActionCheckNode(new GdiCampaign()
                        .withType(CampaignType.TEXT)
                        .withOrderId(1L)
                        .withSum(BigDecimal.ZERO)
                        .withSumLast(BigDecimal.ZERO)
                        .withSumSpent(BigDecimal.ZERO)
                        .withSumToPay(BigDecimal.ZERO)
                        .withCreateTime(LocalDateTime.now().minusDays(1))
                ), false},
                {"With Sum, created 15 days ago", createActionCheckNode(new GdiCampaign()
                        .withType(CampaignType.TEXT)
                        .withOrderId(0L)
                        .withSum(BigDecimal.ONE)
                        .withSumLast(BigDecimal.ZERO)
                        .withSumSpent(BigDecimal.ZERO)
                        .withSumToPay(BigDecimal.ZERO)
                        .withCreateTime(LocalDateTime.now().minusDays(15))
                ), false},
                {"With Sum, created one day ago", createActionCheckNode(new GdiCampaign()
                        .withType(CampaignType.TEXT)
                        .withOrderId(0L)
                        .withSum(BigDecimal.ONE)
                        .withSumLast(BigDecimal.ZERO)
                        .withSumSpent(BigDecimal.ZERO)
                        .withSumToPay(BigDecimal.ZERO)
                        .withCreateTime(LocalDateTime.now().minusDays(1))
                ), false},
                {"With Sum on wallet, created 15 days ago", createActionCheckNode(
                        new GdiCampaign()
                                .withType(CampaignType.TEXT)
                                .withOrderId(0L)
                                .withSum(BigDecimal.ZERO)
                                .withSumLast(BigDecimal.ZERO)
                                .withSumSpent(BigDecimal.ZERO)
                                .withSumToPay(BigDecimal.ZERO)
                                .withCreateTime(LocalDateTime.now().minusDays(15)),
                        new GdWallet()
                                .withSum(BigDecimal.ONE)
                ), false},
                {"With Sum on wallet, created one day ago", createActionCheckNode(
                        new GdiCampaign()
                                .withType(CampaignType.TEXT)
                                .withOrderId(0L)
                                .withSum(BigDecimal.ZERO)
                                .withSumLast(BigDecimal.ZERO)
                                .withSumSpent(BigDecimal.ZERO)
                                .withSumToPay(BigDecimal.ZERO)
                                .withCreateTime(LocalDateTime.now().minusDays(1)),
                        new GdWallet()
                                .withSum(BigDecimal.ONE)
                ), false},
                {"With Shows, created 15 days ago", createActionCheckNode(new GdiCampaign()
                        .withType(CampaignType.TEXT)
                        .withShows(1L)
                        .withOrderId(0L)
                        .withSum(BigDecimal.ZERO)
                        .withSumLast(BigDecimal.ZERO)
                        .withSumSpent(BigDecimal.ZERO)
                        .withSumToPay(BigDecimal.ZERO)
                        .withCreateTime(LocalDateTime.now().minusDays(15))
                ), false},
                {"Internal", createActionCheckNode(new GdiCampaign()
                        .withType(CampaignType.INTERNAL_FREE)
                        .withOrderId(0L)
                        .withSum(BigDecimal.ZERO)
                        .withSumLast(BigDecimal.ZERO)
                        .withSumSpent(BigDecimal.ZERO)
                        .withSumToPay(BigDecimal.ZERO)
                        .withCreateTime(LocalDateTime.now().minusDays(15))
                ), false},
        };
    }


    @SuppressWarnings("unused")
    private Object[] parametersForIsActiveIosCampaign() {
        return new Object[][]{
                {"Active Ios campaign", GridCampaignTestUtil.defaultCampaign()
                        .withType(CampaignType.MOBILE_CONTENT)
                        .withMobileContentInfo(new MobileContentInfo().withOsType(OsType.IOS))
                        .withArchived(false),
                        true},
                {"Archived Ios campaign", GridCampaignTestUtil.defaultCampaign()
                        .withType(CampaignType.MOBILE_CONTENT)
                        .withMobileContentInfo(new MobileContentInfo().withOsType(OsType.IOS))
                        .withArchived(true),
                        false},
                {"Active Android campaign", GridCampaignTestUtil.defaultCampaign()
                        .withType(CampaignType.MOBILE_CONTENT)
                        .withMobileContentInfo(new MobileContentInfo().withOsType(OsType.ANDROID))
                        .withArchived(false),
                        false},
        };
    }

    private static CampaignAccessHelper.ActionCheckNode createActionCheckNode(GdiCampaign gdiCampaign) {
        //noinspection ConstantConditions
        return new CampaignAccessHelper.ActionCheckNode(null, null, gdiCampaign, null, true, false);
    }

    private static CampaignAccessHelper.ActionCheckNode createActionCheckNode(GdiCampaign gdiCampaign,
                                                                              GdWallet wallet) {
        //noinspection ConstantConditions
        return new CampaignAccessHelper.ActionCheckNode(null, null, gdiCampaign, wallet, true, false);
    }

    public static GdiCampaign allowArchiveCampaign() {
        return GridCampaignTestUtil.defaultCampaign()
                .withArchived(false)
                .withShowing(false)
                .withOrderId(RandomNumberUtils.nextPositiveLong())
                .withSumRest(BigDecimal.ZERO);
    }

    @Test
    @Parameters(method = "parametersForCheckAllowArchive")
    @TestCaseName("allow archive: {0}, expectedResult = {2}")
    public void checkAllowArchive(@SuppressWarnings("unused") String description,
                                  Consumer<GdiCampaign> campaignConsumer, Boolean expectedResult) {
        GdiCampaign gdiCampaign = allowArchiveCampaign();
        campaignConsumer.accept(gdiCampaign);
        var actionCheckNode = createActionCheckNode(gdiCampaign);

        assertThat(CampaignAccessHelper.allowArchive(actionCheckNode))
                .isEqualTo(expectedResult);
    }

    @Test
    @Parameters(method = "parametersForShowBsLink")
    @TestCaseName("show BS link: {0}, expectedResult = {2}")
    public void checkAllowShowBsLink(@SuppressWarnings("unused") String description,
                                     CampaignAccessHelper.ActionCheckNode node, boolean show) {
        assertThat(CampaignAccessHelper.allowShowBsLink(node))
                .isEqualTo(show);
    }

    @Test
    @Parameters(method = "parametersForCanBeAutodeleted")
    @TestCaseName("can be autodeleted: {0}, expectedResult = {2}")
    public void checkCanBeAutodeleted(@SuppressWarnings("unused") String description,
                                      CampaignAccessHelper.ActionCheckNode node, boolean show) {
        assertThat(CampaignAccessHelper.canBeAutodeleted(node))
                .isEqualTo(show);
    }

    @Test
    @Parameters(method = "parametersForIsActiveIosCampaign")
    @TestCaseName("is active ios mobile campaign: {0}, expectedResult = {2}")
    public void checkIsActiveIosMobileCampaign(@SuppressWarnings("unused") String description, GdiCampaign campaign,
                                               boolean result) {
        assertThat(CampaignAccessHelper.isActiveIosMobileCampaign(campaign))
                .isEqualTo(result);
    }
}
