package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.function.Consumer;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.general.ExtensionModeration;
import com.yandex.direct.api.v5.general.StatusEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithVcard;
import ru.yandex.direct.core.entity.banner.model.TextBanner;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.ModerationStatusBuilder.buildVCardModeration;

@Api5Test
@RunWith(Parameterized.class)
public class BuildVCardModerationTest {
    private static final Long vcardId = 1L;

    @Autowired
    public TranslationService translationService;

    @Parameterized.Parameter
    public String desc;

    @Parameterized.Parameter(1)
    public BannerWithVcard ad;

    @Parameterized.Parameter(2)
    public Consumer<JAXBElement<ExtensionModeration>> checkExpectations;

    @Parameterized.Parameters(name = "{0}}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"no vcard", new TextBanner(), isNil()},
                {"new => draft",
                        new TextBanner().withVcardId(vcardId).withVcardStatusModerate(BannerVcardStatusModerate.NEW),
                        isEqual(StatusEnum.DRAFT)},
                {"sending => moderation",
                        new TextBanner().withVcardId(vcardId).withVcardStatusModerate(BannerVcardStatusModerate.SENDING),
                        isEqual(StatusEnum.MODERATION)},
                {"sent => moderation",
                        new TextBanner().withVcardId(vcardId).withVcardStatusModerate(BannerVcardStatusModerate.SENT),
                        isEqual(StatusEnum.MODERATION)},
                {"ready => moderation",
                        new TextBanner().withVcardId(vcardId).withVcardStatusModerate(BannerVcardStatusModerate.READY),
                        isEqual(StatusEnum.MODERATION)},
                {"yes => accepted",
                        new TextBanner().withVcardId(vcardId).withVcardStatusModerate(BannerVcardStatusModerate.YES),
                        isEqual(StatusEnum.ACCEPTED)},
                {"no => rejected",
                        new TextBanner().withVcardId(vcardId).withVcardStatusModerate(BannerVcardStatusModerate.NO),
                        isEqual(StatusEnum.REJECTED)},
        };
    }

    private static Consumer<JAXBElement<ExtensionModeration>> isNil() {
        return elem -> assertThat(elem.isNil()).isTrue();
    }

    private static Consumer<JAXBElement<ExtensionModeration>> isEqual(StatusEnum expectedStatus) {
        return elem -> assertThat(elem.getValue()).extracting("Status").isEqualTo(expectedStatus);
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Test
    public void test() {
        JAXBElement<ExtensionModeration> vcardModeration =
                buildVCardModeration(ad, emptyList(), translationService);

        checkExpectations.accept(vcardModeration);
    }
}
