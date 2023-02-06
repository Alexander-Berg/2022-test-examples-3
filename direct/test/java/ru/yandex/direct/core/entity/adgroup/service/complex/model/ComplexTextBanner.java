package ru.yandex.direct.core.entity.adgroup.service.complex.model;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.vcard.model.Vcard;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Модель комплексного текстового баннера для удобства тестирования классов,
 * относящихся к комплексной операции.
 */
@ParametersAreNonnullByDefault
public class ComplexTextBanner extends ComplexBanner {

    @Override
    public TextBanner getBanner() {
        return (TextBanner) super.getBanner();
    }

    @Override
    public void setBanner(BannerWithSystemFields banner) {
        checkArgument(banner instanceof TextBanner);
        setBanner((TextBanner) banner);
    }

    public void setBanner(TextBanner banner) {
        super.setBanner(banner);
    }

    @Override
    public ComplexTextBanner withBanner(BannerWithSystemFields banner) {
        setBanner(banner);
        return this;
    }

    @Override
    public ComplexTextBanner withVcard(Vcard vcard) {
        setVcard(vcard);
        return this;
    }

    @Override
    public ComplexTextBanner withSitelinkSet(SitelinkSet sitelinkSet) {
        setSitelinkSet(sitelinkSet);
        return this;
    }
}
