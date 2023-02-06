package ru.yandex.direct.api.v5.entity.ads.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.general.StatusEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.NewStatusImageModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.StateAndStatusCalculator.calcStatus;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class CalcStatusTest {

    @Parameterized.Parameter
    public BannerWithSystemFields ad;

    @Parameterized.Parameter(1)
    public StatusEnum expectedStatus;

    @Parameterized.Parameters(name = "status {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {new TextBanner().withStatusModerate(BannerStatusModerate.NEW),
                        StatusEnum.DRAFT},
                {new TextBanner()
                        .withStatusModerate(BannerStatusModerate.YES)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        StatusEnum.ACCEPTED},
                {new TextBanner().withStatusModerate(BannerStatusModerate.NO),
                        StatusEnum.REJECTED},
                {new TextBanner()
                        .withStatusModerate(BannerStatusModerate.SENT)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        StatusEnum.PREACCEPTED},
                {new TextBanner()
                        .withStatusModerate(BannerStatusModerate.SENDING)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        StatusEnum.PREACCEPTED},
                {new TextBanner()
                        .withStatusModerate(BannerStatusModerate.SENDING)
                        .withStatusPostModerate(BannerStatusPostModerate.NO),
                        StatusEnum.MODERATION},
                {new TextBanner()
                        .withStatusModerate(BannerStatusModerate.SENT)
                        .withStatusPostModerate(BannerStatusPostModerate.NO),
                        StatusEnum.MODERATION},
                {new TextBanner()
                        .withStatusModerate(BannerStatusModerate.READY)
                        .withStatusPostModerate(BannerStatusPostModerate.NO),
                        StatusEnum.MODERATION},
                {new TextBanner()
                        .withStatusModerate(BannerStatusModerate.SENDING)
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED),
                        StatusEnum.MODERATION},
                {new TextBanner()
                        .withStatusModerate(BannerStatusModerate.SENT)
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED),
                        StatusEnum.MODERATION},
                {new TextBanner()
                        .withStatusModerate(BannerStatusModerate.READY)
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED),
                        StatusEnum.MODERATION},

                {new DynamicBanner().withStatusModerate(BannerStatusModerate.NEW),
                        StatusEnum.DRAFT},
                {new DynamicBanner()
                        .withStatusModerate(BannerStatusModerate.YES)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        StatusEnum.ACCEPTED},
                {new DynamicBanner().withStatusModerate(BannerStatusModerate.NO),
                        StatusEnum.REJECTED},
                {new DynamicBanner()
                        .withStatusModerate(BannerStatusModerate.SENT)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        StatusEnum.PREACCEPTED},
                {new DynamicBanner()
                        .withStatusModerate(BannerStatusModerate.SENDING)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        StatusEnum.PREACCEPTED},
                {new DynamicBanner()
                        .withStatusModerate(BannerStatusModerate.SENDING)
                        .withStatusPostModerate(BannerStatusPostModerate.NO),
                        StatusEnum.MODERATION},
                {new DynamicBanner()
                        .withStatusModerate(BannerStatusModerate.SENT)
                        .withStatusPostModerate(BannerStatusPostModerate.NO),
                        StatusEnum.MODERATION},
                {new DynamicBanner()
                        .withStatusModerate(BannerStatusModerate.READY)
                        .withStatusPostModerate(BannerStatusPostModerate.NO),
                        StatusEnum.MODERATION},
                {new DynamicBanner()
                        .withStatusModerate(BannerStatusModerate.SENDING)
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED),
                        StatusEnum.MODERATION},
                {new DynamicBanner()
                        .withStatusModerate(BannerStatusModerate.SENT)
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED),
                        StatusEnum.MODERATION},
                {new DynamicBanner()
                        .withStatusModerate(BannerStatusModerate.READY)
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED),
                        StatusEnum.MODERATION},

                {new MobileAppBanner().withStatusModerate(BannerStatusModerate.NEW),
                        StatusEnum.DRAFT},
                {new MobileAppBanner()
                        .withStatusModerate(BannerStatusModerate.YES)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        StatusEnum.ACCEPTED},
                {new MobileAppBanner().withStatusModerate(BannerStatusModerate.NO),
                        StatusEnum.REJECTED},
                {new MobileAppBanner()
                        .withStatusModerate(BannerStatusModerate.SENT)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        StatusEnum.PREACCEPTED},
                {new MobileAppBanner()
                        .withStatusModerate(BannerStatusModerate.SENDING)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        StatusEnum.PREACCEPTED},
                {new MobileAppBanner()
                        .withStatusModerate(BannerStatusModerate.SENDING)
                        .withStatusPostModerate(BannerStatusPostModerate.NO),
                        StatusEnum.MODERATION},
                {new MobileAppBanner()
                        .withStatusModerate(BannerStatusModerate.SENT)
                        .withStatusPostModerate(BannerStatusPostModerate.NO),
                        StatusEnum.MODERATION},
                {new MobileAppBanner()
                        .withStatusModerate(BannerStatusModerate.READY)
                        .withStatusPostModerate(BannerStatusPostModerate.NO),
                        StatusEnum.MODERATION},
                {new MobileAppBanner()
                        .withStatusModerate(BannerStatusModerate.SENDING)
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED),
                        StatusEnum.MODERATION},
                {new MobileAppBanner()
                        .withStatusModerate(BannerStatusModerate.SENT)
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED),
                        StatusEnum.MODERATION},
                {new MobileAppBanner()
                        .withStatusModerate(BannerStatusModerate.READY)
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED),
                        StatusEnum.MODERATION},

                {new ImageBanner().withImageHash("2")
                        .withStatusModerate(BannerStatusModerate.NEW), StatusEnum.DRAFT},
                {new ImageBanner().withImageHash("2").withImageStatusModerate(NewStatusImageModerate.NEW),
                        StatusEnum.DRAFT},
                {new ImageBanner().withImageHash("2").withStatusModerate(BannerStatusModerate.YES)
                        .withStatusPostModerate(BannerStatusPostModerate.YES)
                        .withImageStatusModerate(NewStatusImageModerate.YES), StatusEnum.ACCEPTED},
                {new ImageBanner().withImageHash("2")
                        .withStatusModerate(BannerStatusModerate.NO), StatusEnum.REJECTED},
                {new ImageBanner().withImageHash("2").withImageStatusModerate(
                        NewStatusImageModerate.NO), StatusEnum.REJECTED},
                {new ImageBanner().withImageHash("2").withStatusModerate(BannerStatusModerate.SENT)
                        .withStatusPostModerate(BannerStatusPostModerate.YES).withImageStatusModerate(
                        NewStatusImageModerate.YES), StatusEnum.PREACCEPTED},
                {new ImageBanner().withImageHash("2").withStatusModerate(BannerStatusModerate.SENDING)
                        .withStatusPostModerate(BannerStatusPostModerate.YES).withImageStatusModerate(
                        NewStatusImageModerate.YES), StatusEnum.PREACCEPTED},
                {new ImageBanner().withImageHash("2").withImageStatusModerate(
                        NewStatusImageModerate.SENDING), StatusEnum.MODERATION},
                {new ImageBanner().withImageHash("2").withImageStatusModerate(
                        NewStatusImageModerate.SENT), StatusEnum.MODERATION},
                {new ImageBanner().withImageHash("2").withImageStatusModerate(
                        NewStatusImageModerate.READY), StatusEnum.MODERATION},
                {new ImageBanner().withImageHash("2").withStatusModerate(BannerStatusModerate.SENDING)
                        .withStatusPostModerate(BannerStatusPostModerate.NO).withImageStatusModerate(
                        NewStatusImageModerate.YES), StatusEnum.MODERATION},
                {new ImageBanner().withImageHash("2").withStatusModerate(BannerStatusModerate.SENT)
                        .withStatusPostModerate(BannerStatusPostModerate.NO).withImageStatusModerate(
                        NewStatusImageModerate.YES), StatusEnum.MODERATION},
                {new ImageBanner().withImageHash("2").withStatusModerate(BannerStatusModerate.READY)
                        .withStatusPostModerate(BannerStatusPostModerate.NO).withImageStatusModerate(
                        NewStatusImageModerate.YES), StatusEnum.MODERATION},
                {new ImageBanner().withImageHash("2").withStatusModerate(BannerStatusModerate.SENDING)
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED).withImageStatusModerate(
                        NewStatusImageModerate.YES), StatusEnum.MODERATION},
                {new ImageBanner().withImageHash("2").withStatusModerate(BannerStatusModerate.SENT)
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED).withImageStatusModerate(
                        NewStatusImageModerate.YES), StatusEnum.MODERATION},
                {new ImageBanner().withImageHash("2").withStatusModerate(BannerStatusModerate.READY)
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED).withImageStatusModerate(
                        NewStatusImageModerate.YES), StatusEnum.MODERATION},

                {new ImageBanner().withCreativeId(1L).withStatusModerate(BannerStatusModerate.NEW),
                        StatusEnum.DRAFT},
                {new ImageBanner().withCreativeId(1L).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.NEW), StatusEnum.DRAFT},
                {new ImageBanner().withCreativeId(1L)
                        .withStatusModerate(BannerStatusModerate.YES).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.YES), StatusEnum.ACCEPTED},
                {new ImageBanner().withCreativeId(1L).withStatusModerate(BannerStatusModerate.NO),
                        StatusEnum.REJECTED},
                {new ImageBanner().withCreativeId(1L).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.NO), StatusEnum.REJECTED},
                {new ImageBanner().withCreativeId(1L)
                        .withStatusModerate(BannerStatusModerate.YES).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.SENDING), StatusEnum.MODERATION},
                {new ImageBanner().withCreativeId(1L)
                        .withStatusModerate(BannerStatusModerate.YES).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.SENT), StatusEnum.MODERATION},
                {new ImageBanner().withCreativeId(1L)
                        .withStatusModerate(BannerStatusModerate.YES).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.READY), StatusEnum.MODERATION},
                {new ImageBanner().withCreativeId(1L)
                        .withStatusModerate(BannerStatusModerate.SENDING).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.YES), StatusEnum.MODERATION},
                {new ImageBanner().withCreativeId(1L)
                        .withStatusModerate(BannerStatusModerate.SENT).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.YES), StatusEnum.MODERATION},
                {new ImageBanner().withCreativeId(1L)
                        .withStatusModerate(BannerStatusModerate.READY).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.YES), StatusEnum.MODERATION},

                {new CpcVideoBanner().withStatusModerate(BannerStatusModerate.NEW),
                        StatusEnum.DRAFT},
                {new CpcVideoBanner().withCreativeStatusModerate(
                        BannerCreativeStatusModerate.NEW), StatusEnum.DRAFT},
                {new CpcVideoBanner()
                        .withStatusModerate(BannerStatusModerate.YES).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.YES), StatusEnum.ACCEPTED},
                {new CpcVideoBanner().withStatusModerate(BannerStatusModerate.NO),
                        StatusEnum.REJECTED},
                {new CpcVideoBanner().withCreativeStatusModerate(
                        BannerCreativeStatusModerate.NO), StatusEnum.REJECTED},
                {new CpcVideoBanner()
                        .withStatusModerate(BannerStatusModerate.YES).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.SENDING), StatusEnum.MODERATION},
                {new CpcVideoBanner()
                        .withStatusModerate(BannerStatusModerate.YES).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.SENT), StatusEnum.MODERATION},
                {new CpcVideoBanner()
                        .withStatusModerate(BannerStatusModerate.YES).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.READY), StatusEnum.MODERATION},
                {new CpcVideoBanner()
                        .withStatusModerate(BannerStatusModerate.SENDING).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.YES), StatusEnum.MODERATION},
                {new CpcVideoBanner()
                        .withStatusModerate(BannerStatusModerate.SENT).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.YES), StatusEnum.MODERATION},
                {new CpcVideoBanner()
                        .withStatusModerate(BannerStatusModerate.READY).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.YES), StatusEnum.MODERATION},

                {new CpmBanner().withStatusModerate(BannerStatusModerate.NEW),
                        StatusEnum.DRAFT},
                {new CpmBanner().withCreativeStatusModerate(
                        BannerCreativeStatusModerate.NEW), StatusEnum.DRAFT},
                {new CpmBanner()
                        .withStatusModerate(BannerStatusModerate.YES).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.YES), StatusEnum.ACCEPTED},
                {new CpmBanner().withStatusModerate(BannerStatusModerate.NO),
                        StatusEnum.REJECTED},
                {new CpmBanner().withCreativeStatusModerate(
                        BannerCreativeStatusModerate.NO), StatusEnum.REJECTED},
                {new CpmBanner()
                        .withStatusModerate(BannerStatusModerate.YES).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.SENDING), StatusEnum.MODERATION},
                {new CpmBanner()
                        .withStatusModerate(BannerStatusModerate.YES).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.SENT), StatusEnum.MODERATION},
                {new CpmBanner()
                        .withStatusModerate(BannerStatusModerate.YES).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.READY), StatusEnum.MODERATION},
                {new CpmBanner()
                        .withStatusModerate(BannerStatusModerate.SENDING).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.YES), StatusEnum.MODERATION},
                {new CpmBanner()
                        .withStatusModerate(BannerStatusModerate.SENT).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.YES), StatusEnum.MODERATION},
                {new CpmBanner()
                        .withStatusModerate(BannerStatusModerate.READY).withCreativeStatusModerate(
                        BannerCreativeStatusModerate.YES), StatusEnum.MODERATION},

                // TODO: test cases for ad without status moderate and status post moderate
        };
    }

    @Test
    public void test() {
        StatusEnum actualStatus = calcStatus(ad);
        assertThat(actualStatus).isEqualTo(expectedStatus);
    }

}
