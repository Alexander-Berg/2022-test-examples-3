package ru.yandex.direct.intapi.entity.keywords.service;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.keywords.model.CampaignIdAndBannerIdPair;
import ru.yandex.direct.validation.result.PathHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.intapi.validation.ValidationUtils.getErrorText;
import static ru.yandex.direct.validation.result.PathHelper.path;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TextBannerKeywordsValidationTest {
    private static final Long CORRECT_ID = 1L;
    private static final Long INCORRECT_ID = 0L;

    @Autowired
    private BannerKeywordsService bannerKeywordsService;

    @Test
    public void nullParamsException() {
        String validationError =
                getErrorText(bannerKeywordsService.validate(Collections.singletonList(null)),
                        path(PathHelper.field("params")));
        assertThat("получили корректную ошибка валидации", validationError, equalTo("params[0] cannot be null"));
    }

    @Test
    public void emptyParamsException() {
        String validationError =
                getErrorText(bannerKeywordsService.validate(Collections.emptyList()), path(PathHelper.field("params")));
        assertThat("получили корректную ошибка валидации", validationError, equalTo("params cannot be empty"));
    }

    @Test
    public void invalidBidException() {
        String validationError =
                getErrorText(bannerKeywordsService.validate(
                        Collections.singletonList(new CampaignIdAndBannerIdPair()
                                .withCampaignId(CORRECT_ID)
                                .withBannerId(INCORRECT_ID))), path(PathHelper.field("params")));
        assertThat("получили корректную ошибка валидации", validationError,
                equalTo("params[0].bid must be greater than 0"));
    }

    @Test
    public void invalidCidException() {
        String validationError =
                getErrorText(bannerKeywordsService.validate(
                        Collections.singletonList(new CampaignIdAndBannerIdPair()
                                .withCampaignId(INCORRECT_ID)
                                .withBannerId(CORRECT_ID))), path(PathHelper.field("params")));
        assertThat("получили корректную ошибка валидации", validationError,
                equalTo("params[0].cid must be greater than 0"));
    }

    @Test
    public void noCidException() {
        String validationError =
                getErrorText(bannerKeywordsService.validate(
                        Collections.singletonList(new CampaignIdAndBannerIdPair()
                                .withBannerId(CORRECT_ID))), path(PathHelper.field("params")));
        assertThat("получили корректную ошибка валидации", validationError, equalTo("params[0].cid cannot be null"));
    }

    @Test
    public void noBidException() {
        String validationError =
                getErrorText(bannerKeywordsService.validate(
                        Collections.singletonList(new CampaignIdAndBannerIdPair()
                                .withCampaignId(CORRECT_ID))), path(PathHelper.field("params")));
        assertThat("получили корректную ошибка валидации", validationError, equalTo("params[0].bid cannot be null"));
    }
}
