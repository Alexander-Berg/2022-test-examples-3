package ru.yandex.direct.core.entity.banner.bean;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.old.Image;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.banner.old.BannerBeanUtil;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.CloneTestUtil.fill;

@ParametersAreNonnullByDefault
public class BannerBeanUtilTest {

    @Test
    public void clone_Image_WorksFine() {
        Image original = new Image();
        fill(original);

        Image clone = BannerBeanUtil.cloneImage(original);
        assertThat(clone, beanDiffer(original));
    }

    @Test
    public void clone_BannerImage_WorksFine() {
        OldBannerImage original = new OldBannerImage();
        fill(original);

        OldBannerImage clone = BannerBeanUtil.cloneBannerImage(original);
        assertThat(clone, beanDiffer(original));
    }
}
