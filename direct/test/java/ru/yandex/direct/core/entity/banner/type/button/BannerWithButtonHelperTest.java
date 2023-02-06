package ru.yandex.direct.core.entity.banner.type.button;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.i18n.I18NBundle;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
@Description("Проверяем наличие переводов на английский у всех доступных значений кнопки")
public class BannerWithButtonHelperTest {
    @Autowired
    BannerWithButtonHelper newBannerWithButtonHelper;

    @Autowired
    TranslationService translationService;

    @Autowired
    Steps steps;

    @Autowired
    FeatureService featureService;

    @Test
    public void getAllowedButtonActions() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        var clientFeatures = featureService.getEnabledForClientId(client.getClientId());
        Set<ButtonAction> allowedButtonActions =
                newBannerWithButtonHelper.getAllowedButtonActions(clientFeatures);

        for (ButtonAction buttonAction : allowedButtonActions) {
            String descriptionEn = translationService.translate(buttonAction.getDescription(), I18NBundle.EN);
            String descriptionRu = translationService.translate(buttonAction.getDescription(), I18NBundle.RU);
            assertThat(descriptionEn).isNotEqualTo(descriptionRu);
        }
    }
}
