package ru.yandex.direct.core.entity.campaign.service;

import org.junit.Test;

import ru.yandex.direct.core.entity.pricepackage.model.ShowsFrequencyLimit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;

public class CampaignWithPricePackageUtilsImpressionRateValidationTest {
    @Test
    public void pricePackageForbidsImpressionRate() {
        //если значение поля на пакете ShowsFrequencyLimitRequestInput = null
        // Пользователь не может задавать это значение
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().setShowsFrequencyLimit(null);

        assertThat(CampaignWithPricePackageUtils.pricePackageAllowsImpressionRate(pricePackage), is(false));
    }

    @Test
    public void pricePackageAllowImpressionRate() {
        //если значение поля GdShowsFrequencyLimitRequestInput != null и все поля = null
        // пользовать может задать любые значения, в том числе и не задать вообще
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().setShowsFrequencyLimit(new ShowsFrequencyLimit());

        assertThat(CampaignWithPricePackageUtils.pricePackageAllowsImpressionRate(pricePackage), is(true));
    }

    @Test
    public void pricePackageHasImpressionRate() {
        //если значение поля на пакете ShowsFrequencyLimitRequestInput != null и все поля <> null
        // копируем значения с пакета на кампанию, пользовать задать значения не может
        // но сами настройки частоты показа разрешены
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().setShowsFrequencyLimit(new ShowsFrequencyLimit()
                .withFrequencyLimit(7)
                .withFrequencyLimitDays(3)
                .withFrequencyLimitIsForCampaignTime(false)
        );

        assertThat(CampaignWithPricePackageUtils.pricePackageAllowsImpressionRate(pricePackage), is(true));
        assertThat(CampaignWithPricePackageUtils.pricePackageHasImpressionRate(pricePackage), is(true));
    }
}
