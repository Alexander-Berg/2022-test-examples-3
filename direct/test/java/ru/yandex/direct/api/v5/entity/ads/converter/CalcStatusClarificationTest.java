package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.function.BiConsumer;

import com.yandex.direct.api.v5.general.StateEnum;
import com.yandex.direct.api.v5.general.StatusEnum;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.entity.ads.StatusClarificationTranslations;
import ru.yandex.direct.api.v5.entity.ads.container.AdsGetContainer;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.i18n.Translatable;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.StateAndStatusCalculator.calcStatusClarification;

@Api5Test
@RunWith(JUnitParamsRunner.class)
public class CalcStatusClarificationTest {

    private static final String diagText = "Потому что!";

    private static final StatusClarificationTranslations translations = StatusClarificationTranslations.INSTANCE;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public TranslationService translationService;

    @Test
    @Parameters(method = "params")
    @TestCaseName("{0}")
    public void test(String desc, StatusEnum status, StateEnum state, boolean isCampaignShow, AdsGetContainer container,
                     BiConsumer<TranslationService, String> checkExpectations) {
        assert translationService != null;

        String statusClarification =
                calcStatusClarification(status, state, isCampaignShow, container, translationService);

        checkExpectations.accept(translationService, statusClarification);
    }

    public Object[][] params() {
        return new Object[][]{
                {"archived", StatusEnum.ACCEPTED, StateEnum.ARCHIVED, false, getContainer(),
                        isExpected(translations.adArchived())},
                {"draft", StatusEnum.DRAFT, StateEnum.OFF, false, getContainer(), isExpected(translations.adDraft())},
                {"stopped", StatusEnum.ACCEPTED, StateEnum.SUSPENDED, false, getContainer(),
                        isExpected(translations.adStopped())},
                {"stopped by monitoring", StatusEnum.ACCEPTED, StateEnum.OFF_BY_MONITORING, false, getContainer(),
                        isExpected(translations.adStoppedBySiteMonitoring())},
                {"running accepted", StatusEnum.ACCEPTED, StateEnum.ON, false, getContainer(),
                        isExpected(translations.adRunning())},
                {"running preaccepted", StatusEnum.PREACCEPTED, StateEnum.ON, false, getContainer(),
                        isExpected(translations.adRunning())},
                {"previous version", StatusEnum.MODERATION, StateEnum.ON, false, getContainer(),
                        isExpected(translations.previousVersionOfAdRunning())},
                {"campaign stopped", StatusEnum.ACCEPTED, StateEnum.OFF, false, getContainer(),
                        isExpected(translations.campaignStopped())},
                {"awaiting for moderation", StatusEnum.MODERATION, StateEnum.OFF, true, getContainer(),
                        isExpected(translations.adAwaitingModeration())},
                {"preaccepted at moderation", StatusEnum.PREACCEPTED, StateEnum.OFF, true, getContainer(),
                        isExpected(translations.adPreacceptedAtModeration())},
                {"accepted", StatusEnum.ACCEPTED, StateEnum.OFF, true, getContainer(),
                        isExpected(translations.adAccepted())},
                {"rejected", StatusEnum.REJECTED, StateEnum.ON, false,
                        new AdsGetContainer.Builder().withAd(new TextBanner()).withCampaign(new Campaign())
                                .withBannerModerationReasons(
                                        singletonList(new ModerationDiag().withDiagText(diagText))).build(),
                        isExpected(diagText, translations.previousVersionOfAdRunning(), translations.adRejected())},
        };
    }

    /**
     * Проверка написана лениво, т.к. метод parameters вызывается перед автовайрингом translationService.
     */
    private static BiConsumer<TranslationService, String> isExpected(Translatable translatable) {
        return (ts, statusClarification) -> assertThat(statusClarification)
                .isEqualTo(ts.translate(translatable));
    }

    /**
     * Проверка написана лениво, т.к. метод parameters вызывается перед автовайрингом translationService.
     */
    private static BiConsumer<TranslationService, String> isExpected(String diag, Translatable... translatables) {
        return (ts, statusClarification) -> assertThat(statusClarification).isEqualTo(
                StreamEx.of(translatables)
                        .map(ts::translate)
                        .append(diag)
                        .collect(joining("\n")));
    }

    private static AdsGetContainer getContainer() {
        return new AdsGetContainer.Builder().withAd(new TextBanner()).withCampaign(new Campaign()).build();
    }
}
