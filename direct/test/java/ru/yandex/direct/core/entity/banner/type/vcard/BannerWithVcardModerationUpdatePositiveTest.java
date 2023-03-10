package ru.yandex.direct.core.entity.banner.type.vcard;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.BannerWithVcard;
import ru.yandex.direct.core.entity.banner.model.BannerWithVcardModeration;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithVcard;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusPhoneFlagModerate;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.draftTextBanner;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithVcardModerationUpdatePositiveTest extends
        BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithVcard> {

    public static final LocalDateTime DEFAULT_LAST_DISSOCIATION_DATE = now().minusDays(1).withNano(0);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public TestModerationRepository testModerationRepository;

    @Autowired
    public VcardRepository vcardRepository;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BannerProvider bannerProvider;

    @Parameterized.Parameter(2)
    public ModerationMode moderationMode;

    @Parameterized.Parameter(3)
    public BannerModelChangesProvider modelChangesProvider;

    @Parameterized.Parameter(4)
    public BannerVcardStatusModerate expectedVcardStatusModerate;

    @Parameterized.Parameter(5)
    public Boolean expectedClearModerationData;

    @Parameterized.Parameter(6)
    public Boolean expectedDissociateOldVcard;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "?????? ??????????????????, ?????????????????? ?????????? ?????????????????? " +
                                "-> ???????????? ?????????????????? ???? ????????????????",
                        // ???????????????? ????????????
                        bannerWithVcard(activeTextBanner()
                                .withPhoneFlag(StatusPhoneFlagModerate.YES)),
                        // ?????????????????? ????????????????
                        ModerationMode.DEFAULT,
                        //modelchange
                        emptyModelChanges(),
                        // ?????????????????? ????????????????
                        BannerVcardStatusModerate.YES,
                        false,
                        false,
                },
                {
                        "???????????????? ?????????????? ?? new ???????????? " +
                                "-> ???????????? ?????????????????? ?????????????????????????????? ?? new",
                        // ???????????????? ????????????
                        bannerWithoutVcard(activeTextBanner()
                                .withStatusModerate(OldBannerStatusModerate.NEW)),
                        // ?????????????????? ????????????????
                        ModerationMode.DEFAULT,
                        //modelchange
                        newSignificantVcardModelChanges(),
                        // ?????????????????? ????????????????
                        BannerVcardStatusModerate.NEW,
                        false,
                        false,
                },
                {
                        "???????????????? ?????????????? ?? ready ???????????? " +
                                "-> ???????????? ?????????????????? ?????????????????????????????? ?? ready",
                        // ???????????????? ????????????
                        bannerWithoutVcard(activeTextBanner()),
                        // ?????????????????? ????????????????
                        ModerationMode.DEFAULT,
                        //modelchange
                        newSignificantVcardModelChanges(),
                        // ?????????????????? ????????????????
                        BannerVcardStatusModerate.READY,
                        false,
                        false,
                },
                {
                        "?????????????? ?????????????? " +
                                "-> ???????????? ?????????????????? ???????????????????????? ?? new, ?????????????? ???????????? ??????????????????",
                        // ???????????????? ????????????
                        bannerWithVcard(activeTextBanner()
                                .withPhoneFlag(StatusPhoneFlagModerate.YES)),
                        // ?????????????????? ????????????????
                        ModerationMode.DEFAULT,
                        //modelchange
                        deleteVcardModelChanges(),
                        // ?????????????????? ????????????????
                        BannerVcardStatusModerate.NEW,
                        true,
                        true,
                },
                {
                        "?????????? ???????????????????? ?? ?????????????????? " +
                                "-> ???????????????? ???????????? ??????????????????, ?????????????? ???????????? ??????????????????",
                        // ???????????????? ????????????
                        bannerWithVcard(activeTextBanner()
                                .withPhoneFlag(StatusPhoneFlagModerate.YES)),
                        // ?????????????????? ????????????????
                        ModerationMode.FORCE_SAVE_DRAFT,
                        //modelchange
                        newSignificantVcardModelChanges(),
                        // ?????????????????? ????????????????
                        BannerVcardStatusModerate.NEW,
                        true,
                        true,
                },
                {
                        "?????????????? ???? ???????????????????? ?? ??????????????????, ?????????? ???????????????????????????? ???????????????? ?? ?????????????????? " +
                                "-> ???????????????????? ?????????????? ?? ??????????????????",
                        // ???????????????? ????????????
                        bannerWithVcard(draftTextBanner()
                                .withPhoneFlag(StatusPhoneFlagModerate.NEW)),
                        // ?????????????????? ????????????????
                        ModerationMode.FORCE_MODERATE,
                        //modelchange
                        emptyModelChanges(),
                        // ?????????????????? ????????????????
                        BannerVcardStatusModerate.READY,
                        false,
                        false,
                },
                {
                        "???????????? ?????? ??????????????, ?????????? ???????????????????????????? ???????????????? ?? ?????????????????? " +
                                "-> ???????????? ?????????????????? ???????????? new",
                        // ???????????????? ????????????
                        bannerWithoutVcard(draftTextBanner()),
                        // ?????????????????? ????????????????
                        ModerationMode.FORCE_MODERATE,
                        //modelchange
                        emptyModelChanges(),
                        // ?????????????????? ????????????????
                        BannerVcardStatusModerate.NEW,
                        false,
                        false,
                },
                {
                        "?????????? ?????????????? ???????????????? ?????????????????????? " +
                                "-> ???????????????????? ?????????????? ?? ??????????????????",
                        // ???????????????? ????????????
                        bannerWithVcard(activeTextBanner()
                                .withPhoneFlag(StatusPhoneFlagModerate.YES)),
                        // ?????????????????? ????????????????
                        ModerationMode.DEFAULT,
                        //modelchange
                        significantTitleModelChanges(),
                        // ?????????????????? ????????????????
                        BannerVcardStatusModerate.READY,
                        false,
                        false,
                },
                {
                        "?????????? ?????????????? ???????????????? ?????????????????????????? " +
                                "-> ???? ???????????? ???????????? ?????????????????? ??????????????",
                        // ???????????????? ????????????
                        bannerWithVcard(activeTextBanner()
                                .withPhoneFlag(StatusPhoneFlagModerate.YES)),
                        // ?????????????????? ????????????????
                        ModerationMode.DEFAULT,
                        //modelchange
                        nonSignificantTitleModelChanges(),
                        // ?????????????????? ????????????????
                        BannerVcardStatusModerate.YES,
                        false,
                        false,
                },
                {
                        "?????????????? ???????????????? ?????????????????????? " +
                                "-> ???????????????????? ?????????????? ?? ??????????????????",
                        // ???????????????? ????????????
                        bannerWithVcard(activeTextBanner()
                                .withPhoneFlag(StatusPhoneFlagModerate.YES)),
                        // ?????????????????? ????????????????
                        ModerationMode.DEFAULT,
                        //modelchange
                        newSignificantVcardModelChanges(),
                        // ?????????????????? ????????????????
                        BannerVcardStatusModerate.READY,
                        false,
                        true,
                },
                {
                        "?????????????? ???????????????? ?????????????????????????? " +
                                "-> ???? ???????????? ???????????? ?????????????????? ??????????????",
                        // ???????????????? ????????????
                        bannerWithVcard(activeTextBanner()
                                .withPhoneFlag(StatusPhoneFlagModerate.YES)),
                        // ?????????????????? ????????????????
                        ModerationMode.DEFAULT,
                        //modelchange
                        newNonSignificantVcardModelChanges(),
                        // ?????????????????? ????????????????
                        BannerVcardStatusModerate.YES,
                        false,
                        true,
                },
        });
    }

    @Test
    public void test() {
        TextBannerInfo banner = bannerProvider.createBanner(steps);
        bannerInfo = banner;
        Long oldVcardId = banner.getBanner().getVcardId();
        testModerationRepository.addModReasonVcard(banner.getShard(), banner.getBannerId());

        ModelChanges<BannerWithSystemFields> modelChanges = modelChangesProvider.getModelChanges(steps, banner);
        prepareAndApplyValid(modelChanges);

        BannerWithVcardModeration actualBanner = getBanner(banner.getBannerId(), BannerWithVcardModeration.class);
        assertThat(actualBanner.getVcardStatusModerate()).isEqualTo(expectedVcardStatusModerate);

        Long modReason = testModerationRepository.getModReasonVcardIdByBannerId(banner.getShard(),
                banner.getBannerId());
        if (expectedClearModerationData) {
            assertThat(modReason).isNull();
        } else {
            assertThat(modReason).isNotNull();
        }

        if (oldVcardId != null) {
            Vcard oldVcard = vcardRepository.getVcards(banner.getShard(), banner.getUid(), List.of(oldVcardId)).get(0);
            if (expectedDissociateOldVcard) {
                assertThat(oldVcard.getLastDissociation()).is(matchedBy(approximatelyNow()));
            } else {
                assertThat(oldVcard.getLastDissociation()).isEqualTo(DEFAULT_LAST_DISSOCIATION_DATE);
            }
        }
    }

    @Override
    protected ModerationMode getModerationMode() {
        return moderationMode;
    }

    private static BannerProvider bannerWithoutVcard(OldTextBanner banner) {
        return steps -> {
            ClientInfo client = steps.clientSteps().createDefaultClient();
            return steps.bannerSteps().createBanner(banner, client);
        };
    }

    private static BannerProvider bannerWithVcard(OldTextBanner banner) {
        return steps -> {
            ClientInfo client = steps.clientSteps().createDefaultClient();
            CampaignInfo campaign = steps.campaignSteps().createActiveTextCampaign(client);
            VcardInfo vcard = steps.vcardSteps().createVcard(defaultVcard()
                            .withContactPerson("Person 1")
                            .withEmail("person1@email.com"),
                    campaign);
            return steps.bannerSteps().createBanner(banner.withVcardId(vcard.getVcardId()), campaign);
        };
    }

    private static BannerModelChangesProvider emptyModelChanges() {
        return (steps, banner) -> new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                .castModelUp(BannerWithSystemFields.class);
    }

    private static BannerModelChangesProvider newSignificantVcardModelChanges() {
        return (steps, banner) -> {
            VcardInfo newVcard = steps.vcardSteps().createVcard(defaultVcard()
                            .withContactPerson("Person 2")
                            .withEmail("person2@email.com"),
                    banner.getCampaignInfo());
            return new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                    .process(newVcard.getVcardId(), BannerWithVcard.VCARD_ID)
                    .castModelUp(BannerWithSystemFields.class);
        };
    }

    private static BannerModelChangesProvider newNonSignificantVcardModelChanges() {
        return (steps, banner) -> {
            VcardInfo newVcard = steps.vcardSteps().createVcard(defaultVcard()
                            .withContactPerson("Person 1")
                            .withEmail("person1_another@email.com"),
                    banner.getCampaignInfo());
            return new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                    .process(newVcard.getVcardId(), BannerWithVcard.VCARD_ID)
                    .castModelUp(BannerWithSystemFields.class);
        };
    }

    private static BannerModelChangesProvider deleteVcardModelChanges() {
        return (steps, banner) -> new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                .process(null, BannerWithVcard.VCARD_ID)
                .castModelUp(BannerWithSystemFields.class);
    }

    private static BannerModelChangesProvider significantTitleModelChanges() {
        return (steps, banner) -> new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                .process(banner.getBanner().getTitle() + " new", TextBanner.TITLE)
                .castModelUp(BannerWithSystemFields.class);
    }

    private static BannerModelChangesProvider nonSignificantTitleModelChanges() {
        return (steps, banner) -> new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                .process(banner.getBanner().getTitle() + " ", TextBanner.TITLE)
                .castModelUp(BannerWithSystemFields.class);
    }

    private static Vcard defaultVcard() {
        return fullVcard()
                .withLastChange(DEFAULT_LAST_DISSOCIATION_DATE)
                .withLastDissociation(DEFAULT_LAST_DISSOCIATION_DATE);
    }


    interface BannerProvider {
        TextBannerInfo createBanner(Steps steps);
    }

    interface BannerModelChangesProvider {
        ModelChanges<BannerWithSystemFields> getModelChanges(Steps steps, TextBannerInfo banner);
    }
}
