package ru.yandex.direct.core.entity.adgroup.service.complex.model;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.vcard.model.Vcard;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Модель комплексного графического баннера с креативом для удобства тестирования классов,
 * относящихся к комплексной операции.
 */
@ParametersAreNonnullByDefault
public class ComplexImageCreativeBanner extends ComplexBanner {

    @Override
    public ImageBanner getBanner() {
        return (ImageBanner) super.getBanner();
    }

    @Override
    public void setBanner(BannerWithSystemFields banner) {
        checkArgument(banner instanceof ImageBanner);
        setBanner((ImageBanner) banner);
    }

    public void setBanner(ImageBanner banner) {
        super.setBanner(banner);
    }

    @Override
    public ComplexImageCreativeBanner withBanner(BannerWithSystemFields banner) {
        setBanner(banner);
        return this;
    }

    @Override
    public ComplexImageCreativeBanner withVcard(Vcard vcard) {
        setVcard(vcard);
        return this;
    }

    @Override
    public ComplexImageCreativeBanner withSitelinkSet(SitelinkSet sitelinkSet) {
        setSitelinkSet(sitelinkSet);
        return this;
    }
}
