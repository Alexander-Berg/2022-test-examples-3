package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.vcard;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import one.util.streamex.StreamEx;

import ru.yandex.direct.core.entity.adgroup.service.complex.text.update.banner.ComplexUpdateBannerTestBase;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestVcards.vcardUserFields;

public class ComplexUpdateVcardTestBase extends ComplexUpdateBannerTestBase {

    protected VcardInfo createRandomApartVcard() {
        return steps.vcardSteps().createVcard(randomApartVcard(), campaignInfo);
    }

    protected TextBannerInfo createRandomTitleBanner(AdGroupInfo adGroupInfo, VcardInfo vcardInfo) {
        OldTextBanner randomBodyBanner = activeTextBanner()
                .withTitle(randomAlphabetic(10))
                .withVcardId(vcardInfo.getVcardId());
        return steps.bannerSteps().createBanner(randomBodyBanner, adGroupInfo);
    }

    protected Vcard randomApartVcard() {
        return vcardUserFields(campaignInfo.getCampaignId())
                .withApart(randomAlphabetic(10))
                .withGeoId(0L)
                .withLastChange(LocalDateTime.now())
                .withLastDissociation(LocalDateTime.now());
    }

    protected List<Vcard> findClientVcards() {
        return vcardRepository.getVcards(shard, clientUid);
    }

    protected List<Vcard> findAddedVcards(Collection<Long> oldVcardIds) {
        List<Vcard> allClientVcards = vcardRepository.getVcards(shard, clientUid);
        return StreamEx.of(allClientVcards)
                .remove(vcard -> oldVcardIds.contains(vcard.getId()))
                .toList();
    }
}
